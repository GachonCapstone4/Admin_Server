package com.emailagent.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class KubernetesConfig {

    /**
     * Fabric8 KubernetesClient 빈.
     * Kubernetes Pod 내에서 실행될 때 ServiceAccount 기반 InClusterConfig를 자동으로 감지한다.
     * 로컬 개발 환경에서는 ~/.kube/config 를 사용한다.
     */
    @Bean
    public KubernetesClient kubernetesClient() {
        return new KubernetesClientBuilder().build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
