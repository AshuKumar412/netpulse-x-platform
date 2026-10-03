package com.netpulse.whatif.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netpulse.whatif.dto.*;
import com.netpulse.whatif.service.WhatIfScenarioService;
import com.netpulse.whatif.state.ScenarioStatus;
import com.netpulse.whatif.state.ScenarioType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WhatIfControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private WhatIfScenarioService scenarioService;

    @InjectMocks
    private WhatIfController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/what-if/snapshot should return system snapshot")
    void testGetSnapshot() throws Exception {
        SystemSnapshotDto snapshot = new SystemSnapshotDto();
        snapshot.setSnapshotId("SNAP-100");
        snapshot.setTotalNodes(5);
        snapshot.setHealthyNodes(5);

        when(scenarioService.captureSystemSnapshot()).thenReturn(snapshot);

        mockMvc.perform(get("/api/what-if/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotId").value("SNAP-100"))
                .andExpect(jsonPath("$.totalNodes").value(5));
    }

    @Test
    @DisplayName("POST /api/what-if/scenarios should create new scenario")
    void testCreateScenario() throws Exception {
        WhatIfScenarioRequest request = new WhatIfScenarioRequest(
                "Test Outage", "Desc", ScenarioType.NODE_FAILURE, Collections.emptyList(), 60
        );
        WhatIfScenarioResponse response = new WhatIfScenarioResponse();
        response.setScenarioId("SCEN-100");
        response.setName("Test Outage");
        response.setStatus(ScenarioStatus.READY);

        when(scenarioService.createScenario(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/what-if/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scenarioId").value("SCEN-100"))
                .andExpect(jsonPath("$.name").value("Test Outage"));
    }

    @Test
    @DisplayName("POST /api/what-if/scenarios/{id}/run should execute simulation")
    void testRunSimulation() throws Exception {
        SimulationRunResponse runResponse = new SimulationRunResponse();
        runResponse.setRunId("RUN-100");
        runResponse.setScenarioId("SCEN-100");
        runResponse.setStatus(ScenarioStatus.COMPLETED);

        when(scenarioService.runSimulation("SCEN-100")).thenReturn(runResponse);

        mockMvc.perform(post("/api/what-if/scenarios/SCEN-100/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("RUN-100"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/what-if/templates should return list of templates")
    void testGetTemplates() throws Exception {
        WhatIfTemplateDto tpl = new WhatIfTemplateDto("TPL-01", "Node Failure", "Desc", ScenarioType.NODE_FAILURE, Collections.emptyList(), 60);
        when(scenarioService.getTemplates()).thenReturn(List.of(tpl));

        mockMvc.perform(get("/api/what-if/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].templateId").value("TPL-01"));
    }
}
