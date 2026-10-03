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

class LeastConnectionsRoutingStrategyTest {

    private LeastConnectionsRoutingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new LeastConnectionsRoutingStrategy();
    }

    @Test
    @DisplayName("Should select candidate with lowest active connections")
    void testSelectLowestActiveConnections() {
        CandidateEvaluationDto n1 = new CandidateEvaluationDto("node-a", "Node A", 2000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 50.0, 50.0, 20.0, 0.0, 45, 0.0, true, "OK", null);
        CandidateEvaluationDto n2 = new CandidateEvaluationDto("node-b", "Node B", 2000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 50.0, 50.0, 20.0, 0.0, 12, 0.0, true, "OK", null);
        CandidateEvaluationDto n3 = new CandidateEvaluationDto("node-c", "Node C", 2000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 50.0, 50.0, 20.0, 0.0, 30, 0.0, true, "OK", null);

        RoutingDecisionResponse resp = strategy.route("REQ-01", List.of(n1, n2, n3), List.of(n1, n2, n3));

        assertNotNull(resp);
        assertEquals("SUCCESS", resp.getStatus());
        assertEquals("node-b", resp.getSelectedNodeId());
        assertEquals(RoutingStrategyType.LEAST_CONNECTIONS, resp.getStrategy());
    }

    @Test
    @DisplayName("Should break ties deterministically using lowest nodeId")
    void testTieBreaking() {
        CandidateEvaluationDto n1 = new CandidateEvaluationDto("node-z", "Node Z", 2000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 50.0, 50.0, 20.0, 0.0, 10, 0.0, true, "OK", null);
        CandidateEvaluationDto n2 = new CandidateEvaluationDto("node-a", "Node A", 2000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 50.0, 50.0, 20.0, 0.0, 10, 0.0, true, "OK", null);

        RoutingDecisionResponse resp = strategy.route("REQ-02", List.of(n1, n2), List.of(n1, n2));

        assertEquals("node-a", resp.getSelectedNodeId());
    }
}
