package com.spawnbase.metadata.controller;

import com.spawnbase.metadata.event.InstanceEvent;
import com.spawnbase.metadata.event.InstanceEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for instance event history.
 *
 * Used by the admin UI to show the event log timeline
 * for each instance.
 */
@RestController
@RequestMapping("/api/instances")
@Slf4j
@RequiredArgsConstructor
public class EventController {

    private final InstanceEventRepository eventRepository;

    /**
     * GET /api/instances/{id}/events
     *
     * Returns the full event history for an instance.
     * Newest events first — timeline reads top to bottom.
     */
    @GetMapping("/{id}/events")
    public ResponseEntity<List<InstanceEvent>> getEvents(
            @PathVariable UUID id) {

        log.info("GET /api/instances/{}/events", id);

        List<InstanceEvent> events =
                eventRepository
                        .findByInstanceIdOrderByOccurredAtDesc(id);

        return ResponseEntity.ok(events);
    }
}