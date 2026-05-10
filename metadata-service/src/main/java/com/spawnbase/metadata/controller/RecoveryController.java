package com.spawnbase.metadata.controller;

import com.spawnbase.metadata.dto.InstanceResponse;
import com.spawnbase.metadata.dto.UpdateStateRequest;
import com.spawnbase.metadata.service.InstanceService;
import com.spawnbase.common.model.InstanceState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/instances")
@Slf4j
@RequiredArgsConstructor
public class RecoveryController {

    private final InstanceService instanceService;

    /**
     * POST /api/instances/{id}/recover
     *
     * Resets a FAILED instance back to REQUESTED
     * so provisioning can be retried.
     *
     * The FSM allows: FAILED → REQUESTED
     * This endpoint enforces that — you cannot
     * recover a RUNNING or DELETED instance.
     */
    @PostMapping("/{id}/recover")
    public ResponseEntity<Map<String, Object>> recover(
            @PathVariable UUID id) {

        log.info("Recovery requested for instance: {}", id);

        // Get current instance
        InstanceResponse current =
                instanceService.getInstance(id);

        // Only FAILED instances can be recovered
        if (current.getState() != InstanceState.FAILED) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Cannot recover instance in state: "
                            + current.getState(),
                    "message", "Only FAILED instances can be recovered",
                    "currentState", current.getState().name()
            ));
        }

        // Reset to REQUESTED — operator will re-provision
        UpdateStateRequest resetRequest = new UpdateStateRequest();
        resetRequest.setState(InstanceState.REQUESTED);

        InstanceResponse recovered =
                instanceService.updateState(id, resetRequest);

        log.info("Instance {} recovered: FAILED → REQUESTED", id);

        return ResponseEntity.ok(Map.of(
                "message", "Instance recovered successfully",
                "instanceId", id.toString(),
                "previousState", "FAILED",
                "currentState", recovered.getState().name(),
                "nextStep", "Call POST /api/provisioning/instances/"
                        + id + "/provision to retry"
        ));
    }

    /**
     * GET /api/instances/{id}/status
     *
     * Returns a detailed status summary of an instance.
     * Useful for operators to understand current state.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> status(
            @PathVariable UUID id) {

        InstanceResponse instance =
                instanceService.getInstance(id);

        boolean isTerminal = instance.getState().isTerminal();
        boolean containerExpected =
                instance.getState().containerShouldExist();
        boolean isActionable =
                instance.getState().isActionable();

        return ResponseEntity.ok(Map.of(
                "instanceId", id.toString(),
                "name", instance.getName(),
                "state", instance.getState().name(),
                "dbType", instance.getDbType().name(),
                "hostPort", instance.getHostPort() != null
                        ? instance.getHostPort() : "not assigned",
                "isTerminal", isTerminal,
                "containerExpected", containerExpected,
                "isActionable", isActionable,
                "createdAt", instance.getCreatedAt().toString(),
                "updatedAt", instance.getUpdatedAt().toString()
        ));
    }
}