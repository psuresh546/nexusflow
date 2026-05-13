package com.spawnbase.common.event;

import com.spawnbase.common.model.DatabaseType;
import com.spawnbase.common.model.InstanceState;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;


@Getter
public abstract class InstanceEvent {

    private final UUID instanceId;
    private final String instanceName;
    private final DatabaseType dbType;
    private final InstanceState newState;
    private final InstanceState previousState;
    private final LocalDateTime occurredAt;
    private final String eventType;

    protected InstanceEvent(
            UUID instanceId,
            String instanceName,
            DatabaseType dbType,
            InstanceState newState,
            InstanceState previousState,
            String eventType) {
        this.instanceId = instanceId;
        this.instanceName = instanceName;
        this.dbType = dbType;
        this.newState = newState;
        this.previousState = previousState;
        this.eventType = eventType;
        this.occurredAt = LocalDateTime.now();
    }

    // ─────────────────────────────────────────
    // CONCRETE EVENT TYPES
    // ─────────────────────────────────────────

    public static class InstanceCreated extends InstanceEvent {
        public InstanceCreated(UUID id, String name,
                               DatabaseType dbType) {
            super(id, name, dbType,
                    InstanceState.REQUESTED, null,
                    "INSTANCE_CREATED");
        }
    }

    public static class InstanceProvisioning extends InstanceEvent {
        public InstanceProvisioning(UUID id, String name,
                                    DatabaseType dbType) {
            super(id, name, dbType,
                    InstanceState.PROVISIONING,
                    InstanceState.REQUESTED,
                    "PROVISIONING_STARTED");
        }
    }

    public static class InstanceRunning extends InstanceEvent {
        public InstanceRunning(UUID id, String name,
                               DatabaseType dbType) {
            super(id, name, dbType,
                    InstanceState.RUNNING,
                    InstanceState.PROVISIONING,
                    "INSTANCE_RUNNING");
        }
    }

    public static class InstanceStopped extends InstanceEvent {
        public InstanceStopped(UUID id, String name,
                               DatabaseType dbType) {
            super(id, name, dbType,
                    InstanceState.STOPPED,
                    InstanceState.RUNNING,
                    "INSTANCE_STOPPED");
        }
    }

    public static class InstanceFailed extends InstanceEvent {
        private final String reason;

        public InstanceFailed(UUID id, String name,
                              DatabaseType dbType, String reason) {
            super(id, name, dbType,
                    InstanceState.FAILED, null,
                    "INSTANCE_FAILED");
            this.reason = reason;
        }

        public String getReason() { return reason; }
    }

    public static class InstanceDeleted extends InstanceEvent {
        public InstanceDeleted(UUID id, String name,
                               DatabaseType dbType) {
            super(id, name, dbType,
                    InstanceState.DELETED,
                    InstanceState.DELETING,
                    "INSTANCE_DELETED");
        }
    }

    public static class InstanceStateChanged extends InstanceEvent {
        public InstanceStateChanged(UUID id, String name,
                                    DatabaseType dbType,
                                    InstanceState newState,
                                    InstanceState previousState) {
            super(id, name, dbType, newState, previousState,
                    "STATE_CHANGED");
        }
    }

    public static class DriftDetected extends InstanceEvent {
        private final InstanceState correctedTo;

        public DriftDetected(UUID id, String name,
                             DatabaseType dbType,
                             InstanceState driftedFrom,
                             InstanceState correctedTo) {
            super(id, name, dbType, correctedTo,
                    driftedFrom, "DRIFT_DETECTED");
            this.correctedTo = correctedTo;
        }

        public InstanceState getCorrectedTo() {
            return correctedTo;
        }
    }
}