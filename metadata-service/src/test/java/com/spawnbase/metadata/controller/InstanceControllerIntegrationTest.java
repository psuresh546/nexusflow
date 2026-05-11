package com.spawnbase.metadata.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spawnbase.common.model.DatabaseType;
import com.spawnbase.common.model.InstanceState;
import com.spawnbase.metadata.dto.CreateInstanceRequest;
import com.spawnbase.metadata.dto.UpdateStateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for InstanceController.
 *
 * @SpringBootTest loads the FULL Spring context.
 * @AutoConfigureMockMvc gives us MockMvc to make HTTP calls.
 * @ActiveProfiles("test") uses application-test.properties → H2.
 * @Transactional rolls back each test — clean slate every time.
 *
 * Tests the full stack:
 * HTTP → Controller → Service → Repository → H2 DB
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InstanceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ─────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────

    @Test
    @DisplayName("POST /api/instances → 201 with REQUESTED state")
    void createInstance_returns201_withRequestedState()
            throws Exception {

        CreateInstanceRequest request =
                new CreateInstanceRequest();
        request.setName("test-db");
        request.setDbType(DatabaseType.POSTGRESQL);
        request.setOwnerId("user-001");

        mockMvc.perform(post("/api/instances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("test-db"))
                .andExpect(jsonPath("$.state")
                        .value("REQUESTED"))
                .andExpect(jsonPath("$.dbType")
                        .value("POSTGRESQL"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/instances → 400 when name is invalid")
    void createInstance_returns400_whenNameInvalid()
            throws Exception {

        CreateInstanceRequest request =
                new CreateInstanceRequest();
        request.setName("INVALID NAME!!!");   // fails @Pattern
        request.setDbType(DatabaseType.POSTGRESQL);
        request.setOwnerId("user-001");

        mockMvc.perform(post("/api/instances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists())
                .andExpect(jsonPath("$.fieldErrors.name")
                        .isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/instances → 400 when body is empty")
    void createInstance_returns400_whenBodyEmpty()
            throws Exception {

        mockMvc.perform(post("/api/instances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────
    // GET BY ID
    // ─────────────────────────────────────────

    @Test
    @DisplayName("GET /api/instances/{id} → 200 when found")
    void getInstance_returns200_whenFound()
            throws Exception {

        // First create an instance
        String id = createTestInstance("get-test-db");

        mockMvc.perform(get("/api/instances/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name")
                        .value("get-test-db"));
    }

    @Test
    @DisplayName("GET /api/instances/{id} → 404 when not found")
    void getInstance_returns404_whenNotFound()
            throws Exception {

        mockMvc.perform(get("/api/instances/{id}",
                        "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value(containsString("not found")));
    }

    // ─────────────────────────────────────────
    // GET BY OWNER
    // ─────────────────────────────────────────

    @Test
    @DisplayName("GET /api/instances?ownerId → returns owner's instances")
    void getByOwner_returnsOnlyOwnerInstances()
            throws Exception {

        // Create 2 instances for user-001
        createTestInstance("db-one");
        createTestInstance("db-two");

        mockMvc.perform(get("/api/instances")
                        .param("ownerId", "user-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    // ─────────────────────────────────────────
    // UPDATE STATE
    // ─────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/instances/{id}/state → updates state")
    void updateState_updatesStateCorrectly()
            throws Exception {

        String id = createTestInstance("state-test-db");

        UpdateStateRequest stateRequest = new UpdateStateRequest();
        stateRequest.setState(InstanceState.PROVISIONING);

        mockMvc.perform(patch("/api/instances/{id}/state", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper
                                .writeValueAsString(stateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state")
                        .value("PROVISIONING"));
    }

    @Test
    @DisplayName("PATCH /api/instances/{id}/state → 404 when not found")
    void updateState_returns404_whenNotFound()
            throws Exception {

        UpdateStateRequest stateRequest = new UpdateStateRequest();
        stateRequest.setState(InstanceState.PROVISIONING);

        mockMvc.perform(patch("/api/instances/{id}/state",
                        "00000000-0000-0000-0000-000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper
                                .writeValueAsString(stateRequest)))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────
    // STATUS ENDPOINT
    // ─────────────────────────────────────────

    @Test
    @DisplayName("GET /api/instances/{id}/status → returns status details")
    void getStatus_returnsDetailedStatus()
            throws Exception {

        String id = createTestInstance("status-db");

        mockMvc.perform(get("/api/instances/{id}/status", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state")
                        .value("REQUESTED"))
                .andExpect(jsonPath("$.isTerminal")
                        .value(false))
                .andExpect(jsonPath("$.isActionable")
                        .value(true))
                .andExpect(jsonPath("$.containerExpected")
                        .value(false));
    }

    // ─────────────────────────────────────────
    // RECOVERY
    // ─────────────────────────────────────────

    @Test
    @DisplayName("POST /recover → resets FAILED to REQUESTED")
    void recover_resetsFailed_toRequested()
            throws Exception {

        String id = createTestInstance("recover-db");

        // Set to FAILED first
        UpdateStateRequest failRequest = new UpdateStateRequest();
        failRequest.setState(InstanceState.FAILED);
        mockMvc.perform(patch("/api/instances/{id}/state", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper
                                .writeValueAsString(failRequest)))
                .andExpect(status().isOk());

        // Now recover
        mockMvc.perform(post("/api/instances/{id}/recover", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentState")
                        .value("REQUESTED"))
                .andExpect(jsonPath("$.previousState")
                        .value("FAILED"));
    }

    @Test
    @DisplayName("POST /recover → 400 when instance not FAILED")
    void recover_returns400_whenNotFailed()
            throws Exception {

        String id = createTestInstance("no-recover-db");

        // Instance is REQUESTED — cannot recover
        mockMvc.perform(post("/api/instances/{id}/recover", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value(containsString("Cannot recover")));
    }

    // ─────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────

    /**
     * Creates a test instance and returns its UUID.
     * Reused across multiple tests.
     */
    private String createTestInstance(String name)
            throws Exception {

        CreateInstanceRequest request =
                new CreateInstanceRequest();
        request.setName(name);
        request.setDbType(DatabaseType.POSTGRESQL);
        request.setOwnerId("user-001");

        MvcResult result = mockMvc.perform(
                        post("/api/instances")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper
                                        .writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse()
                .getContentAsString();
        return objectMapper.readTree(response)
                .get("id").asText();
    }
}