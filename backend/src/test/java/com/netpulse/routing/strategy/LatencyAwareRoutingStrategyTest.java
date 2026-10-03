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

class LatencyAwareRoutingStrategyTest {

    private LatencyAwareRoutingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new LatencyAwareRoutingStrategy();
    }

    @Test
    @DisplayName("Should select candidate with lowest measured telemetry latency")
    void testSelectLowestLatency() {
        CandidateEvaluationDto slow = new CandidateEvaluationDto("node-slow", "Slow", 2000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 30.0, 30.0, 85.0, 0.0, 50, 0.0, true, "OK", null);
        CandidateEvaluationDto fast = new CandidateEvaluationDto("node-fast", "Fast", 2000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 30.0, 30.0, 12.5, 0.0, 50, 0.0, true, "OK", null);

        RoutingDecisionResponse resp = strategy.route("REQ-01", List.of(slow, fast), List.of(slow, fast));

        assertNotNull(resp);
        assertEquals("SUCCESS", resp.getStatus());
        assertEquals("node-fast", resp.getSelectedNodeId());
        assertEquals(12.5, resp.getScore());
    }
}
