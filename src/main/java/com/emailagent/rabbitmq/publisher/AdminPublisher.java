package com.emailagent.rabbitmq.publisher;

import com.emailagent.rabbitmq.config.RabbitMQConfig;
import com.emailagent.rabbitmq.dto.AdminDeploymentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Admin 전용 RabbitMQ Publisher.
 * 기존 MailPublisher / RagTemplateIndexPublisher와 무관한 독립 컴포넌트.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 모델 배포 요청을 x.app2ai.direct / 2ai_deployment 로 발행.
     *
     * @param userId 요청한 관리자 userId
     */
    public void publishDeployment(Long userId) {
        AdminDeploymentMessage message = new AdminDeploymentMessage(userId);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_APP2AI,
                RabbitMQConfig.RK_DEPLOYMENT,
                message
        );

        log.info("[AdminPublisher] 모델 배포 요청 발행 완료 — userId={}", userId);
    }
}
