package com.netpulse.routing.dto;

import com.netpulse.routing.entity.RoutingStrategyType;
import jakarta.validation.constraints.NotNull;

public class UpdateStrategyRequest {

    @NotNull(message = "Strategy must not be null")
    private RoutingStrategyType strategy;

    public UpdateStrategyRequest() {
    }

    public UpdateStrategyRequest(RoutingStrategyType strategy) {
        this.strategy = strategy;
    }

    public RoutingStrategyType getStrategy() {
        return strategy;
    }

    public void setStrategy(RoutingStrategyType strategy) {
        this.strategy = strategy;
    }
}
