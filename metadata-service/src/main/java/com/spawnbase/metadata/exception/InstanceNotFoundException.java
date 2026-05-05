package com.spawnbase.metadata.exception;

import java.util.UUID;

public class InstanceNotFoundException extends RuntimeException {

    private final UUID instanceId;

    public InstanceNotFoundException(UUID instanceId) {
        super("Instance not found with id: " + instanceId);
        this.instanceId = instanceId;
    }

    public UUID getInstanceId() {
        return instanceId;
    }
}