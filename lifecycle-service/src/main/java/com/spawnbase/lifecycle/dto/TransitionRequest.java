package com.spawnbase.lifecycle.dto;

import com.spawnbase.common.model.InstanceState;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for triggering a state transition.
 *
 * The caller says:
 * "I want instance {id} to move to state {targetState}"
 *
 * lifecycle-service validates this against the FSM,
 * then tells metadata-service to update if valid.
 */
@Data
public class TransitionRequest {

    @NotNull(message = "Target state is required")
    private InstanceState targetState;

    /**
     * Optional — Docker container ID.
     * Only provided when transitioning to RUNNING
     * after successful provisioning.
     */
    private String containerId;

    /**
     * Optional — host port Docker assigned.
     * Only provided when transitioning to RUNNING.
     */
    private Integer hostPort;
}