package com.netpulse.routing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netpulse.routing.dto.*;
import com.netpulse.routing.entity.RoutingStrategyType;
import com.netpulse.routing.service.RoutingCandidateService;
import com.netpulse.routing.service.RoutingService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RoutingControllerTest {

    @Mock
    private RoutingService routingService;

    @Mock
    private RoutingCandidateService candidateService;

    @InjectMocks
    private RoutingController routingController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(routingController).build();
    }

    @Test
    @DisplayName("POST /api/routing/decide should return simulated decision")
    void testDecideRouting() throws Exception {
        RoutingDecisionResponse resp = new RoutingDecisionResponse("REQ-01", RoutingStrategyType.ADAPTIVE, "SUCCESS",
                "node-1", "Node 1", 3, 3, 0.25, "Optimal score", Instant.now(), Collections.emptyList());

        when(routingService.decideRouting(any())).thenReturn(resp);

        mockMvc.perform(post("/api/routing/decide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoutingRequest(RoutingStrategyType.ADAPTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.selectedNodeId").value("node-1"))
                .andExpect(jsonPath("$.data.strategy").value("ADAPTIVE"));
    }

    @Test
    @DisplayName("POST /api/routing/request should execute and record traffic decision")
    void testExecuteRoutingRequest() throws Exception {
        RoutingDecisionResponse resp = new RoutingDecisionResponse("REQ-02", RoutingStrategyType.ROUND_ROBIN, "SUCCESS",
                "node-2", "Node 2", 3, 3, 1.0, "Round robin", Instant.now(), Collections.emptyList());

        when(routingService.executeAndRecordRouting(any())).thenReturn(resp);

        mockMvc.perform(post("/api/routing/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RoutingRequest(RoutingStrategyType.ROUND_ROBIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.selectedNodeId").value("node-2"));
    }

    @Test
    @DisplayName("GET /api/routing/status should return engine status")
    void testGetStatus() throws Exception {
        RoutingStatusResponse resp = new RoutingStatusResponse(RoutingStrategyType.ADAPTIVE, 42L, 120L, 5,
                List.of(RoutingStrategyType.values()), Instant.now());

        when(routingService.getStatus()).thenReturn(resp);

        mockMvc.perform(get("/api/routing/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activeStrategy").value("ADAPTIVE"))
                .andExpect(jsonPath("$.data.totalDecisionsCount").value(42));
    }
}
