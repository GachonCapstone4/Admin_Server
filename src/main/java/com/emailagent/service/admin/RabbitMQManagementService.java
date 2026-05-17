package com.emailagent.service.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class RabbitMQManagementService {

    @Value("${app.rabbitmq.management.url}")
    private String managementUrl;

    @Value("${app.rabbitmq.management.username}")
    private String username;

    @Value("${app.rabbitmq.management.password}")
    private String password;

    @Value("${app.rabbitmq.queue.dlx-failed:q.dlx.failed}")
    private String dlqQueueName;

    private final RestTemplate restTemplate;

    public RabbitMQManagementService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // virtual host '/'를 URL-encode한 값
    private static final String VHOST = "%2F";

    /**
     * RabbitMQ Management API를 통해 설정된 DLQ의 메시지를 전부 삭제(purge)한다.
     * DELETE /api/queues/%2F/{queue-name}/contents
     */
    public void purgeDlqQueue() {
        URI url = buildQueueUri("/contents");
        HttpEntity<Void> request = new HttpEntity<>(buildAuthHeaders());
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
            log.info("[RabbitMQManagement] DLQ purge 완료: queue={}", dlqQueueName);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("[RabbitMQManagement] DLQ purge 생략: queue={} 큐가 존재하지 않음", dlqQueueName);
        } catch (Exception e) {
            log.error("[RabbitMQManagement] DLQ purge 실패: {}", e.getMessage(), e);
            throw new RuntimeException("DLQ purge 실패: " + e.getMessage(), e);
        }
    }

    /**
     * RabbitMQ Management API를 통해 설정된 DLQ의 현재 메시지 수를 조회한다.
     * GET /api/queues/%2F/{queue-name} → 응답 JSON의 messages 필드 추출
     */
    @SuppressWarnings("unchecked")
    public int getDlqMessageCount() {
        URI url = buildQueueUri("");
        HttpEntity<Void> request = new HttpEntity<>(buildAuthHeaders());
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("messages")) {
                return 0;
            }
            return ((Number) body.get("messages")).intValue();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("[RabbitMQManagement] DLQ count 0 처리: queue={} 큐가 존재하지 않음", dlqQueueName);
            return 0;
        }
    }

    private HttpHeaders buildAuthHeaders() {
        String credentials = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + credentials);
        return headers;
    }

    private URI buildQueueUri(String suffix) {
        String normalizedManagementUrl = managementUrl.endsWith("/")
                ? managementUrl.substring(0, managementUrl.length() - 1)
                : managementUrl;
        String encodedQueueName = UriUtils.encodePathSegment(dlqQueueName, StandardCharsets.UTF_8);
        return URI.create(normalizedManagementUrl + "/api/queues/" + VHOST + "/" + encodedQueueName + suffix);
    }
}
