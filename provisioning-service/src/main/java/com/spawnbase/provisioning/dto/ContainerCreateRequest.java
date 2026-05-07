package com.spawnbase.provisioning.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;


@Data
@Builder
public class ContainerCreateRequest {

    @JsonProperty("Image")
    private String image;

    @JsonProperty("Env")
    private List<String> env;

    @JsonProperty("HostConfig")
    private HostConfig hostConfig;

    @JsonProperty("Healthcheck")
    private HealthCheck healthcheck;

    @Data
    @Builder
    public static class HostConfig {

        @JsonProperty("PortBindings")
        private Map<String, List<PortBinding>> portBindings;

        @JsonProperty("Memory")
        private Long memory;

        @JsonProperty("RestartPolicy")
        private RestartPolicy restartPolicy;
    }

    @Data
    @Builder
    public static class PortBinding {
        @JsonProperty("HostPort")
        private String hostPort;
    }

    @Data
    @Builder
    public static class RestartPolicy {
        @JsonProperty("Name")
        private String name;

        @JsonProperty("MaximumRetryCount")
        private int maximumRetryCount;
    }

    @Data
    @Builder
    public static class HealthCheck {
        /**
         * Command to run inside container.
         * Format: ["CMD", "pg_isready", "-U", "spawnbase"]
         */
        @JsonProperty("Test")
        private List<String> test;

        /** How often to run health check (nanoseconds) */
        @JsonProperty("Interval")
        private long interval;

        /** How long to wait for result (nanoseconds) */
        @JsonProperty("Timeout")
        private long timeout;

        /** How many failures before marking unhealthy */
        @JsonProperty("Retries")
        private int retries;

        /** Time to wait before starting checks (nanoseconds) */
        @JsonProperty("StartPeriod")
        private long startPeriod;
    }
}