package com.netpulse.monitoring.dto;

import java.time.Instant;

public class MonitoringStatusResponse {

    private String status; // RUNNING, STOPPED
    private Long cycleCount;
    private Long uptimeSeconds;
    private Integer monitoredNodeCount;
    private Instant startedAt;
    private Instant lastCycleAt;

    public MonitoringStatusResponse() {
    }

    public MonitoringStatusResponse(String status, Long cycleCount, Long uptimeSeconds,
                                    Integer monitoredNodeCount, Instant startedAt, Instant lastCycleAt) {
        this.status = status;
        this.cycleCount = cycleCount;
        this.uptimeSeconds = uptimeSeconds;
        this.monitoredNodeCount = monitoredNodeCount;
        this.startedAt = startedAt;
        this.lastCycleAt = lastCycleAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCycleCount() {
        return cycleCount;
    }

    public void setCycleCount(Long cycleCount) {
        this.cycleCount = cycleCount;
    }

    public Long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(Long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    public Integer getMonitoredNodeCount() {
        return monitoredNodeCount;
    }

    public void setMonitoredNodeCount(Integer monitoredNodeCount) {
        this.monitoredNodeCount = monitoredNodeCount;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getLastCycleAt() {
        return lastCycleAt;
    }

    public void setLastCycleAt(Instant lastCycleAt) {
        this.lastCycleAt = lastCycleAt;
    }
}
