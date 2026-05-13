package com.spawnbase.metadata.event;

import com.spawnbase.common.model.DatabaseType;
import com.spawnbase.common.model.InstanceState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persisted record of every lifecycle event.
 *
 * This is the event log shown in the admin UI timeline.
 * Every state change, failure, drift correction
 * is stored here permanently.
 *
 * Read-only after creation — events are immutable.
 * Never update or delete event records.
 */
@Entity
@Table(name = "instance_events",
        indexes = @Index(
                name = "idx_instance_events_instance_id",
                columnList = "instance_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstanceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;

    @Column(name = "instance_name", nullable = false)
    private String instanceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "db_type", nullable = false)
    private DatabaseType dbType;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_state", nullable = false)
    private InstanceState newState;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_state")
    private InstanceState previousState;

    @Column(name = "message")
    private String message;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}