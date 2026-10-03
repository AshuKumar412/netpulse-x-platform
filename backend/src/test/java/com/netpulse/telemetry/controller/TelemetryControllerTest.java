package com.netpulse.telemetry.controller;

import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.telemetry.dto.TelemetryRecordResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TelemetryControllerTest {

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private NodeRepository nodeRepository;

    @InjectMocks
    private TelemetryController telemetryController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(telemetryController).build();
    }

    @Test
    @DisplayName("GET /api/telemetry should return latest telemetry for all nodes")
    void testGetAllCurrentTelemetry() throws Exception {
        NetworkNode node = new NetworkNode("node-1", "Router 1", "10.0.0.1", 8080, NodeStatus.HEALTHY, 1000);
        TelemetryRecord record = new TelemetryRecord("node-1", Instant.now(), 25.0, 35.0, 12.0, 0.0, 50, 0.0);
        TelemetryRecordResponse resp = new TelemetryRecordResponse(1L, "node-1", Instant.now(), 25.0, 35.0, 12.0, 0.0, 50, 0.0);

        when(nodeRepository.findAll()).thenReturn(List.of(node));
        when(telemetryService.getAllLatestTelemetry(List.of("node-1"))).thenReturn(List.of(record));
        when(telemetryService.toResponse(record)).thenReturn(resp);

        mockMvc.perform(get("/api/telemetry")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].nodeId").value("node-1"))
                .andExpect(jsonPath("$.data[0].cpuUsage").value(25.0));
    }

    @Test
    @DisplayName("GET /api/telemetry/nodes/{nodeId} should return node telemetry")
    void testGetNodeTelemetry() throws Exception {
        TelemetryRecord record = new TelemetryRecord("node-1", Instant.now(), 30.0, 40.0, 15.0, 0.0, 60, 0.0);
        TelemetryRecordResponse resp = new TelemetryRecordResponse(1L, "node-1", Instant.now(), 30.0, 40.0, 15.0, 0.0, 60, 0.0);

        when(nodeRepository.existsByNodeId("node-1")).thenReturn(true);
        when(telemetryService.getLatestTelemetry("node-1")).thenReturn(record);
        when(telemetryService.toResponse(record)).thenReturn(resp);

        mockMvc.perform(get("/api/telemetry/nodes/node-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nodeId").value("node-1"))
                .andExpect(jsonPath("$.data.cpuUsage").value(30.0));
    }

    @Test
    @DisplayName("GET /api/telemetry/nodes/{nodeId}/history should return historical points")
    void testGetNodeTelemetryHistory() throws Exception {
        TelemetryRecordResponse resp1 = new TelemetryRecordResponse(1L, "node-1", Instant.now(), 30.0, 40.0, 15.0, 0.0, 60, 0.0);
        when(nodeRepository.existsByNodeId("node-1")).thenReturn(true);
        when(telemetryService.getRecentHistory(eq("node-1"), eq(20))).thenReturn(List.of(resp1));

        mockMvc.perform(get("/api/telemetry/nodes/node-1/history?limit=20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].nodeId").value("node-1"));
    }
}
