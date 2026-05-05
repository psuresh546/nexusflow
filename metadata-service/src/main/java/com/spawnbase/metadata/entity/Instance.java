package com.spawnbase.metadata.entity;

import com.spawnbase.common.model.InstanceState;
import com.spawnbase.provisioning.model.DatabaseType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "instances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Instance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "db_type", nullable = false, length = 20)
    private DatabaseType dbType;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private InstanceState state;

    @Column(name = "container_id", length = 64)
    private String containerId;

    @Column(name = "host_port")
    private Integer hostPort;

    @Column(name = "owner_id", nullable = false, length = 100)
    private String ownerId;

    /**
     * OPTIMISTIC LOCKING
     * JPA increments this on every UPDATE.
     * Prevents silent overwrites when two transactions
     * update the same row simultaneously.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Set automatically by @PrePersist before INSERT.
     * updatable=false ensures it never changes after creation.
     */
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /**
     * Updated automatically by @PreUpdate before every UPDATE.
     * Tells you exactly when the last state change happened.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Set when instance transitions to DELETING.
     * null for all active instances.
     * Soft delete timestamp.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * JPA Lifecycle Callback — fires before every INSERT.
     * Sets both timestamps on first save.
     * More reliable than @CreationTimestamp with Lombok @Builder.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * JPA Lifecycle Callback — fires before every UPDATE.
     * Keeps updatedAt always fresh.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}