package com.emailagent.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI Training Worker → Backend 상태 이벤트 메시지 DTO.
 * queue: q.2app.training (AI가 x.ai2app.direct를 통해 발행)
 * status: "QUEUED" | "RUNNING" | "COMPLETED" | "FAILED"
 */
@Getter
@NoArgsConstructor
public class TrainingJobResultMessage {

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("job_type")
    private String jobType;

    @JsonProperty("task_type")
    private String taskType;

    @JsonProperty("dataset_version")
    private String datasetVersion;

    @JsonProperty("requested_by")
    private String requestedBy;

    private String status;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("started_at")
    private String startedAt;

    @JsonProperty("finished_at")
    private String finishedAt;

    private Map<String, Object> metrics;

    @JsonProperty("error_message")
    private String errorMessage;
}
