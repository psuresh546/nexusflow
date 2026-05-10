package com.spawnbase.provisioning.service;

import com.spawnbase.provisioning.docker.DockerApiException;
import com.spawnbase.provisioning.docker.DockerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
public class RollbackService {

    private final DockerClient dockerClient;

    /**
     * Attempt to clean up a partially created container.
     *
     * Called when provisioning fails after container
     * creation but before RUNNING state is reached.
     *
     * @param instanceId  for logging context
     * @param containerId Docker container to remove
     */
    public void rollback(UUID instanceId, String containerId) {
        if (containerId == null) {
            log.info("No container to rollback for instance: {}",
                    instanceId);
            return;
        }

        log.warn("Rolling back container {} for instance: {}",
                containerId.substring(0, 12), instanceId);

        // Step 1 — Try to stop first (graceful)
        tryStop(containerId, instanceId);

        // Step 2 — Remove the container
        tryRemove(containerId, instanceId);

        log.info("Rollback complete for instance: {}", instanceId);
    }

    /**
     * Verify a container still exists in Docker.
     * Used by drift detector to check for orphaned containers.
     *
     * @return true if container exists, false if not found
     */
    public boolean containerExists(String containerId) {
        try {
            dockerClient.inspectContainer(containerId);
            return true;
        } catch (DockerApiException e) {
            if (e.getStatusCode() == 404) {
                return false;
            }
            // Other error — assume it exists to be safe
            log.warn("Error checking container {}: {}",
                    containerId.substring(0, 12), e.getMessage());
            return true;
        }
    }

    /**
     * Remove an orphaned container that exists in Docker
     * but whose instance is in FAILED or DELETED state.
     */
    public void removeOrphan(String containerId) {
        log.warn("Removing orphaned container: {}",
                containerId.substring(0, 12));
        tryStop(containerId, null);
        tryRemove(containerId, null);
    }

    // ─────────────────────────────────────────
    // PRIVATE — best-effort operations
    // ─────────────────────────────────────────

    private void tryStop(String containerId, UUID instanceId) {
        try {
            dockerClient.stopContainer(containerId);
            log.info("Container stopped during rollback: {}",
                    containerId.substring(0, 12));
        } catch (DockerApiException e) {
            // 304 = already stopped, that's fine
            if (e.getStatusCode() != 304) {
                log.warn("Could not stop container {} " +
                                "during rollback (continuing): {}",
                        containerId.substring(0, 12),
                        e.getMessage());
            }
        }
    }

    private void tryRemove(String containerId, UUID instanceId) {
        try {
            dockerClient.removeContainer(containerId);
            log.info("Container removed during rollback: {}",
                    containerId.substring(0, 12));
        } catch (DockerApiException e) {
            // 404 = already gone, that's fine
            if (e.getStatusCode() != 404) {
                log.error("ROLLBACK FAILED — manual cleanup " +
                                "required for container {}: {}",
                        containerId.substring(0, 12),
                        e.getMessage());
            }
        }
    }
}