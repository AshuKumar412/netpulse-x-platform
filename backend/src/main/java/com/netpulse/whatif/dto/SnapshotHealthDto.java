package com.netpulse.whatif.dto;

import com.netpulse.health.entity.HealthClassification;
import com.netpulse.heartbeat.entity.LivenessStatus;

public class SnapshotHealthDto {

    private String nodeId;
    private HealthClassification healthStatus;
    private LivenessStatus livenessStatus;
    private Double healthScore;
    private Long lastHeartbeatTick;

    public SnapshotHealthDto() {
    }

    public SnapshotHealthDto(String nodeId, HealthClassification healthStatus, LivenessStatus livenessStatus, Double healthScore, Long lastHeartbeatTick) {
        this.nodeId = nodeId;
        this.healthStatus = healthStatus;
        this.livenessStatus = livenessStatus;
        this.healthScore = healthScore;
        this.lastHeartbeatTick = lastHeartbeatTick;
    }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public HealthClassification getHealthStatus() { return healthStatus; }
    public void setHealthStatus(HealthClassification healthStatus) { this.healthStatus = healthStatus; }
    public LivenessStatus getLivenessStatus() { return livenessStatus; }
    public void setLivenessStatus(LivenessStatus livenessStatus) { this.livenessStatus = livenessStatus; }
    public Double getHealthScore() { return healthScore; }
    public void setHealthScore(Double healthScore) { this.healthScore = healthScore; }
    public Long getLastHeartbeatTick() { return lastHeartbeatTick; }
    public void setLastHeartbeatTick(Long lastHeartbeatTick) { this.lastHeartbeatTick = lastHeartbeatTick; }
}
