package com.spawnbase.metadata.entity;

import com.spawnbase.common.model.InstanceState;
import com.spawnbase.provisioning.model.DatabaseType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "instances")
@Data                   // generates getters, setters, toString, equals, hashCode
@Builder                // enables Instance.builder().name("mydb").build()
@NoArgsConstructor      // JPA requires a no-arg constructor
@AllArgsConstructor     // Builder needs an all-arg constructor
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


    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}