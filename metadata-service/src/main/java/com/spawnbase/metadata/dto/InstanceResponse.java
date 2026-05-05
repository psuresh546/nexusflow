package com.spawnbase.metadata.dto;

import com.spawnbase.common.model.InstanceState;
import com.spawnbase.provisioning.model.DatabaseType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@Builder
public class InstanceResponse {

    private UUID id;
    private String name;
    private DatabaseType dbType;
    private InstanceState state;
    private String ownerId;
    private Integer hostPort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    public static InstanceResponse from(com.spawnbase.metadata.entity.Instance instance) {
        return InstanceResponse.builder()
                .id(instance.getId())
                .name(instance.getName())
                .dbType(instance.getDbType())
                .state(instance.getState())
                .ownerId(instance.getOwnerId())
                .hostPort(instance.getHostPort())
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .build();
    }
}