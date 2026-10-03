package com.netpulse.heartbeat.service;

import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.entity.LivenessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeartbeatServiceTest {

    private HeartbeatService heartbeatService;

    @BeforeEach
    void setUp() {
        heartbeatService = new HeartbeatService(null);
        heartbeatService.setThresholds(10, 5);
    }

    @Test
    @DisplayName("Should report UNREACHABLE for node that never emitted a heartbeat")
    void testNeverSeenNodeUnreachable() {
        LivenessStatus liveness = heartbeatService.evaluateLiveness("unseen-node");
        assertEquals(LivenessStatus.UNREACHABLE, liveness);

        HeartbeatStatusResponse status = heartbeatService.getHeartbeatStatus("unseen-node");
        assertNotNull(status);
        assertNull(status.getLastHeartbeat());
        assertEquals(LivenessStatus.UNREACHABLE, status.getLiveness());
    }

    @Test
    @DisplayName("Should report ALIVE immediately after recording heartbeat")
    void testRecordedHeartbeatAlive() {
        heartbeatService.recordHeartbeat("node-active");

        LivenessStatus liveness = heartbeatService.evaluateLiveness("node-active");
        assertEquals(LivenessStatus.ALIVE, liveness);

        HeartbeatStatusResponse status = heartbeatService.getHeartbeatStatus("node-active");
        assertNotNull(status);
        assertNotNull(status.getLastHeartbeat());
        assertEquals(LivenessStatus.ALIVE, status.getLiveness());
        assertFalse(status.getIsSuppressed());
    }

    @Test
    @DisplayName("Should suppress heartbeat when operator requests suppression")
    void testHeartbeatSuppressionToggle() {
        heartbeatService.suppressHeartbeat("node-test", true);
        assertTrue(heartbeatService.isHeartbeatSuppressed("node-test"));

        heartbeatService.recordHeartbeat("node-test");
        assertNull(heartbeatService.getLastHeartbeat("node-test"));

        // Unsuppress
        heartbeatService.suppressHeartbeat("node-test", false);
        assertFalse(heartbeatService.isHeartbeatSuppressed("node-test"));

        heartbeatService.recordHeartbeat("node-test");
        assertNotNull(heartbeatService.getLastHeartbeat("node-test"));
    }

    @Test
    @DisplayName("Should remove node from heartbeat registry")
    void testRemoveNode() {
        heartbeatService.recordHeartbeat("node-remove");
        assertNotNull(heartbeatService.getLastHeartbeat("node-remove"));

        heartbeatService.removeNode("node-remove");
        assertNull(heartbeatService.getLastHeartbeat("node-remove"));
    }
}
