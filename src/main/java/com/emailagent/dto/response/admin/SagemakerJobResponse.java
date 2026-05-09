package com.emailagent.dto.response.admin;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class SagemakerJobResponse extends BaseResponse {

    @JsonProperty("training_job_arn")
    private final String trainingJobArn;

    @JsonProperty("created_at")
    private final String createdAt;

    public SagemakerJobResponse(String trainingJobArn, String createdAt) {
        this.trainingJobArn = trainingJobArn;
        this.createdAt = createdAt;
    }
}
