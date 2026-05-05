package com.spawnbase.lifecycle.exception;

import com.spawnbase.common.model.InstanceState;

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

