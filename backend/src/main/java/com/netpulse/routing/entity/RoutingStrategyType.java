package com.netpulse.routing.entity;

public enum RoutingStrategyType {
    ROUND_ROBIN,
    LEAST_CONNECTIONS,
    LEAST_LOAD,
    LATENCY_AWARE,
    WEIGHTED,
    ADAPTIVE
}
