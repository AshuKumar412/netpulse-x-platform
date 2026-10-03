package com.netpulse.failover.dto;

import com.netpulse.failover.entity.FailoverEvent;
import com.netpulse.failover.state.FailoverState;
import com.netpulse.failover.state.FailoverTrigger;
import java.time.Duration;
import java.time.Instant;

public class FailoverEventDto {

    private String eventId;
    private String recoveryCycleId;
    private String failureNodeId;
    private String failureNodeName;
    private String replacementNodeId;
    private String replacementNodeName;
    private FailoverTrigger trigger;
    private FailoverState previousState;
    private FailoverState currentState;
    private int affectedSessionCount;
    private int reroutedSessionCount;
    private int unrecoveredSessionCount;
    private int attemptNumber;
    private String reason;
    private Instant startedAt;
    private Instant completedAt;
    private Long durationMs;
    private Boolean success;
    private String failureReason;

    public FailoverEventDto() {
    }

    public static FailoverEventDto fromEntity(FailoverEvent entity) {
        if (entity == null) return null;
        FailoverEventDto dto = new FailoverEventDto();
        dto.setEventId(entity.getEventId());
        dto.setRecoveryCycleId(entity.getRecoveryCycleId());
        dto.setFailureNodeId(entity.getFailureNodeId());
        dto.setFailureNodeName(entity.getFailureNodeName());
        dto.setReplacementNodeId(entity.getReplacementNodeId());
        dto.setReplacementNodeName(entity.getReplacementNodeName());
        dto.setTrigger(entity.getTrigger());
        dto.setPreviousState(entity.getPreviousState());
        dto.setCurrentState(entity.getCurrentState());
        dto.setAffectedSessionCount(entity.getAffectedSessionCount());
        dto.setReroutedSessionCount(entity.getReroutedSessionCount());
        dto.setUnrecoveredSessionCount(entity.getUnrecoveredSessionCount());
        dto.setAttemptNumber(entity.getAttemptNumber());
        dto.setReason(entity.getReason());
        dto.setStartedAt(entity.getStartedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        if (entity.getStartedAt() != null && entity.getCompletedAt() != null) {
            dto.setDurationMs(Duration.between(entity.getStartedAt(), entity.getCompletedAt()).toMillis());
        }
        dto.setSuccess(entity.getSuccess());
        dto.setFailureReason(entity.getFailureReason());
        return dto;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getRecoveryCycleId() {
        return recoveryCycleId;
    }

    public void setRecoveryCycleId(String recoveryCycleId) {
        this.recoveryCycleId = recoveryCycleId;
    }

    public String getFailureNodeId() {
        return failureNodeId;
    }

    public void setFailureNodeId(String failureNodeId) {
        this.failureNodeId = failureNodeId;
    }

    public String getFailureNodeName() {
        return failureNodeName;
    }

    public void setFailureNodeName(String failureNodeName) {
        this.failureNodeName = failureNodeName;
    }

    public String getReplacementNodeId() {
        return replacementNodeId;
    }

    public void setReplacementNodeId(String replacementNodeId) {
        this.replacementNodeId = replacementNodeId;
    }

    public String getReplacementNodeName() {
        return replacementNodeName;
    }

    public void setReplacementNodeName(String replacementNodeName) {
        this.replacementNodeName = replacementNodeName;
    }

    public FailoverTrigger getTrigger() {
        return trigger;
    }

    public void setTrigger(FailoverTrigger trigger) {
        this.trigger = trigger;
    }

    public FailoverState getPreviousState() {
        return previousState;
    }

    public void setPreviousState(FailoverState previousState) {
        this.previousState = previousState;
    }

    public FailoverState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(FailoverState currentState) {
        this.currentState = currentState;
    }

    public int getAffectedSessionCount() {
        return affectedSessionCount;
    }

    public void setAffectedSessionCount(int affectedSessionCount) {
        this.affectedSessionCount = affectedSessionCount;
    }

    public int getReroutedSessionCount() {
        return reroutedSessionCount;
    }

    public void setReroutedSessionCount(int reroutedSessionCount) {
        this.reroutedSessionCount = reroutedSessionCount;
    }

    public int getUnrecoveredSessionCount() {
        return unrecoveredSessionCount;
    }

    public void setUnrecoveredSessionCount(int unrecoveredSessionCount) {
        this.unrecoveredSessionCount = unrecoveredSessionCount;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
