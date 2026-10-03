package com.netpulse.routing.strategy;

import com.netpulse.health.entity.HealthClassification;
import com.netpulse.routing.dto.CandidateEvaluationDto;
import com.netpulse.routing.dto.RoutingDecisionResponse;
import com.netpulse.routing.entity.RoutingStrategyType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Component
public class AdaptiveRoutingStrategy implements RoutingStrategy {

    @Value("${app.routing.weights.cpu:0.20}")
    private double weightCpu = 0.20;

    @Value("${app.routing.weights.memory:0.15}")
    private double weightMemory = 0.15;

    @Value("${app.routing.weights.latency:0.20}")
    private double weightLatency = 0.20;

    @Value("${app.routing.weights.packet-loss:0.10}")
    private double weightPacketLoss = 0.10;

    @Value("${app.routing.weights.error-rate:0.10}")
    private double weightErrorRate = 0.10;

    @Value("${app.routing.weights.connections:0.15}")
    private double weightConnections = 0.15;

    @Value("${app.routing.weights.capacity:0.10}")
    private double weightCapacity = 0.10;

    @Override
    public RoutingStrategyType getType() {
        return RoutingStrategyType.ADAPTIVE;
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
                    "No healthy reachable candidates available for adaptive multi-metric routing",
                    Instant.now(),
                    allCandidates
            );
        }

        for (CandidateEvaluationDto c : eligibleCandidates) {
            double normCpu = Math.min(1.0, Math.max(0.0, (c.getCpuUsage() != null ? c.getCpuUsage() : 0.0) / 100.0));
            double normMem = Math.min(1.0, Math.max(0.0, (c.getMemoryUsage() != null ? c.getMemoryUsage() : 0.0) / 100.0));
            double normLat = Math.min(1.0, Math.max(0.0, (c.getLatency() != null ? c.getLatency() : 0.0) / 250.0));
            double normLoss = Math.min(1.0, Math.max(0.0, (c.getPacketLoss() != null ? c.getPacketLoss() : 0.0) / 10.0));
            double normErr = Math.min(1.0, Math.max(0.0, (c.getErrorRate() != null ? c.getErrorRate() : 0.0) / 10.0));

            int cap = c.getCapacity() != null ? c.getCapacity() : 1000;
            int conns = c.getActiveConnections() != null ? c.getActiveConnections() : 0;
            double normConns = Math.min(1.0, (double) conns / Math.max(10, cap * 0.8));
            double normCapPenalty = 1.0 - Math.min(1.0, (double) cap / 10000.0);

            double healthPenalty = 0.0;
            if (c.getHealthStatus() == HealthClassification.WARNING) {
                healthPenalty = 0.15;
            } else if (c.getHealthStatus() == HealthClassification.DEGRADED) {
                healthPenalty = 0.35;
            }

            double compositeScore = (normCpu * weightCpu)
                    + (normMem * weightMemory)
                    + (normLat * weightLatency)
                    + (normLoss * weightPacketLoss)
                    + (normErr * weightErrorRate)
                    + (normConns * weightConnections)
                    + (normCapPenalty * weightCapacity)
                    + healthPenalty;

            c.setScore(Math.round(compositeScore * 1000.0) / 1000.0);
        }

        CandidateEvaluationDto selected = eligibleCandidates.stream()
                .min(Comparator.comparing(CandidateEvaluationDto::getScore)
                        .thenComparing(CandidateEvaluationDto::getNodeId))
                .orElse(eligibleCandidates.getFirst());

        String reason = String.format("Optimal composite routing score (Score: %.3f, CPU: %.1f%%, RAM: %.1f%%, Latency: %.1fms, Loss: %.1f%%, Health: %s)",
                selected.getScore(), selected.getCpuUsage(), selected.getMemoryUsage(),
                selected.getLatency(), selected.getPacketLoss(), selected.getHealthStatus());

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

    public void setWeights(double cpu, double mem, double lat, double loss, double err, double conns, double cap) {
        this.weightCpu = cpu;
        this.weightMemory = mem;
        this.weightLatency = lat;
        this.weightPacketLoss = loss;
        this.weightErrorRate = err;
        this.weightConnections = conns;
        this.weightCapacity = cap;
    }
}
