package com.netpulse.routing.strategy;

import com.netpulse.routing.dto.CandidateEvaluationDto;
import com.netpulse.routing.dto.RoutingDecisionResponse;
import com.netpulse.routing.entity.RoutingStrategyType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class RoundRobinRoutingStrategy implements RoutingStrategy {

    private final AtomicInteger cursor = new AtomicInteger(0);

    @Override
    public RoutingStrategyType getType() {
        return RoutingStrategyType.ROUND_ROBIN;
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
                    "No healthy reachable candidates available for round-robin routing",
                    Instant.now(),
                    allCandidates
            );
        }

        // Sort eligible candidates deterministically by nodeId
        List<CandidateEvaluationDto> sorted = eligibleCandidates.stream()
                .sorted(Comparator.comparing(CandidateEvaluationDto::getNodeId))
                .collect(Collectors.toList());

        int index = Math.abs(cursor.getAndIncrement() % sorted.size());
        CandidateEvaluationDto selected = sorted.get(index);

        String reason = String.format("Sequential round-robin dispatch (Node %d of %d eligible candidates)",
                index + 1, sorted.size());

        return new RoutingDecisionResponse(
                requestId,
                getType(),
                "SUCCESS",
                selected.getNodeId(),
                selected.getName(),
                allCandidates.size(),
                sorted.size(),
                (double) (index + 1),
                reason,
                Instant.now(),
                allCandidates
        );
    }
}
