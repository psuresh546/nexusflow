package com.spawnbase.lifecycle.statemachine;

import com.spawnbase.common.model.InstanceState;
import com.spawnbase.lifecycle.exception.InvalidTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class LifecycleStateMachineTest {

    // No @Mock needed — LifecycleStateMachine has no dependencies
    private LifecycleStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        // Create a fresh instance before each test
        stateMachine = new LifecycleStateMachine();
    }

    // ─────────────────────────────────────────
    // VALID TRANSITIONS
    // ─────────────────────────────────────────

    @Nested
    @DisplayName("Valid transitions should succeed")
    class ValidTransitions {

        @Test
        @DisplayName("REQUESTED → PROVISIONING")
        void requested_to_provisioning() {
            InstanceState result = stateMachine.transition(
                    InstanceState.REQUESTED,
                    InstanceState.PROVISIONING
            );
            assertThat(result).isEqualTo(InstanceState.PROVISIONING);
        }

        @Test
        @DisplayName("PROVISIONING → RUNNING")
        void provisioning_to_running() {
            InstanceState result = stateMachine.transition(
                    InstanceState.PROVISIONING,
                    InstanceState.RUNNING
            );
            assertThat(result).isEqualTo(InstanceState.RUNNING);
        }

        @Test
        @DisplayName("PROVISIONING → FAILED")
        void provisioning_to_failed() {
            InstanceState result = stateMachine.transition(
                    InstanceState.PROVISIONING,
                    InstanceState.FAILED
            );
            assertThat(result).isEqualTo(InstanceState.FAILED);
        }

        @Test
        @DisplayName("RUNNING → STOPPED")
        void running_to_stopped() {
            assertThat(stateMachine.transition(
                    InstanceState.RUNNING, InstanceState.STOPPED))
                    .isEqualTo(InstanceState.STOPPED);
        }

        @Test
        @DisplayName("RUNNING → RESTARTING")
        void running_to_restarting() {
            assertThat(stateMachine.transition(
                    InstanceState.RUNNING, InstanceState.RESTARTING))
                    .isEqualTo(InstanceState.RESTARTING);
        }

        @Test
        @DisplayName("RUNNING → DELETING")
        void running_to_deleting() {
            assertThat(stateMachine.transition(
                    InstanceState.RUNNING, InstanceState.DELETING))
                    .isEqualTo(InstanceState.DELETING);
        }

        @Test
        @DisplayName("STOPPED → STARTING")
        void stopped_to_starting() {
            assertThat(stateMachine.transition(
                    InstanceState.STOPPED, InstanceState.STARTING))
                    .isEqualTo(InstanceState.STARTING);
        }

        @Test
        @DisplayName("STOPPED → DELETING")
        void stopped_to_deleting() {
            assertThat(stateMachine.transition(
                    InstanceState.STOPPED, InstanceState.DELETING))
                    .isEqualTo(InstanceState.DELETING);
        }

        @Test
        @DisplayName("STARTING → RUNNING")
        void starting_to_running() {
            assertThat(stateMachine.transition(
                    InstanceState.STARTING, InstanceState.RUNNING))
                    .isEqualTo(InstanceState.RUNNING);
        }

        @Test
        @DisplayName("RESTARTING → RUNNING")
        void restarting_to_running() {
            assertThat(stateMachine.transition(
                    InstanceState.RESTARTING, InstanceState.RUNNING))
                    .isEqualTo(InstanceState.RUNNING);
        }

        @Test
        @DisplayName("FAILED → REQUESTED (operator recovery)")
        void failed_to_requested() {
            assertThat(stateMachine.transition(
                    InstanceState.FAILED, InstanceState.REQUESTED))
                    .isEqualTo(InstanceState.REQUESTED);
        }

        @Test
        @DisplayName("FAILED → DELETING (operator gives up)")
        void failed_to_deleting() {
            assertThat(stateMachine.transition(
                    InstanceState.FAILED, InstanceState.DELETING))
                    .isEqualTo(InstanceState.DELETING);
        }

        @Test
        @DisplayName("DELETING → DELETED")
        void deleting_to_deleted() {
            assertThat(stateMachine.transition(
                    InstanceState.DELETING, InstanceState.DELETED))
                    .isEqualTo(InstanceState.DELETED);
        }
    }

    // ─────────────────────────────────────────
    // INVALID TRANSITIONS
    // ─────────────────────────────────────────

    @Nested
    @DisplayName("Invalid transitions should throw InvalidTransitionException")
    class InvalidTransitions {

        @Test
        @DisplayName("REQUESTED → DELETED is illegal")
        void requested_to_deleted_throws() {
            assertThatThrownBy(() ->
                    stateMachine.transition(
                            InstanceState.REQUESTED,
                            InstanceState.DELETED))
                    .isInstanceOf(InvalidTransitionException.class)
                    .hasMessageContaining("REQUESTED")
                    .hasMessageContaining("DELETED");
        }

        @Test
        @DisplayName("REQUESTED → RUNNING is illegal")
        void requested_to_running_throws() {
            assertThatThrownBy(() ->
                    stateMachine.transition(
                            InstanceState.REQUESTED,
                            InstanceState.RUNNING))
                    .isInstanceOf(InvalidTransitionException.class);
        }

        @Test
        @DisplayName("RUNNING → REQUESTED is illegal")
        void running_to_requested_throws() {
            assertThatThrownBy(() ->
                    stateMachine.transition(
                            InstanceState.RUNNING,
                            InstanceState.REQUESTED))
                    .isInstanceOf(InvalidTransitionException.class);
        }

        @Test
        @DisplayName("STOPPED → RUNNING is illegal — must go through STARTING")
        void stopped_to_running_directly_throws() {
            assertThatThrownBy(() ->
                    stateMachine.transition(
                            InstanceState.STOPPED,
                            InstanceState.RUNNING))
                    .isInstanceOf(InvalidTransitionException.class)
                    .hasMessageContaining("STOPPED")
                    .hasMessageContaining("RUNNING");
        }

        @Test
        @DisplayName("PROVISIONING → DELETED is illegal")
        void provisioning_to_deleted_throws() {
            assertThatThrownBy(() ->
                    stateMachine.transition(
                            InstanceState.PROVISIONING,
                            InstanceState.DELETED))
                    .isInstanceOf(InvalidTransitionException.class);
        }
    }

    // ─────────────────────────────────────────
    // TERMINAL STATE
    // ─────────────────────────────────────────

    @Nested
    @DisplayName("Terminal state DELETED — no transitions allowed")
    class TerminalState {

        @Test
        @DisplayName("DELETED → REQUESTED throws")
        void deleted_to_requested_throws() {
            assertThatThrownBy(() ->
                    stateMachine.transition(
                            InstanceState.DELETED,
                            InstanceState.REQUESTED))
                    .isInstanceOf(InvalidTransitionException.class);
        }

        @Test
        @DisplayName("DELETED → RUNNING throws")
        void deleted_to_running_throws() {
            assertThatThrownBy(() ->
                    stateMachine.transition(
                            InstanceState.DELETED,
                            InstanceState.RUNNING))
                    .isInstanceOf(InvalidTransitionException.class);
        }

        @Test
        @DisplayName("DELETED has no allowed transitions")
        void deleted_has_no_allowed_transitions() {
            Set<InstanceState> allowed =
                    stateMachine.getAllowedTransitions(InstanceState.DELETED);
            assertThat(allowed).isEmpty();
        }
    }

    // ─────────────────────────────────────────
    // ALLOWED TRANSITIONS
    // ─────────────────────────────────────────

    @Nested
    @DisplayName("getAllowedTransitions returns correct sets")
    class AllowedTransitions {

        @Test
        @DisplayName("RUNNING can go to STOPPED, RESTARTING, DELETING, FAILED")
        void running_allowed_transitions() {
            Set<InstanceState> allowed =
                    stateMachine.getAllowedTransitions(InstanceState.RUNNING);

            assertThat(allowed).containsExactlyInAnyOrder(
                    InstanceState.STOPPED,
                    InstanceState.RESTARTING,
                    InstanceState.DELETING,
                    InstanceState.FAILED
            );
        }

        @Test
        @DisplayName("PROVISIONING can go to RUNNING or FAILED")
        void provisioning_allowed_transitions() {
            Set<InstanceState> allowed =
                    stateMachine.getAllowedTransitions(InstanceState.PROVISIONING);

            assertThat(allowed).containsExactlyInAnyOrder(
                    InstanceState.RUNNING,
                    InstanceState.FAILED
            );
        }

        @Test
        @DisplayName("REQUESTED can only go to PROVISIONING")
        void requested_allowed_transitions() {
            Set<InstanceState> allowed =
                    stateMachine.getAllowedTransitions(InstanceState.REQUESTED);

            assertThat(allowed).containsExactly(
                    InstanceState.PROVISIONING
            );
        }
    }

    // ─────────────────────────────────────────
    // isValidTransition helper
    // ─────────────────────────────────────────

    @Nested
    @DisplayName("isValidTransition returns correct boolean")
    class IsValidTransition {

        @Test
        @DisplayName("Valid transition returns true")
        void valid_returns_true() {
            assertThat(stateMachine.isValidTransition(
                    InstanceState.REQUESTED,
                    InstanceState.PROVISIONING))
                    .isTrue();
        }

        @Test
        @DisplayName("Invalid transition returns false")
        void invalid_returns_false() {
            assertThat(stateMachine.isValidTransition(
                    InstanceState.REQUESTED,
                    InstanceState.DELETED))
                    .isFalse();
        }
    }
}