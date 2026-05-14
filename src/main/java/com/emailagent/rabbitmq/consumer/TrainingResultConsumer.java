package com.emailagent.rabbitmq.consumer;

import com.emailagent.domain.entity.TrainingJob;
import com.emailagent.domain.enums.TrainingJobStatus;
import com.emailagent.rabbitmq.config.RabbitMQConfig;
import com.emailagent.rabbitmq.dto.TrainingJobResultMessage;
import com.emailagent.repository.MlopsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AI Training Worker → Admin 서버 학습 결과 수신 컨슈머.
 *
 * [처리 흐름]
 * 신규 job_id: TrainingJob 전체 필드 INSERT
 * 기존 job_id: non-null 변경 필드만 UPDATE (applyUpdate)
 * 소비 실패:   basicNack(requeue=false) → DLX 이동
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingResultConsumer {

    private final MlopsRepository mlopsRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @RabbitListener(
            queues = RabbitMQConfig.QUEUE_TRAINING_RESULT,
            ackMode = "MANUAL",
            containerFactory = "rabbitListenerContainerFactory"
    )
    @Transactional
    public void consumeTrainingResult(TrainingJobResultMessage msg, Message message, Channel channel)
            throws IOException {

        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            TrainingJobStatus newStatus = TrainingJobStatus.valueOf(msg.getStatus());

            mlopsRepository.findById(msg.getJobId()).ifPresentOrElse(
                    existing -> existing.applyUpdate(
                            newStatus,
                            msg.getModelVersion(),
                            serializeMetrics(msg),
                            msg.getErrorMessage(),
                            parseDateTime(msg.getStartedAt()),
                            parseDateTime(msg.getFinishedAt())
                    ),
                    () -> mlopsRepository.save(TrainingJob.builder()
                            .jobId(msg.getJobId())
                            .jobType(msg.getJobType())
                            .taskType(msg.getTaskType())
                            .datasetVersion(msg.getDatasetVersion())
                            .requestedBy(msg.getRequestedBy())
                            .status(newStatus)
                            .modelVersion(msg.getModelVersion())
                            .metricsJson(serializeMetrics(msg))
                            .errorMessage(msg.getErrorMessage())
                            .startedAt(parseDateTime(msg.getStartedAt()))
                            .finishedAt(parseDateTime(msg.getFinishedAt()))
                            .build())
            );

            channel.basicAck(deliveryTag, false);
            log.info("[TrainingResultConsumer] 처리 완료 — jobId={}, status={}", msg.getJobId(), newStatus);

        } catch (Exception e) {
            log.error("[TrainingResultConsumer] 처리 실패 → nack — jobId={}, error={}",
                    msg.getJobId(), e.getMessage(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /** Map<String,Object> metrics → JSON 문자열 변환. metrics가 null이면 null 반환. */
    private String serializeMetrics(TrainingJobResultMessage msg) {
        if (msg.getMetrics() == null) return null;
        try {
            return objectMapper.writeValueAsString(msg.getMetrics());
        } catch (JsonProcessingException e) {
            log.warn("[TrainingResultConsumer] metrics 직렬화 실패 — jobId={}", msg.getJobId(), e);
            return null;
        }
    }

    /** ISO-8601 문자열 → LocalDateTime. null 또는 파싱 불가 시 null 반환. */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, DT_FORMATTER);
        } catch (Exception e) {
            log.warn("[TrainingResultConsumer] datetime 파싱 실패 — value={}", value);
            return null;
        }
    }
}
