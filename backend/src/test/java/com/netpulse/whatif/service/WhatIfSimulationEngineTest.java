package com.netpulse.whatif.service;

import com.netpulse.node.entity.NodeStatus;
import com.netpulse.routing.entity.RoutingStrategyType;
import com.netpulse.scheduler.dto.AlgorithmBenchmarkResult;
import com.netpulse.scheduler.dto.AlgorithmComparisonResponse;
import com.netpulse.scheduler.service.SchedulerEngine;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import com.netpulse.topology.entity.LinkStatus;
import com.netpulse.whatif.dto.*;
import com.netpulse.whatif.state.RiskIndicator;
import com.netpulse.whatif.state.ScenarioStatus;
import com.netpulse.whatif.state.ScenarioType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class WhatIfSimulationEngineTest {

    @Mock
    private SchedulerEngine schedulerEngine;

    private WhatIfSimulationEngine engine;
    private SystemSnapshotDto mockSnapshot;

    @BeforeEach
    void setUp() {
        engine = new WhatIfSimulationEngine(schedulerEngine);

        mockSnapshot = new SystemSnapshotDto();
        mockSnapshot.setSnapshotId("SNAP-TEST01");
        mockSnapshot.setCapturedAt(Instant.now());
        mockSnapshot.setTotalNodes(3);
        mockSnapshot.setHealthyNodes(3);
        mockSnapshot.setFailedNodes(0);
        mockSnapshot.setAverageCpu(30.0);
        mockSnapshot.setAverageMemory(40.0);
        mockSnapshot.setAverageLatency(20.0);
        mockSnapshot.setAveragePacketLoss(0.0);
        mockSnapshot.setTotalActiveConnections(50);

        List<SnapshotNodeDto> nodes = List.of(
                new SnapshotNodeDto("NODE-001", "Primary Node", "10.0.0.1", 8080, NodeStatus.HEALTHY, 100),
                new SnapshotNodeDto("NODE-002", "Secondary Node", "10.0.0.2", 8080, NodeStatus.HEALTHY, 100),
                new SnapshotNodeDto("NODE-003", "Tertiary Node", "10.0.0.3", 8080, NodeStatus.HEALTHY, 100)
        );
        mockSnapshot.setNodes(nodes);

        List<SnapshotLinkDto> links = List.of(
                new SnapshotLinkDto("LINK-001", "NODE-001", "NODE-002", LinkStatus.ACTIVE, 1000, 15, 0.0),
                new SnapshotLinkDto("LINK-002", "NODE-002", "NODE-003", LinkStatus.ACTIVE, 1000, 20, 0.0)
        );
        mockSnapshot.setLinks(links);

        List<SnapshotTelemetryDto> tel = List.of(
                new SnapshotTelemetryDto("NODE-001", 30.0, 40.0, 15.0, 0.0, 20, 0.0, 2000L),
                new SnapshotTelemetryDto("NODE-002", 30.0, 40.0, 20.0, 0.0, 20, 0.0, 2000L),
                new SnapshotTelemetryDto("NODE-003", 30.0, 40.0, 25.0, 0.0, 10, 0.0, 2000L)
        );
        mockSnapshot.setTelemetry(tel);
        mockSnapshot.setHealth(Collections.emptyList());
        mockSnapshot.setRoutingState(new SnapshotRoutingStateDto(RoutingStrategyType.ADAPTIVE, 100L, 0.2, 0.15, 0.2, 0.1, 0.15));
        mockSnapshot.setSchedulerState(new SnapshotSchedulerStateDto(null, SchedulerAlgorithm.ROUND_ROBIN, 4, 4, 0L, Collections.emptyList()));

        AlgorithmBenchmarkResult benchRes = new AlgorithmBenchmarkResult();
        benchRes.setAlgorithm(SchedulerAlgorithm.ROUND_ROBIN);
        benchRes.setAlgorithmName("Round Robin");
        benchRes.setAverageWaitingTime(12.0);
        benchRes.setThroughput(180.0);
        benchRes.setCompletedProcessesCount(5);

        AlgorithmComparisonResponse compResponse = new AlgorithmComparisonResponse();
        compResponse.setResults(List.of(benchRes));
        lenient().when(schedulerEngine.runBenchmarkComparison(any())).thenReturn(compResponse);
    }

    @Test
    @DisplayName("Node failure simulation should evaluate failover and select eligible replacement")
    void testNodeFailureSimulation() {
        WhatIfScenarioChangeDto change = new WhatIfScenarioChangeDto(
                ScenarioType.NODE_FAILURE, "NODE-002", null, null, null, null, null, null, null, null
        );

        SimulationRunResponse response = engine.executeSimulation(
                "SCEN-001", "Node 2 Outage", ScenarioType.NODE_FAILURE, List.of(change), 60, mockSnapshot
        );

        assertNotNull(response);
        assertEquals(ScenarioStatus.COMPLETED, response.getStatus());
        assertEquals(1, response.getComparisonMetrics().getSimulatedFailedNodes());
        assertEquals(2, response.getComparisonMetrics().getSimulatedHealthyNodes());
        assertTrue(response.getRiskIndicators().contains(RiskIndicator.FAILOVER_REQUIRED));

        assertFalse(response.getDecisions().isEmpty());
        SimulationDecisionDto decision = response.getDecisions().get(0);
        assertNotNull(decision.getSelectedNodeId());
        assertNotEquals("NODE-002", decision.getSelectedNodeId());
    }

    @Test
    @DisplayName("Traffic increase simulation should scale active connections and CPU deterministically")
    void testTrafficIncreaseSimulation() {
        WhatIfScenarioChangeDto change = new WhatIfScenarioChangeDto(
                ScenarioType.TRAFFIC_INCREASE, null, null, 40.0, null, null, null, null, null, null
        );

        SimulationRunResponse response = engine.executeSimulation(
                "SCEN-002", "Traffic Spike", ScenarioType.TRAFFIC_INCREASE, List.of(change), 60, mockSnapshot
        );

        assertNotNull(response);
        assertTrue(response.getComparisonMetrics().getSimulatedActiveConnections() > mockSnapshot.getTotalActiveConnections());
        assertTrue(response.getComparisonMetrics().getSimulatedAvgCpu() >= mockSnapshot.getAverageCpu());
    }

    @Test
    @DisplayName("Deterministic execution: identical snapshot and scenario produce identical metrics and decisions")
    void testDeterminism() {
        WhatIfScenarioChangeDto change = new WhatIfScenarioChangeDto(
                ScenarioType.NODE_FAILURE, "NODE-002", null, null, null, null, null, null, null, null
        );

        SimulationRunResponse run1 = engine.executeSimulation("SCEN-D1", "Test", ScenarioType.NODE_FAILURE, List.of(change), 60, mockSnapshot);
        SimulationRunResponse run2 = engine.executeSimulation("SCEN-D2", "Test", ScenarioType.NODE_FAILURE, List.of(change), 60, mockSnapshot);

        assertEquals(run1.getComparisonMetrics().getSimulatedHealthyNodes(), run2.getComparisonMetrics().getSimulatedHealthyNodes());
        assertEquals(run1.getComparisonMetrics().getSimulatedFailedNodes(), run2.getComparisonMetrics().getSimulatedFailedNodes());
        assertEquals(run1.getComparisonMetrics().getSimulatedAvgCpu(), run2.getComparisonMetrics().getSimulatedAvgCpu());
        assertEquals(run1.getDecisions().get(0).getSelectedNodeId(), run2.getDecisions().get(0).getSelectedNodeId());
    }

    @Test
    @DisplayName("Routing strategy comparison evaluates alternative strategies across identical topology")
    void testRoutingComparison() {
        RoutingComparisonRequest req = new RoutingComparisonRequest();
        req.setStrategies(List.of(RoutingStrategyType.ADAPTIVE, RoutingStrategyType.ROUND_ROBIN, RoutingStrategyType.LATENCY_AWARE));

        RoutingComparisonResponse resp = engine.compareRoutingStrategies(mockSnapshot, req);

        assertNotNull(resp);
        assertEquals(3, resp.getStrategyResults().size());
        assertNotNull(resp.getAnalysisSummary());
    }
}
