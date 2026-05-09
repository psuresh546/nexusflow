package com.spawnbase.provisioning.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Component
@Slf4j
public class CredentialClient {

    private final RestClient restClient;

    public CredentialClient(
            @Value("${credential.service.url}")
            String credentialServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(credentialServiceUrl)
                .build();
    }

    /**
     * Store credentials for a newly provisioned instance.
     * Called after container reaches RUNNING state.
     */
    public void storeCredentials(
            UUID instanceId,
            String username,
            String password,
            String connectionUrl,
            Integer hostPort,
            String dbName) {

        log.info("Storing credentials for instance: {}",
                instanceId);

        Map<String, Object> body = new HashMap<>();
        body.put("instanceId", instanceId.toString());
        body.put("username", username);
        body.put("password", password);
        body.put("connectionUrl", connectionUrl);
        body.put("hostPort", hostPort);
        body.put("dbName", dbName);

        try {
            restClient.post()
                    .uri("/api/credentials")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Credentials stored successfully " +
                    "for instance: {}", instanceId);

        } catch (Exception e) {
            // Log but don't fail provisioning
            // Credentials can be re-stored manually
            log.error("Failed to store credentials " +
                            "for instance {}: {}",
                    instanceId, e.getMessage());
        }
    }

    /**
     * Delete credentials when instance is deleted.
     */
    public void deleteCredentials(UUID instanceId) {
        log.info("Deleting credentials for instance: {}",
                instanceId);

        try {
            restClient.delete()
                    .uri("/api/credentials/{instanceId}",
                            instanceId)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Credentials deleted for instance: {}",
                    instanceId);

        } catch (Exception e) {
            log.error("Failed to delete credentials " +
                            "for instance {}: {}",
                    instanceId, e.getMessage());
        }
    }
}