package com.spawnbase.credential.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "credentials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The SpawnBase instance this credential belongs to.
     * NOT a JPA foreign key — services are independent.
     * Unique constraint: one credential per instance.
     */
    @Column(name = "instance_id",
            nullable = false,
            unique = true,
            updatable = false)
    private UUID instanceId;

    /**
     * Database username.
     * e.g. "spawnbase" for all providers
     */
    @Column(name = "username", nullable = false)
    private String username;

    /**
     * Encrypted password.
     * NEVER store plain text passwords.
     * Encrypted with AES using the key from application.properties.
     */
    @Column(name = "encrypted_password", nullable = false)
    private String encryptedPassword;

    /**
     * Connection URL for this database.
     * e.g. "jdbc:postgresql://localhost:32768/db_9be66e45"
     */
    @Column(name = "connection_url", nullable = false)
    private String connectionUrl;

    /**
     * Host port Docker assigned to this container.
     */
    @Column(name = "host_port", nullable = false)
    private Integer hostPort;

    /**
     * Database name inside the container.
     * e.g. "db_9be66e45"
     */
    @Column(name = "db_name", nullable = false)
    private String dbName;

    @Column(name = "created_at",
            updatable = false,
            nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}