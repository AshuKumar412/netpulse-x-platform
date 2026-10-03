package com.netpulse.failover.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "active_sessions",
        indexes = {
                @Index(name = "idx_as_session_id", columnList = "session_id", unique = true),
                @Index(name = "idx_as_node_id", columnList = "node_id"),
                @Index(name = "idx_as_status", columnList = "status"),
                @Index(name = "idx_as_node_status", columnList = "node_id, status")
        }
)
public class ActiveSessionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;

    @Column(name = "node_id", nullable = false, length = 64)
    private String nodeId;

    @Column(name = "original_node_id", length = 64)
    private String originalNodeId;

    @Column(name = "request_type", length = 64)
    private String requestType;

    @Column(name = "strategy_used", length = 64)
    private String strategyUsed;

    @Column(name = "status", nullable = false, length = 32)
    private String status; // "ACTIVE", "REROUTED", "TERMINATED"

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "failover_cycle_id", length = 64)
    private String failoverCycleId;

    public ActiveSessionRecord() {
    }

    public ActiveSessionRecord(String sessionId,
                               String nodeId,
                               String originalNodeId,
                               String requestType,
                               String strategyUsed,
                               String status,
                               Instant createdAt,
                               Instant updatedAt,
                               String failoverCycleId) {
        this.sessionId = sessionId;
        this.nodeId = nodeId;
        this.originalNodeId = originalNodeId;
        this.requestType = requestType;
        this.strategyUsed = strategyUsed;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.failoverCycleId = failoverCycleId;
    }

    public Long getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getOriginalNodeId() {
        return originalNodeId;
    }

    public void setOriginalNodeId(String originalNodeId) {
        this.originalNodeId = originalNodeId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getStrategyUsed() {
        return strategyUsed;
    }

    public void setStrategyUsed(String strategyUsed) {
        this.strategyUsed = strategyUsed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFailoverCycleId() {
        return failoverCycleId;
    }

    public void setFailoverCycleId(String failoverCycleId) {
        this.failoverCycleId = failoverCycleId;
    }
}
