package com.spawnbase.lifecycle.service;

import com.spawnbase.lifecycle.client.MetadataClient;
import com.spawnbase.lifecycle.dto.InstanceResponse;
import com.spawnbase.lifecycle.dto.TransitionRequest;
import com.spawnbase.lifecycle.exception.InvalidTransitionException;
import com.spawnbase.lifecycle.statemachine.LifecycleStateMachine;
import com.spawnbase.common.model.InstanceState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Core lifecycle business logic.
 *
 * Flow for every state transition:
 * 1. Fetch current state from metadata-service
 * 2. Validate transition via LifecycleStateMachine
 * 3. If valid → tell metadata-service to update
 * 4. If invalid → throw InvalidTransitionException
 *
 * This is the class that gives the FSM real teeth.
 * No state change happens without going through here.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LifecycleService {

    private final LifecycleStateMachine stateMachine;
    private final MetadataClient metadataClient;

    /**
     * Attempts a state transition for an instance.
     *
     * @param instanceId  the instance to transition
     * @param request     contains targetState + optional Docker info
     * @return            updated instance response
     * @throws InvalidTransitionException if transition is illegal
     */
    public InstanceResponse transition(
            UUID instanceId,
            TransitionRequest request) {

        log.info("Transition requested for instance {}: → {}",
                instanceId, request.getTargetState());

        // Step 1 — Get current state from metadata-service
        InstanceResponse current =
                metadataClient.getInstance(instanceId);

        InstanceState currentState = current.getState();
        InstanceState targetState = request.getTargetState();

        log.info("Instance {} current state: {}", instanceId, currentState);

        // Step 2 — Validate via FSM
        // Throws InvalidTransitionException if illegal
        stateMachine.transition(currentState, targetState);

        // Step 3 — FSM approved. Tell metadata-service to update.
        InstanceResponse updated = metadataClient.updateState(
                instanceId,
                targetState,
                request.getContainerId(),
                request.getHostPort()
        );

        log.info("Instance {} successfully transitioned: {} → {}",
                instanceId, currentState, targetState);

        return updated;
    }

    /**
     * Returns all valid next states for an instance.
     * Used by UI to show only valid action buttons.
     */
    public java.util.Set<InstanceState> getAllowedTransitions(
            UUID instanceId) {

        InstanceResponse current =
                metadataClient.getInstance(instanceId);

        return stateMachine.getAllowedTransitions(current.getState());
    }
}