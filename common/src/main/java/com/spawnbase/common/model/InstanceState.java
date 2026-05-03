package com.spawnbase.common.model;

public enum InstanceState {

    REQUESTED,
    PROVISIONING,
    RUNNING,
    STOPPED,

    STARTING,      // ← NEW: docker start (from STOPPED)
    RESTARTING,    // ← KEPT: docker restart (from RUNNING)

    DELETING,
    DELETED,
    FAILED;

    public boolean isTerminal() {
        return this == DELETED || this == FAILED;
    }

    public boolean containerShouldExist() {
        return this == PROVISIONING
                || this == RUNNING
                || this == STOPPED
                || this == STARTING      // ← add this
                || this == RESTARTING;
    }

    public boolean isActionable() {
        return this != DELETED && this != DELETING;
    }
}

