package com.netpulse.routing.strategy;

import com.netpulse.health.entity.HealthClassification;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.routing.dto.CandidateEvaluationDto;
import com.netpulse.routing.dto.RoutingDecisionResponse;
import com.netpulse.routing.entity.RoutingStrategyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoundRobinRoutingStrategyTest {

    private RoundRobinRoutingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RoundRobinRoutingStrategy();
    }

    @Test
    @DisplayName("Should return NO_ELIGIBLE_NODE when eligible candidates list is empty")
    void testEmptyEligibleCandidates() {
        RoutingDecisionResponse response = strategy.route("REQ-01", Collections.emptyList(), Collections.emptyList());

        assertNotNull(response);
        assertEquals("NO_ELIGIBLE_NODE", response.getStatus());
        assertNull(response.getSelectedNodeId());
        assertEquals(RoutingStrategyType.ROUND_ROBIN, response.getStrategy());
    }

    @Test
    @DisplayName("Should cycle through eligible nodes sequentially")
    void testSequentialRoundRobin() {
        CandidateEvaluationDto n1 = new CandidateEvaluationDto("node-a", "Node A", 2000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 20.0, 30.0, 15.0, 0.0, 10, 0.0, true, "OK", null);
        CandidateEvaluationDto n2 = new CandidateEvaluationDto("node-b", "Node B", 3000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 25.0, 35.0, 18.0, 0.0, 15, 0.0, true, "OK", null);
        CandidateEvaluationDto n3 = new CandidateEvaluationDto("node-c", "Node C", 1000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 30.0, 40.0, 20.0, 0.0, 20, 0.0, true, "OK", null);

        List<CandidateEvaluationDto> eligible = List.of(n1, n2, n3);

        RoutingDecisionResponse r1 = strategy.route("REQ-1", eligible, eligible);
        RoutingDecisionResponse r2 = strategy.route("REQ-2", eligible, eligible);
        RoutingDecisionResponse r3 = strategy.route("REQ-3", eligible, eligible);
        RoutingDecisionResponse r4 = strategy.route("REQ-4", eligible, eligible);

        assertEquals("node-a", r1.getSelectedNodeId());
        assertEquals("node-b", r2.getSelectedNodeId());
        assertEquals("node-c", r3.getSelectedNodeId());
        assertEquals("node-a", r4.getSelectedNodeId());
    }
}
