package com.netpulse.health.dto;

import com.netpulse.health.entity.HealthClassification;
import com.netpulse.heartbeat.entity.LivenessStatus;
import java.time.Instant;

public class NodeHealthResponse {

    private String nodeId;
    private HealthClassification healthStatus;
    private LivenessStatus livenessStatus;
    private Instant lastHeartbeat;
    private Instant evaluatedAt;
    private String reason;
    private Double cpuUsage;
    private Double memoryUsage;
    private Double latency;
    private Double packetLoss;
    private Double errorRate;

    public NodeHealthResponse() {
    }

    public NodeHealthResponse(String nodeId, HealthClassification healthStatus,
                              LivenessStatus livenessStatus, Instant lastHeartbeat,
                              Instant evaluatedAt, String reason, Double cpuUsage,
                              Double memoryUsage, Double latency, Double packetLoss,
                              Double errorRate) {
        this.nodeId = nodeId;
        this.healthStatus = healthStatus;
        this.livenessStatus = livenessStatus;
        this.lastHeartbeat = lastHeartbeat;
        this.evaluatedAt = evaluatedAt;
        this.reason = reason;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.latency = latency;
        this.packetLoss = packetLoss;
        this.errorRate = errorRate;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public HealthClassification getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(HealthClassification healthStatus) {
        this.healthStatus = healthStatus;
    }

    public LivenessStatus getLivenessStatus() {
        return livenessStatus;
    }

    public void setLivenessStatus(LivenessStatus livenessStatus) {
        this.livenessStatus = livenessStatus;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public Double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(Double errorRate) {
        this.errorRate = errorRate;
    }
}
