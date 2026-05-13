package com.spawnbase.metadata.event;

import com.spawnbase.common.event.InstanceEvent;
import com.spawnbase.common.model.DatabaseType;
import com.spawnbase.common.model.InstanceState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes Spring ApplicationEvents for instance
 * lifecycle changes.
 *
 * Uses Spring's built-in ApplicationEventPublisher —
 * zero configuration, zero dependencies.
 *
 * Events are synchronous by default (same thread).
 * Add @Async on listeners to make them asynchronous.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InstanceEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publishCreated(UUID instanceId, String name, DatabaseType dbType) {
        log.debug("Publishing CREATED event: {}", instanceId);
        publisher.publishEvent(
                new InstanceEvent.InstanceCreated(
                        instanceId, name, dbType));
    }

    public void publishStateChanged(UUID instanceId,
                                    String name, DatabaseType dbType,
                                    InstanceState newState,
                                    InstanceState previousState) {
        log.debug("Publishing STATE_CHANGED event: {} → {}",
                previousState, newState);
        publisher.publishEvent(
                new InstanceEvent.InstanceStateChanged(
                        instanceId, name, dbType,
                        newState, previousState));
    }

    public void publishFailed(UUID instanceId,
                              String name, DatabaseType dbType,
                              String reason) {
        log.warn("Publishing FAILED event: {} reason: {}",
                instanceId, reason);
        publisher.publishEvent(
                new InstanceEvent.InstanceFailed(
                        instanceId, name, dbType, reason));
    }

    public void publishDriftDetected(UUID instanceId,
                                     String name, DatabaseType dbType,
                                     InstanceState driftedFrom,
                                     InstanceState correctedTo) {
        log.warn("Publishing DRIFT_DETECTED event: " +
                        "{} {} → {}",
                instanceId, driftedFrom, correctedTo);
        publisher.publishEvent(
                new InstanceEvent.DriftDetected(
                        instanceId, name, dbType,
                        driftedFrom, correctedTo));
    }
}