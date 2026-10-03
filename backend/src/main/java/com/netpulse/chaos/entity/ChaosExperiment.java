package com.netpulse.chaos.entity;

import com.netpulse.chaos.state.ChaosExperimentResult;
import com.netpulse.chaos.state.ChaosExperimentStatus;
import com.netpulse.chaos.state.ChaosExperimentType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "chaos_experiments", indexes = {
        @Index(name = "idx_chaos_exp_id", columnList = "experiment_id", unique = true),
        @Index(name = "idx_chaos_status", columnList = "status"),
        @Index(name = "idx_chaos_target_node", columnList = "target_node_id"),
        @Index(name = "idx_chaos_started_at", columnList = "started_at")
})
public class ChaosExperiment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "experiment_id", nullable = false, unique = true, length = 64)
    private String experimentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "experiment_type", nullable = false, length = 32)
    private ChaosExperimentType experimentType;

    @Column(name = "target_node_id", length = 64)
    private String targetNodeId;

    @Column(name = "target_link_id", length = 64)
    private String targetLinkId;

    @Column(name = "severity")
    private Double severity;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChaosExperimentStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "failure_observed")
    private Boolean failureObserved = false;

    @Column(name = "failover_triggered")
    private Boolean failoverTriggered = false;

    @Column(name = "recovery_completed")
    private Boolean recoveryCompleted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 32)
    private ChaosExperimentResult result;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "explanation", length = 2000)
    private String explanation;

    @Column(name = "timeline_json", columnDefinition = "TEXT")
    private String timelineJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ChaosExperiment() {
    }

    public ChaosExperiment(String experimentId,
                           ChaosExperimentType experimentType,
                           String targetNodeId,
                           String targetLinkId,
                           Double severity,
                           Integer durationSeconds,
                           ChaosExperimentStatus status,
                           Instant startedAt,
                           String createdBy) {
        this.experimentId = experimentId;
        this.experimentType = experimentType;
        this.targetNodeId = targetNodeId;
        this.targetLinkId = targetLinkId;
        this.severity = severity;
        this.durationSeconds = durationSeconds;
        this.status = status;
        this.startedAt = startedAt;
        this.createdBy = createdBy;
        this.failureObserved = false;
        this.failoverTriggered = false;
        this.recoveryCompleted = false;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public ChaosExperimentType getExperimentType() {
        return experimentType;
    }

    public void setExperimentType(ChaosExperimentType experimentType) {
        this.experimentType = experimentType;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public String getTargetLinkId() {
        return targetLinkId;
    }

    public void setTargetLinkId(String targetLinkId) {
        this.targetLinkId = targetLinkId;
    }

    public Double getSeverity() {
        return severity;
    }

    public void setSeverity(Double severity) {
        this.severity = severity;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public ChaosExperimentStatus getStatus() {
        return status;
    }

    public void setStatus(ChaosExperimentStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Boolean getFailureObserved() {
        return failureObserved;
    }

    public void setFailureObserved(Boolean failureObserved) {
        this.failureObserved = failureObserved;
    }

    public Boolean getFailoverTriggered() {
        return failoverTriggered;
    }

    public void setFailoverTriggered(Boolean failoverTriggered) {
        this.failoverTriggered = failoverTriggered;
    }

    public Boolean getRecoveryCompleted() {
        return recoveryCompleted;
    }

    public void setRecoveryCompleted(Boolean recoveryCompleted) {
        this.recoveryCompleted = recoveryCompleted;
    }

    public ChaosExperimentResult getResult() {
        return result;
    }

    public void setResult(ChaosExperimentResult result) {
        this.result = result;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getTimelineJson() {
        return timelineJson;
    }

    public void setTimelineJson(String timelineJson) {
        this.timelineJson = timelineJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
