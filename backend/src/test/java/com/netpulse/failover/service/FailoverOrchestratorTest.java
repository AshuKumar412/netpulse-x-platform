package com.netpulse.failover.service;

import com.netpulse.failover.dto.FailoverEventDto;
import com.netpulse.failover.entity.ActiveSessionRecord;
import com.netpulse.failover.entity.FailoverEvent;
import com.netpulse.failover.state.FailoverState;
import com.netpulse.failover.state.FailoverTrigger;
import com.netpulse.health.entity.HealthClassification;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.routing.dto.CandidateEvaluationDto;
import com.netpulse.routing.dto.RoutingDecisionResponse;
import com.netpulse.routing.dto.RoutingRequest;
import com.netpulse.routing.entity.RoutingStrategyType;
import com.netpulse.routing.service.RoutingCandidateService;
import com.netpulse.routing.service.RoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailoverOrchestratorTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private RoutingService routingService;

    @Mock
    private RoutingCandidateService candidateService;

    @Mock
    private TrafficRecoveryService trafficRecoveryService;

    @Mock
    private NodeRecoveryService nodeRecoveryService;

    @Mock
    private FailoverAuditService auditService;

    @Mock
    private FailoverPolicyService policyService;

    private FailoverOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new FailoverOrchestrator(
                nodeRepository,
                routingService,
                candidateService,
                trafficRecoveryService,
                nodeRecoveryService,
                auditService,
                policyService,
                null
        );
    }

    @Test
    @DisplayName("Should execute successful automatic failover and reroute active sessions")
    void testSuccessfulFailover() {
        when(policyService.isEnabled()).thenReturn(true);
        when(policyService.isRecoveryEnabled()).thenReturn(true);
        when(nodeRecoveryService.isInCooldown("node-failed")).thenReturn(false);
        when(auditService.generateEventId()).thenReturn("FO-20260930-000001");
        when(auditService.generateRecoveryCycleId()).thenReturn("RC-20260930-000001");

        NetworkNode failedNode = new NetworkNode("node-failed", "Failed Router", "192.168.1.1", 8080, NodeStatus.HEALTHY, 2000);
        when(nodeRepository.findByNodeId("node-failed")).thenReturn(Optional.of(failedNode));

        ActiveSessionRecord session1 = new ActiveSessionRecord("REQ-001", "node-failed", "node-failed", "WEB", "ADAPTIVE", "ACTIVE", Instant.now(), Instant.now(), null);
        when(trafficRecoveryService.getAffectedSessions("node-failed")).thenReturn(List.of(session1));

        CandidateEvaluationDto c1 = new CandidateEvaluationDto("node-failed", "Failed Router", 2000,
                HealthClassification.FAILED, LivenessStatus.UNREACHABLE, 0.0, 0.0, 0.0, 100.0, 0, 100.0, false, "Offline", null);
        CandidateEvaluationDto c2 = new CandidateEvaluationDto("node-repl", "Replacement Router", 2000,
                HealthClassification.HEALTHY, LivenessStatus.ALIVE, 20.0, 30.0, 15.0, 0.0, 5, 0.0, true, "OK", null);
        when(candidateService.evaluateAllCandidates()).thenReturn(List.of(c1, c2));

        when(routingService.getActiveStrategy()).thenReturn(RoutingStrategyType.ADAPTIVE);
        RoutingDecisionResponse decision = new RoutingDecisionResponse(
                "REQ-002",
                RoutingStrategyType.ADAPTIVE,
                "SUCCESS",
                "node-repl",
                "Replacement Router",
                2, 1,
                15.5,
                "Selected node-repl via ADAPTIVE",
                Instant.now(),
                List.of(c1, c2)
        );
        when(routingService.decideRouting(any(RoutingRequest.class))).thenReturn(decision);

        when(trafficRecoveryService.rerouteSessions("node-failed", "node-repl", "RC-20260930-000001"))
                .thenReturn(new TrafficRecoveryService.RerouteResult(1, 1, 0, List.of("REQ-001")));

        when(auditService.recordEvent(any(FailoverEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        FailoverEventDto event = orchestrator.handleConfirmedFailure("node-failed", FailoverTrigger.HEARTBEAT_UNREACHABLE, "Heartbeat lost");

        assertNotNull(event);
        assertEquals("FO-20260930-000001", event.getEventId());
        assertEquals("node-failed", event.getFailureNodeId());
        assertEquals("node-repl", event.getReplacementNodeId());
        assertEquals(1, event.getAffectedSessionCount());
        assertEquals(1, event.getReroutedSessionCount());
        assertEquals(0, event.getUnrecoveredSessionCount());
    }

    @Test
    @DisplayName("Should record FAILOVER_FAILED when no eligible replacement candidate exists")
    void testFailoverFailedNoEligibleCandidate() {
        when(policyService.isEnabled()).thenReturn(true);
        when(nodeRecoveryService.isInCooldown("node-failed")).thenReturn(false);
        when(auditService.generateEventId()).thenReturn("FO-20260930-000002");
        when(auditService.generateRecoveryCycleId()).thenReturn("RC-20260930-000002");

        NetworkNode failedNode = new NetworkNode("node-failed", "Failed Router", "192.168.1.1", 8080, NodeStatus.HEALTHY, 2000);
        when(nodeRepository.findByNodeId("node-failed")).thenReturn(Optional.of(failedNode));

        CandidateEvaluationDto c1 = new CandidateEvaluationDto("node-failed", "Failed Router", 2000,
                HealthClassification.FAILED, LivenessStatus.UNREACHABLE, 0.0, 0.0, 0.0, 100.0, 0, 100.0, false, "Offline", null);
        when(candidateService.evaluateAllCandidates()).thenReturn(List.of(c1));

        when(auditService.recordEvent(any(FailoverEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        FailoverEventDto event = orchestrator.handleConfirmedFailure("node-failed", FailoverTrigger.MULTI_SIGNAL_FAILURE, "Node unreachable");

        assertNotNull(event);
        assertEquals(FailoverState.FAILOVER_FAILED, event.getCurrentState());
        assertEquals("NO_ELIGIBLE_REPLACEMENT", event.getFailureReason());
        assertFalse(event.getSuccess());
        verify(nodeRecoveryService).setCooldown("node-failed");
    }

    @Test
    @DisplayName("Should reject duplicate failover execution when node is already isolated")
    void testDuplicateFailoverRejection() {
        when(policyService.isEnabled()).thenReturn(true);
        orchestrator.updateNodeState("node-1", FailoverState.ISOLATING);

        FailoverEventDto event = orchestrator.handleConfirmedFailure("node-1", FailoverTrigger.HEARTBEAT_UNREACHABLE, "Duplicate trigger");

        assertNull(event);
        verify(auditService, never()).recordEvent(any());
    }
}
