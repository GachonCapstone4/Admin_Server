package com.emailagent.dto.response.admin;

import com.emailagent.dto.response.auth.BaseResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class KubernetesJobResponse extends BaseResponse {

    @JsonProperty("job_name")
    private final String jobName;

    private final String namespace;

    /** Kubernetes가 발급한 Job 고유 식별자 */
    private final String uid;

    @JsonProperty("yaml_path")
    private final String yamlPath;

    @JsonProperty("created_at")
    private final String createdAt;

    public KubernetesJobResponse(String jobName, String namespace, String uid,
                                  String yamlPath, String createdAt) {
        this.jobName = jobName;
        this.namespace = namespace;
        this.uid = uid;
        this.yamlPath = yamlPath;
        this.createdAt = createdAt;
    }
}
