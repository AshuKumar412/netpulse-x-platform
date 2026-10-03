package com.netpulse.routing.service;

import com.netpulse.health.dto.NodeHealthResponse;
import com.netpulse.health.entity.HealthClassification;
import com.netpulse.health.service.NodeHealthService;
import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.routing.dto.CandidateEvaluationDto;
import com.netpulse.telemetry.entity.TelemetryRecord;
import com.netpulse.telemetry.service.TelemetryService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingCandidateServiceTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private HeartbeatService heartbeatService;

    @Mock
    private NodeHealthService nodeHealthService;

    private RoutingCandidateService candidateService;

    @BeforeEach
    void setUp() {
        candidateService = new RoutingCandidateService(nodeRepository, telemetryService, heartbeatService, nodeHealthService);
        candidateService.setEligibilityPolicy(true, false, false);
    }

    @Test
    @DisplayName("Should exclude OFFLINE and FAILED nodes from eligibility")
    void testFilterOfflineAndFailedNodes() {
        NetworkNode healthy = new NetworkNode("node-ok", "Ok", "10.0.0.1", 8080, NodeStatus.HEALTHY, 2000);
        NetworkNode offline = new NetworkNode("node-off", "Off", "10.0.0.2", 8080, NodeStatus.OFFLINE, 2000);
        NetworkNode failed = new NetworkNode("node-fail", "Fail", "10.0.0.3", 8080, NodeStatus.FAILED, 2000);

        when(nodeRepository.findAll()).thenReturn(List.of(healthy, offline, failed));

        when(heartbeatService.getHeartbeatStatus("node-ok")).thenReturn(new HeartbeatStatusResponse("node-ok", Instant.now(), 0L, LivenessStatus.ALIVE, false));
        when(heartbeatService.getHeartbeatStatus("node-off")).thenReturn(new HeartbeatStatusResponse("node-off", null, null, LivenessStatus.UNREACHABLE, false));
        when(heartbeatService.getHeartbeatStatus("node-fail")).thenReturn(new HeartbeatStatusResponse("node-fail", Instant.now(), 0L, LivenessStatus.ALIVE, false));

        when(nodeHealthService.evaluateNodeHealth(eq(healthy), any())).thenReturn(new NodeHealthResponse("node-ok", HealthClassification.HEALTHY, LivenessStatus.ALIVE, Instant.now(), Instant.now(), "OK", 20.0, 30.0, 15.0, 0.0, 0.0));
        when(nodeHealthService.evaluateNodeHealth(eq(offline), any())).thenReturn(new NodeHealthResponse("node-off", HealthClassification.OFFLINE, LivenessStatus.UNREACHABLE, null, Instant.now(), "Off", 0.0, 0.0, 0.0, 0.0, 0.0));
        when(nodeHealthService.evaluateNodeHealth(eq(failed), any())).thenReturn(new NodeHealthResponse("node-fail", HealthClassification.FAILED, LivenessStatus.ALIVE, Instant.now(), Instant.now(), "Fail", 98.0, 95.0, 450.0, 100.0, 100.0));

        List<CandidateEvaluationDto> candidates = candidateService.evaluateAllCandidates();

        assertEquals(3, candidates.size());

        CandidateEvaluationDto cOk = candidates.stream().filter(c -> c.getNodeId().equals("node-ok")).findFirst().orElseThrow();
        CandidateEvaluationDto cOff = candidates.stream().filter(c -> c.getNodeId().equals("node-off")).findFirst().orElseThrow();
        CandidateEvaluationDto cFail = candidates.stream().filter(c -> c.getNodeId().equals("node-fail")).findFirst().orElseThrow();

        assertTrue(cOk.getEligible());
        assertFalse(cOff.getEligible());
        assertFalse(cFail.getEligible());
    }

    @Test
    @DisplayName("Should exclude UNREACHABLE heartbeat nodes from eligibility")
    void testFilterUnreachableHeartbeat() {
        NetworkNode node = new NetworkNode("node-unreach", "Unreach", "10.0.0.4", 8080, NodeStatus.HEALTHY, 2000);
        when(nodeRepository.findAll()).thenReturn(List.of(node));
        when(heartbeatService.getHeartbeatStatus("node-unreach")).thenReturn(new HeartbeatStatusResponse("node-unreach", Instant.now().minusSeconds(20), 20L, LivenessStatus.UNREACHABLE, false));
        when(nodeHealthService.evaluateNodeHealth(eq(node), any())).thenReturn(new NodeHealthResponse("node-unreach", HealthClassification.FAILED, LivenessStatus.UNREACHABLE, Instant.now().minusSeconds(20), Instant.now(), "Unreachable", 0.0, 0.0, 0.0, 100.0, 100.0));

        List<CandidateEvaluationDto> candidates = candidateService.evaluateAllCandidates();

        assertFalse(candidates.getFirst().getEligible());
        assertTrue(candidates.getFirst().getEvaluationReason().contains("UNREACHABLE"));
    }
}
