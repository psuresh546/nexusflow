package com.spawnbase.metadata.dto;

import com.spawnbase.common.model.InstanceState;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStateRequest {

    @NotNull(message = "State is required")
    private InstanceState state;

    // Optional — set when container is created
    private String containerId;

    // Optional — set when container port is assigned
    private Integer hostPort;
}