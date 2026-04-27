package com.emailagent.service.admin;

import com.emailagent.dto.request.admin.training.TrainingJobCreateRequest;
import com.emailagent.dto.response.admin.training.DeploymentJobResponse;
import com.emailagent.dto.response.admin.training.TrainingJobCreateResponse;
import com.emailagent.dto.response.admin.training.TrainingJobDetailResponse;
import com.emailagent.rabbitmq.dto.TrainingJobResultMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiTrainingService {

    /** AI 학습 Job 생성 — training_jobs insert + Launcher 실행 */
    TrainingJobCreateResponse createTrainingJob(Long adminUserId, TrainingJobCreateRequest request);

    /** Job 상태 및 결과 조회 */
    TrainingJobDetailResponse getTrainingJob(String jobId);

    /** 배포 Job 생성 — Inference 서버 preload→validate→switch 순차 호출 */
    DeploymentJobResponse createDeploymentJob(Long adminUserId);

    /** Job 이벤트 SSE 스트림 — x.sse.fanout 기반 */
    SseEmitter streamJobEvents(String jobId);

    /** AI worker 완료 이벤트 처리 — training_jobs 상태 업데이트 */
    void handleTrainingResult(TrainingJobResultMessage result);
}
