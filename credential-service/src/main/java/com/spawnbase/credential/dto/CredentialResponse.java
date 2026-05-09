package com.spawnbase.credential.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Response DTO for credential retrieval.
 *
 * Returns the DECRYPTED password so the
 * user can connect to their database.
 *
 * Note: In production, consider returning
 * a temporary token instead of the raw password.
 */
@Data
@Builder
public class CredentialResponse {

    private UUID instanceId;
    private String username;
    private String password;        // decrypted
    private String connectionUrl;
    private Integer hostPort;
    private String dbName;

    public static CredentialResponse from(
            com.spawnbase.credential.entity.Credential credential,
            String decryptedPassword) {
        return CredentialResponse.builder()
                .instanceId(credential.getInstanceId())
                .username(credential.getUsername())
                .password(decryptedPassword)
                .connectionUrl(credential.getConnectionUrl())
                .hostPort(credential.getHostPort())
                .dbName(credential.getDbName())
                .build();
    }
}