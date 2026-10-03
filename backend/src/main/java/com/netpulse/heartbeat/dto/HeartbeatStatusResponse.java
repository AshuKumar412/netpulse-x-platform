package com.netpulse.heartbeat.dto;

import com.netpulse.heartbeat.entity.LivenessStatus;
import java.time.Instant;

public class HeartbeatStatusResponse {

    private String nodeId;
    private Instant lastHeartbeat;
    private Long ageSeconds;
    private LivenessStatus liveness;
    private Boolean isSuppressed;

    public HeartbeatStatusResponse() {
    }

    public HeartbeatStatusResponse(String nodeId, Instant lastHeartbeat, Long ageSeconds,
                                   LivenessStatus liveness, Boolean isSuppressed) {
        this.nodeId = nodeId;
        this.lastHeartbeat = lastHeartbeat;
        this.ageSeconds = ageSeconds;
        this.liveness = liveness;
        this.isSuppressed = isSuppressed;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public Long getAgeSeconds() {
        return ageSeconds;
    }

    public void setAgeSeconds(Long ageSeconds) {
        this.ageSeconds = ageSeconds;
    }

    public LivenessStatus getLiveness() {
        return liveness;
    }

    public void setLiveness(LivenessStatus liveness) {
        this.liveness = liveness;
    }

    public Boolean getIsSuppressed() {
        return isSuppressed;
    }

    public void setIsSuppressed(Boolean isSuppressed) {
        this.isSuppressed = isSuppressed;
    }
}
