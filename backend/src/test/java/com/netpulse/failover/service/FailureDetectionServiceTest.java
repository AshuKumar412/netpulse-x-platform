package com.netpulse.failover.service;

import com.netpulse.failover.entity.FailoverEvent;
import com.netpulse.failover.repository.FailoverEventRepository;
import com.netpulse.failover.state.FailoverState;
import com.netpulse.failover.state.FailoverTrigger;
import com.netpulse.health.dto.NodeHealthResponse;
import com.netpulse.health.entity.HealthClassification;
import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailureDetectionServiceTest {

    @Mock
    private FailoverOrchestrator orchestrator;

    @Mock
    private NodeRecoveryService nodeRecoveryService;

    @Mock
    private FailoverAuditService auditService;

    @Mock
    private FailoverPolicyService policyService;

    @Mock
    private FailoverEventRepository eventRepository;

    private FailureDetectionService failureDetectionService;

    @BeforeEach
    void setUp() {
        failureDetectionService = new FailureDetectionService(
                orchestrator,
                nodeRecoveryService,
                auditService,
                policyService,
                eventRepository
        );
    }

    @Test
    @DisplayName("Should immediately confirm failure for administrative OFFLINE node")
    void testOfflineNodeImmediateConfirmation() {
        when(orchestrator.isNodeIsolatedOrFailing("node-offline")).thenReturn(false);

        NetworkNode node = new NetworkNode("node-offline", "Offline Node", "192.168.1.1", 8080, NodeStatus.OFFLINE, 1000);
        node.setStatus(NodeStatus.OFFLINE);

        NodeHealthResponse health = new NodeHealthResponse("node-offline", HealthClassification.OFFLINE,
                LivenessStatus.UNREACHABLE, null, Instant.now(), "Offline", 0.0, 0.0, 0.0, 0.0, 0.0);
        HeartbeatStatusResponse heartbeat = new HeartbeatStatusResponse("node-offline", null, null, LivenessStatus.UNREACHABLE, false);

        failureDetectionService.evaluateNode(node, health, heartbeat);

        verify(orchestrator).handleConfirmedFailure(eq("node-offline"), eq(FailoverTrigger.NODE_OFFLINE), anyString());
    }

    @Test
    @DisplayName("Should confirm failure after consecutive failure threshold is reached")
    void testConsecutiveFailuresConfirmation() {
        when(policyService.getConsecutiveFailuresRequired()).thenReturn(2);
        when(orchestrator.isNodeIsolatedOrFailing("node-1")).thenReturn(false);

        NetworkNode node = new NetworkNode("node-1", "Router 1", "192.168.1.1", 8080, NodeStatus.HEALTHY, 2000);

        NodeHealthResponse health = new NodeHealthResponse("node-1", HealthClassification.FAILED,
                LivenessStatus.UNREACHABLE, null, Instant.now(), "Timeout", 0.0, 0.0, 0.0, 100.0, 100.0);
        HeartbeatStatusResponse heartbeat = new HeartbeatStatusResponse("node-1", null, null, LivenessStatus.UNREACHABLE, false);

        // Cycle 1: count = 1 -> does not trigger failover yet
        failureDetectionService.evaluateNode(node, health, heartbeat);
        assertEquals(1, failureDetectionService.getConsecutiveFailures("node-1"));
        verify(orchestrator, never()).handleConfirmedFailure(anyString(), any(), anyString());

        // Cycle 2: count = 2 -> reaches threshold -> triggers failover
        failureDetectionService.evaluateNode(node, health, heartbeat);
        assertEquals(0, failureDetectionService.getConsecutiveFailures("node-1"));
        verify(orchestrator).handleConfirmedFailure(eq("node-1"), eq(FailoverTrigger.MULTI_SIGNAL_FAILURE), anyString());
    }

    @Test
    @DisplayName("Should reset consecutive failure counter when node recovers to HEALTHY")
    void testResetOnHealthyNode() {
        when(policyService.getConsecutiveFailuresRequired()).thenReturn(2);
        when(orchestrator.isNodeIsolatedOrFailing("node-1")).thenReturn(false);

        NetworkNode node = new NetworkNode("node-1", "Router 1", "192.168.1.1", 8080, NodeStatus.HEALTHY, 2000);

        NodeHealthResponse unhealthy = new NodeHealthResponse("node-1", HealthClassification.FAILED,
                LivenessStatus.UNREACHABLE, null, Instant.now(), "Timeout", 0.0, 0.0, 0.0, 100.0, 100.0);
        HeartbeatStatusResponse hbUnhealthy = new HeartbeatStatusResponse("node-1", null, null, LivenessStatus.UNREACHABLE, false);

        // Cycle 1: Unhealthy
        failureDetectionService.evaluateNode(node, unhealthy, hbUnhealthy);
        assertEquals(1, failureDetectionService.getConsecutiveFailures("node-1"));

        // Cycle 2: Healthy
        NodeHealthResponse healthy = new NodeHealthResponse("node-1", HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, Instant.now(), Instant.now(), "Optimal", 20.0, 30.0, 10.0, 0.0, 0.0);
        HeartbeatStatusResponse hbHealthy = new HeartbeatStatusResponse("node-1", Instant.now(), 1L, LivenessStatus.ALIVE, false);

        failureDetectionService.evaluateNode(node, healthy, hbHealthy);
        assertEquals(0, failureDetectionService.getConsecutiveFailures("node-1"));
        verify(orchestrator, never()).handleConfirmedFailure(anyString(), any(), anyString());
    }
}
