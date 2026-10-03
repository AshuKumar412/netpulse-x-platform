package com.netpulse.failover.dto;

public class FailoverPolicyConfig {

    private boolean enabled;
    private int consecutiveFailuresRequired;
    private boolean recoveryEnabled;
    private boolean autoRecover;
    private boolean heartbeatRequiredForRecovery;
    private int healthyCyclesRequiredForRecovery;
    private int maxAttempts;
    private int cooldownSeconds;

    public FailoverPolicyConfig() {
    }

    public FailoverPolicyConfig(boolean enabled,
                                int consecutiveFailuresRequired,
                                boolean recoveryEnabled,
                                boolean autoRecover,
                                boolean heartbeatRequiredForRecovery,
                                int healthyCyclesRequiredForRecovery,
                                int maxAttempts,
                                int cooldownSeconds) {
        this.enabled = enabled;
        this.consecutiveFailuresRequired = consecutiveFailuresRequired;
        this.recoveryEnabled = recoveryEnabled;
        this.autoRecover = autoRecover;
        this.heartbeatRequiredForRecovery = heartbeatRequiredForRecovery;
        this.healthyCyclesRequiredForRecovery = healthyCyclesRequiredForRecovery;
        this.maxAttempts = maxAttempts;
        this.cooldownSeconds = cooldownSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getConsecutiveFailuresRequired() {
        return consecutiveFailuresRequired;
    }

    public void setConsecutiveFailuresRequired(int consecutiveFailuresRequired) {
        this.consecutiveFailuresRequired = consecutiveFailuresRequired;
    }

    public boolean isRecoveryEnabled() {
        return recoveryEnabled;
    }

    public void setRecoveryEnabled(boolean recoveryEnabled) {
        this.recoveryEnabled = recoveryEnabled;
    }

    public boolean isAutoRecover() {
        return autoRecover;
    }

    public void setAutoRecover(boolean autoRecover) {
        this.autoRecover = autoRecover;
    }

    public boolean isHeartbeatRequiredForRecovery() {
        return heartbeatRequiredForRecovery;
    }

    public void setHeartbeatRequiredForRecovery(boolean heartbeatRequiredForRecovery) {
        this.heartbeatRequiredForRecovery = heartbeatRequiredForRecovery;
    }

    public int getHealthyCyclesRequiredForRecovery() {
        return healthyCyclesRequiredForRecovery;
    }

    public void setHealthyCyclesRequiredForRecovery(int healthyCyclesRequiredForRecovery) {
        this.healthyCyclesRequiredForRecovery = healthyCyclesRequiredForRecovery;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }
}
