package com.spawnbase.lifecycle.service;

import com.spawnbase.common.model.InstanceState;
import com.spawnbase.lifecycle.client.MetadataClient;
import com.spawnbase.lifecycle.dto.InstanceResponse;
import com.spawnbase.lifecycle.dto.TransitionRequest;
import com.spawnbase.lifecycle.exception.InvalidTransitionException;
import com.spawnbase.lifecycle.statemachine.LifecycleStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class LifecycleServiceTest {

    @Mock
    private MetadataClient metadataClient;

    @Mock
    private LifecycleStateMachine stateMachine;

    @InjectMocks
    private LifecycleService lifecycleService;

    private UUID instanceId;
    private InstanceResponse currentInstance;

    @BeforeEach
    void setUp() {
        instanceId = UUID.randomUUID();

        // Build a fake instance response
        currentInstance = new InstanceResponse();
        currentInstance.setId(instanceId);
        currentInstance.setState(InstanceState.REQUESTED);
    }

    @Test
    @DisplayName("Valid transition — calls FSM, updates metadata, returns response")
    void transition_valid_succeeds() {
        // ARRANGE
        TransitionRequest request = new TransitionRequest();
        request.setTargetState(InstanceState.PROVISIONING);

        InstanceResponse updatedInstance = new InstanceResponse();
        updatedInstance.setId(instanceId);
        updatedInstance.setState(InstanceState.PROVISIONING);

        // Tell fakes what to return
        when(metadataClient.getInstance(instanceId))
                .thenReturn(currentInstance);
        when(stateMachine.transition(
                InstanceState.REQUESTED,
                InstanceState.PROVISIONING))
                .thenReturn(InstanceState.PROVISIONING);
        when(metadataClient.updateState(
                eq(instanceId),
                eq(InstanceState.PROVISIONING),
                any(), any()))
                .thenReturn(updatedInstance);

        // ACT
        InstanceResponse result =
                lifecycleService.transition(instanceId, request);

        // ASSERT
        assertThat(result.getState())
                .isEqualTo(InstanceState.PROVISIONING);

        // Verify interactions
        verify(metadataClient, times(1)).getInstance(instanceId);
        verify(stateMachine, times(1)).transition(
                InstanceState.REQUESTED, InstanceState.PROVISIONING);
        verify(metadataClient, times(1)).updateState(
                eq(instanceId), eq(InstanceState.PROVISIONING), any(), any());
    }

    @Test
    @DisplayName("Invalid transition — FSM throws, metadata never updated")
    void transition_invalid_throws_and_never_updates() {
        // ARRANGE
        TransitionRequest request = new TransitionRequest();
        request.setTargetState(InstanceState.DELETED);

        when(metadataClient.getInstance(instanceId))
                .thenReturn(currentInstance);
        when(stateMachine.transition(
                InstanceState.REQUESTED,
                InstanceState.DELETED))
                .thenThrow(new InvalidTransitionException(
                        InstanceState.REQUESTED, InstanceState.DELETED));

        // ACT + ASSERT
        assertThatThrownBy(() ->
                lifecycleService.transition(instanceId, request))
                .isInstanceOf(InvalidTransitionException.class);

        // CRITICAL: verify metadata was NEVER updated
        verify(metadataClient, never()).updateState(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("Transition reads current state from metadata — never trusts caller")
    void transition_always_reads_current_state() {
        // ARRANGE
        TransitionRequest request = new TransitionRequest();
        request.setTargetState(InstanceState.PROVISIONING);

        when(metadataClient.getInstance(instanceId))
                .thenReturn(currentInstance);
        when(stateMachine.transition(any(), any()))
                .thenReturn(InstanceState.PROVISIONING);
        when(metadataClient.updateState(any(), any(), any(), any()))
                .thenReturn(currentInstance);

        // ACT
        lifecycleService.transition(instanceId, request);

        // ASSERT — getInstance MUST be called
        // We never trust the caller's version of current state
        verify(metadataClient, times(1)).getInstance(instanceId);
    }
}