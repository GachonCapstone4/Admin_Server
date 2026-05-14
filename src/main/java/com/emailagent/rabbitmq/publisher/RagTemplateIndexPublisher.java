package com.emailagent.rabbitmq.publisher;

import com.emailagent.rabbitmq.config.RabbitMQConfig;
import com.emailagent.rabbitmq.dto.RagTemplateIndexRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagTemplateIndexPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(RagTemplateIndexRequestDTO payload) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_APP2RAG,
                RabbitMQConfig.RK_TEMPLATE_INDEX_INBOUND,
                payload,
                new CorrelationData(payload.getRequestId())
        );

        log.info(
                "[RagTemplateIndexPublisher] template index 발행 완료 — userId={}, requestId={}",
                payload.getUserId(),
                payload.getRequestId()
        );
    }
}
