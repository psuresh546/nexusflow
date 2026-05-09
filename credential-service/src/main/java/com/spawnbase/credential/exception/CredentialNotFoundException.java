package com.spawnbase.credential.exception;

import java.util.UUID;

public class CredentialNotFoundException extends RuntimeException {

    private final UUID instanceId;

    public CredentialNotFoundException(UUID instanceId) {
        super("Credentials not found for instance: " + instanceId);
        this.instanceId = instanceId;
    }

    public UUID getInstanceId() {
        return instanceId;
    }
}