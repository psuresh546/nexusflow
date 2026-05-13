package com.spawnbase.metadata.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstanceEventRepository extends JpaRepository<InstanceEvent, UUID> {

    /**
     * Get all events for an instance, newest first.
     * Used by the admin UI event log timeline.
     */
    List<InstanceEvent> findByInstanceIdOrderByOccurredAtDesc(UUID instanceId);
}