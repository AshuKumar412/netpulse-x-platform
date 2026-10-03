package com.netpulse.health.service;

import com.netpulse.health.dto.NodeHealthResponse;
import com.netpulse.health.entity.HealthClassification;
import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.telemetry.entity.TelemetryRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NodeHealthServiceTest {

    @Mock
    private HeartbeatService heartbeatService;

    private NodeHealthService nodeHealthService;

    @BeforeEach
    void setUp() {
        nodeHealthService = new NodeHealthService(heartbeatService);
    }

    @Test
    @DisplayName("Should classify OFFLINE when node is administratively OFFLINE")
    void testEvaluateOfflineNode() {
        NetworkNode node = new NetworkNode("node-off", "Off", "10.0.0.1", 8080, NodeStatus.OFFLINE, 1000);
        when(heartbeatService.getHeartbeatStatus("node-off"))
                .thenReturn(new HeartbeatStatusResponse("node-off", null, null, LivenessStatus.UNREACHABLE, false));

        NodeHealthResponse response = nodeHealthService.evaluateNodeHealth(node, null);

        assertNotNull(response);
        assertEquals(HealthClassification.OFFLINE, response.getHealthStatus());
        assertTrue(response.getReason().contains("OFFLINE"));
    }

    @Test
    @DisplayName("Should classify FAILED when node heartbeat times out (UNREACHABLE)")
    void testEvaluateUnreachableHeartbeat() {
        NetworkNode node = new NetworkNode("node-unreach", "Unreach", "10.0.0.2", 8080, NodeStatus.HEALTHY, 1000);
        when(heartbeatService.getHeartbeatStatus("node-unreach"))
                .thenReturn(new HeartbeatStatusResponse("node-unreach", Instant.now().minusSeconds(15), 15L, LivenessStatus.UNREACHABLE, false));

        NodeHealthResponse response = nodeHealthService.evaluateNodeHealth(node, null);

        assertNotNull(response);
        assertEquals(HealthClassification.FAILED, response.getHealthStatus());
        assertTrue(response.getReason().contains("unreachable"));
    }

    @Test
    @DisplayName("Should classify HEALTHY for normal operational metrics and active heartbeat")
    void testEvaluateHealthyNode() {
        NetworkNode node = new NetworkNode("node-ok", "Ok", "10.0.0.3", 8080, NodeStatus.HEALTHY, 1000);
        TelemetryRecord telemetry = new TelemetryRecord("node-ok", Instant.now(), 25.0, 35.0, 15.0, 0.0, 50, 0.0);
        when(heartbeatService.getHeartbeatStatus("node-ok"))
                .thenReturn(new HeartbeatStatusResponse("node-ok", Instant.now(), 0L, LivenessStatus.ALIVE, false));

        NodeHealthResponse response = nodeHealthService.evaluateNodeHealth(node, telemetry);

        assertNotNull(response);
        assertEquals(HealthClassification.HEALTHY, response.getHealthStatus());
        assertEquals(LivenessStatus.ALIVE, response.getLivenessStatus());
        assertEquals(25.0, response.getCpuUsage());
    }

    @Test
    @DisplayName("Should classify WARNING when metrics cross warning thresholds")
    void testEvaluateWarningNode() {
        NetworkNode node = new NetworkNode("node-warn", "Warn", "10.0.0.4", 8080, NodeStatus.HEALTHY, 1000);
        TelemetryRecord telemetry = new TelemetryRecord("node-warn", Instant.now(), 75.0, 50.0, 20.0, 0.0, 50, 0.0);
        when(heartbeatService.getHeartbeatStatus("node-warn"))
                .thenReturn(new HeartbeatStatusResponse("node-warn", Instant.now(), 0L, LivenessStatus.ALIVE, false));

        NodeHealthResponse response = nodeHealthService.evaluateNodeHealth(node, telemetry);

        assertNotNull(response);
        assertEquals(HealthClassification.WARNING, response.getHealthStatus());
        assertTrue(response.getReason().contains("High CPU"));
    }

    @Test
    @DisplayName("Should classify DEGRADED when metrics cross critical thresholds")
    void testEvaluateDegradedNode() {
        NetworkNode node = new NetworkNode("node-deg", "Deg", "10.0.0.5", 8080, NodeStatus.HEALTHY, 1000);
        TelemetryRecord telemetry = new TelemetryRecord("node-deg", Instant.now(), 92.0, 50.0, 20.0, 0.0, 50, 0.0);
        when(heartbeatService.getHeartbeatStatus("node-deg"))
                .thenReturn(new HeartbeatStatusResponse("node-deg", Instant.now(), 0L, LivenessStatus.ALIVE, false));

        NodeHealthResponse response = nodeHealthService.evaluateNodeHealth(node, telemetry);

        assertNotNull(response);
        assertEquals(HealthClassification.DEGRADED, response.getHealthStatus());
        assertTrue(response.getReason().contains("Critical CPU"));
    }
}
