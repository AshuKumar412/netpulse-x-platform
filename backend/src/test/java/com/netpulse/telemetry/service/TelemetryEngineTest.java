package com.netpulse.telemetry.service;

import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.telemetry.entity.TelemetryRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryEngineTest {

    private TelemetryEngine telemetryEngine;

    @BeforeEach
    void setUp() {
        telemetryEngine = new TelemetryEngine();
    }

    @Test
    @DisplayName("Should generate zero telemetry for OFFLINE node")
    void testComputeTelemetryOfflineNode() {
        NetworkNode node = new NetworkNode("node-off", "Offline Edge", "10.0.0.1", 8080, NodeStatus.OFFLINE, 1000);

        TelemetryRecord record = telemetryEngine.computeTelemetry(node, 0, 10, null);

        assertNotNull(record);
        assertEquals("node-off", record.getNodeId());
        assertEquals(0.0, record.getCpuUsage());
        assertEquals(0.0, record.getMemoryUsage());
        assertEquals(0.0, record.getLatency());
        assertEquals(0.0, record.getPacketLoss());
        assertEquals(0, record.getActiveConnections());
        assertEquals(0.0, record.getErrorRate());
    }

    @Test
    @DisplayName("Should generate failure telemetry for FAILED node")
    void testComputeTelemetryFailedNode() {
        NetworkNode node = new NetworkNode("node-fail", "Failed Edge", "10.0.0.2", 8080, NodeStatus.FAILED, 1000);

        TelemetryRecord record = telemetryEngine.computeTelemetry(node, 2, 15, null);

        assertNotNull(record);
        assertEquals("node-fail", record.getNodeId());
        assertEquals(98.5, record.getCpuUsage());
        assertEquals(95.0, record.getMemoryUsage());
        assertEquals(450.0, record.getLatency());
        assertEquals(100.0, record.getPacketLoss());
        assertEquals(0, record.getActiveConnections());
        assertEquals(100.0, record.getErrorRate());
    }

    @Test
    @DisplayName("Should generate valid bounded telemetry for HEALTHY node")
    void testComputeTelemetryHealthyNode() {
        NetworkNode node = new NetworkNode("node-healthy", "Healthy Edge", "10.0.0.3", 8080, NodeStatus.HEALTHY, 5000);

        TelemetryRecord record = telemetryEngine.computeTelemetry(node, 4, 20, null);

        assertNotNull(record);
        assertEquals("node-healthy", record.getNodeId());
        assertTrue(record.getCpuUsage() >= 0.0 && record.getCpuUsage() <= 100.0);
        assertTrue(record.getMemoryUsage() >= 0.0 && record.getMemoryUsage() <= 100.0);
        assertTrue(record.getLatency() >= 0.0);
        assertTrue(record.getPacketLoss() >= 0.0 && record.getPacketLoss() <= 100.0);
        assertTrue(record.getActiveConnections() > 0);
        assertEquals(0.0, record.getErrorRate());
    }

    @Test
    @DisplayName("Should generate elevated metrics for WARNING and CONGESTED nodes")
    void testComputeTelemetryDegradedNodes() {
        NetworkNode warningNode = new NetworkNode("node-warn", "Warning Edge", "10.0.0.4", 8080, NodeStatus.WARNING, 2000);
        NetworkNode congestedNode = new NetworkNode("node-cong", "Congested Edge", "10.0.0.5", 8080, NodeStatus.CONGESTED, 2000);

        TelemetryRecord warnRecord = telemetryEngine.computeTelemetry(warningNode, 2, 5, null);
        TelemetryRecord congRecord = telemetryEngine.computeTelemetry(congestedNode, 2, 5, null);

        assertNotNull(warnRecord);
        assertNotNull(congRecord);

        assertTrue(warnRecord.getCpuUsage() >= 65.0);
        assertTrue(congRecord.getCpuUsage() >= 85.0);
        assertTrue(congRecord.getLatency() > warnRecord.getLatency());
        assertTrue(congRecord.getPacketLoss() > warnRecord.getPacketLoss());
    }

    @Test
    @DisplayName("Should produce reproducible deterministic results for same tick and node")
    void testDeterministicReproducibility() {
        NetworkNode node = new NetworkNode("node-det", "Deterministic Node", "10.0.0.6", 8080, NodeStatus.HEALTHY, 3000);

        TelemetryRecord r1 = telemetryEngine.computeTelemetry(node, 3, 42, null);
        TelemetryRecord r2 = telemetryEngine.computeTelemetry(node, 3, 42, null);

        assertEquals(r1.getCpuUsage(), r2.getCpuUsage());
        assertEquals(r1.getMemoryUsage(), r2.getMemoryUsage());
        assertEquals(r1.getLatency(), r2.getLatency());
        assertEquals(r1.getPacketLoss(), r2.getPacketLoss());
        assertEquals(r1.getActiveConnections(), r2.getActiveConnections());
        assertEquals(r1.getErrorRate(), r2.getErrorRate());
    }
}
