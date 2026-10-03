package com.netpulse.failover.service;

import com.netpulse.health.dto.NodeHealthResponse;
import com.netpulse.health.entity.HealthClassification;
import com.netpulse.health.service.NodeHealthService;
import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.telemetry.entity.TelemetryRecord;
import com.netpulse.telemetry.service.TelemetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecoveryVerificationServiceTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private HeartbeatService heartbeatService;

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private NodeHealthService nodeHealthService;

    @Mock
    private FailoverPolicyService policyService;

    private RecoveryVerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new RecoveryVerificationService(
                nodeRepository,
                heartbeatService,
                telemetryService,
                nodeHealthService,
                policyService
        );
    }

    @Test
    @DisplayName("Should verify node recovery after required consecutive healthy cycles")
    void testSuccessfulRecoveryVerification() {
        NetworkNode node = new NetworkNode("node-1", "Router 1", "192.168.1.1", 8080, NodeStatus.HEALTHY, 2000);
        when(nodeRepository.findByNodeId("node-1")).thenReturn(Optional.of(node));
        when(policyService.isHeartbeatRequiredForRecovery()).thenReturn(true);
        when(policyService.getHealthyCyclesRequiredForRecovery()).thenReturn(2);

        HeartbeatStatusResponse heartbeat = new HeartbeatStatusResponse("node-1", Instant.now(), 1L, LivenessStatus.ALIVE, false);
        when(heartbeatService.getHeartbeatStatus("node-1")).thenReturn(heartbeat);

        TelemetryRecord telemetry = new TelemetryRecord("node-1", Instant.now(), 20.0, 30.0, 15.0, 0.0, 5, 0.0);
        when(telemetryService.getLatestTelemetry("node-1")).thenReturn(telemetry);

        NodeHealthResponse health = new NodeHealthResponse("node-1", HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, Instant.now(), Instant.now(), "Optimal", 20.0, 30.0, 15.0, 0.0, 0.0);
        when(nodeHealthService.evaluateNodeHealth(node, telemetry)).thenReturn(health);

        // Cycle 1: 1/2 cycles -> not verified yet
        RecoveryVerificationService.VerificationResult res1 = verificationService.checkNodeRecovery("node-1");
        assertFalse(res1.isVerified());
        assertEquals(1, res1.getCurrentCycles());

        // Cycle 2: 2/2 cycles -> verified!
        RecoveryVerificationService.VerificationResult res2 = verificationService.checkNodeRecovery("node-1");
        assertTrue(res2.isVerified());
        assertEquals(2, res2.getCurrentCycles());
    }

    @Test
    @DisplayName("Should reset verification cycle count if node degrades during verification")
    void testResetOnDegradedHealth() {
        NetworkNode node = new NetworkNode("node-1", "Router 1", "192.168.1.1", 8080, NodeStatus.HEALTHY, 2000);
        when(nodeRepository.findByNodeId("node-1")).thenReturn(Optional.of(node));
        when(policyService.isHeartbeatRequiredForRecovery()).thenReturn(true);
        when(policyService.getHealthyCyclesRequiredForRecovery()).thenReturn(2);

        HeartbeatStatusResponse heartbeat = new HeartbeatStatusResponse("node-1", Instant.now(), 1L, LivenessStatus.ALIVE, false);
        when(heartbeatService.getHeartbeatStatus("node-1")).thenReturn(heartbeat);

        TelemetryRecord telemetry = new TelemetryRecord("node-1", Instant.now(), 85.0, 30.0, 150.0, 0.0, 5, 0.0);
        when(telemetryService.getLatestTelemetry("node-1")).thenReturn(telemetry);

        NodeHealthResponse degraded = new NodeHealthResponse("node-1", HealthClassification.DEGRADED,
                LivenessStatus.ALIVE, Instant.now(), Instant.now(), "High Latency", 85.0, 30.0, 150.0, 0.0, 0.0);
        when(nodeHealthService.evaluateNodeHealth(node, telemetry)).thenReturn(degraded);

        RecoveryVerificationService.VerificationResult res = verificationService.checkNodeRecovery("node-1");
        assertFalse(res.isVerified());
        assertEquals(0, res.getCurrentCycles());
    }
}
