package com.spawnbase.provisioning.dto;

import com.spawnbase.common.model.DatabaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class ProvisionRequest {

    @NotNull(message = "Database type is required")
    private DatabaseType dbType;


    @NotBlank(message = "Password is required")
    private String password;
}