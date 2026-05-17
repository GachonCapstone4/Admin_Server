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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
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

    @Test
    void getDlqMessagesRequeuesMessagesAndReturnsPreview() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        RabbitMQManagementService service = service(restTemplate);

        server.expect(once(), requestTo("http://rabbitmq:15672/api/queues/%2F/q.custom.dlq/get"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "count": 2,
                          "ackmode": "ack_requeue_true",
                          "encoding": "auto",
                          "truncate": 1000
                        }
                        """))
                .andRespond(withSuccess("""
                        [
                          {
                            "exchange": "x.app2ai.direct",
                            "routing_key": "2ai.draft",
                            "redelivered": true,
                            "message_count": 111,
                            "payload_bytes": 17,
                            "payload_encoding": "string",
                            "payload": "{\\"jobId\\":\\"job-1\\"}",
                            "properties": {
                              "message_id": "message-1",
                              "content_type": "application/json",
                              "timestamp": 1779000000,
                              "headers": {
                                "x-death-reason": "rejected"
                              }
                            }
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        var response = service.getDlqMessages(2);

        assertThat(response.getRequestedCount()).isEqualTo(2);
        assertThat(response.getReturnedCount()).isEqualTo(1);
        assertThat(response.getMessages()).hasSize(1);
        assertThat(response.getMessages().getFirst().getMessageId()).isEqualTo("message-1");
        assertThat(response.getMessages().getFirst().getHeaders()).containsEntry("x-death-reason", "rejected");
        server.verify();
    }

    @Test
    void getDlqMessagesReturnsEmptyListWhenQueueDoesNotExist() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        RabbitMQManagementService service = service(restTemplate);

        server.expect(once(), requestTo("http://rabbitmq:15672/api/queues/%2F/q.custom.dlq/get"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withResourceNotFound());

        var response = service.getDlqMessages(20);

        assertThat(response.getReturnedCount()).isZero();
        assertThat(response.getMessages()).isEmpty();
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
