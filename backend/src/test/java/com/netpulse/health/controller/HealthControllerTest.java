package com.netpulse.health.controller;

import com.netpulse.health.dto.NodeHealthResponse;
import com.netpulse.health.entity.HealthClassification;
import com.netpulse.health.service.NodeHealthService;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.telemetry.entity.TelemetryRecord;
import com.netpulse.telemetry.service.TelemetryService;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private NodeHealthService nodeHealthService;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private TelemetryService telemetryService;

    @InjectMocks
    private HealthController healthController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(healthController).build();
    }

    @Test
    @DisplayName("GET /api/health/nodes should return health for all nodes")
    void testGetAllNodeHealth() throws Exception {
        NetworkNode node = new NetworkNode("node-1", "Router 1", "10.0.0.1", 8080, NodeStatus.HEALTHY, 1000);
        TelemetryRecord telemetry = new TelemetryRecord("node-1", Instant.now(), 25.0, 35.0, 12.0, 0.0, 50, 0.0);
        NodeHealthResponse health = new NodeHealthResponse("node-1", HealthClassification.HEALTHY, LivenessStatus.ALIVE,
                Instant.now(), Instant.now(), "Optimal operating conditions", 25.0, 35.0, 12.0, 0.0, 0.0);

        when(nodeRepository.findAll()).thenReturn(List.of(node));
        when(telemetryService.getLatestTelemetry("node-1")).thenReturn(telemetry);
        when(nodeHealthService.evaluateNodeHealth(eq(node), eq(telemetry))).thenReturn(health);

        mockMvc.perform(get("/api/health/nodes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].nodeId").value("node-1"))
                .andExpect(jsonPath("$.data[0].healthStatus").value("HEALTHY"));
    }

    @Test
    @DisplayName("GET /api/health/nodes/{nodeId} should return specific node health")
    void testGetNodeHealth() throws Exception {
        NetworkNode node = new NetworkNode("node-1", "Router 1", "10.0.0.1", 8080, NodeStatus.HEALTHY, 1000);
        TelemetryRecord telemetry = new TelemetryRecord("node-1", Instant.now(), 25.0, 35.0, 12.0, 0.0, 50, 0.0);
        NodeHealthResponse health = new NodeHealthResponse("node-1", HealthClassification.HEALTHY, LivenessStatus.ALIVE,
                Instant.now(), Instant.now(), "Optimal operating conditions", 25.0, 35.0, 12.0, 0.0, 0.0);

        when(nodeRepository.findByNodeId("node-1")).thenReturn(Optional.of(node));
        when(telemetryService.getLatestTelemetry("node-1")).thenReturn(telemetry);
        when(nodeHealthService.evaluateNodeHealth(eq(node), eq(telemetry))).thenReturn(health);

        mockMvc.perform(get("/api/health/nodes/node-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nodeId").value("node-1"))
                .andExpect(jsonPath("$.data.healthStatus").value("HEALTHY"));
    }
}
