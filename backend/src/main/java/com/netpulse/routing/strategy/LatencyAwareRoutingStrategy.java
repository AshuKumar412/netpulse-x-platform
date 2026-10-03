package com.netpulse.routing.strategy;

import com.netpulse.routing.dto.CandidateEvaluationDto;
import com.netpulse.routing.dto.RoutingDecisionResponse;
import com.netpulse.routing.entity.RoutingStrategyType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Component
public class LatencyAwareRoutingStrategy implements RoutingStrategy {

    @Override
    public RoutingStrategyType getType() {
        return RoutingStrategyType.LATENCY_AWARE;
    }

    @Override
    public RoutingDecisionResponse route(String requestId, List<CandidateEvaluationDto> allCandidates, List<CandidateEvaluationDto> eligibleCandidates) {
        if (eligibleCandidates == null || eligibleCandidates.isEmpty()) {
            return new RoutingDecisionResponse(
                    requestId,
                    getType(),
                    "NO_ELIGIBLE_NODE",
                    null,
                    null,
                    allCandidates.size(),
                    0,
                    null,
                    "No healthy reachable candidates available for latency-aware routing",
                    Instant.now(),
                    allCandidates
            );
        }

        for (CandidateEvaluationDto c : eligibleCandidates) {
            c.setScore(c.getLatency());
        }

        CandidateEvaluationDto selected = eligibleCandidates.stream()
                .min(Comparator.comparing(CandidateEvaluationDto::getLatency)
                        .thenComparing(CandidateEvaluationDto::getNodeId))
                .orElse(eligibleCandidates.getFirst());

        String reason = String.format("Minimum measured network latency (%.1f ms round-trip)", selected.getLatency());

        return new RoutingDecisionResponse(
                requestId,
                getType(),
                "SUCCESS",
                selected.getNodeId(),
                selected.getName(),
                allCandidates.size(),
                eligibleCandidates.size(),
                selected.getLatency(),
                reason,
                Instant.now(),
                allCandidates
        );
    }
}
