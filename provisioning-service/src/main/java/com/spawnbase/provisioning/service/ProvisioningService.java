package com.spawnbase.provisioning.service;

import com.spawnbase.common.model.DatabaseType;
import com.spawnbase.common.model.InstanceState;
import com.spawnbase.provisioning.docker.DockerApiException;
import com.spawnbase.provisioning.docker.DockerClient;
import com.spawnbase.provisioning.dto.ContainerCreateRequest;
import com.spawnbase.provisioning.dto.ContainerInfo;
import com.spawnbase.provisioning.provider.DatabaseProvider;
import com.spawnbase.provisioning.provider.DatabaseProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
public class ProvisioningService {

    private final DockerClient dockerClient;
    private final DatabaseProviderFactory providerFactory;

    @Value("${metadata.service.url}")
    private String metadataServiceUrl;


    public void provision(UUID instanceId, DatabaseType dbType, String password) {

        log.info("Provisioning {} instance: {}", dbType, instanceId);

        DatabaseProvider provider = providerFactory.getProvider(dbType);

        String containerName = "spawnbase-" + instanceId;
        String containerId = null;

        try {
            // Step 1 — Pull image (cached after first pull)
            log.info("Pulling image: {}", provider.getDockerImage());
            dockerClient.pullImage(provider.getDockerImage());

            // Step 2 — Build container config
            ContainerCreateRequest createRequest = buildCreateRequest(provider, password, instanceId);

            // Step 3 — Create container
            containerId = dockerClient.createContainer(containerName, createRequest);

            // Step 4 — Start container
            dockerClient.startContainer(containerId);

            // Step 5 — Wait for health check
            log.info("Waiting for container to become healthy...");
            waitForHealthy(containerId, provider);

            // Step 6 — Get assigned host port
            ContainerInfo info =
                    dockerClient.inspectContainer(containerId);
            Integer hostPort = info.getHostPort(provider.getContainerPort() + "/tcp");

            log.info("Instance {} provisioned successfully. " + "Port: {}", instanceId, hostPort);

            // Step 7 — Update metadata-service → RUNNING
            updateState(instanceId, InstanceState.RUNNING, containerId, hostPort);

        } catch (Exception e) {
            log.error("Failed to provision instance {}: {}", instanceId, e.getMessage());

            // Clean up partial container if it was created
            if (containerId != null) {
                try {
                    dockerClient.removeContainer(containerId);
                } catch (Exception cleanupEx) {
                    log.warn("Cleanup failed for container {}: {}", containerId, cleanupEx.getMessage());
                }
            }

            // Tell metadata-service → FAILED
            updateState(instanceId, InstanceState.FAILED, null, null);
        }
    }

    /**
     * Stop a running container.
     * Transitions: RUNNING → STOPPED
     */
    public void stop(UUID instanceId, String containerId) {
        log.info("Stopping instance: {}", instanceId);
        try {
            dockerClient.stopContainer(containerId);
            updateState(instanceId, InstanceState.STOPPED,null, null);
        } catch (DockerApiException e) {
            log.error("Failed to stop instance {}: {}", instanceId, e.getMessage());
            updateState(instanceId, InstanceState.FAILED, null, null);
        }
    }

    /**
     * Start a stopped container.
     * Transitions: STOPPED → STARTING → RUNNING
     */
    public void start(UUID instanceId, String containerId, DatabaseProvider provider) {
        log.info("Starting instance: {}", instanceId);
        try {
            dockerClient.startContainer(containerId);
            waitForHealthy(containerId, provider);

            ContainerInfo info = dockerClient.inspectContainer(containerId);
            Integer hostPort = info.getHostPort(provider.getContainerPort() + "/tcp");

            updateState(instanceId, InstanceState.RUNNING, containerId, hostPort);
        } catch (Exception e) {
            log.error("Failed to start instance {}: {}", instanceId, e.getMessage());
            updateState(instanceId, InstanceState.FAILED, null, null);
        }
    }

    /**
     * Delete a container permanently.
     * Transitions: DELETING → DELETED
     */
    public void delete(UUID instanceId, String containerId) {
        log.info("Deleting instance: {}", instanceId);
        try {
            if (containerId != null) {
                dockerClient.removeContainer(containerId);
            }
            updateState(instanceId, InstanceState.DELETED, null, null);
        } catch (DockerApiException e) {
            log.error("Failed to delete instance {}: {}", instanceId, e.getMessage());
            updateState(instanceId, InstanceState.FAILED, null, null);
        }
    }


    /**
     * Build the Docker container create request
     * from the DatabaseProvider contract.
     */
    private ContainerCreateRequest buildCreateRequest(DatabaseProvider provider, String password, UUID instanceId) {

        String dbName = "db_" + instanceId.toString().replace("-", "").substring(0, 8);

        // Memory limit: "512m" → 512 * 1024 * 1024 bytes
        String memLimit = provider.getMemoryLimit();
        long memBytes = parseMemory(memLimit);

        // Port binding: auto-assign host port ("0")
        String portKey = provider.getContainerPort() + "/tcp";
        Map<String, List<ContainerCreateRequest.PortBinding>> ports = new HashMap<>();
        ports.put(portKey, List.of(
                ContainerCreateRequest.PortBinding.builder()
                        .hostPort("0")   // 0 = auto-assign
                        .build()
        ));

        // Health check command
        String[] healthCmd = provider.getHealthCheckCommand();
        List<String> healthTest = new java.util.ArrayList<>();
        healthTest.add("CMD");
        healthTest.addAll(Arrays.asList(healthCmd));

        return ContainerCreateRequest.builder()
                .image(provider.getDockerImage())
                .env(Arrays.asList(provider.getEnvironmentVariables(password, dbName)))
                .hostConfig(
                        ContainerCreateRequest.HostConfig.builder()
                                .portBindings(ports)
                                .memory(memBytes)
                                .restartPolicy(ContainerCreateRequest
                                                .RestartPolicy.builder()
                                                .name("on-failure")
                                                .maximumRetryCount(3)
                                                .build())
                                .build()).healthcheck(ContainerCreateRequest.HealthCheck.builder()
                                .test(healthTest)
                                .interval(10_000_000_000L)  // 10s
                                .timeout(5_000_000_000L)    // 5s
                                .retries(5)
                                .startPeriod(30_000_000_000L) // 30s
                                .build())
                .build();
    }

    /**
     * Wait until the container health check passes.
     * Polls every 5 seconds, up to 2 minutes.
     */
    private void waitForHealthy(
            String containerId,
            DatabaseProvider provider) {

        int maxAttempts = 24; // 24 * 5s = 2 minutes
        int attempts = 0;

        while (attempts < maxAttempts) {
            try {
                Thread.sleep(5000); // wait 5 seconds
                ContainerInfo info = dockerClient.inspectContainer(containerId);

                if (info.isHealthy()) {
                    log.info("Container is healthy after {}s", (attempts + 1) * 5);
                    return;
                }

                log.info("Waiting for health check... " + "attempt {}/{}", attempts + 1, maxAttempts);
                attempts++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DockerApiException("Interrupted while waiting for container", e);
            }
        }

        throw new DockerApiException(-1, "Container did not become healthy within 2 minutes");
    }

    /**
     * Tell metadata-service to update the instance state.
     * Called after every Docker operation.
     */
    private void updateState(UUID instanceId, InstanceState state, String containerId, Integer hostPort) {

        try {
            RestClient restClient = RestClient.builder()
                    .baseUrl(metadataServiceUrl)
                    .build();

            Map<String, Object> body = new HashMap<>();
            body.put("state", state.name());
            if (containerId != null)
                body.put("containerId", containerId);
            if (hostPort != null)
                body.put("hostPort", hostPort);

            restClient.patch()
                    .uri("/api/instances/{id}/state", instanceId)
                    .contentType(
                            org.springframework.http.MediaType
                                    .APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Instance {} state updated to {}", instanceId, state);

        } catch (Exception e) {
            log.error("Failed to update metadata for {}: {}", instanceId, e.getMessage());
        }
    }

    /**
     * Parse memory string to bytes.
     * "512m" → 536870912
     * "1g"   → 1073741824
     */
    private long parseMemory(String memLimit) {
        if (memLimit.endsWith("m") || memLimit.endsWith("M")) {
            return Long.parseLong(memLimit.substring(0, memLimit.length() - 1)) * 1024 * 1024;
        } else if (memLimit.endsWith("g") || memLimit.endsWith("G")) {
            return Long.parseLong(memLimit.substring(0, memLimit.length() - 1)) * 1024 * 1024 * 1024;
        }
        return Long.parseLong(memLimit);
    }
}