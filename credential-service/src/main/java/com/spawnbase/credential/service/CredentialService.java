package com.spawnbase.credential.service;

import com.spawnbase.credential.dto.CredentialRequest;
import com.spawnbase.credential.dto.CredentialResponse;
import com.spawnbase.credential.entity.Credential;
import com.spawnbase.credential.exception.CredentialNotFoundException;
import com.spawnbase.credential.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final EncryptionService encryptionService;

    /**
     * Store credentials for a newly provisioned instance.
     * Password is encrypted before storing.
     *
     * Called by provisioning-service after
     * container becomes healthy.
     */
    @Transactional
    public CredentialResponse storeCredentials(
            CredentialRequest request) {

        log.info("Storing credentials for instance: {}",
                request.getInstanceId());

        // Prevent duplicates
        if (credentialRepository
                .existsByInstanceId(request.getInstanceId())) {
            log.warn("Credentials already exist for instance: {}",
                    request.getInstanceId());
            // Update existing instead of creating new
            return updateCredentials(request);
        }

        // Encrypt password before storing
        String encrypted = encryptionService
                .encrypt(request.getPassword());

        Credential credential = Credential.builder()
                .instanceId(request.getInstanceId())
                .username(request.getUsername())
                .encryptedPassword(encrypted)
                .connectionUrl(request.getConnectionUrl())
                .hostPort(request.getHostPort())
                .dbName(request.getDbName())
                .build();

        Credential saved = credentialRepository.save(credential);

        log.info("Credentials stored for instance: {}",
                request.getInstanceId());

        // Return with decrypted password
        return CredentialResponse.from(
                saved, request.getPassword());
    }

    /**
     * Retrieve credentials for an instance.
     * Password is decrypted before returning.
     */
    @Transactional(readOnly = true)
    public CredentialResponse getCredentials(UUID instanceId) {
        log.info("Retrieving credentials for instance: {}",
                instanceId);

        Credential credential = credentialRepository
                .findByInstanceId(instanceId)
                .orElseThrow(() ->
                        new CredentialNotFoundException(instanceId));

        String decrypted = encryptionService
                .decrypt(credential.getEncryptedPassword());

        return CredentialResponse.from(credential, decrypted);
    }

    /**
     * Delete credentials when an instance is deleted.
     */
    @Transactional
    public void deleteCredentials(UUID instanceId) {
        log.info("Deleting credentials for instance: {}",
                instanceId);

        if (!credentialRepository.existsByInstanceId(instanceId)) {
            log.warn("No credentials found for instance: {}",
                    instanceId);
            return;
        }

        credentialRepository.deleteByInstanceId(instanceId);
        log.info("Credentials deleted for instance: {}",
                instanceId);
    }

    // ─────────────────────────────────────────
    // PRIVATE
    // ─────────────────────────────────────────

    private CredentialResponse updateCredentials(
            CredentialRequest request) {

        Credential existing = credentialRepository
                .findByInstanceId(request.getInstanceId())
                .orElseThrow(() ->
                        new CredentialNotFoundException(
                                request.getInstanceId()));

        String encrypted = encryptionService
                .encrypt(request.getPassword());

        existing.setEncryptedPassword(encrypted);
        existing.setConnectionUrl(request.getConnectionUrl());
        existing.setHostPort(request.getHostPort());

        Credential updated = credentialRepository.save(existing);

        return CredentialResponse.from(
                updated, request.getPassword());
    }
}