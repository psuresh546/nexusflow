package com.spawnbase.lifecycle.dto;

import com.spawnbase.common.model.InstanceState;
import com.spawnbase.common.model.DatabaseType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing an instance as returned
 * by metadata-service.
 *
 * lifecycle-service reads this to get the
 * CURRENT state before validating the transition.
 */
@Data
public class InstanceResponse {
    private UUID id;
    private String name;
    private DatabaseType dbType;
    private InstanceState state;
    private String ownerId;
    private Integer hostPort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}