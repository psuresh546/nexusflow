package com.spawnbase.metadata.event;

import com.spawnbase.common.event.InstanceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Listens to Spring ApplicationEvents and persists
 * them to the instance_events table.
 *
 * This creates the audit trail shown in the UI timeline.
 *
 * @EventListener — called synchronously after the
 * publisher's method completes. Transaction is still
 * active so events are saved in the same transaction.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InstanceEventListener {

    private final InstanceEventRepository eventRepository;

    @EventListener
    public void onInstanceEvent(InstanceEvent event) {
        log.info("Event received: {} for instance {}",
                event.getEventType(), event.getInstanceId());

        String message = buildMessage(event);

        com.spawnbase.metadata.event.InstanceEvent record =
                com.spawnbase.metadata.event.InstanceEvent
                        .builder()
                        .instanceId(event.getInstanceId())
                        .instanceName(event.getInstanceName())
                        .dbType(event.getDbType())
                        .eventType(event.getEventType())
                        .newState(event.getNewState())
                        .previousState(event.getPreviousState())
                        .message(message)
                        .occurredAt(LocalDateTime.now())
                        .build();

        eventRepository.save(record);

        log.debug("Event persisted: {} id={}",
                event.getEventType(), record.getId());
    }

    private String buildMessage(InstanceEvent event) {
        return switch (event.getEventType()) {
            case "INSTANCE_CREATED" ->
                    "Instance created — waiting for provisioning";
            case "PROVISIONING_STARTED" ->
                    "Provisioning started — pulling Docker image";
            case "INSTANCE_RUNNING" ->
                    "Container is running and healthy";
            case "INSTANCE_STOPPED" ->
                    "Container stopped gracefully";
            case "INSTANCE_FAILED" -> {
                if (event instanceof
                        InstanceEvent.InstanceFailed f) {
                    yield "Instance failed: " + f.getReason();
                }
                yield "Instance entered FAILED state";
            }
            case "INSTANCE_DELETED" ->
                    "Container removed — instance deleted";
            case "DRIFT_DETECTED" ->
                    "Drift detected and auto-corrected by " +
                            "drift detector";
            default ->
                    "State changed: " +
                            event.getPreviousState() +
                            " → " + event.getNewState();
        };
    }
}