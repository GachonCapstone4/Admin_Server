package com.emailagent.service.admin;

import com.emailagent.config.GithubYamlProperties;
import com.emailagent.dto.request.admin.SagemakerJobSpec;
import com.emailagent.dto.response.admin.SagemakerJobResponse;
import com.emailagent.exception.GithubYamlFetchException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.sagemaker.SageMakerClient;
import software.amazon.awssdk.services.sagemaker.model.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SagemakerJobExecutorService {

    private final SageMakerClient sageMakerClient;
    private final GithubYamlProperties githubProps;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${aws.sagemaker.spec-path:sagemaker.json}")
    private String sagemakerSpecPath;

    /**
     * GitHub에서 sagemaker.json을 내려받아 AWS SageMaker 학습 Job을 생성한다.
     */
    public SagemakerJobResponse executeTrainingJob() {
        String jsonContent = fetchFromGithub(sagemakerSpecPath);
        SagemakerJobSpec spec = parseSpec(jsonContent);
        CreateTrainingJobResponse response = createTrainingJob(spec);

        log.info("[SagemakerExecutor] Training Job 생성 완료 — arn={}", response.trainingJobArn());
        return new SagemakerJobResponse(response.trainingJobArn(), Instant.now().toString());
    }

    /**
     * GitHub Contents API를 통해 파일을 raw 문자열로 반환.
     * KubernetesJobExecutorService와 동일한 패턴.
     */
    private String fetchFromGithub(String relativePath) {
        String rawUrl = String.format(
                "https://api.github.com/repos/%s/%s/contents/%s/%s?ref=%s",
                githubProps.getOwner(),
                githubProps.getRepo(),
                githubProps.getYamlBasePath(),
                relativePath,
                githubProps.getBranch()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github.v3.raw");

        if (githubProps.getPat() != null && !githubProps.getPat().isBlank()) {
            headers.set("Authorization", "token " + githubProps.getPat());
        }

        log.info("[SagemakerExecutor] GitHub fetch 시작 — url={}", rawUrl);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    rawUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new GithubYamlFetchException(
                        "sagemaker.json fetch 실패 — HTTP " + response.getStatusCode());
            }
            log.info("[SagemakerExecutor] GitHub fetch 완료 — path={}", relativePath);
            return response.getBody();
        } catch (RestClientException e) {
            throw new GithubYamlFetchException(
                    "GitHub 통신 실패 — path=" + relativePath + ": " + e.getMessage(), e);
        }
    }

    private SagemakerJobSpec parseSpec(String jsonContent) {
        try {
            return objectMapper.readValue(jsonContent, SagemakerJobSpec.class);
        } catch (Exception e) {
            throw new RuntimeException("sagemaker.json 파싱 실패: " + e.getMessage(), e);
        }
    }

    /**
     * SagemakerJobSpec을 AWS SDK 요청으로 변환하여 Training Job을 생성한다.
     *
     * 주요 매핑:
     * - jobId            → TrainingJobName
     * - trainingImageUri → AlgorithmSpecification.TrainingImage
     * - datasetS3Uri     → InputDataConfig "train" 채널
     * - s3Bucket + s3ModelPrefix + jobId → OutputDataConfig.S3OutputPath
     * - useSpotInstance  → EnableManagedSpotTraining (Spot 사용 시 MaxWaitTimeInSeconds 필수)
     */
    private static final DateTimeFormatter JOB_TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("UTC"));

    private CreateTrainingJobResponse createTrainingJob(SagemakerJobSpec spec) {
        // 동일 jobId로 재실행 시 AWS가 중복 이름을 거부하므로, 현재 시각을 suffix로 추가해 유니크 이름 생성
        String uniqueJobName = spec.getJobId() + "-" + JOB_TIMESTAMP_FMT.format(Instant.now());

        // s3OutputPath: s3://{bucket}/{prefix}/{uniqueJobName}/
        String s3OutputPath = String.format("s3://%s/%s/%s/",
                spec.getS3Bucket(), spec.getS3ModelPrefix(), uniqueJobName);

        AlgorithmSpecification algorithmSpec = AlgorithmSpecification.builder()
                .trainingImage(spec.getTrainingImageUri())
                .trainingInputMode(TrainingInputMode.FILE)
                .build();

        Channel inputChannel = Channel.builder()
                .channelName("train")
                .dataSource(DataSource.builder()
                        .s3DataSource(S3DataSource.builder()
                                .s3DataType(S3DataType.S3_PREFIX)
                                .s3Uri(spec.getDatasetS3Uri())
                                .s3DataDistributionType(S3DataDistribution.FULLY_REPLICATED)
                                .build())
                        .build())
                .build();

        OutputDataConfig outputDataConfig = OutputDataConfig.builder()
                .s3OutputPath(s3OutputPath)
                .build();

        ResourceConfig resourceConfig = ResourceConfig.builder()
                .instanceType(spec.getInstanceType())
                .instanceCount(spec.getInstanceCount())
                .volumeSizeInGB(spec.getVolumeSizeGb())
                .build();

        // Spot 인스턴스 사용 시 MaxWaitTimeInSeconds가 MaxRuntimeInSeconds보다 커야 한다
        StoppingCondition stoppingCondition = StoppingCondition.builder()
                .maxRuntimeInSeconds(spec.getMaxRuntimeSeconds())
                .maxWaitTimeInSeconds(spec.isUseSpotInstance() ? spec.getMaxWaitSeconds() : null)
                .build();

        CreateTrainingJobRequest.Builder requestBuilder = CreateTrainingJobRequest.builder()
                .trainingJobName(uniqueJobName)
                .roleArn(spec.getRoleArn())
                .algorithmSpecification(algorithmSpec)
                .inputDataConfig(List.of(inputChannel))
                .outputDataConfig(outputDataConfig)
                .resourceConfig(resourceConfig)
                .stoppingCondition(stoppingCondition)
                .enableManagedSpotTraining(spec.isUseSpotInstance());

        // Python 스크립트는 os.getenv("JOB_ID") 등 UPPERCASE 환경변수를 argparse default로 읽는다.
        // 소문자(job_id)로 주입하면 os.getenv가 None을 반환하므로 반드시 UPPERCASE로 설정해야 한다.
        Map<String, String> mergedEnv = new LinkedHashMap<>();
        if (spec.getEnvironment() != null) {
            mergedEnv.putAll(spec.getEnvironment());
        }
        mergedEnv.put("JOB_ID", uniqueJobName);
        mergedEnv.put("MODEL_VERSION", spec.getModelVersion());
        mergedEnv.put("S3_BUCKET", spec.getS3Bucket());
        mergedEnv.put("S3_MODEL_PREFIX", spec.getS3ModelPrefix());
        mergedEnv.put("DATASET_S3_URI", spec.getDatasetS3Uri());
        requestBuilder.environment(mergedEnv);

        // hyperParameters: epochs, batch_size 등 ML 학습 파라미터만 전달
        if (spec.getHyperParameters() != null && !spec.getHyperParameters().isEmpty()) {
            requestBuilder.hyperParameters(spec.getHyperParameters());
        }

        log.info("[SagemakerExecutor] CreateTrainingJob 요청 — jobName={}, spot={}",
                uniqueJobName, spec.isUseSpotInstance());
        return sageMakerClient.createTrainingJob(requestBuilder.build());
    }
}
