package com.netpulse.scheduler.dto;

import com.netpulse.scheduler.state.SchedulerAlgorithm;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SchedulerPolicyConfig {

    @NotNull(message = "Algorithm is required")
    private SchedulerAlgorithm algorithm = SchedulerAlgorithm.ROUND_ROBIN;

    @Min(value = 1, message = "Time quantum must be at least 1")
    @Max(value = 50, message = "Time quantum cannot exceed 50")
    private int timeQuantum = 4;

    @Min(value = 0, message = "Context switch cost must be non-negative")
    @Max(value = 10, message = "Context switch cost cannot exceed 10ms")
    private double contextSwitchCostMs = 0.5;

    @Min(value = 1, message = "Cores per node must be at least 1")
    @Max(value = 16, message = "Cores per node cannot exceed 16")
    private int coresPerNode = 4;

    private boolean agingEnabled = true;

    @Min(value = 1, message = "Aging interval ticks must be at least 1")
    private int agingIntervalTicks = 10;

    @Min(value = 5, message = "Starvation threshold ticks must be at least 5")
    private int starvationThresholdTicks = 25;

    @Min(value = 10, message = "Tick interval must be at least 10ms")
    @Max(value = 2000, message = "Tick interval cannot exceed 2000ms")
    private int tickIntervalMs = 100;

    public SchedulerPolicyConfig() {
    }

    public SchedulerPolicyConfig(SchedulerAlgorithm algorithm, int timeQuantum, double contextSwitchCostMs, int coresPerNode, boolean agingEnabled, int agingIntervalTicks, int starvationThresholdTicks, int tickIntervalMs) {
        this.algorithm = algorithm;
        this.timeQuantum = timeQuantum;
        this.contextSwitchCostMs = contextSwitchCostMs;
        this.coresPerNode = coresPerNode;
        this.agingEnabled = agingEnabled;
        this.agingIntervalTicks = agingIntervalTicks;
        this.starvationThresholdTicks = starvationThresholdTicks;
        this.tickIntervalMs = tickIntervalMs;
    }

    public SchedulerAlgorithm getAlgorithm() { return algorithm; }
    public void setAlgorithm(SchedulerAlgorithm algorithm) { this.algorithm = algorithm; }
    public int getTimeQuantum() { return timeQuantum; }
    public void setTimeQuantum(int timeQuantum) { this.timeQuantum = timeQuantum; }
    public double getContextSwitchCostMs() { return contextSwitchCostMs; }
    public void setContextSwitchCostMs(double contextSwitchCostMs) { this.contextSwitchCostMs = contextSwitchCostMs; }
    public int getCoresPerNode() { return coresPerNode; }
    public void setCoresPerNode(int coresPerNode) { this.coresPerNode = coresPerNode; }
    public boolean isAgingEnabled() { return agingEnabled; }
    public void setAgingEnabled(boolean agingEnabled) { this.agingEnabled = agingEnabled; }
    public int getAgingIntervalTicks() { return agingIntervalTicks; }
    public void setAgingIntervalTicks(int agingIntervalTicks) { this.agingIntervalTicks = agingIntervalTicks; }
    public int getStarvationThresholdTicks() { return starvationThresholdTicks; }
    public void setStarvationThresholdTicks(int starvationThresholdTicks) { this.starvationThresholdTicks = starvationThresholdTicks; }
    public int getTickIntervalMs() { return tickIntervalMs; }
    public void setTickIntervalMs(int tickIntervalMs) { this.tickIntervalMs = tickIntervalMs; }
}
