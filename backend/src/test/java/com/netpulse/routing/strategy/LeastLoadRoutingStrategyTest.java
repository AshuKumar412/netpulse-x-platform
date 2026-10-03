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

class LeastLoadRoutingStrategyTest {

    private LeastLoadRoutingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new LeastLoadRoutingStrategy();
    }

    @Test
    @DisplayName("Should select candidate with lowest normalized resource load score")
    void testSelectLowestResourceLoad() {
        CandidateEvaluationDto heavy = new CandidateEvaluationDto("node-heavy", "Heavy", 2000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 85.0, 80.0, 20.0, 0.0, 500, 0.0, true, "OK", null);
        CandidateEvaluationDto light = new CandidateEvaluationDto("node-light", "Light", 2000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 20.0, 25.0, 20.0, 0.0, 50, 0.0, true, "OK", null);

        RoutingDecisionResponse resp = strategy.route("REQ-01", List.of(heavy, light), List.of(heavy, light));

        assertNotNull(resp);
        assertEquals("SUCCESS", resp.getStatus());
        assertEquals("node-light", resp.getSelectedNodeId());
        assertTrue(light.getScore() < heavy.getScore());
    }
}
