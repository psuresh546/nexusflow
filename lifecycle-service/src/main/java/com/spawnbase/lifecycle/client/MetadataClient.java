package com.spawnbase.lifecycle.client;

import com.spawnbase.common.model.InstanceState;
import com.spawnbase.lifecycle.dto.InstanceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * HTTP client for calling metadata-service.
 *
 * Uses Spring Boot 3.x RestClient — the modern,
 * fluent replacement for RestTemplate.
 *
 * lifecycle-service uses this to:
 * 1. GET current state of an instance
 * 2. PATCH state after FSM validation passes
 */
@Component
@Slf4j
public class MetadataClient {

    private final RestClient restClient;

    /**
     * @Value injects the URL from application.properties:
     * metadata.service.url=http://localhost:8081
     */
    public MetadataClient(
            @Value("${metadata.service.url}") String metadataUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(metadataUrl)
                .build();
    }

    /**
     * GET /api/instances/{id}
     * Fetches the current state of an instance.
     */
    public InstanceResponse getInstance(UUID instanceId) {
        log.info("Fetching instance {} from metadata-service",
                instanceId);

        return restClient.get()
                .uri("/api/instances/{id}", instanceId)
                .retrieve()
                .body(InstanceResponse.class);
    }

    /**
     * PATCH /api/instances/{id}/state
     * Tells metadata-service to update the state.
     * Called AFTER FSM validation passes.
     */
    public InstanceResponse updateState(
            UUID instanceId,
            InstanceState newState,
            String containerId,
            Integer hostPort) {

        log.info("Updating instance {} state to {} in metadata-service",
                instanceId, newState);

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("state", newState.name());
        if (containerId != null) body.put("containerId", containerId);
        if (hostPort != null) body.put("hostPort", hostPort);

        return restClient.patch()
                .uri("/api/instances/{id}/state", instanceId)
                .contentType(
                        org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(InstanceResponse.class);
    }
}