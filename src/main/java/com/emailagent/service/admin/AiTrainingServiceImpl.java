package com.emailagent.service.admin;

import com.emailagent.domain.entity.TrainedModel;
import com.emailagent.domain.entity.TrainingJob;
import com.emailagent.domain.enums.TrainingJobStatus;
import com.emailagent.dto.request.admin.training.TrainingJobCreateRequest;
import com.emailagent.dto.response.admin.training.TrainingJobCreateResponse;
import com.emailagent.dto.response.admin.training.TrainingJobDetailResponse;
import com.emailagent.exception.ResourceNotFoundException;
import com.emailagent.rabbitmq.dto.TrainingJobResultMessage;
import com.emailagent.repository.TrainedModelRepository;
import com.emailagent.repository.TrainingJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTrainingServiceImpl implements AiTrainingService {

    private final TrainingJobRepository trainingJobRepository;
    private final TrainedModelRepository trainedModelRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry registry;

    private Counter trainingCompletedCounter;
    private Counter trainingFailedCounter;
    private Counter trainingRunningCounter;

    @PostConstruct
    public void initMetrics() {
        trainingCompletedCounter = Counter.builder("training_job_events_total")
                .tag("status", "COMPLETED")
                .description("Total training job completed events")
                .register(registry);

        trainingFailedCounter = Counter.builder("training_job_events_total")
                .tag("status", "FAILED")
                .description("Total training job failed events")
                .register(registry);

        trainingRunningCounter = Counter.builder("training_job_events_total")
                .tag("status", "RUNNING")
                .description("Total training job running events")
                .register(registry);
    }

    @Override
    @Transactional
    public TrainingJobCreateResponse createTrainingJob(Long adminUserId, TrainingJobCreateRequest request) {
        return createJobInternal(adminUserId, request.getDatasetVersion(), "training", "training");
    }

    @Override
    @Transactional
    public TrainingJobCreateResponse createPreprocessingJob(Long adminUserId, TrainingJobCreateRequest request) {
        return createJobInternal(adminUserId, request.getDatasetVersion(), "preprocessing", "preprocessing");
    }

    @Override
    @Transactional
    public TrainingJobCreateResponse createPairJob(Long adminUserId, TrainingJobCreateRequest request) {
        return createJobInternal(adminUserId, request.getDatasetVersion(), "pair", "pair");
    }

    @Override
    @Transactional
    public TrainingJobCreateResponse createEvaluationJob(Long adminUserId, TrainingJobCreateRequest request) {
        return createJobInternal(adminUserId, request.getDatasetVersion(), "evaluation", "evaluation");
    }

    /**
     * Job 생성 공통 로직.
     * training_jobs 테이블에 QUEUED 상태로 INSERT하고 반환.
     * 실제 Job 실행은 GitOps(ArgoCD)가 Git 레포 변경을 감지하여 처리한다.
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
     * AI worker 완료 이벤트 처리 (기존 그대로 유지).
     * q.2app.training 에서 수신한 결과로 training_jobs 상태 업데이트.
     * status "completed" → COMPLETED + trained_models 자동 등록
     * status 그 외   → FAILED
     */
    @Override
    @Transactional
    public void handleTrainingResult(TrainingJobResultMessage result) {
        TrainingJob job = trainingJobRepository.findById(result.getJobId())
                .orElseThrow(() -> new IllegalArgumentException("Job을 찾을 수 없습니다: " + result.getJobId()));

        LocalDateTime finishedAt = parseFinishedAt(result.getFinishedAt());

        String status = result.getStatus().toUpperCase();

        if ("RUNNING".equals(status)) {
            job.running();
            trainingRunningCounter.increment();
            log.info("[AiTrainingService] Job RUNNING — jobId={}", job.getJobId());
        } else if ("COMPLETED".equals(status)) {
            String metricsJson = serializeMetrics(result);
            job.complete(result.getModelVersion(), metricsJson, finishedAt);
            registerTrainedModelIfAbsent(job.getJobId(), result, metricsJson);
            trainingCompletedCounter.increment();
            log.info("[AiTrainingService] Job COMPLETED — jobId={}, modelVersion={}",
                    job.getJobId(), result.getModelVersion());
        } else {
            job.fail(result.getErrorMessage(), finishedAt);
            trainingFailedCounter.increment();
            log.warn("[AiTrainingService] Job FAILED — jobId={}, error={}",
                    job.getJobId(), result.getErrorMessage());
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
