package com.emailagent.exception;

public class GithubYamlFetchException extends RuntimeException {

    public GithubYamlFetchException(String message) {
        super(message);
    }

    public GithubYamlFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
