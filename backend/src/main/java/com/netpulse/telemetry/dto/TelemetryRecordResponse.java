package com.netpulse.telemetry.dto;

import java.time.Instant;

public class TelemetryRecordResponse {

    private Long id;
    private String nodeId;
    private Instant timestamp;
    private Double cpuUsage;
    private Double memoryUsage;
    private Double latency;
    private Double packetLoss;
    private Integer activeConnections;
    private Double errorRate;

    public TelemetryRecordResponse() {
    }

    public TelemetryRecordResponse(Long id, String nodeId, Instant timestamp, Double cpuUsage,
                                   Double memoryUsage, Double latency, Double packetLoss,
                                   Integer activeConnections, Double errorRate) {
        this.id = id;
        this.nodeId = nodeId;
        this.timestamp = timestamp;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.latency = latency;
        this.packetLoss = packetLoss;
        this.activeConnections = activeConnections;
        this.errorRate = errorRate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(Double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public Double getLatency() {
        return latency;
    }

    public void setLatency(Double latency) {
        this.latency = latency;
    }

    public Double getPacketLoss() {
        return packetLoss;
    }

    public void setPacketLoss(Double packetLoss) {
        this.packetLoss = packetLoss;
    }

    public Integer getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(Integer activeConnections) {
        this.activeConnections = activeConnections;
    }

    public Double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(Double errorRate) {
        this.errorRate = errorRate;
    }
}
