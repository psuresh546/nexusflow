package com.spawnbase.metadata.service;

import com.spawnbase.common.model.InstanceState;
import com.spawnbase.metadata.dto.CreateInstanceRequest;
import com.spawnbase.metadata.dto.InstanceResponse;
import com.spawnbase.metadata.dto.UpdateStateRequest;
import com.spawnbase.metadata.entity.Instance;
import com.spawnbase.metadata.event.InstanceEventPublisher;
import com.spawnbase.metadata.exception.InstanceNotFoundException;
import com.spawnbase.metadata.repository.InstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InstanceService {

    private final InstanceRepository instanceRepository;
    private final InstanceEventPublisher eventPublisher; // ← NEW

    @Transactional
    public InstanceResponse createInstance(CreateInstanceRequest request) {

        log.info("Creating new {} instance '{}' for owner '{}'",
                request.getDbType(),
                request.getName(),
                request.getOwnerId());

        Instance instance = Instance.builder()
                .name(request.getName())
                .dbType(request.getDbType())
                .ownerId(request.getOwnerId())
                .state(InstanceState.REQUESTED)
                .build();

        Instance saved = instanceRepository.save(instance);

        log.info("Instance created with id: {}", saved.getId());

        // Publish CREATED event ← NEW
        eventPublisher.publishCreated(
                saved.getId(),
                saved.getName(),
                saved.getDbType());

        return InstanceResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public InstanceResponse getInstance(UUID id) {
        log.info("Fetching instance: {}", id);

        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() ->
                        new InstanceNotFoundException(id));

        return InstanceResponse.from(instance);
    }

    @Transactional(readOnly = true)
    public List<InstanceResponse> getInstancesByOwner(
            String ownerId) {

        log.info("Fetching active instances for owner: {}",
                ownerId);

        return instanceRepository
                .findActiveByOwnerId(ownerId)
                .stream()
                .map(InstanceResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public InstanceResponse updateState(
            UUID id,
            UpdateStateRequest request) {

        log.info("Updating instance {} state to {}",
                id, request.getState());

        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() ->
                        new InstanceNotFoundException(id));

        // Capture BEFORE updating ← NEW
        InstanceState previousState = instance.getState();

        instance.setState(request.getState());

        if (request.getContainerId() != null) {
            instance.setContainerId(request.getContainerId());
        }

        if (request.getHostPort() != null) {
            instance.setHostPort(request.getHostPort());
        }

        Instance saved = instanceRepository.save(instance);

        log.info("Instance {} state updated to {}",
                id, request.getState());

        // Publish STATE_CHANGED event ← NEW
        eventPublisher.publishStateChanged(
                saved.getId(),
                saved.getName(),
                saved.getDbType(),
                saved.getState(),
                previousState);

        return InstanceResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<InstanceResponse> getAllInstances() {
        log.info("Fetching all instances (admin)");

        return instanceRepository.findAll()
                .stream()
                .map(InstanceResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteInstance(UUID id) {
        log.info("Hard deleting instance record: {}", id);

        Instance instance = instanceRepository.findById(id)
                .orElseThrow(() ->
                        new InstanceNotFoundException(id));

        // Publish STATE_CHANGED event before deletion ← NEW
        eventPublisher.publishStateChanged(
                instance.getId(),
                instance.getName(),
                instance.getDbType(),
                InstanceState.DELETED,
                instance.getState());

        instanceRepository.delete(instance);

        log.info("Instance record {} permanently deleted", id);
    }
}