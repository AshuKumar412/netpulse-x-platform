package com.netpulse.heartbeat.controller;

import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HeartbeatControllerTest {

    @Mock
    private HeartbeatService heartbeatService;

    @Mock
    private NodeRepository nodeRepository;

    @InjectMocks
    private HeartbeatController heartbeatController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(heartbeatController).build();
    }

    @Test
    @DisplayName("GET /api/heartbeat/status should return liveness statuses")
    void testGetAllHeartbeatStatuses() throws Exception {
        NetworkNode node = new NetworkNode("node-1", "Router 1", "10.0.0.1", 8080, NodeStatus.HEALTHY, 1000);
        HeartbeatStatusResponse resp = new HeartbeatStatusResponse("node-1", Instant.now(), 0L, LivenessStatus.ALIVE, false);

        when(nodeRepository.findAll()).thenReturn(List.of(node));
        when(heartbeatService.getAllHeartbeatStatuses(List.of("node-1"))).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/heartbeat/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].nodeId").value("node-1"))
                .andExpect(jsonPath("$.data[0].liveness").value("ALIVE"));
    }

    @Test
    @DisplayName("POST /api/heartbeat/nodes/{nodeId}/suppress should toggle suppression")
    void testToggleHeartbeatSuppression() throws Exception {
        HeartbeatStatusResponse resp = new HeartbeatStatusResponse("node-1", Instant.now(), 0L, LivenessStatus.ALIVE, true);
        when(nodeRepository.existsByNodeId("node-1")).thenReturn(true);
        when(heartbeatService.getHeartbeatStatus("node-1")).thenReturn(resp);

        mockMvc.perform(post("/api/heartbeat/nodes/node-1/suppress?suppress=true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isSuppressed").value(true));
    }
}
