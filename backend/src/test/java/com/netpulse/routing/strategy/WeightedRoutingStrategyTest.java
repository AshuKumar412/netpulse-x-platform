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

class WeightedRoutingStrategyTest {

    private WeightedRoutingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new WeightedRoutingStrategy();
    }

    @Test
    @DisplayName("Should distribute requests proportionally to node capacity")
    void testWeightedDistribution() {
        CandidateEvaluationDto highCap = new CandidateEvaluationDto("node-high", "High Cap", 4000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 30.0, 30.0, 20.0, 0.0, 50, 0.0, true, "OK", null);
        CandidateEvaluationDto lowCap = new CandidateEvaluationDto("node-low", "Low Cap", 1000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 30.0, 30.0, 20.0, 0.0, 50, 0.0, true, "OK", null);

        List<CandidateEvaluationDto> list = List.of(highCap, lowCap);

        int highCount = 0;
        int lowCount = 0;

        for (int i = 0; i < 10; i++) {
            RoutingDecisionResponse resp = strategy.route("REQ-" + i, list, list);
            if ("node-high".equals(resp.getSelectedNodeId())) {
                highCount++;
            } else {
                lowCount++;
            }
        }

        // 4:1 ratio -> highCount should be 8, lowCount should be 2
        assertEquals(8, highCount);
        assertEquals(2, lowCount);
    }
}
