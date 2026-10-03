package com.netpulse.routing.strategy;

import com.netpulse.health.entity.HealthClassification;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.routing.dto.CandidateEvaluationDto;
import com.netpulse.routing.dto.RoutingDecisionResponse;
import com.netpulse.routing.entity.RoutingStrategyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveRoutingStrategyTest {

    private AdaptiveRoutingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new AdaptiveRoutingStrategy();
        strategy.setWeights(0.20, 0.15, 0.20, 0.10, 0.10, 0.15, 0.10);
    }

    @Test
    @DisplayName("Should select optimal candidate combining telemetry metrics and health score")
    void testAdaptiveSelection() {
        CandidateEvaluationDto degradedNode = new CandidateEvaluationDto("node-a", "Node A", 2000, HealthClassification.WARNING,
                LivenessStatus.ALIVE, 75.0, 70.0, 90.0, 2.5, 40, 1.5, true, "OK", null);
        CandidateEvaluationDto optimalNode = new CandidateEvaluationDto("node-b", "Node B", 5000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 22.0, 30.0, 14.0, 0.0, 10, 0.0, true, "OK", null);

        RoutingDecisionResponse resp = strategy.route("REQ-01", List.of(degradedNode, optimalNode), List.of(degradedNode, optimalNode));

        assertNotNull(resp);
        assertEquals("SUCCESS", resp.getStatus());
        assertEquals("node-b", resp.getSelectedNodeId());
        assertEquals(RoutingStrategyType.ADAPTIVE, resp.getStrategy());
        assertTrue(optimalNode.getScore() < degradedNode.getScore());
    }

    @Test
    @DisplayName("Should break ties deterministically using lowest nodeId")
    void testAdaptiveTieBreaking() {
        CandidateEvaluationDto n1 = new CandidateEvaluationDto("node-z", "Node Z", 2000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 20.0, 30.0, 15.0, 0.0, 10, 0.0, true, "OK", null);
        CandidateEvaluationDto n2 = new CandidateEvaluationDto("node-a", "Node A", 2000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 20.0, 30.0, 15.0, 0.0, 10, 0.0, true, "OK", null);

        RoutingDecisionResponse resp = strategy.route("REQ-02", List.of(n1, n2), List.of(n1, n2));

        assertEquals("node-a", resp.getSelectedNodeId());
    }
}
