package com.netpulse.routing.dto;

import com.netpulse.routing.entity.RoutingStrategyType;

public class RoutingRequest {

    private RoutingStrategyType strategy;
    private String sourceNodeId;
    private String serviceType;

    public RoutingRequest() {
    }

    public RoutingRequest(RoutingStrategyType strategy) {
        this.strategy = strategy;
    }

    public RoutingRequest(RoutingStrategyType strategy, String sourceNodeId, String serviceType) {
        this.strategy = strategy;
        this.sourceNodeId = sourceNodeId;
        this.serviceType = serviceType;
    }

    public RoutingStrategyType getStrategy() {
        return strategy;
    }

    public void setStrategy(RoutingStrategyType strategy) {
        this.strategy = strategy;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(String sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }
}
