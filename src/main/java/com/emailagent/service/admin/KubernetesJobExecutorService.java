package com.emailagent.service.admin;

import com.emailagent.config.GithubYamlProperties;
import com.emailagent.domain.enums.KubernetesJobType;
import com.emailagent.dto.response.admin.KubernetesJobResponse;
import com.emailagent.exception.GithubYamlFetchException;
import com.emailagent.exception.KubernetesJobDeployException;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.utils.Serialization;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class KubernetesJobExecutorService {

    private final KubernetesClient kubernetesClient;
    private final GithubYamlProperties githubProps;
    private final RestTemplate restTemplate;

    @Value("${kubernetes.job.default-namespace:admin}")
    private String defaultNamespace;

    /**
     * Enum으로 식별된 Job을 GitHub YAML 기반으로 Kubernetes에 배포.
     * namespace는 application.yml 고정값(kubernetes.job.default-namespace) 사용.
     */
    public KubernetesJobResponse executeJob(KubernetesJobType jobType) {
        String yamlRelativePath = jobType.getYamlPath();
        String resolvedNamespace = resolveNamespace();

        // 1단계: GitHub Raw Content 다운로드
        String yamlContent = fetchYamlFromGithub(yamlRelativePath);

        // 2단계: Fabric8로 YAML 파싱 (YAML 뼈대 무수정 원칙)
        Job job = parseJob(yamlContent, yamlRelativePath);

        // 3단계: Kubernetes 클러스터에 Job 생성
        Job created = createInCluster(job, resolvedNamespace);

        return buildResponse(created, yamlRelativePath);
    }

    /**
     * GitHub Raw Content API를 통해 YAML 파일을 문자열로 반환.
     * Private Repository인 경우 PAT 헤더를 포함한다.
     * URL 형식: https://raw.githubusercontent.com/{owner}/{repo}/{branch}/{basePath}/{relativePath}
     */
    private String fetchYamlFromGithub(String relativePath) {
        String rawUrl = String.format(
                "https://raw.githubusercontent.com/%s/%s/%s/%s/%s",
                githubProps.getOwner(),
                githubProps.getRepo(),
                githubProps.getBranch(),
                githubProps.getYamlBasePath(),
                relativePath
        );

        HttpHeaders headers = new HttpHeaders();
        // raw.githubusercontent.com은 CDN 직접 서빙 — GitHub Contents API 전용 헤더 불필요

        // Private Repo: PAT가 설정된 경우에만 Authorization 헤더 추가
        if (githubProps.getPat() != null && !githubProps.getPat().isBlank()) {
            headers.set("Authorization", "token " + githubProps.getPat());
        }

        log.info("[K8sJobExecutor] GitHub YAML fetch 시작 — url={}", rawUrl);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    rawUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new GithubYamlFetchException(
                        "GitHub YAML fetch 실패 — HTTP " + response.getStatusCode() + ", path=" + relativePath);
            }
            log.info("[K8sJobExecutor] GitHub YAML fetch 완료 — path={}", relativePath);
            return response.getBody();
        } catch (RestClientException e) {
            throw new GithubYamlFetchException(
                    "GitHub 통신 실패 — path=" + relativePath + ": " + e.getMessage(), e);
        }
    }

    /**
     * Fabric8 Serialization.unmarshal로 YAML → Job 객체 변환.
     * 이 단계에서 YAML의 어떤 필드도 수정하지 않는다.
     */
    private Job parseJob(String yamlContent, String path) {
        try (InputStream is = new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8))) {
            Job job = Serialization.unmarshal(is, Job.class);
            if (job == null) {
                throw new KubernetesJobDeployException("YAML 파싱 결과가 null — path=" + path);
            }
            return job;
        } catch (IOException | ClassCastException e) {
            throw new KubernetesJobDeployException(
                    "Kubernetes Job YAML 파싱 실패 — path=" + path + ": " + e.getMessage(), e);
        }
    }

    /**
     * In-Cluster ServiceAccount의 RBAC 권한으로 Kubernetes Job 생성.
     * KubernetesClientException 코드별 의미:
     *   403 — RBAC 권한 부족 (ServiceAccount에 jobs/create 권한 필요)
     *   409 — 동일 이름의 Job이 이미 존재
     */
    private Job createInCluster(Job job, String namespace) {
        try {
            Job created = kubernetesClient.batch().v1().jobs()
                    .inNamespace(namespace)
                    .resource(job)
                    .create();
            log.info("[K8sJobExecutor] Job 생성 완료 — name={}, namespace={}, uid={}",
                    created.getMetadata().getName(), namespace, created.getMetadata().getUid());
            return created;
        } catch (KubernetesClientException e) {
            log.error("[K8sJobExecutor] K8s Job 생성 실패 — namespace={}, statusCode={}",
                    namespace, e.getCode(), e);
            throw new KubernetesJobDeployException(
                    "Kubernetes Job 생성 실패 (code=" + e.getCode() + "): " + e.getMessage(), e);
        }
    }

    /**
     * 현재 Pod가 속한 Namespace를 Fabric8 클라이언트에서 조회.
     * In-Cluster 환경에서는 /var/run/secrets/kubernetes.io/serviceaccount/namespace 파일 기반.
     * 로컬 개발 환경이거나 감지 실패 시 application.yml의 기본값 사용.
     */
    private String resolveNamespace() {
        String ns = kubernetesClient.getNamespace();
        return (ns != null && !ns.isBlank()) ? ns : defaultNamespace;
    }

    private KubernetesJobResponse buildResponse(Job created, String yamlPath) {
        ObjectMeta meta = created.getMetadata();
        return new KubernetesJobResponse(
                meta.getName(),
                meta.getNamespace(),
                meta.getUid(),
                yamlPath,
                Instant.now().toString()
        );
    }
}
