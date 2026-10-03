package com.netpulse.predictive.dto;

import java.time.Instant;

/**
 * Encapsulates the normalized feature vector extracted from point-in-time and historical telemetry.
 */
public class PredictiveFeatureVector {

    private String nodeId;
    private Instant timestamp;

    // Direct telemetry features
    private double cpuUsage;
    private double memoryUsage;
    private double latency;
    private double packetLoss;
    private int activeConnections;
    private double errorRate;

    // Derived rate of change features (1st derivatives Δ/Δt)
    private double cpuRateOfChange;
    private double memoryRateOfChange;
    private double latencyRateOfChange;
    private double packetLossRateOfChange;
    private double connectionGrowthRate;

    // Rolling moving averages (smoothed window)
    private double rollingAvgCpu;
    private double rollingAvgMemory;
    private double rollingAvgLatency;
    private double rollingAvgPacketLoss;
    private double rollingAvgConnections;

    // Infrastructure health signals
    private double healthScore; // 0 - 100
    private long heartbeatAgeSeconds;

    public PredictiveFeatureVector() {
    }

    /**
     * Converts the structured features to a standardized double array for ML model consumption.
     * Total feature dimension: 16 features.
     */
    public double[] toFeatureArray() {
        return new double[] {
                cpuUsage,
                memoryUsage,
                latency,
                packetLoss,
                (double) activeConnections,
                errorRate,
                cpuRateOfChange,
                memoryRateOfChange,
                latencyRateOfChange,
                packetLossRateOfChange,
                connectionGrowthRate,
                rollingAvgCpu,
                rollingAvgMemory,
                rollingAvgLatency,
                rollingAvgPacketLoss,
                rollingAvgConnections
        };
    }

    public static String[] getFeatureNames() {
        return new String[] {
                "CPU_UTILIZATION",
                "MEMORY_UTILIZATION",
                "LATENCY_MS",
                "PACKET_LOSS_PCT",
                "ACTIVE_CONNECTIONS",
                "ERROR_RATE_PCT",
                "CPU_RATE_OF_CHANGE",
                "MEMORY_RATE_OF_CHANGE",
                "LATENCY_RATE_OF_CHANGE",
                "PACKET_LOSS_RATE_OF_CHANGE",
                "CONNECTION_GROWTH_RATE",
                "ROLLING_AVG_CPU",
                "ROLLING_AVG_MEMORY",
                "ROLLING_AVG_LATENCY",
                "ROLLING_AVG_PACKET_LOSS",
                "ROLLING_AVG_CONNECTIONS"
        };
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public double getLatency() {
        return latency;
    }

    public void setLatency(double latency) {
        this.latency = latency;
    }

    public double getPacketLoss() {
        return packetLoss;
    }

    public void setPacketLoss(double packetLoss) {
        this.packetLoss = packetLoss;
    }

    public int getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(int activeConnections) {
        this.activeConnections = activeConnections;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(double errorRate) {
        this.errorRate = errorRate;
    }

    public double getCpuRateOfChange() {
        return cpuRateOfChange;
    }

    public void setCpuRateOfChange(double cpuRateOfChange) {
        this.cpuRateOfChange = cpuRateOfChange;
    }

    public double getMemoryRateOfChange() {
        return memoryRateOfChange;
    }

    public void setMemoryRateOfChange(double memoryRateOfChange) {
        this.memoryRateOfChange = memoryRateOfChange;
    }

    public double getLatencyRateOfChange() {
        return latencyRateOfChange;
    }

    public void setLatencyRateOfChange(double latencyRateOfChange) {
        this.latencyRateOfChange = latencyRateOfChange;
    }

    public double getPacketLossRateOfChange() {
        return packetLossRateOfChange;
    }

    public void setPacketLossRateOfChange(double packetLossRateOfChange) {
        this.packetLossRateOfChange = packetLossRateOfChange;
    }

    public double getConnectionGrowthRate() {
        return connectionGrowthRate;
    }

    public void setConnectionGrowthRate(double connectionGrowthRate) {
        this.connectionGrowthRate = connectionGrowthRate;
    }

    public double getRollingAvgCpu() {
        return rollingAvgCpu;
    }

    public void setRollingAvgCpu(double rollingAvgCpu) {
        this.rollingAvgCpu = rollingAvgCpu;
    }

    public double getRollingAvgMemory() {
        return rollingAvgMemory;
    }

    public void setRollingAvgMemory(double rollingAvgMemory) {
        this.rollingAvgMemory = rollingAvgMemory;
    }

    public double getRollingAvgLatency() {
        return rollingAvgLatency;
    }

    public void setRollingAvgLatency(double rollingAvgLatency) {
        this.rollingAvgLatency = rollingAvgLatency;
    }

    public double getRollingAvgPacketLoss() {
        return rollingAvgPacketLoss;
    }

    public void setRollingAvgPacketLoss(double rollingAvgPacketLoss) {
        this.rollingAvgPacketLoss = rollingAvgPacketLoss;
    }

    public double getRollingAvgConnections() {
        return rollingAvgConnections;
    }

    public void setRollingAvgConnections(double rollingAvgConnections) {
        this.rollingAvgConnections = rollingAvgConnections;
    }

    public double getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(double healthScore) {
        this.healthScore = healthScore;
    }

    public long getHeartbeatAgeSeconds() {
        return heartbeatAgeSeconds;
    }

    public void setHeartbeatAgeSeconds(long heartbeatAgeSeconds) {
        this.heartbeatAgeSeconds = heartbeatAgeSeconds;
    }
}
