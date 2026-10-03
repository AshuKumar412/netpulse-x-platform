package com.netpulse.simulation.dto;

import java.time.Instant;

public class SimulationStatusResponse {

    private String status; // STOPPED, RUNNING, PAUSED
    private long tickCount;
    private long uptimeSeconds;
    private long totalNodes;
    private long totalLinks;
    private Instant startedAt;
    private Instant lastTickAt;

    public SimulationStatusResponse() {
    }

    public SimulationStatusResponse(String status, long tickCount, long uptimeSeconds,
                                    long totalNodes, long totalLinks,
                                    Instant startedAt, Instant lastTickAt) {
        this.status = status;
        this.tickCount = tickCount;
        this.uptimeSeconds = uptimeSeconds;
        this.totalNodes = totalNodes;
        this.totalLinks = totalLinks;
        this.startedAt = startedAt;
        this.lastTickAt = lastTickAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTickCount() {
        return tickCount;
    }

    public void setTickCount(long tickCount) {
        this.tickCount = tickCount;
    }

    public long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    public long getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(long totalNodes) {
        this.totalNodes = totalNodes;
    }

    public long getTotalLinks() {
        return totalLinks;
    }

    public void setTotalLinks(long totalLinks) {
        this.totalLinks = totalLinks;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getLastTickAt() {
        return lastTickAt;
    }

    public void setLastTickAt(Instant lastTickAt) {
        this.lastTickAt = lastTickAt;
    }
}
