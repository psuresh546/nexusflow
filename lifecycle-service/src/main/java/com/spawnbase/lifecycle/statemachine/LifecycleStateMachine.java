package com.spawnbase.lifecycle.statemachine;

import com.spawnbase.common.model.InstanceState;
import com.spawnbase.lifecycle.exception.InvalidTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class LifecycleStateMachine {

    private static final Logger log =
            LoggerFactory.getLogger(LifecycleStateMachine.class);

    private static final Map<InstanceState, Set<InstanceState>>
            VALID_TRANSITIONS = new EnumMap<>(InstanceState.class);

    static {
        // Entry point
        VALID_TRANSITIONS.put(InstanceState.REQUESTED,
                EnumSet.of(InstanceState.PROVISIONING));

        // Provisioning → success or failure
        VALID_TRANSITIONS.put(InstanceState.PROVISIONING,
                EnumSet.of(InstanceState.RUNNING,
                        InstanceState.FAILED));

        // Running → stop it, restart it, delete it, or it fails
        VALID_TRANSITIONS.put(InstanceState.RUNNING,
                EnumSet.of(InstanceState.STOPPED,
                        InstanceState.RESTARTING,  // docker restart
                        InstanceState.DELETING,
                        InstanceState.FAILED));

        // Stopped → START it (not restart), delete it, or it fails
        VALID_TRANSITIONS.put(InstanceState.STOPPED,
                EnumSet.of(InstanceState.STARTING,    // docker start ← changed
                        InstanceState.DELETING,
                        InstanceState.FAILED));

        // STARTING (from STOPPED) → success or failure
        // This is docker start — container already exists
        VALID_TRANSITIONS.put(InstanceState.STARTING,
                EnumSet.of(InstanceState.RUNNING,
                        InstanceState.FAILED));

        // RESTARTING (from RUNNING) → success or failure
        // This is docker restart — container was running
        VALID_TRANSITIONS.put(InstanceState.RESTARTING,
                EnumSet.of(InstanceState.RUNNING,
                        InstanceState.FAILED));

        // Failed → operator retries or deletes
        VALID_TRANSITIONS.put(InstanceState.FAILED,
                EnumSet.of(InstanceState.REQUESTED,
                        InstanceState.DELETING));

        // Deleting → always ends in DELETED or FAILED
        VALID_TRANSITIONS.put(InstanceState.DELETING,
                EnumSet.of(InstanceState.DELETED,
                        InstanceState.FAILED));

        // Terminal — no exit
        VALID_TRANSITIONS.put(InstanceState.DELETED,
                EnumSet.noneOf(InstanceState.class));
    }


    public InstanceState transition(InstanceState from, InstanceState to) {
        log.info("Attempting transition: [{}] → [{}]", from, to);

        validate(from, to);

        log.info("Transition successful: [{}] → [{}]", from, to);
        return to;
    }


    public void validate(InstanceState from, InstanceState to) {
        Set<InstanceState> allowed = VALID_TRANSITIONS.get(from);

        if (allowed == null || !allowed.contains(to)) {
            log.warn("Illegal transition blocked: [{}] → [{}]", from, to);
            throw new InvalidTransitionException(from, to);
        }
    }


    public Set<InstanceState> getAllowedTransitions(InstanceState from) {
        return VALID_TRANSITIONS.getOrDefault(
                from,
                EnumSet.noneOf(InstanceState.class)
        );
    }


    public boolean isValidTransition(InstanceState from, InstanceState to) {
        try {
            validate(from, to);
            return true;
        } catch (InvalidTransitionException e) {
            return false;
        }
    }
}

