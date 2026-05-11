package com.emailagent.dto.response.admin.operation;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class AdminDlqPurgeResponse extends BaseResponse {

    private final String message;

    @JsonProperty("purged_at")
    private final String purgedAt;

    public AdminDlqPurgeResponse(String message, String purgedAt) {
        this.message = message;
        this.purgedAt = purgedAt;
    }
}