package com.netpulse.whatif.dto;

import com.netpulse.health.entity.HealthClassification;
import com.netpulse.heartbeat.entity.LivenessStatus;

public class SimulationCandidateEvaluationDto {

    private String nodeId;
    private String nodeName;
    private Integer capacity;
    private HealthClassification healthStatus;
    private LivenessStatus liveness;
    private Double cpuUsage;
    private Double memoryUsage;
    private Double latency;
    private Double packetLoss;
    private Integer activeConnections;
    private Double routingScore;
    private boolean eligible;
    private String rejectionReason;

    public SimulationCandidateEvaluationDto() {
    }

    public SimulationCandidateEvaluationDto(String nodeId, String nodeName, Integer capacity, HealthClassification healthStatus, LivenessStatus liveness, Double cpuUsage, Double memoryUsage, Double latency, Double packetLoss, Integer activeConnections, Double routingScore, boolean eligible, String rejectionReason) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.capacity = capacity;
        this.healthStatus = healthStatus;
        this.liveness = liveness;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.latency = latency;
        this.packetLoss = packetLoss;
        this.activeConnections = activeConnections;
        this.routingScore = routingScore;
        this.eligible = eligible;
        this.rejectionReason = rejectionReason;
    }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public HealthClassification getHealthStatus() { return healthStatus; }
    public void setHealthStatus(HealthClassification healthStatus) { this.healthStatus = healthStatus; }
    public LivenessStatus getLiveness() { return liveness; }
    public void setLiveness(LivenessStatus liveness) { this.liveness = liveness; }
    public Double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }
    public Double getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(Double memoryUsage) { this.memoryUsage = memoryUsage; }
    public Double getLatency() { return latency; }
    public void setLatency(Double latency) { this.latency = latency; }
    public Double getPacketLoss() { return packetLoss; }
    public void setPacketLoss(Double packetLoss) { this.packetLoss = packetLoss; }
    public Integer getActiveConnections() { return activeConnections; }
    public void setActiveConnections(Integer activeConnections) { this.activeConnections = activeConnections; }
    public Double getRoutingScore() { return routingScore; }
    public void setRoutingScore(Double routingScore) { this.routingScore = routingScore; }
    public boolean isEligible() { return eligible; }
    public void setEligible(boolean eligible) { this.eligible = eligible; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
