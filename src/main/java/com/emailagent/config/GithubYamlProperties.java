package com.emailagent.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kubernetes.github")
public class GithubYamlProperties {

    private String owner;
    private String repo;
    private String branch = "main";
    /** Private Repository 접근용 Personal Access Token. 공개 레포라면 비워 둔다. */
    private String pat;
    /** 레포 내 YAML 파일이 위치한 기본 경로 (예: k8s/jobs) */
    private String yamlBasePath = "k8s/jobs";
}
