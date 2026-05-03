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

/**
 * The core FSM engine for SpawnBase.
 *
 * All valid state transitions are declared in one place —
 * the VALID_TRANSITIONS map. No if-else chains anywhere.
 *
 * How to use:
 *   InstanceState next = stateMachine.transition(current, target);
 *
 * If the transition is invalid → throws InvalidTransitionException
 * If the transition is valid   → returns the target state
 *
 * @Component makes Spring manage this as a singleton bean.
 * One instance, shared across the entire lifecycle-service.
 */
@Component
public class LifecycleStateMachine {

    private static final Logger log =
            LoggerFactory.getLogger(LifecycleStateMachine.class);

    /**
     * THE TRANSITION TABLE — The single source of truth.
     *
     * Read it as: "From state X, you are allowed to go to states Y, Z..."
     * Any transition NOT listed here is ILLEGAL.
     *
     * EnumMap is used instead of HashMap because:
     * 1. Keys are enums — EnumMap is O(1) and more memory efficient
     * 2. Iteration order follows enum declaration order
     * 3. It's the correct collection for enum-keyed maps
     */
    private static final Map<InstanceState, Set<InstanceState>>
            VALID_TRANSITIONS = new EnumMap<>(InstanceState.class);

    /*
     * Static initializer — runs once when the class loads.
     * Builds the entire transition table upfront.
     */
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

    /**
     * Attempts a state transition from → to.
     *
     * @param from  the current state of the instance
     * @param to    the desired next state
     * @return      the new state (same as 'to') if transition is valid
     * @throws InvalidTransitionException if the transition is not allowed
     */
    public InstanceState transition(InstanceState from, InstanceState to) {
        log.info("Attempting transition: [{}] → [{}]", from, to);

        validate(from, to);

        log.info("Transition successful: [{}] → [{}]", from, to);
        return to;
    }

    /**
     * Validates whether a transition is allowed.
     * Throws InvalidTransitionException if not.
     *
     * Separated from transition() so other services can
     * check validity WITHOUT actually performing the transition.
     */
    public void validate(InstanceState from, InstanceState to) {
        Set<InstanceState> allowed = VALID_TRANSITIONS.get(from);

        if (allowed == null || !allowed.contains(to)) {
            log.warn("Illegal transition blocked: [{}] → [{}]", from, to);
            throw new InvalidTransitionException(from, to);
        }
    }

    /**
     * Returns all valid next states from a given state.
     * Useful for the UI to show only valid action buttons.
     *
     * Example: if instance is RUNNING, UI shows: Stop | Restart | Delete
     *          if instance is DELETED, UI shows: nothing
     */
    public Set<InstanceState> getAllowedTransitions(InstanceState from) {
        return VALID_TRANSITIONS.getOrDefault(
                from,
                EnumSet.noneOf(InstanceState.class)
        );
    }

    /**
     * Checks if a specific transition is valid without throwing.
     * Useful for conditional UI rendering or pre-checks.
     */
    public boolean isValidTransition(InstanceState from, InstanceState to) {
        try {
            validate(from, to);
            return true;
        } catch (InvalidTransitionException e) {
            return false;
        }
    }
}

