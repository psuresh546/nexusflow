package com.spawnbase.provisioning.dto;

import com.spawnbase.common.model.DatabaseType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class ProvisionRequest {

    @NotNull(message = "Database type is required")
    private DatabaseType dbType;

    private String password;
}