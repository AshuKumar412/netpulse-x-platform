package com.netpulse.failover.dto;

import com.netpulse.failover.state.FailoverState;
import java.util.List;
import java.util.Map;

public class FailoverStatusResponse {

    private boolean failoverEnabled;
    private long activeFailoversCount;
    private long recoveredNodesCount;
    private long failedRecoveriesCount;
    private long totalFailoverEvents;
    private Double recoverySuccessRate; // Null if total events = 0
    private Double averageRecoveryDurationMs; // Null if no successful recovery
    private List<FailoverEventDto> activeEvents;
    private Map<String, FailoverState> nodeFailoverStates;

    public FailoverStatusResponse() {
    }

    public FailoverStatusResponse(boolean failoverEnabled,
                                  long activeFailoversCount,
                                  long recoveredNodesCount,
                                  long failedRecoveriesCount,
                                  long totalFailoverEvents,
                                  Double recoverySuccessRate,
                                  Double averageRecoveryDurationMs,
                                  List<FailoverEventDto> activeEvents,
                                  Map<String, FailoverState> nodeFailoverStates) {
        this.failoverEnabled = failoverEnabled;
        this.activeFailoversCount = activeFailoversCount;
        this.recoveredNodesCount = recoveredNodesCount;
        this.failedRecoveriesCount = failedRecoveriesCount;
        this.totalFailoverEvents = totalFailoverEvents;
        this.recoverySuccessRate = recoverySuccessRate;
        this.averageRecoveryDurationMs = averageRecoveryDurationMs;
        this.activeEvents = activeEvents;
        this.nodeFailoverStates = nodeFailoverStates;
    }

    public boolean isFailoverEnabled() {
        return failoverEnabled;
    }

    public void setFailoverEnabled(boolean failoverEnabled) {
        this.failoverEnabled = failoverEnabled;
    }

    public long getActiveFailoversCount() {
        return activeFailoversCount;
    }

    public void setActiveFailoversCount(long activeFailoversCount) {
        this.activeFailoversCount = activeFailoversCount;
    }

    public long getRecoveredNodesCount() {
        return recoveredNodesCount;
    }

    public void setRecoveredNodesCount(long recoveredNodesCount) {
        this.recoveredNodesCount = recoveredNodesCount;
    }

    public long getFailedRecoveriesCount() {
        return failedRecoveriesCount;
    }

    public void setFailedRecoveriesCount(long failedRecoveriesCount) {
        this.failedRecoveriesCount = failedRecoveriesCount;
    }

    public long getTotalFailoverEvents() {
        return totalFailoverEvents;
    }

    public void setTotalFailoverEvents(long totalFailoverEvents) {
        this.totalFailoverEvents = totalFailoverEvents;
    }

    public Double getRecoverySuccessRate() {
        return recoverySuccessRate;
    }

    public void setRecoverySuccessRate(Double recoverySuccessRate) {
        this.recoverySuccessRate = recoverySuccessRate;
    }

    public Double getAverageRecoveryDurationMs() {
        return averageRecoveryDurationMs;
    }

    public void setAverageRecoveryDurationMs(Double averageRecoveryDurationMs) {
        this.averageRecoveryDurationMs = averageRecoveryDurationMs;
    }

    public List<FailoverEventDto> getActiveEvents() {
        return activeEvents;
    }

    public void setActiveEvents(List<FailoverEventDto> activeEvents) {
        this.activeEvents = activeEvents;
    }

    public Map<String, FailoverState> getNodeFailoverStates() {
        return nodeFailoverStates;
    }

    public void setNodeFailoverStates(Map<String, FailoverState> nodeFailoverStates) {
        this.nodeFailoverStates = nodeFailoverStates;
    }
}
