package com.spawnbase.provisioning.docker;


public class DockerApiException extends RuntimeException {

    private final int statusCode;

    public DockerApiException(int statusCode, String message) {
        super(String.format(
                "Docker API error [HTTP %d]: %s", statusCode, message));
        this.statusCode = statusCode;
    }

    public DockerApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public int getStatusCode() {
        return statusCode;
    }
}