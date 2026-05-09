package com.spawnbase.credential.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/**
 * Request to store credentials for an instance.
 * Called by provisioning-service after
 * a container is successfully started.
 */
@Data
public class CredentialRequest {

    @NotNull(message = "Instance ID is required")
    private UUID instanceId;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Connection URL is required")
    private String connectionUrl;

    @NotNull(message = "Host port is required")
    private Integer hostPort;

    @NotBlank(message = "Database name is required")
    private String dbName;
}