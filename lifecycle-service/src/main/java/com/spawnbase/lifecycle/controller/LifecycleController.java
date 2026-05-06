package com.spawnbase.lifecycle.controller;

import com.spawnbase.common.model.InstanceState;
import com.spawnbase.lifecycle.dto.InstanceResponse;
import com.spawnbase.lifecycle.dto.TransitionRequest;
import com.spawnbase.lifecycle.service.LifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

/**
 * REST Controller for lifecycle transitions.
 *
 * ALL state changes must go through here.
 * Never call metadata-service PATCH /state directly
 * from outside — always go through lifecycle-service.
 */
@RestController
@RequestMapping("/api/lifecycle")
@Slf4j
@RequiredArgsConstructor
public class LifecycleController {

    private final LifecycleService lifecycleService;

    /**
     * POST /api/lifecycle/instances/{id}/transition
     * Attempt a state transition.
     *
     * This is the ONLY way state should change
     * in production. FSM validates before any DB update.
     */
    @PostMapping("/instances/{id}/transition")
    public ResponseEntity<InstanceResponse> transition(
            @PathVariable UUID id,
            @Valid @RequestBody TransitionRequest request) {

        log.info("POST /api/lifecycle/instances/{}/transition → {}",
                id, request.getTargetState());

        InstanceResponse response =
                lifecycleService.transition(id, request);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/lifecycle/instances/{id}/allowed-transitions
     * Returns all valid next states for this instance.
     * UI uses this to show/hide action buttons.
     */
    @GetMapping("/instances/{id}/allowed-transitions")
    public ResponseEntity<Set<InstanceState>> getAllowedTransitions(
            @PathVariable UUID id) {

        log.info("GET /api/lifecycle/instances/{}/allowed-transitions", id);

        return ResponseEntity.ok(
                lifecycleService.getAllowedTransitions(id));
    }
}