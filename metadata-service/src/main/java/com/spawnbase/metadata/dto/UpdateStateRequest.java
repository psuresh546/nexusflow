package com.spawnbase.metadata.dto;

import com.spawnbase.common.model.InstanceState;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class UpdateStateRequest {

    @NotNull(message = "Target state is required")
    private InstanceState state;

    private String containerId;

    private Integer hostPort;
}