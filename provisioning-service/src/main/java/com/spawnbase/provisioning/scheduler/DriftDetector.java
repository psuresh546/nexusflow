package com.spawnbase.provisioning.scheduler;

import com.spawnbase.common.model.InstanceState;
import com.spawnbase.provisioning.client.MetadataClient;
import com.spawnbase.provisioning.client.MetadataClient.InstanceSummary;
import com.spawnbase.provisioning.docker.DockerApiException;
import com.spawnbase.provisioning.docker.DockerClient;
import com.spawnbase.provisioning.dto.ContainerInfo;
import com.spawnbase.provisioning.service.RollbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Scheduled job that detects state drift between
 * SpawnBase metadata and Docker reality.
 *
 * Runs every 60 seconds.
 *
 * Three drift scenarios handled:
 *
 * 1. RUNNING in SpawnBase, container missing in Docker
 *    → Mark FAILED (container is gone, DB is unreachable)
 *
 * 2. RUNNING in SpawnBase, container stopped in Docker
 *    → Mark STOPPED (someone stopped it externally)
 *
 * 3. DELETED in SpawnBase, container still running in Docker
 *    → Remove the orphaned container
 *
 * Why is drift detection important?
 * Without it, SpawnBase becomes a liar — showing RUNNING
 * for databases that haven't existed for hours.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DriftDetector {

    private final MetadataClient metadataClient;
    private final DockerClient dockerClient;
    private final RollbackService rollbackService;

    @Value("${metadata.service.url}")
    private String metadataServiceUrl;

    /**
     * Run drift detection every 60 seconds.
     *
     * fixedDelay = 60s after previous run completes.
     * This prevents overlap if a run takes longer than 60s.
     *
     * initialDelay = 30s to let services start up first.
     */
    @Scheduled(
            fixedDelayString = "${drift.detector.interval.ms:60000}",
            initialDelayString = "${drift.detector.initial.delay.ms:30000}"
    )
    public void detectDrift() {
        log.info("Starting drift detection scan...");

        int driftFound = 0;

        try {
            driftFound += checkRunningInstances();
            driftFound += checkFailedInstances();   // ← ADD THIS
            driftFound += checkDeletedInstances();
        } catch (Exception e) {
            log.error("Drift detection scan failed: {}",
                    e.getMessage(), e);
        }

        if (driftFound == 0) {
            log.info("Drift detection complete — " +
                    "no drift found ✅");
        } else {
            log.warn("Drift detection complete — " +
                    "{} drift(s) found and corrected", driftFound);
        }
    }

    // ─────────────────────────────────────────
    // SCENARIO 1 + 2: RUNNING instances
    // ─────────────────────────────────────────

    /**
     * Check all RUNNING instances — verify their
     * containers actually exist and are running in Docker.
     *
     * @return number of drift cases found
     */
    private int checkRunningInstances() {
        List<InstanceSummary> running =
                metadataClient.getInstancesByState(
                        InstanceState.RUNNING);

        log.info("Checking {} RUNNING instances for drift...",
                running.size());

        int driftCount = 0;

        for (InstanceSummary instance : running) {
            if (instance.getContainerId() == null) {
                log.warn("DRIFT: Instance {} is RUNNING " +
                                "but has no containerId — marking FAILED",
                        instance.getId());
                markFailed(instance.getId(),
                        "No container ID — likely provisioning bug");
                driftCount++;
                continue;
            }

            try {
                ContainerInfo info = dockerClient
                        .inspectContainer(instance.getContainerId());

                if (!info.getState().isRunning()) {
                    // Container exists but is stopped
                    log.warn("DRIFT: Instance {} is RUNNING " +
                                    "in SpawnBase but container is {} " +
                                    "in Docker — marking STOPPED",
                            instance.getId(),
                            info.getState().getStatus());
                    markStopped(instance.getId());
                    driftCount++;
                } else {
                    log.debug("Instance {} container is healthy ✅",
                            instance.getId());
                }

            } catch (DockerApiException e) {
                if (e.getStatusCode() == 404) {
                    // Container doesn't exist at all
                    log.warn("DRIFT: Instance {} is RUNNING " +
                                    "in SpawnBase but container is " +
                                    "GONE from Docker — marking FAILED",
                            instance.getId());
                    markFailed(instance.getId(),
                            "Container not found in Docker");
                    driftCount++;
                } else {
                    log.error("Error checking container for " +
                                    "instance {}: {}",
                            instance.getId(), e.getMessage());
                }
            }
        }

        return driftCount;
    }

    // ─────────────────────────────────────────
    // SCENARIO 3: DELETED instances
    // ─────────────────────────────────────────

    /**
     * Check DELETED instances — verify their containers
     * are actually gone from Docker.
     * If a container is still running, remove it.
     *
     * @return number of orphaned containers removed
     */
    private int checkDeletedInstances() {
        List<InstanceSummary> deleted =
                metadataClient.getInstancesByState(
                        InstanceState.DELETED);

        log.info("Checking {} DELETED instances for orphans...",
                deleted.size());

        int orphansRemoved = 0;

        for (InstanceSummary instance : deleted) {
            if (instance.getContainerId() == null) continue;

            if (rollbackService.containerExists(
                    instance.getContainerId())) {
                log.warn("ORPHAN: Instance {} is DELETED " +
                                "in SpawnBase but container still " +
                                "exists in Docker — removing orphan",
                        instance.getId());
                rollbackService.removeOrphan(
                        instance.getContainerId());
                orphansRemoved++;
            }
        }

        return orphansRemoved;
    }

    // ─────────────────────────────────────────
    // SCENARIO 4: FAILED instances
    // ─────────────────────────────────────────

    /**
     * Check FAILED instances — if their container is
     * actually running and healthy, correct to RUNNING.
     *
     * This happens when:
     * - Health check timed out during provisioning
     *   but container recovered afterward
     * - Manual recovery attempt partially succeeded
     */
    private int checkFailedInstances() {
        List<InstanceSummary> failed =
                metadataClient.getInstancesByState(
                        InstanceState.FAILED);

        log.info("Checking {} FAILED instances for " +
                "self-recovered containers...", failed.size());

        int recovered = 0;

        for (InstanceSummary instance : failed) {
            if (instance.getContainerId() == null) continue;

            try {
                ContainerInfo info = dockerClient
                        .inspectContainer(instance.getContainerId());

                if (info.isHealthy()) {
                    log.warn("DRIFT: Instance {} is FAILED " +
                                    "in SpawnBase but container is " +
                                    "RUNNING and HEALTHY in Docker " +
                                    "— correcting to RUNNING",
                            instance.getId());

                    updateState(instance.getId(),
                            InstanceState.RUNNING);
                    recovered++;
                }

            } catch (DockerApiException e) {
                if (e.getStatusCode() == 404) {
                    log.debug("FAILED instance {} has no " +
                                    "container — correct state",
                            instance.getId());
                }
            }
        }

        return recovered;
    }

    // ─────────────────────────────────────────
    // PRIVATE — state update helpers
    // ─────────────────────────────────────────

    private void markFailed(UUID instanceId, String reason) {
        log.warn("Marking instance {} as FAILED. Reason: {}",
                instanceId, reason);
        updateState(instanceId, InstanceState.FAILED);
    }

    private void markStopped(UUID instanceId) {
        log.warn("Marking instance {} as STOPPED " +
                "(external stop detected)", instanceId);
        updateState(instanceId, InstanceState.STOPPED);
    }

    private void updateState(
            UUID instanceId,
            InstanceState state) {
        try {
            RestClient restClient = RestClient.builder()
                    .baseUrl(metadataServiceUrl)
                    .build();

            Map<String, Object> body = new HashMap<>();
            body.put("state", state.name());

            restClient.patch()
                    .uri("/api/instances/{id}/state", instanceId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Drift corrected: instance {} → {}",
                    instanceId, state);

        } catch (Exception e) {
            log.error("Failed to update state for {}: {}",
                    instanceId, e.getMessage());
        }
    }
}