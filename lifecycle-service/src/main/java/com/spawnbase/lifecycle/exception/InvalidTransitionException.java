package com.spawnbase.lifecycle.exception;

import com.spawnbase.common.model.InstanceState;

/**
 * Thrown when a state transition is attempted that is
 * not defined in the FSM transition table.
 *
 * Example: trying to go from REQUESTED → DELETED directly
 * is illegal. This exception tells the caller exactly why.
 *
 * It extends RuntimeException, so you don't have to
 * declare it in every method signature (unchecked exception).
 */
public class InvalidTransitionException extends RuntimeException {

    private final InstanceState from;
    private final InstanceState to;

    public InvalidTransitionException(InstanceState from, InstanceState to) {
        super(String.format(
                "Invalid state transition: [%s] → [%s]. " +
                        "This transition is not permitted by the lifecycle rules.",
                from, to
        ));
        this.from = from;
        this.to = to;
    }

    public InstanceState getFrom() { return from; }
    public InstanceState getTo()   { return to;   }
}

