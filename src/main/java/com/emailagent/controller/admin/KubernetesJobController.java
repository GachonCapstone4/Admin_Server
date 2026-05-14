package com.emailagent.controller.admin;

import com.emailagent.domain.enums.KubernetesJobType;
import com.emailagent.dto.response.admin.KubernetesJobResponse;
import com.emailagent.service.admin.KubernetesJobExecutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/k8s")
@RequiredArgsConstructor
public class KubernetesJobController {

    private final KubernetesJobExecutorService kubernetesJobExecutorService;

    /**
     * POST /api/admin/k8s/jobs/network-dict
     * 네트워크 사전 생성 Job을 실행한다.
     */
    @PostMapping("/jobs/network-dict")
    public ResponseEntity<KubernetesJobResponse> executeNetworkDict() {
        return ResponseEntity.ok(
                kubernetesJobExecutorService.executeJob(KubernetesJobType.NETWORK_DICT)
        );
    }

    // 새 Job 추가 시: @PostMapping("/jobs/{job-name}") 메서드 1개 + KubernetesJobType 값 1개 추가

    @PostMapping("/jobs/os-dict")
    public ResponseEntity<KubernetesJobResponse> executeOSDict() {
        return ResponseEntity.ok(
                kubernetesJobExecutorService.executeJob(KubernetesJobType.OS_DICT)
        );
    }

    @PostMapping("/jobs/vpn-dict")
    public ResponseEntity<KubernetesJobResponse> executeVPNDict() {
        return ResponseEntity.ok(
                kubernetesJobExecutorService.executeJob(KubernetesJobType.VPN_DICT)
        );
    }

    @PostMapping("/jobs/dataset")
    public ResponseEntity<KubernetesJobResponse> executeDATAJOB() {
        return ResponseEntity.ok(
                kubernetesJobExecutorService.executeJob(KubernetesJobType.DATA_JOB)
        );
    }

    @PostMapping("/jobs/node")
    public ResponseEntity<KubernetesJobResponse> executeNODEDict() {
        return ResponseEntity.ok(
                kubernetesJobExecutorService.executeJob(KubernetesJobType.NODE_DICT)
        );
    }

    @PostMapping("/jobs/rabbitmq")
    public ResponseEntity<KubernetesJobResponse> executeMQDICT() {
        return ResponseEntity.ok(
                kubernetesJobExecutorService.executeJob(KubernetesJobType.MQ_DICT)
        );
    }

}
