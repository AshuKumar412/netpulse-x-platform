package com.netpulse.whatif.dto;

import java.util.List;

public class BaselineVsSimulatedMetricsDto {

    private int baselineHealthyNodes;
    private int simulatedHealthyNodes;
    private int baselineFailedNodes;
    private int simulatedFailedNodes;
    private int baselineActiveLinks;
    private int simulatedActiveLinks;
    private double baselineAvgCpu;
    private double simulatedAvgCpu;
    private double baselineAvgMemory;
    private double simulatedAvgMemory;
    private double baselineAvgLatency;
    private double simulatedAvgLatency;
    private double baselineAvgPacketLoss;
    private double simulatedAvgPacketLoss;
    private int baselineActiveConnections;
    private int simulatedActiveConnections;
    private double baselineAvgWaitTime;
    private double simulatedAvgWaitTime;
    private double baselineThroughput;
    private double simulatedThroughput;
    private List<MetricImpactDto> impacts;

    public BaselineVsSimulatedMetricsDto() {
    }

    public int getBaselineHealthyNodes() { return baselineHealthyNodes; }
    public void setBaselineHealthyNodes(int baselineHealthyNodes) { this.baselineHealthyNodes = baselineHealthyNodes; }
    public int getSimulatedHealthyNodes() { return simulatedHealthyNodes; }
    public void setSimulatedHealthyNodes(int simulatedHealthyNodes) { this.simulatedHealthyNodes = simulatedHealthyNodes; }
    public int getBaselineFailedNodes() { return baselineFailedNodes; }
    public void setBaselineFailedNodes(int baselineFailedNodes) { this.baselineFailedNodes = baselineFailedNodes; }
    public int getSimulatedFailedNodes() { return simulatedFailedNodes; }
    public void setSimulatedFailedNodes(int simulatedFailedNodes) { this.simulatedFailedNodes = simulatedFailedNodes; }
    public int getBaselineActiveLinks() { return baselineActiveLinks; }
    public void setBaselineActiveLinks(int baselineActiveLinks) { this.baselineActiveLinks = baselineActiveLinks; }
    public int getSimulatedActiveLinks() { return simulatedActiveLinks; }
    public void setSimulatedActiveLinks(int simulatedActiveLinks) { this.simulatedActiveLinks = simulatedActiveLinks; }
    public double getBaselineAvgCpu() { return baselineAvgCpu; }
    public void setBaselineAvgCpu(double baselineAvgCpu) { this.baselineAvgCpu = baselineAvgCpu; }
    public double getSimulatedAvgCpu() { return simulatedAvgCpu; }
    public void setSimulatedAvgCpu(double simulatedAvgCpu) { this.simulatedAvgCpu = simulatedAvgCpu; }
    public double getBaselineAvgMemory() { return baselineAvgMemory; }
    public void setBaselineAvgMemory(double baselineAvgMemory) { this.baselineAvgMemory = baselineAvgMemory; }
    public double getSimulatedAvgMemory() { return simulatedAvgMemory; }
    public void setSimulatedAvgMemory(double simulatedAvgMemory) { this.simulatedAvgMemory = simulatedAvgMemory; }
    public double getBaselineAvgLatency() { return baselineAvgLatency; }
    public void setBaselineAvgLatency(double baselineAvgLatency) { this.baselineAvgLatency = baselineAvgLatency; }
    public double getSimulatedAvgLatency() { return simulatedAvgLatency; }
    public void setSimulatedAvgLatency(double simulatedAvgLatency) { this.simulatedAvgLatency = simulatedAvgLatency; }
    public double getBaselineAvgPacketLoss() { return baselineAvgPacketLoss; }
    public void setBaselineAvgPacketLoss(double baselineAvgPacketLoss) { this.baselineAvgPacketLoss = baselineAvgPacketLoss; }
    public double getSimulatedAvgPacketLoss() { return simulatedAvgPacketLoss; }
    public void setSimulatedAvgPacketLoss(double simulatedAvgPacketLoss) { this.simulatedAvgPacketLoss = simulatedAvgPacketLoss; }
    public int getBaselineActiveConnections() { return baselineActiveConnections; }
    public void setBaselineActiveConnections(int baselineActiveConnections) { this.baselineActiveConnections = baselineActiveConnections; }
    public int getSimulatedActiveConnections() { return simulatedActiveConnections; }
    public void setSimulatedActiveConnections(int simulatedActiveConnections) { this.simulatedActiveConnections = simulatedActiveConnections; }
    public double getBaselineAvgWaitTime() { return baselineAvgWaitTime; }
    public void setBaselineAvgWaitTime(double baselineAvgWaitTime) { this.baselineAvgWaitTime = baselineAvgWaitTime; }
    public double getSimulatedAvgWaitTime() { return simulatedAvgWaitTime; }
    public void setSimulatedAvgWaitTime(double simulatedAvgWaitTime) { this.simulatedAvgWaitTime = simulatedAvgWaitTime; }
    public double getBaselineThroughput() { return baselineThroughput; }
    public void setBaselineThroughput(double baselineThroughput) { this.baselineThroughput = baselineThroughput; }
    public double getSimulatedThroughput() { return simulatedThroughput; }
    public void setSimulatedThroughput(double simulatedThroughput) { this.simulatedThroughput = simulatedThroughput; }
    public List<MetricImpactDto> getImpacts() { return impacts; }
    public void setImpacts(List<MetricImpactDto> impacts) { this.impacts = impacts; }
}
