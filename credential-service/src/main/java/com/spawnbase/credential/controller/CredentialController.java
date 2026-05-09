package com.spawnbase.credential.controller;

import com.spawnbase.credential.dto.CredentialRequest;
import com.spawnbase.credential.dto.CredentialResponse;
import com.spawnbase.credential.service.CredentialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/credentials")
@Slf4j
@RequiredArgsConstructor
public class CredentialController {

    private final CredentialService credentialService;

    /**
     * POST /api/credentials
     * Store credentials for a provisioned instance.
     * Called by provisioning-service after container is RUNNING.
     */
    @PostMapping
    public ResponseEntity<CredentialResponse> storeCredentials(
            @Valid @RequestBody CredentialRequest request) {

        log.info("POST /api/credentials — instance: {}",
                request.getInstanceId());

        CredentialResponse response =
                credentialService.storeCredentials(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * GET /api/credentials/{instanceId}
     * Get credentials for an instance.
     * Called when user wants to connect to their database.
     */
    @GetMapping("/{instanceId}")
    public ResponseEntity<CredentialResponse> getCredentials(
            @PathVariable UUID instanceId) {

        log.info("GET /api/credentials/{}", instanceId);

        return ResponseEntity.ok(
                credentialService.getCredentials(instanceId));
    }

    /**
     * DELETE /api/credentials/{instanceId}
     * Delete credentials when instance is deleted.
     * Called by provisioning-service during deletion.
     */
    @DeleteMapping("/{instanceId}")
    public ResponseEntity<Void> deleteCredentials(
            @PathVariable UUID instanceId) {

        log.info("DELETE /api/credentials/{}", instanceId);

        credentialService.deleteCredentials(instanceId);

        return ResponseEntity.noContent().build(); // 204
    }
}