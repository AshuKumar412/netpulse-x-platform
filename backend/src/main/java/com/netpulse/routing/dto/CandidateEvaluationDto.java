package com.netpulse.routing.dto;

import com.netpulse.health.entity.HealthClassification;
import com.netpulse.heartbeat.entity.LivenessStatus;

public class CandidateEvaluationDto {

    private String nodeId;
    private String name;
    private Integer capacity;
    private HealthClassification healthStatus;
    private LivenessStatus livenessStatus;
    private Double cpuUsage;
    private Double memoryUsage;
    private Double latency;
    private Double packetLoss;
    private Integer activeConnections;
    private Double errorRate;
    private Boolean eligible;
    private String evaluationReason;
    private Double score;

    public CandidateEvaluationDto() {
    }

    public CandidateEvaluationDto(String nodeId, String name, Integer capacity,
                                  HealthClassification healthStatus, LivenessStatus livenessStatus,
                                  Double cpuUsage, Double memoryUsage, Double latency,
                                  Double packetLoss, Integer activeConnections, Double errorRate,
                                  Boolean eligible, String evaluationReason, Double score) {
        this.nodeId = nodeId;
        this.name = name;
        this.capacity = capacity;
        this.healthStatus = healthStatus;
        this.livenessStatus = livenessStatus;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.latency = latency;
        this.packetLoss = packetLoss;
        this.activeConnections = activeConnections;
        this.errorRate = errorRate;
        this.eligible = eligible;
        this.evaluationReason = evaluationReason;
        this.score = score;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
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

    public Boolean getEligible() {
        return eligible;
    }

    public void setEligible(Boolean eligible) {
        this.eligible = eligible;
    }

    public String getEvaluationReason() {
        return evaluationReason;
    }

    public void setEvaluationReason(String evaluationReason) {
        this.evaluationReason = evaluationReason;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
