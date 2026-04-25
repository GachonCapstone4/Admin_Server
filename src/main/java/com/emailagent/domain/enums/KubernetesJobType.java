package com.emailagent.domain.enums;

/**
 * Kubernetes Job 식별자 Enum.
 * 새 Job 추가 시: 1) 여기에 값 추가, 2) Controller에 메서드 1개 추가.
 * yamlPath는 github.yaml-base-path 기준 상대 경로.
 */
public enum KubernetesJobType {

    NETWORK_DICT("02-network-dict-job.yaml", "네트워크 사전 생성");
    // 새 Job 추가 시 위 줄 아래에 값을 추가하세요

    private final String yamlPath;
    private final String description;

    KubernetesJobType(String yamlPath, String description) {
        this.yamlPath = yamlPath;
        this.description = description;
    }

    public String getYamlPath() {
        return yamlPath;
    }

    public String getDescription() {
        return description;
    }
}
