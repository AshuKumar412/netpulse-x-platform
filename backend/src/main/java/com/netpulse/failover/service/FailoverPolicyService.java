package com.netpulse.failover.service;

import com.netpulse.failover.dto.FailoverPolicyConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FailoverPolicyService {

    @Value("${app.failover.enabled:true}")
    private boolean enabled = true;

    @Value("${app.failover.confirmation.consecutive-failures:2}")
    private int consecutiveFailuresRequired = 2;

    @Value("${app.failover.recovery.enabled:true}")
    private boolean recoveryEnabled = true;

    @Value("${app.failover.recovery.auto-recover:true}")
    private boolean autoRecover = true;

    @Value("${app.failover.verification.heartbeat-required:true}")
    private boolean heartbeatRequiredForRecovery = true;

    @Value("${app.failover.verification.healthy-cycles-required:2}")
    private int healthyCyclesRequiredForRecovery = 2;

    @Value("${app.failover.protection.max-attempts:3}")
    private int maxAttempts = 3;

    @Value("${app.failover.protection.cooldown-seconds:15}")
    private int cooldownSeconds = 15;

    public FailoverPolicyConfig getPolicyConfig() {
        return new FailoverPolicyConfig(
                enabled,
                consecutiveFailuresRequired,
                recoveryEnabled,
                autoRecover,
                heartbeatRequiredForRecovery,
                healthyCyclesRequiredForRecovery,
                maxAttempts,
                cooldownSeconds
        );
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
