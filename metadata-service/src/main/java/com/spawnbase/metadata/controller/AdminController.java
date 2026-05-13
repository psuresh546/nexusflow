package com.spawnbase.metadata.controller;

import com.spawnbase.common.model.DatabaseType;
import com.spawnbase.common.model.InstanceState;
import com.spawnbase.metadata.dto.DashboardSummary;
import com.spawnbase.metadata.dto.InstanceResponse;
import com.spawnbase.metadata.dto.PagedResponse;
import com.spawnbase.metadata.entity.Instance;
import com.spawnbase.metadata.event.InstanceEvent;
import com.spawnbase.metadata.event.InstanceEventRepository;
import com.spawnbase.metadata.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin dashboard API.
 *
 * These endpoints are for the React admin UI.
 * They support pagination and filtering so the UI
 * can display large numbers of instances efficiently.
 *
 * All endpoints require ADMIN role in production
 * (Day 20 — JWT RBAC enforcement).
 */
@RestController
@RequestMapping("/api/admin")
@Slf4j
@RequiredArgsConstructor
public class AdminController {

    private final InstanceRepository instanceRepository;
    private final InstanceEventRepository eventRepository;

    // ─────────────────────────────────────────
    // DASHBOARD SUMMARY
    // ─────────────────────────────────────────

    /**
     * GET /api/admin/dashboard
     *
     * Returns summary statistics for the dashboard
     * header cards — total, running, failed counts
     * and breakdowns by type and state.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummary> getDashboard() {
        log.info("GET /api/admin/dashboard");

        List<Instance> all = instanceRepository.findAll();

        // Count by state
        Map<String, Long> byState = all.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getState().name(),
                        Collectors.counting()));

        // Count by DB type
        Map<String, Long> byDbType = all.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getDbType().name(),
                        Collectors.counting()));

        DashboardSummary summary = DashboardSummary.builder()
                .totalInstances(all.size())
                .runningInstances(
                        byState.getOrDefault("RUNNING", 0L))
                .failedInstances(
                        byState.getOrDefault("FAILED", 0L))
                .provisioningInstances(
                        byState.getOrDefault("PROVISIONING", 0L))
                .stoppedInstances(
                        byState.getOrDefault("STOPPED", 0L))
                .byDbType(byDbType)
                .byState(byState)
                .build();

        return ResponseEntity.ok(summary);
    }

    // ─────────────────────────────────────────
    // PAGINATED INSTANCE LIST
    // ─────────────────────────────────────────

    /**
     * GET /api/admin/instances
     *
     * Paginated list with optional filtering.
     *
     * Query params:
     * - state=RUNNING          filter by state
     * - dbType=POSTGRESQL      filter by DB type
     * - page=0                 page number (0-based)
     * - size=20                page size
     * - sort=createdAt,desc    sort field and direction
     *
     * Examples:
     * GET /api/admin/instances?state=RUNNING&page=0&size=10
     * GET /api/admin/instances?dbType=MYSQL&sort=name,asc
     */
    @GetMapping("/instances")
    public ResponseEntity<PagedResponse<InstanceResponse>>
    getInstances(
            @RequestParam(required = false)
            InstanceState state,
            @RequestParam(required = false)
            DatabaseType dbType,
            @RequestParam(defaultValue = "0")
            int page,
            @RequestParam(defaultValue = "20")
            int size,
            @RequestParam(defaultValue = "createdAt")
            String sortBy,
            @RequestParam(defaultValue = "desc")
            String sortDir) {

        log.info("GET /api/admin/instances " +
                        "state={} dbType={} page={} size={}",
                state, dbType, page, size);

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Instance> resultPage;

        if (state != null && dbType != null) {
            resultPage = instanceRepository
                    .findByStateAndDbType(state, dbType, pageable);
        } else if (state != null) {
            resultPage = instanceRepository
                    .findByState(state, pageable);
        } else if (dbType != null) {
            resultPage = instanceRepository
                    .findByDbType(dbType, pageable);
        } else {
            resultPage = instanceRepository
                    .findAll(pageable);
        }

        Page<InstanceResponse> responsePage =
                resultPage.map(InstanceResponse::from);

        return ResponseEntity.ok(
                PagedResponse.from(responsePage));
    }

    // ─────────────────────────────────────────
    // INSTANCE EVENTS
    // ─────────────────────────────────────────

    /**
     * GET /api/admin/instances/{id}/events
     *
     * Full event history for one instance.
     * Used by the UI instance detail panel.
     */
    @GetMapping("/instances/{id}/events")
    public ResponseEntity<List<InstanceEvent>> getEvents(
            @PathVariable UUID id) {

        log.info("GET /api/admin/instances/{}/events", id);

        return ResponseEntity.ok(
                eventRepository
                        .findByInstanceIdOrderByOccurredAtDesc(id));
    }

    // ─────────────────────────────────────────
    // SUPPORTED TYPES (for UI dropdowns)
    // ─────────────────────────────────────────

    /**
     * GET /api/admin/database-types
     *
     * Returns all supported DB types for the
     * Create Instance form dropdown.
     */
    @GetMapping("/database-types")
    public ResponseEntity<List<String>> getDatabaseTypes() {
        return ResponseEntity.ok(
                Arrays.stream(DatabaseType.values())
                        .map(Enum::name)
                        .collect(Collectors.toList()));
    }

    /**
     * GET /api/admin/states
     *
     * Returns all possible instance states for
     * the filter dropdown.
     */
    @GetMapping("/states")
    public ResponseEntity<List<String>> getStates() {
        return ResponseEntity.ok(
                Arrays.stream(InstanceState.values())
                        .map(Enum::name)
                        .collect(Collectors.toList()));
    }
}