package com.spawnbase.metadata.repository;

import com.spawnbase.common.model.DatabaseType;
import com.spawnbase.common.model.InstanceState;
import com.spawnbase.metadata.entity.Instance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for InstanceRepository.
 *
 * @DataJpaTest loads ONLY the JPA layer.
 * Faster than @SpringBootTest — no web layer loaded.
 * Auto-configures H2 in-memory DB.
 * @Transactional by default — each test rolls back.
 */
@DataJpaTest
@ActiveProfiles("test")
class InstanceRepositoryIntegrationTest {

    @Autowired
    private InstanceRepository instanceRepository;

    private Instance savedInstance;

    @BeforeEach
    void setUp() {
        savedInstance = instanceRepository.save(
                Instance.builder()
                        .name("test-db")
                        .dbType(DatabaseType.POSTGRESQL)
                        .state(InstanceState.REQUESTED)
                        .ownerId("user-001")
                        .build()
        );
    }

    @Test
    @DisplayName("save() → persists instance with auto-generated ID")
    void save_persistsInstance() {
        assertThat(savedInstance.getId()).isNotNull();
        assertThat(savedInstance.getCreatedAt()).isNotNull();
        assertThat(savedInstance.getUpdatedAt()).isNotNull();
        assertThat(savedInstance.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("findById() → returns instance when exists")
    void findById_returnsInstance() {
        Optional<Instance> found = instanceRepository
                .findById(savedInstance.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("test-db");
    }

    @Test
    @DisplayName("findByOwnerId() → returns instances for owner")
    void findByOwnerId_returnsOwnerInstances() {
        instanceRepository.save(Instance.builder()
                .name("test-db-2")
                .dbType(DatabaseType.MYSQL)
                .state(InstanceState.REQUESTED)
                .ownerId("user-001")
                .build());

        List<Instance> instances = instanceRepository
                .findByOwnerId("user-001");

        assertThat(instances).hasSize(2);
        assertThat(instances)
                .extracting(Instance::getName)
                .containsExactlyInAnyOrder("test-db", "test-db-2");
    }

    @Test
    @DisplayName("findByState() → returns only matching state")
    void findByState_returnsMatchingInstances() {
        instanceRepository.save(Instance.builder()
                .name("running-db")
                .dbType(DatabaseType.POSTGRESQL)
                .state(InstanceState.RUNNING)
                .ownerId("user-002")
                .build());

        List<Instance> running = instanceRepository
                .findByState(InstanceState.RUNNING);
        List<Instance> requested = instanceRepository
                .findByState(InstanceState.REQUESTED);

        assertThat(running).hasSize(1);
        assertThat(requested).hasSize(1);
        assertThat(running.get(0).getName())
                .isEqualTo("running-db");
    }

    @Test
    @DisplayName("findActiveByOwnerId() → excludes DELETED instances")
    void findActiveByOwnerId_excludesDeleted() {
        instanceRepository.save(Instance.builder()
                .name("deleted-db")
                .dbType(DatabaseType.POSTGRESQL)
                .state(InstanceState.DELETED)
                .ownerId("user-001")
                .build());

        List<Instance> active = instanceRepository
                .findActiveByOwnerId("user-001");

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getName()).isEqualTo("test-db");
    }

    @Test
    @DisplayName("@Version increments on each update")
    void version_incrementsOnEachUpdate() {
        assertThat(savedInstance.getVersion()).isEqualTo(0L);

        savedInstance.setState(InstanceState.PROVISIONING);
        Instance updated = instanceRepository.saveAndFlush(savedInstance); // ← Fixed

        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findByContainerId() → returns correct instance")
    void findByContainerId_returnsCorrectInstance() {
        savedInstance.setContainerId("abc123container");
        instanceRepository.save(savedInstance);

        Optional<Instance> found = instanceRepository
                .findByContainerId("abc123container");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("test-db");
    }
}