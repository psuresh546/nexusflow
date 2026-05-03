package com.nexusflow.common.model;

public enum InstanceState {

    REQUESTED,
    PROVISIONING,
    RUNNING,
    STOPPED,
    RESTARTING,
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
                || this == RESTARTING;
    }

    public boolean isActionable() {
        return this != DELETED && this != DELETING;
    }
}