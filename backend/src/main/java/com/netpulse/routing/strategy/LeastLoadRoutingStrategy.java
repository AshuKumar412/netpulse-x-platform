package com.netpulse.routing.strategy;

import com.netpulse.routing.dto.CandidateEvaluationDto;
import com.netpulse.routing.dto.RoutingDecisionResponse;
import com.netpulse.routing.entity.RoutingStrategyType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Component
public class LeastLoadRoutingStrategy implements RoutingStrategy {

    @Override
    public RoutingStrategyType getType() {
        return RoutingStrategyType.LEAST_LOAD;
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
                    "No healthy reachable candidates available for least-load routing",
                    Instant.now(),
                    allCandidates
            );
        }

        for (CandidateEvaluationDto c : eligibleCandidates) {
            double normCpu = Math.min(1.0, Math.max(0.0, c.getCpuUsage() / 100.0));
            double normMem = Math.min(1.0, Math.max(0.0, c.getMemoryUsage() / 100.0));
            double normConns = Math.min(1.0, (double) c.getActiveConnections() / Math.max(10, c.getCapacity() * 0.5));
            double loadScore = Math.round(((0.4 * normCpu) + (0.3 * normMem) + (0.3 * normConns)) * 1000.0) / 1000.0;
            c.setScore(loadScore);
        }

        CandidateEvaluationDto selected = eligibleCandidates.stream()
                .min(Comparator.comparing(CandidateEvaluationDto::getScore)
                        .thenComparing(CandidateEvaluationDto::getNodeId))
                .orElse(eligibleCandidates.getFirst());

        String reason = String.format("Minimum resource load score (Score: %.3f, CPU: %.1f%%, RAM: %.1f%%, Conns: %d)",
                selected.getScore(), selected.getCpuUsage(), selected.getMemoryUsage(), selected.getActiveConnections());

        return new RoutingDecisionResponse(
                requestId,
                getType(),
                "SUCCESS",
                selected.getNodeId(),
                selected.getName(),
                allCandidates.size(),
                eligibleCandidates.size(),
                selected.getScore(),
                reason,
                Instant.now(),
                allCandidates
        );
    }
}
