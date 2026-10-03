package com.netpulse.routing.dto;

import com.netpulse.routing.entity.RoutingStrategyType;

import java.util.Map;

public class RoutingConfigResponse {

    private RoutingStrategyType defaultStrategy;
    private Boolean allowWarning;
    private Boolean allowDegraded;
    private Boolean allowSuspected;
    private Map<String, Double> weights;

    public RoutingConfigResponse() {
    }

    public RoutingConfigResponse(RoutingStrategyType defaultStrategy, Boolean allowWarning,
                                 Boolean allowDegraded, Boolean allowSuspected,
                                 Map<String, Double> weights) {
        this.defaultStrategy = defaultStrategy;
        this.allowWarning = allowWarning;
        this.allowDegraded = allowDegraded;
        this.allowSuspected = allowSuspected;
        this.weights = weights;
    }

    public RoutingStrategyType getDefaultStrategy() {
        return defaultStrategy;
    }

    public void setDefaultStrategy(RoutingStrategyType defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
    }

    public Boolean getAllowWarning() {
        return allowWarning;
    }

    public void setAllowWarning(Boolean allowWarning) {
        this.allowWarning = allowWarning;
    }

    public Boolean getAllowDegraded() {
        return allowDegraded;
    }

    public void setAllowDegraded(Boolean allowDegraded) {
        this.allowDegraded = allowDegraded;
    }

    public Boolean getAllowSuspected() {
        return allowSuspected;
    }

    public void setAllowSuspected(Boolean allowSuspected) {
        this.allowSuspected = allowSuspected;
    }

    public Map<String, Double> getWeights() {
        return weights;
    }

    public void setWeights(Map<String, Double> weights) {
        this.weights = weights;
    }
}
