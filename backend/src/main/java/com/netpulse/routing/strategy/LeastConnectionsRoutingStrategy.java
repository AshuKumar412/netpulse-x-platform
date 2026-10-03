package com.netpulse.routing.strategy;

import com.netpulse.routing.dto.CandidateEvaluationDto;
import com.netpulse.routing.dto.RoutingDecisionResponse;
import com.netpulse.routing.entity.RoutingStrategyType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Component
public class LeastConnectionsRoutingStrategy implements RoutingStrategy {

    @Override
    public RoutingStrategyType getType() {
        return RoutingStrategyType.LEAST_CONNECTIONS;
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
                    "No healthy reachable candidates available for least-connections routing",
                    Instant.now(),
                    allCandidates
            );
        }

        // Compare by activeConnections ascending, tie-breaker: nodeId
        CandidateEvaluationDto selected = eligibleCandidates.stream()
                .min(Comparator.comparing(CandidateEvaluationDto::getActiveConnections)
                        .thenComparing(CandidateEvaluationDto::getNodeId))
                .orElse(eligibleCandidates.getFirst());

        String reason = String.format("Minimum active connection load (%d active connections)", selected.getActiveConnections());

        return new RoutingDecisionResponse(
                requestId,
                getType(),
                "SUCCESS",
                selected.getNodeId(),
                selected.getName(),
                allCandidates.size(),
                eligibleCandidates.size(),
                (double) selected.getActiveConnections(),
                reason,
                Instant.now(),
                allCandidates
        );
    }
}
