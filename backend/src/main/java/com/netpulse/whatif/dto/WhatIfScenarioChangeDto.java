package com.netpulse.whatif.dto;

import com.netpulse.routing.entity.RoutingStrategyType;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import com.netpulse.whatif.state.ScenarioType;
import jakarta.validation.constraints.NotNull;

public class WhatIfScenarioChangeDto {

    @NotNull(message = "Change scenario type is required")
    private ScenarioType type;

    private String targetNodeId;
    private String targetLinkId;
    private Double percentage;
    private Double cpu;
    private Double memory;
    private Double additionalLatencyMs;
    private Double packetLoss;
    private RoutingStrategyType routingStrategy;
    private SchedulerAlgorithm schedulerAlgorithm;

    public WhatIfScenarioChangeDto() {
    }

    public WhatIfScenarioChangeDto(ScenarioType type, String targetNodeId, String targetLinkId, Double percentage, Double cpu, Double memory, Double additionalLatencyMs, Double packetLoss, RoutingStrategyType routingStrategy, SchedulerAlgorithm schedulerAlgorithm) {
        this.type = type;
        this.targetNodeId = targetNodeId;
        this.targetLinkId = targetLinkId;
        this.percentage = percentage;
        this.cpu = cpu;
        this.memory = memory;
        this.additionalLatencyMs = additionalLatencyMs;
        this.packetLoss = packetLoss;
        this.routingStrategy = routingStrategy;
        this.schedulerAlgorithm = schedulerAlgorithm;
    }

    public ScenarioType getType() { return type; }
    public void setType(ScenarioType type) { this.type = type; }
    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }
    public String getTargetLinkId() { return targetLinkId; }
    public void setTargetLinkId(String targetLinkId) { this.targetLinkId = targetLinkId; }
    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }
    public Double getCpu() { return cpu; }
    public void setCpu(Double cpu) { this.cpu = cpu; }
    public Double getMemory() { return memory; }
    public void setMemory(Double memory) { this.memory = memory; }
    public Double getAdditionalLatencyMs() { return additionalLatencyMs; }
    public void setAdditionalLatencyMs(Double additionalLatencyMs) { this.additionalLatencyMs = additionalLatencyMs; }
    public Double getPacketLoss() { return packetLoss; }
    public void setPacketLoss(Double packetLoss) { this.packetLoss = packetLoss; }
    public RoutingStrategyType getRoutingStrategy() { return routingStrategy; }
    public void setRoutingStrategy(RoutingStrategyType routingStrategy) { this.routingStrategy = routingStrategy; }
    public SchedulerAlgorithm getSchedulerAlgorithm() { return schedulerAlgorithm; }
    public void setSchedulerAlgorithm(SchedulerAlgorithm schedulerAlgorithm) { this.schedulerAlgorithm = schedulerAlgorithm; }
}
