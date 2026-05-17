package com.emailagent.service.admin;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RabbitMQManagementServiceTest {

    @Test
    void getDlqMessageCountUsesConfiguredQueueName() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        RabbitMQManagementService service = service(restTemplate);

        server.expect(once(), requestTo("http://rabbitmq:15672/api/queues/%2F/q.custom.dlq"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Basic YWRtaW46c2VjcmV0"))
                .andRespond(withSuccess("{\"messages\":7}", MediaType.APPLICATION_JSON));

        assertThat(service.getDlqMessageCount()).isEqualTo(7);
        server.verify();
    }

    @Test
    void getDlqMessageCountReturnsZeroWhenQueueDoesNotExist() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        RabbitMQManagementService service = service(restTemplate);

        server.expect(once(), requestTo("http://rabbitmq:15672/api/queues/%2F/q.custom.dlq"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound());

        assertThat(service.getDlqMessageCount()).isZero();
        server.verify();
    }

    @Test
    void purgeDlqQueueIgnoresMissingQueue() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        RabbitMQManagementService service = service(restTemplate);

        server.expect(once(), requestTo("http://rabbitmq:15672/api/queues/%2F/q.custom.dlq/contents"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withResourceNotFound());

        assertThatCode(service::purgeDlqQueue).doesNotThrowAnyException();
        server.verify();
    }

    private RabbitMQManagementService service(RestTemplate restTemplate) {
        RabbitMQManagementService service = new RabbitMQManagementService(restTemplate);
        ReflectionTestUtils.setField(service, "managementUrl", "http://rabbitmq:15672/");
        ReflectionTestUtils.setField(service, "username", "admin");
        ReflectionTestUtils.setField(service, "password", "secret");
        ReflectionTestUtils.setField(service, "dlqQueueName", "q.custom.dlq");
        return service;
    }
}
