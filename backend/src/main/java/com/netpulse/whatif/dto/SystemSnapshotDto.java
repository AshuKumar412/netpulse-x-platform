package com.netpulse.whatif.dto;

import java.time.Instant;
import java.util.List;

public class SystemSnapshotDto {

    private String snapshotId;
    private Instant capturedAt;
    private List<SnapshotNodeDto> nodes;
    private List<SnapshotLinkDto> links;
    private List<SnapshotTelemetryDto> telemetry;
    private List<SnapshotHealthDto> health;
    private SnapshotRoutingStateDto routingState;
    private SnapshotSchedulerStateDto schedulerState;
    private int totalNodes;
    private int healthyNodes;
    private int failedNodes;
    private double averageCpu;
    private double averageMemory;
    private double averageLatency;
    private double averagePacketLoss;
    private int totalActiveConnections;

    public SystemSnapshotDto() {
    }

    public String getSnapshotId() { return snapshotId; }
    public void setSnapshotId(String snapshotId) { this.snapshotId = snapshotId; }
    public Instant getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Instant capturedAt) { this.capturedAt = capturedAt; }
    public List<SnapshotNodeDto> getNodes() { return nodes; }
    public void setNodes(List<SnapshotNodeDto> nodes) { this.nodes = nodes; }
    public List<SnapshotLinkDto> getLinks() { return links; }
    public void setLinks(List<SnapshotLinkDto> links) { this.links = links; }
    public List<SnapshotTelemetryDto> getTelemetry() { return telemetry; }
    public void setTelemetry(List<SnapshotTelemetryDto> telemetry) { this.telemetry = telemetry; }
    public List<SnapshotHealthDto> getHealth() { return health; }
    public void setHealth(List<SnapshotHealthDto> health) { this.health = health; }
    public SnapshotRoutingStateDto getRoutingState() { return routingState; }
    public void setRoutingState(SnapshotRoutingStateDto routingState) { this.routingState = routingState; }
    public SnapshotSchedulerStateDto getSchedulerState() { return schedulerState; }
    public void setSchedulerState(SnapshotSchedulerStateDto schedulerState) { this.schedulerState = schedulerState; }
    public int getTotalNodes() { return totalNodes; }
    public void setTotalNodes(int totalNodes) { this.totalNodes = totalNodes; }
    public int getHealthyNodes() { return healthyNodes; }
    public void setHealthyNodes(int healthyNodes) { this.healthyNodes = healthyNodes; }
    public int getFailedNodes() { return failedNodes; }
    public void setFailedNodes(int failedNodes) { this.failedNodes = failedNodes; }
    public double getAverageCpu() { return averageCpu; }
    public void setAverageCpu(double averageCpu) { this.averageCpu = averageCpu; }
    public double getAverageMemory() { return averageMemory; }
    public void setAverageMemory(double averageMemory) { this.averageMemory = averageMemory; }
    public double getAverageLatency() { return averageLatency; }
    public void setAverageLatency(double averageLatency) { this.averageLatency = averageLatency; }
    public double getAveragePacketLoss() { return averagePacketLoss; }
    public void setAveragePacketLoss(double averagePacketLoss) { this.averagePacketLoss = averagePacketLoss; }
    public int getTotalActiveConnections() { return totalActiveConnections; }
    public void setTotalActiveConnections(int totalActiveConnections) { this.totalActiveConnections = totalActiveConnections; }
}
