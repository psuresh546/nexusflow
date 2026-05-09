package com.spawnbase.credential.repository;

import com.spawnbase.credential.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CredentialRepository
        extends JpaRepository<Credential, UUID> {

    /**
     * Find credential by instance ID.
     * This is the primary lookup — callers know the instance ID.
     */
    Optional<Credential> findByInstanceId(UUID instanceId);

    /**
     * Check if credentials exist for an instance.
     * Used to prevent duplicate credential creation.
     */
    boolean existsByInstanceId(UUID instanceId);

    /**
     * Delete credentials when instance is deleted.
     */
    void deleteByInstanceId(UUID instanceId);
}