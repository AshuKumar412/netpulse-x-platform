package com.netpulse.routing.dto;

import com.netpulse.routing.entity.RoutingStrategyType;

import java.time.Instant;
import java.util.List;

public class RoutingStatusResponse {

    private RoutingStrategyType activeStrategy;
    private Long totalDecisionsCount;
    private Long uptimeSeconds;
    private Integer availableNodesCount;
    private List<RoutingStrategyType> supportedStrategies;
    private Instant lastDecisionAt;

    public RoutingStatusResponse() {
    }

    public RoutingStatusResponse(RoutingStrategyType activeStrategy, Long totalDecisionsCount,
                                 Long uptimeSeconds, Integer availableNodesCount,
                                 List<RoutingStrategyType> supportedStrategies, Instant lastDecisionAt) {
        this.activeStrategy = activeStrategy;
        this.totalDecisionsCount = totalDecisionsCount;
        this.uptimeSeconds = uptimeSeconds;
        this.availableNodesCount = availableNodesCount;
        this.supportedStrategies = supportedStrategies;
        this.lastDecisionAt = lastDecisionAt;
    }

    public RoutingStrategyType getActiveStrategy() {
        return activeStrategy;
    }

    public void setActiveStrategy(RoutingStrategyType activeStrategy) {
        this.activeStrategy = activeStrategy;
    }

    public Long getTotalDecisionsCount() {
        return totalDecisionsCount;
    }

    public void setTotalDecisionsCount(Long totalDecisionsCount) {
        this.totalDecisionsCount = totalDecisionsCount;
    }

    public Long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(Long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    public Integer getAvailableNodesCount() {
        return availableNodesCount;
    }

    public void setAvailableNodesCount(Integer availableNodesCount) {
        this.availableNodesCount = availableNodesCount;
    }

    public List<RoutingStrategyType> getSupportedStrategies() {
        return supportedStrategies;
    }

    public void setSupportedStrategies(List<RoutingStrategyType> supportedStrategies) {
        this.supportedStrategies = supportedStrategies;
    }

    public Instant getLastDecisionAt() {
        return lastDecisionAt;
    }

    public void setLastDecisionAt(Instant lastDecisionAt) {
        this.lastDecisionAt = lastDecisionAt;
    }
}
