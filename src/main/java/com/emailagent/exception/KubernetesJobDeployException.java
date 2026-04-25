package com.emailagent.exception;

public class KubernetesJobDeployException extends RuntimeException {

    public KubernetesJobDeployException(String message) {
        super(message);
    }

    public KubernetesJobDeployException(String message, Throwable cause) {
        super(message, cause);
    }
}
