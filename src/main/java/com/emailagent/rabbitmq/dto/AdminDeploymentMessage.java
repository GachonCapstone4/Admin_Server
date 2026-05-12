package com.emailagent.rabbitmq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Admin → AI 배포 요청 메시지.
 * x.app2ai.direct / routing-key: 2ai_deployment
 */
@Getter
public class AdminDeploymentMessage {

    @JsonProperty("user_id")
    private final Long userId;

    @JsonProperty("task_type")
    private final String taskType = "deployment";

    public AdminDeploymentMessage(Long userId) {
        this.userId = userId;
    }
}
