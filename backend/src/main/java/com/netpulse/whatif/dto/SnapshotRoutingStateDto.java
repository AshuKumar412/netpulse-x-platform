package com.netpulse.whatif.dto;

import com.netpulse.routing.entity.RoutingStrategyType;

public class SnapshotRoutingStateDto {

    private RoutingStrategyType activeStrategy;
    private Long totalDecisions;
    private Double weightCpu;
    private Double weightMemory;
    private Double weightLatency;
    private Double weightPacketLoss;
    private Double weightConnections;

    public SnapshotRoutingStateDto() {
    }

    public SnapshotRoutingStateDto(RoutingStrategyType activeStrategy, Long totalDecisions, Double weightCpu, Double weightMemory, Double weightLatency, Double weightPacketLoss, Double weightConnections) {
        this.activeStrategy = activeStrategy;
        this.totalDecisions = totalDecisions;
        this.weightCpu = weightCpu;
        this.weightMemory = weightMemory;
        this.weightLatency = weightLatency;
        this.weightPacketLoss = weightPacketLoss;
        this.weightConnections = weightConnections;
    }

    public RoutingStrategyType getActiveStrategy() { return activeStrategy; }
    public void setActiveStrategy(RoutingStrategyType activeStrategy) { this.activeStrategy = activeStrategy; }
    public Long getTotalDecisions() { return totalDecisions; }
    public void setTotalDecisions(Long totalDecisions) { this.totalDecisions = totalDecisions; }
    public Double getWeightCpu() { return weightCpu; }
    public void setWeightCpu(Double weightCpu) { this.weightCpu = weightCpu; }
    public Double getWeightMemory() { return weightMemory; }
    public void setWeightMemory(Double weightMemory) { this.weightMemory = weightMemory; }
    public Double getWeightLatency() { return weightLatency; }
    public void setWeightLatency(Double weightLatency) { this.weightLatency = weightLatency; }
    public Double getWeightPacketLoss() { return weightPacketLoss; }
    public void setWeightPacketLoss(Double weightPacketLoss) { this.weightPacketLoss = weightPacketLoss; }
    public Double getWeightConnections() { return weightConnections; }
    public void setWeightConnections(Double weightConnections) { this.weightConnections = weightConnections; }
}
