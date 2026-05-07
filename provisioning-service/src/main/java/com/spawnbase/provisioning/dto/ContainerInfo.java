package com.spawnbase.provisioning.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;
import java.util.List;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContainerInfo {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("State")
    private ContainerState state;

    @JsonProperty("NetworkSettings")
    private NetworkSettings networkSettings;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContainerState {

        @JsonProperty("Status")
        private String status; // "running", "exited", "paused"

        @JsonProperty("Running")
        private boolean running;

        @JsonProperty("Health")
        private Health health;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Health {

        @JsonProperty("Status")
        private String status; // "healthy", "unhealthy", "starting"
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NetworkSettings {

        @JsonProperty("Ports")
        private Map<String, List<PortBinding>> ports;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PortBinding {

        @JsonProperty("HostIp")
        private String hostIp;

        @JsonProperty("HostPort")
        private String hostPort;
    }

    /**
     * Helper — extract the host port for a given container port.
     * e.g. getHostPort("5432/tcp") → "32768"
     */
    public Integer getHostPort(String containerPort) {
        if (networkSettings == null
                || networkSettings.getPorts() == null) {
            return null;
        }

        List<PortBinding> bindings =
                networkSettings.getPorts().get(containerPort);

        if (bindings == null || bindings.isEmpty()) {
            return null;
        }

        String hostPort = bindings.get(0).getHostPort();
        return hostPort != null ? Integer.parseInt(hostPort) : null;
    }

    /**
     * Is this container healthy and ready to accept connections?
     */
    public boolean isHealthy() {
        if (state == null) return false;
        if (!state.isRunning()) return false;
        if (state.getHealth() == null) return true; // no healthcheck
        return "healthy".equals(state.getHealth().getStatus());
    }
}