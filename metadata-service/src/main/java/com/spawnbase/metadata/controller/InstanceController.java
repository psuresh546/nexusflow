package com.spawnbase.metadata.controller;

import com.spawnbase.metadata.dto.CreateInstanceRequest;
import com.spawnbase.metadata.dto.InstanceResponse;
import com.spawnbase.metadata.dto.UpdateStateRequest;
import com.spawnbase.metadata.service.InstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/instances")
@Slf4j
@RequiredArgsConstructor
public class InstanceController {

    private final InstanceService instanceService;


    @PostMapping
    public ResponseEntity<InstanceResponse> createInstance(
            @Valid @RequestBody CreateInstanceRequest request) {

        log.info("POST /api/instances — creating {} instance",
                request.getDbType());

        InstanceResponse response =
                instanceService.createInstance(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)  // 201
                .body(response);
    }


    @GetMapping("/{id}")
    public ResponseEntity<InstanceResponse> getInstance(
            @PathVariable UUID id) {

        log.info("GET /api/instances/{}", id);

        return ResponseEntity.ok(
                instanceService.getInstance(id));
    }


    @GetMapping
    public ResponseEntity<List<InstanceResponse>> getInstances(
            @RequestParam String ownerId) {

        log.info("GET /api/instances?ownerId={}", ownerId);

        return ResponseEntity.ok(
                instanceService.getInstancesByOwner(ownerId));
    }


    @PatchMapping("/{id}/state")
    public ResponseEntity<InstanceResponse> updateState(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStateRequest request) {

        log.info("PATCH /api/instances/{}/state → {}",
                id, request.getState());

        return ResponseEntity.ok(
                instanceService.updateState(id, request));
    }


    @GetMapping("/admin/all")
    public ResponseEntity<List<InstanceResponse>> getAllInstances() {

        log.info("GET /api/instances/admin/all");

        return ResponseEntity.ok(
                instanceService.getAllInstances());
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInstance(
            @PathVariable UUID id) {

        log.info("DELETE /api/instances/{}", id);

        instanceService.deleteInstance(id);

        return ResponseEntity.noContent().build(); // 204
    }
}