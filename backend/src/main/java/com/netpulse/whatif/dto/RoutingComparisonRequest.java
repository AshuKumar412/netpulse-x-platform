package com.netpulse.whatif.dto;

import com.netpulse.routing.entity.RoutingStrategyType;
import java.util.List;

public class RoutingComparisonRequest {

    private List<RoutingStrategyType> strategies;
    private List<WhatIfScenarioChangeDto> scenarioChanges;
    private int trafficSamplesCount = 20;

    public RoutingComparisonRequest() {
    }

    public List<RoutingStrategyType> getStrategies() { return strategies; }
    public void setStrategies(List<RoutingStrategyType> strategies) { this.strategies = strategies; }
    public List<WhatIfScenarioChangeDto> getScenarioChanges() { return scenarioChanges; }
    public void setScenarioChanges(List<WhatIfScenarioChangeDto> scenarioChanges) { this.scenarioChanges = scenarioChanges; }
    public int getTrafficSamplesCount() { return trafficSamplesCount; }
    public void setTrafficSamplesCount(int trafficSamplesCount) { this.trafficSamplesCount = trafficSamplesCount; }
}
