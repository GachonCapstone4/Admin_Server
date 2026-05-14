package com.emailagent.controller.admin;

import com.emailagent.dto.response.admin.SagemakerJobResponse;
import com.emailagent.dto.response.auth.BaseResponse;
import com.emailagent.rabbitmq.publisher.admin.AdminPublisher;
import com.emailagent.security.CurrentUser;
import com.emailagent.service.admin.SagemakerJobExecutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class SagemakerJobController {

    private final SagemakerJobExecutorService sagemakerJobExecutorService;
    private final AdminPublisher adminPublisher;

    /**
     * POST /api/admin/sagemakertraining
     * GitHub에 정의된 sagemaker.json 스펙으로 AWS SageMaker 학습 Job을 생성한다.
     */
    @PostMapping("/sagemakertraining")
    public ResponseEntity<SagemakerJobResponse> triggerSagemakerTraining() {
        return ResponseEntity.ok(sagemakerJobExecutorService.executeTrainingJob());
    }

    /**
     * POST /api/admin/modeldeploy
     * 모델 배포 요청을 x.app2ai.direct / 2ai_deployment 큐에 발행한다.
     */
    @PostMapping("/modeldeploy")
    public ResponseEntity<BaseResponse> triggerModelDeploy(@CurrentUser Long userId) {
        adminPublisher.publishDeployment(userId);
        return ResponseEntity.ok(new BaseResponse());
    }
}
