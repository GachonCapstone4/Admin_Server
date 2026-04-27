package com.emailagent.service.admin;

import com.emailagent.domain.entity.TrainedModel;
import com.emailagent.domain.entity.TrainingJob;
import com.emailagent.domain.enums.TrainingJobStatus;
import com.emailagent.dto.request.admin.training.TrainingJobCreateRequest;
import com.emailagent.dto.response.admin.training.DeploymentJobResponse;
import com.emailagent.dto.response.admin.training.TrainingJobCreateResponse;
import com.emailagent.dto.response.admin.training.TrainingJobDetailResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.rabbitmq.dto.TrainingJobResultMessage;
import com.emailagent.rabbitmq.event.SseFanoutPublisher;
import com.emailagent.repository.TrainedModelRepository;
import com.emailagent.repository.TrainingJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTrainingServiceImpl implements AiTrainingService {

    private final TrainingJobRepository trainingJobRepository;
    private final TrainedModelRepository trainedModelRepository;
    private final ObjectMapper objectMapper;
    private final SseFanoutPublisher sseFanoutPublisher;
    private final RestTemplate restTemplate;

    @Value("${app.inference.server-url}")
    private String inferenceServerUrl;

    @Value("${app.launcher.script-path}")
    private String launcherScriptPath;

    // 관리자 SSE 브로드캐스트용 userId (SSE Hub에서 관리자 채널로 라우팅)
    private static final Long ADMIN_SSE_USER_ID = 0L;

    @Override
    @Transactional
    public TrainingJobCreateResponse createTrainingJob(Long adminUserId, TrainingJobCreateRequest request) {
        return createJobInternal(adminUserId, request.getDatasetVersion(), "training", "training");
    }

    /**
     * Job 생성 공통 로직.
     * training_jobs 테이블에 QUEUED 상태로 INSERT하고 Launcher(ProcessBuilder)로 실행을 트리거한다.
     */
    private TrainingJobCreateResponse createJobInternal(Long adminUserId, String datasetVersion,
                                                        String jobType, String taskType) {
        String jobId = UUID.randomUUID().toString();
        String requestedBy = String.valueOf(adminUserId);
        String createdAt = Instant.now().toString();

        TrainingJob job = TrainingJob.builder()
                .jobId(jobId)
                .jobType(jobType)
                .taskType(taskType)
                .datasetVersion(datasetVersion)
                .requestedBy(requestedBy)
                .build();
        trainingJobRepository.save(job);

        log.info("[AiTrainingService] Job 등록 완료 — jobType={}, jobId={}, datasetVersion={}",
                jobType, jobId, datasetVersion);

        // Launcher(Python 스크립트)로 실제 Job 실행 트리거
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "python3", launcherScriptPath,
                    "--job-id", jobId,
                    "--job-type", jobType
            );
            pb.start();
            log.info("[AiTrainingService] Launcher 실행 완료 — jobId={}, jobType={}", jobId, jobType);
        } catch (Exception e) {
            log.error("[AiTrainingService] Launcher 실행 실패 — jobId={}, error={}", jobId, e.getMessage(), e);
            throw new RuntimeException("Launcher 실행에 실패했습니다. jobId=" + jobId, e);
        }

        return new TrainingJobCreateResponse(
                jobId,
                TrainingJobStatus.QUEUED.name(),
                datasetVersion,
                createdAt
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingJobDetailResponse getTrainingJob(String jobId) {
        TrainingJob job = trainingJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("학습 Job을 찾을 수 없습니다: " + jobId));
        return new TrainingJobDetailResponse(job);
    }

    /**
     * 배포 Job 생성.
     * training_jobs에 QUEUED로 저장 후 비동기로 Inference 서버에
     * preload → validate → switch 순서로 HTTP 호출하여 모델을 교체한다.
     * 각 단계별 진행 상황은 x.sse.fanout exchange를 통해 SSE Hub로 발행된다.
     */
    @Override
    @Transactional
    public DeploymentJobResponse createDeploymentJob(Long adminUserId) {
        String jobId = UUID.randomUUID().toString();
        String createdAt = Instant.now().toString();

        TrainingJob job = TrainingJob.builder()
                .jobId(jobId)
                .jobType("DEPLOYMENT")
                .taskType("DEPLOYMENT")
                .requestedBy(String.valueOf(adminUserId))
                .build();
        trainingJobRepository.save(job);

        sendSseEvent(adminUserId, jobId, "QUEUED");

        CompletableFuture.runAsync(() -> {
            try {
                job.running();
                trainingJobRepository.save(job);
                sendSseEvent(adminUserId, jobId, "[INFO] preload 시작");
                restTemplate.postForEntity(inferenceServerUrl + "/preload", null, String.class);

                sendSseEvent(adminUserId, jobId, "[INFO] validate 시작");
                restTemplate.postForEntity(inferenceServerUrl + "/validate", null, String.class);

                sendSseEvent(adminUserId, jobId, "[INFO] switch 시작");
                restTemplate.postForEntity(inferenceServerUrl + "/switch", null, String.class);

                job.complete(null, null, LocalDateTime.now());
                trainingJobRepository.save(job);
                sendSseEvent(adminUserId, jobId, "[INFO] 배포 완료");
                sendSseEvent(adminUserId, jobId, "COMPLETED");
            } catch (Exception e) {
                log.error("[AiTrainingService] 배포 실패 — jobId={}, error={}", jobId, e.getMessage());
                job.fail(e.getMessage(), LocalDateTime.now());
                trainingJobRepository.save(job);
                sendSseEvent(adminUserId, jobId, "FAILED");
            }
        });

        return new DeploymentJobResponse(jobId, "QUEUED", "DEPLOYMENT", createdAt);
    }

    /**
     * Job 이벤트 SSE 스트림.
     * 실제 이벤트 수신은 SSE Hub(별도 서비스)가 x.sse.fanout 구독 후 브라우저에 push한다.
     * 이 emitter는 연결 유지용으로만 사용한다.
     */
    @Override
    public SseEmitter streamJobEvents(String jobId) {
        SseEmitter emitter = new SseEmitter(300_000L);
        emitter.onTimeout(emitter::complete);
        return emitter;
    }

    /**
     * AI worker 완료 이벤트 처리 (기존 그대로 유지).
     * q.2app.training 에서 수신한 결과로 training_jobs 상태 업데이트.
     * status "completed" → COMPLETED + trained_models 자동 등록
     * status 그 외       → FAILED
     */
    @Override
    @Transactional
    public void handleTrainingResult(TrainingJobResultMessage result) {
        TrainingJob job = trainingJobRepository.findById(result.getJobId())
                .orElseThrow(() -> new IllegalArgumentException("Job을 찾을 수 없습니다: " + result.getJobId()));

        LocalDateTime finishedAt = parseFinishedAt(result.getFinishedAt());

        if ("completed".equalsIgnoreCase(result.getStatus())) {
            String metricsJson = serializeMetrics(result);
            job.complete(result.getModelVersion(), metricsJson, finishedAt);
            registerTrainedModelIfAbsent(job.getJobId(), result, metricsJson);
            log.info("[AiTrainingService] Job COMPLETED — jobId={}, modelVersion={}",
                    job.getJobId(), result.getModelVersion());
        } else {
            job.fail(result.getErrorMessage(), finishedAt);
            log.warn("[AiTrainingService] Job FAILED — jobId={}, error={}",
                    job.getJobId(), result.getErrorMessage());
        }

        sendSseEvent(ADMIN_SSE_USER_ID, result.getJobId(), result.getStatus().toUpperCase());
    }

    /**
     * x.sse.fanout exchange로 SSE 이벤트 발행.
     * SseFanoutPublisher.publish()를 사용하여 트랜잭션 컨텍스트에 무관하게 즉시 발행한다.
     */
    private void sendSseEvent(Long userId, String jobId, String message) {
        try {
            sseFanoutPublisher.publish(userId, "ai-training-updated", message);
        } catch (Exception e) {
            log.warn("[AiTrainingService] SSE 발행 실패 — jobId={}, message={}", jobId, message);
        }
    }

    /**
     * 학습 완료 후 trained_models 테이블에 모델 등록.
     * model_version 기준으로 중복 체크하여 멱등성 보장.
     */
    private void registerTrainedModelIfAbsent(String jobId, TrainingJobResultMessage result, String metricsJson) {
        String modelVersion = result.getModelVersion();
        if (modelVersion == null || trainedModelRepository.existsByModelVersion(modelVersion)) {
            log.debug("[AiTrainingService] trained_model 등록 건너뜀 — modelVersion={}", modelVersion);
            return;
        }

        TrainedModel model = TrainedModel.builder()
                .modelVersion(modelVersion)
                .jobId(jobId)
                .intentF1(extractDouble(result.getMetrics(), "intent_f1"))
                .domainAccuracy(extractDouble(result.getMetrics(), "domain_accuracy"))
                .metricsJson(metricsJson)
                .build();
        trainedModelRepository.save(model);

        log.info("[AiTrainingService] trained_model 등록 완료 — modelVersion={}", modelVersion);
    }

    private Double extractDouble(Map<String, Object> metrics, String key) {
        if (metrics == null || !metrics.containsKey(key)) return null;
        Object value = metrics.get(key);
        if (value instanceof Number num) return num.doubleValue();
        return null;
    }

    private LocalDateTime parseFinishedAt(String finishedAt) {
        if (finishedAt == null) return LocalDateTime.now();
        try {
            return Instant.parse(finishedAt).atZone(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException e) {
            log.warn("[AiTrainingService] finished_at 파싱 실패, 현재 시각으로 대체: {}", finishedAt);
            return LocalDateTime.now();
        }
    }

    private String serializeMetrics(TrainingJobResultMessage result) {
        if (result.getMetrics() == null) return null;
        try {
            return objectMapper.writeValueAsString(result.getMetrics());
        } catch (JsonProcessingException e) {
            log.warn("[AiTrainingService] metrics 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}
