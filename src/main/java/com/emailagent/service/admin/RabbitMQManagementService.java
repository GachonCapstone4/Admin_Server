package com.emailagent.service.admin;

import com.emailagent.dto.response.admin.operation.AdminDlqMessageListResponse;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static final int DEFAULT_MESSAGE_LIMIT = 20;
    private static final int MAX_MESSAGE_LIMIT = 50;
    private static final int PAYLOAD_PREVIEW_LIMIT = 1000;

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

    /**
     * RabbitMQ Management API의 get endpoint로 DLQ 메시지 샘플을 조회한다.
     * ack_requeue_true를 사용하므로 조회한 메시지는 다시 큐에 들어간다.
     */
    @SuppressWarnings("unchecked")
    public AdminDlqMessageListResponse getDlqMessages(int requestedLimit) {
        int limit = normalizeLimit(requestedLimit);
        URI url = buildQueueUri("/get");
        Map<String, Object> body = Map.of(
                "count", limit,
                "ackmode", "ack_requeue_true",
                "encoding", "auto",
                "truncate", PAYLOAD_PREVIEW_LIMIT
        );
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildJsonAuthHeaders());

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.POST, request, List.class);
            List<Map<String, Object>> rawMessages = response.getBody() == null
                    ? Collections.emptyList()
                    : (List<Map<String, Object>>) response.getBody();
            AtomicInteger index = new AtomicInteger(1);
            List<AdminDlqMessageListResponse.MessageItem> messages = rawMessages.stream()
                    .map(message -> toMessageItem(index.getAndIncrement(), message))
                    .toList();
            return new AdminDlqMessageListResponse(limit, messages);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("[RabbitMQManagement] DLQ messages 빈 목록 처리: queue={} 큐가 존재하지 않음", dlqQueueName);
            return new AdminDlqMessageListResponse(limit, Collections.emptyList());
        }
    }

    private HttpHeaders buildAuthHeaders() {
        String credentials = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + credentials);
        headers.set("Accept", "*/*");
        return headers;
    }

    private HttpHeaders buildJsonAuthHeaders() {
        HttpHeaders headers = buildAuthHeaders();
        headers.set("Content-Type", "application/json");
        return headers;
    }

    private URI buildQueueUri(String suffix) {
        String normalizedManagementUrl = managementUrl.endsWith("/")
                ? managementUrl.substring(0, managementUrl.length() - 1)
                : managementUrl;
        String encodedQueueName = UriUtils.encodePathSegment(dlqQueueName, StandardCharsets.UTF_8);
        return URI.create(normalizedManagementUrl + "/api/queues/" + VHOST + "/" + encodedQueueName + suffix);
    }

    private int normalizeLimit(int requestedLimit) {
        if (requestedLimit <= 0) {
            return DEFAULT_MESSAGE_LIMIT;
        }
        return Math.min(requestedLimit, MAX_MESSAGE_LIMIT);
    }

    @SuppressWarnings("unchecked")
    private AdminDlqMessageListResponse.MessageItem toMessageItem(int index, Map<String, Object> message) {
        Map<String, Object> properties = message.get("properties") instanceof Map<?, ?> rawProperties
                ? (Map<String, Object>) rawProperties
                : Collections.emptyMap();
        return new AdminDlqMessageListResponse.MessageItem(
                index,
                asString(message.get("exchange")),
                asString(message.get("routing_key")),
                asBoolean(message.get("redelivered")),
                asInt(message.get("message_count")),
                asInt(message.get("payload_bytes")),
                asString(message.get("payload_encoding")),
                truncate(asString(message.get("payload")), PAYLOAD_PREVIEW_LIMIT),
                asString(properties.get("message_id")),
                asString(properties.get("content_type")),
                asString(properties.get("timestamp")),
                toStringMap(properties.get("headers"))
        );
    }

    private Map<String, String> toStringMap(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        source.forEach((key, mapValue) -> result.put(String.valueOf(key), asString(mapValue)));
        return result;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private boolean asBoolean(Object value) {
        return value instanceof Boolean bool && bool;
    }
}
