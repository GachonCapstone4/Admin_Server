package com.emailagent.controller.admin;

import com.emailagent.dto.response.admin.SagemakerJobResponse;
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

    /**
     * POST /api/admin/job/sagemakertraining
     * GitHub에 정의된 sagemaker.json 스펙으로 AWS SageMaker 학습 Job을 생성한다.
     */
    @PostMapping("/sagemakertraining")
    public ResponseEntity<SagemakerJobResponse> triggerSagemakerTraining() {
        return ResponseEntity.ok(sagemakerJobExecutorService.executeTrainingJob());
    }
}
