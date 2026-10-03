package com.netpulse.chaos.dto;

import java.util.List;

public class ChaosStatusResponse {

    private boolean enabled;
    private long activeExperimentsCount;
    private long totalExperimentsCount;
    private long successfulCount;
    private long failedCount;
    private List<ChaosExperimentDto> activeExperiments;
    private ChaosPolicyConfig policy;

    public ChaosStatusResponse() {
    }

    public ChaosStatusResponse(boolean enabled,
                               long activeExperimentsCount,
                               long totalExperimentsCount,
                               long successfulCount,
                               long failedCount,
                               List<ChaosExperimentDto> activeExperiments,
                               ChaosPolicyConfig policy) {
        this.enabled = enabled;
        this.activeExperimentsCount = activeExperimentsCount;
        this.totalExperimentsCount = totalExperimentsCount;
        this.successfulCount = successfulCount;
        this.failedCount = failedCount;
        this.activeExperiments = activeExperiments;
        this.policy = policy;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getActiveExperimentsCount() {
        return activeExperimentsCount;
    }

    public void setActiveExperimentsCount(long activeExperimentsCount) {
        this.activeExperimentsCount = activeExperimentsCount;
    }

    public long getTotalExperimentsCount() {
        return totalExperimentsCount;
    }

    public void setTotalExperimentsCount(long totalExperimentsCount) {
        this.totalExperimentsCount = totalExperimentsCount;
    }

    public long getSuccessfulCount() {
        return successfulCount;
    }

    public void setSuccessfulCount(long successfulCount) {
        this.successfulCount = successfulCount;
    }

    public long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(long failedCount) {
        this.failedCount = failedCount;
    }

    public List<ChaosExperimentDto> getActiveExperiments() {
        return activeExperiments;
    }

    public void setActiveExperiments(List<ChaosExperimentDto> activeExperiments) {
        this.activeExperiments = activeExperiments;
    }

    public ChaosPolicyConfig getPolicy() {
        return policy;
    }

    public void setPolicy(ChaosPolicyConfig policy) {
        this.policy = policy;
    }
}
