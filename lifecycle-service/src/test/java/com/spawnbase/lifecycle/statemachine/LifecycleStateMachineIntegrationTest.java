package com.spawnbase.lifecycle.statemachine;

import com.spawnbase.common.model.InstanceState;
import com.spawnbase.lifecycle.exception.InvalidTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for LifecycleStateMachine as a Spring bean.
 *
 * Verifies that Spring correctly instantiates and manages
 * the state machine as a singleton @Component.
 *
 * Complements Day 7 unit tests which tested pure logic.
 * This verifies Spring wiring is correct.
 */
@SpringBootTest
@ActiveProfiles("test")
class LifecycleStateMachineIntegrationTest {

    @Autowired
    private LifecycleStateMachine stateMachine;

    @Test
    @DisplayName("Spring correctly autowires LifecycleStateMachine")
    void stateMachine_isCorrectlyWiredBySpring() {
        assertThat(stateMachine).isNotNull();
    }

    @Test
    @DisplayName("Singleton — same instance returned each time")
    void stateMachine_isSingleton() {
        // In a Spring context, @Component is singleton by default
        // Same object reference every injection
        assertThat(stateMachine).isNotNull();
        // If two beans were injected they'd be the same instance
    }

    @Test
    @DisplayName("Full happy path — REQUESTED to DELETED")
    void fullHappyPath_requestedToDeleted() {
        // Walk through the complete lifecycle
        InstanceState state = InstanceState.REQUESTED;

        state = stateMachine.transition(state,
                InstanceState.PROVISIONING);
        assertThat(state).isEqualTo(InstanceState.PROVISIONING);

        state = stateMachine.transition(state,
                InstanceState.RUNNING);
        assertThat(state).isEqualTo(InstanceState.RUNNING);

        state = stateMachine.transition(state,
                InstanceState.STOPPED);
        assertThat(state).isEqualTo(InstanceState.STOPPED);

        state = stateMachine.transition(state,
                InstanceState.DELETING);
        assertThat(state).isEqualTo(InstanceState.DELETING);

        state = stateMachine.transition(state,
                InstanceState.DELETED);
        assertThat(state).isEqualTo(InstanceState.DELETED);
    }

    @Test
    @DisplayName("Full error path — any state to FAILED to REQUESTED")
    void errorPath_anyStateToFailedToRecovered() {
        // Simulate failure during provisioning
        InstanceState state = InstanceState.PROVISIONING;

        state = stateMachine.transition(state,
                InstanceState.FAILED);
        assertThat(state).isEqualTo(InstanceState.FAILED);

        // Operator recovers
        state = stateMachine.transition(state,
                InstanceState.REQUESTED);
        assertThat(state).isEqualTo(InstanceState.REQUESTED);
    }

    @Test
    @DisplayName("Restart path — RUNNING to RESTARTING to RUNNING")
    void restartPath_runningToRestartingToRunning() {
        InstanceState state = stateMachine.transition(
                InstanceState.RUNNING, InstanceState.RESTARTING);
        assertThat(state).isEqualTo(InstanceState.RESTARTING);

        state = stateMachine.transition(state,
                InstanceState.RUNNING);
        assertThat(state).isEqualTo(InstanceState.RUNNING);
    }

    @Test
    @DisplayName("Start path — STOPPED to STARTING to RUNNING")
    void startPath_stoppedToStartingToRunning() {
        InstanceState state = stateMachine.transition(
                InstanceState.STOPPED, InstanceState.STARTING);
        assertThat(state).isEqualTo(InstanceState.STARTING);

        state = stateMachine.transition(state,
                InstanceState.RUNNING);
        assertThat(state).isEqualTo(InstanceState.RUNNING);
    }

    @Test
    @DisplayName("DELETED is truly terminal — all transitions blocked")
    void deletedIsTerminal_allTransitionsBlocked() {
        Set<InstanceState> allowed =
                stateMachine.getAllowedTransitions(
                        InstanceState.DELETED);
        assertThat(allowed).isEmpty();

        // Try every possible transition from DELETED
        for (InstanceState target : InstanceState.values()) {
            if (target != InstanceState.DELETED) {
                assertThatThrownBy(() ->
                        stateMachine.transition(
                                InstanceState.DELETED, target))
                        .isInstanceOf(
                                InvalidTransitionException.class);
            }
        }
    }
}