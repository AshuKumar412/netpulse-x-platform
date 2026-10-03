package com.netpulse.chaos.dto;

import java.util.List;

public class ChaosPolicyConfig {

    private boolean enabled;
    private int maxDurationSeconds;
    private int maxConcurrentExperiments;
    private int cooldownSeconds;
    private List<String> protectedNodes;

    public ChaosPolicyConfig() {
    }

    public ChaosPolicyConfig(boolean enabled,
                             int maxDurationSeconds,
                             int maxConcurrentExperiments,
                             int cooldownSeconds,
                             List<String> protectedNodes) {
        this.enabled = enabled;
        this.maxDurationSeconds = maxDurationSeconds;
        this.maxConcurrentExperiments = maxConcurrentExperiments;
        this.cooldownSeconds = cooldownSeconds;
        this.protectedNodes = protectedNodes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxDurationSeconds() {
        return maxDurationSeconds;
    }

    public void setMaxDurationSeconds(int maxDurationSeconds) {
        this.maxDurationSeconds = maxDurationSeconds;
    }

    public int getMaxConcurrentExperiments() {
        return maxConcurrentExperiments;
    }

    public void setMaxConcurrentExperiments(int maxConcurrentExperiments) {
        this.maxConcurrentExperiments = maxConcurrentExperiments;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public List<String> getProtectedNodes() {
        return protectedNodes;
    }

    public void setProtectedNodes(List<String> protectedNodes) {
        this.protectedNodes = protectedNodes;
    }
}
