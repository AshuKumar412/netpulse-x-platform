package com.netpulse.routing.strategy;

import com.netpulse.routing.dto.CandidateEvaluationDto;
import com.netpulse.routing.dto.RoutingDecisionResponse;
import com.netpulse.routing.entity.RoutingStrategyType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class WeightedRoutingStrategy implements RoutingStrategy {

    // Running state for smooth weighted round-robin
    private final Map<String, Integer> currentWeights = new ConcurrentHashMap<>();

    @Override
    public RoutingStrategyType getType() {
        return RoutingStrategyType.WEIGHTED;
    }

    @Override
    public synchronized RoutingDecisionResponse route(String requestId, List<CandidateEvaluationDto> allCandidates, List<CandidateEvaluationDto> eligibleCandidates) {
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
                    "No healthy reachable candidates available for weighted capacity routing",
                    Instant.now(),
                    allCandidates
            );
        }

        // Sort candidates deterministically
        List<CandidateEvaluationDto> sorted = eligibleCandidates.stream()
                .sorted(Comparator.comparing(CandidateEvaluationDto::getNodeId))
                .collect(Collectors.toList());

        int totalWeight = 0;
        CandidateEvaluationDto best = null;
        int maxCurrentWeight = Integer.MIN_VALUE;

        for (CandidateEvaluationDto c : sorted) {
            // Calculate effective weight from node capacity (clamped between 1 and 100)
            int weight = Math.max(1, Math.min(100, (c.getCapacity() != null ? c.getCapacity() : 1000) / 100));
            totalWeight += weight;

            int current = currentWeights.getOrDefault(c.getNodeId(), 0) + weight;
            currentWeights.put(c.getNodeId(), current);
            c.setScore((double) weight);

            if (current > maxCurrentWeight) {
                maxCurrentWeight = current;
                best = c;
            }
        }

        if (best != null) {
            currentWeights.put(best.getNodeId(), maxCurrentWeight - totalWeight);
        } else {
            best = sorted.getFirst();
        }

        String reason = String.format("Smooth weighted capacity scheduling (Capacity Weight: %d req/s units, Total Pool: %d)",
                best.getCapacity(), totalWeight);

        return new RoutingDecisionResponse(
                requestId,
                getType(),
                "SUCCESS",
                best.getNodeId(),
                best.getName(),
                allCandidates.size(),
                sorted.size(),
                best.getScore(),
                reason,
                Instant.now(),
                allCandidates
        );
    }
}
