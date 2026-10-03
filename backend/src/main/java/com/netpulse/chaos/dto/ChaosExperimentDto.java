package com.netpulse.chaos.dto;

import com.netpulse.chaos.entity.ChaosExperiment;
import com.netpulse.chaos.state.ChaosExperimentResult;
import com.netpulse.chaos.state.ChaosExperimentStatus;
import com.netpulse.chaos.state.ChaosExperimentType;

import java.time.Instant;

public class ChaosExperimentDto {

    private Long id;
    private String experimentId;
    private ChaosExperimentType experimentType;
    private String targetNodeId;
    private String targetLinkId;
    private Double severity;
    private Integer durationSeconds;
    private ChaosExperimentStatus status;
    private Instant startedAt;
    private Instant endedAt;
    private String createdBy;
    private Boolean failureObserved;
    private Boolean failoverTriggered;
    private Boolean recoveryCompleted;
    private ChaosExperimentResult result;
    private String failureReason;
    private String explanation;
    private String timelineJson;
    private Instant createdAt;

    public ChaosExperimentDto() {
    }

    public ChaosExperimentDto(Long id,
                              String experimentId,
                              ChaosExperimentType experimentType,
                              String targetNodeId,
                              String targetLinkId,
                              Double severity,
                              Integer durationSeconds,
                              ChaosExperimentStatus status,
                              Instant startedAt,
                              Instant endedAt,
                              String createdBy,
                              Boolean failureObserved,
                              Boolean failoverTriggered,
                              Boolean recoveryCompleted,
                              ChaosExperimentResult result,
                              String failureReason,
                              String explanation,
                              String timelineJson,
                              Instant createdAt) {
        this.id = id;
        this.experimentId = experimentId;
        this.experimentType = experimentType;
        this.targetNodeId = targetNodeId;
        this.targetLinkId = targetLinkId;
        this.severity = severity;
        this.durationSeconds = durationSeconds;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.createdBy = createdBy;
        this.failureObserved = failureObserved;
        this.failoverTriggered = failoverTriggered;
        this.recoveryCompleted = recoveryCompleted;
        this.result = result;
        this.failureReason = failureReason;
        this.explanation = explanation;
        this.timelineJson = timelineJson;
        this.createdAt = createdAt;
    }

    public static ChaosExperimentDto fromEntity(ChaosExperiment entity) {
        if (entity == null) {
            return null;
        }
        return new ChaosExperimentDto(
                entity.getId(),
                entity.getExperimentId(),
                entity.getExperimentType(),
                entity.getTargetNodeId(),
                entity.getTargetLinkId(),
                entity.getSeverity(),
                entity.getDurationSeconds(),
                entity.getStatus(),
                entity.getStartedAt(),
                entity.getEndedAt(),
                entity.getCreatedBy(),
                entity.getFailureObserved(),
                entity.getFailoverTriggered(),
                entity.getRecoveryCompleted(),
                entity.getResult(),
                entity.getFailureReason(),
                entity.getExplanation(),
                entity.getTimelineJson(),
                entity.getCreatedAt()
        );
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
