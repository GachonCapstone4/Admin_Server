package com.emailagent.dto.response.admin.operation;

import com.emailagent.dto.response.auth.BaseResponse;
import lombok.Getter;

@Getter
public class AdminDlqCountResponse extends BaseResponse {

    private final int count;

    public AdminDlqCountResponse(int count) {
        this.count = count;
    }
}