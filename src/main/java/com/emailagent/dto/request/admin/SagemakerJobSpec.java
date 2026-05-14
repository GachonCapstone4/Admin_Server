package com.emailagent.dto.request.admin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * GitHub의 sagemaker.json을 역직렬화하는 DTO.
 * JSON 필드는 camelCase이며 @JsonProperty 없이 Jackson 기본 매핑을 사용한다.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SagemakerJobSpec {

    private String jobId;
    private String jobType;
    private String awsRegion;
    private String roleArn;
    private String trainingImageUri;
    private String instanceType;
    private int instanceCount;
    private boolean useSpotInstance;
    private int maxRuntimeSeconds;
    private int maxWaitSeconds;
    private int volumeSizeGb;
    private String s3Bucket;
    private String datasetS3Uri;
    private String modelVersion;
    private String s3ModelPrefix;
    private VpcConfig vpcConfig;
    private Map<String, String> environment;
    private Map<String, String> hyperParameters;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VpcConfig {
        private List<String> subnets;
        private List<String> securityGroupIds;
    }
}
