package com.netpulse.failover.entity;

import com.netpulse.failover.state.FailoverState;
import com.netpulse.failover.state.FailoverTrigger;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "failover_events",
        indexes = {
                @Index(name = "idx_fo_event_id", columnList = "event_id", unique = true),
                @Index(name = "idx_fo_cycle_id", columnList = "recovery_cycle_id"),
                @Index(name = "idx_fo_failure_node", columnList = "failure_node_id"),
                @Index(name = "idx_fo_replacement_node", columnList = "replacement_node_id"),
                @Index(name = "idx_fo_started_at", columnList = "started_at DESC"),
                @Index(name = "idx_fo_state", columnList = "current_state")
        }
)
public class FailoverEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(name = "recovery_cycle_id", nullable = false, length = 64)
    private String recoveryCycleId;

    @Column(name = "failure_node_id", nullable = false, length = 64)
    private String failureNodeId;

    @Column(name = "failure_node_name", length = 128)
    private String failureNodeName;

    @Column(name = "replacement_node_id", length = 64)
    private String replacementNodeId;

    @Column(name = "replacement_node_name", length = 128)
    private String replacementNodeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 32)
    private FailoverTrigger trigger;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_state", length = 32)
    private FailoverState previousState;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_state", nullable = false, length = 32)
    private FailoverState currentState;

    @Column(name = "affected_session_count", nullable = false)
    private int affectedSessionCount;

    @Column(name = "rerouted_session_count", nullable = false)
    private int reroutedSessionCount;

    @Column(name = "unrecovered_session_count", nullable = false)
    private int unrecoveredSessionCount;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "is_success")
    private Boolean success;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    public FailoverEvent() {
    }

    public FailoverEvent(String eventId,
                         String recoveryCycleId,
                         String failureNodeId,
                         String failureNodeName,
                         String replacementNodeId,
                         String replacementNodeName,
                         FailoverTrigger trigger,
                         FailoverState previousState,
                         FailoverState currentState,
                         int affectedSessionCount,
                         int reroutedSessionCount,
                         int unrecoveredSessionCount,
                         int attemptNumber,
                         String reason,
                         Instant startedAt,
                         Instant completedAt,
                         Boolean success,
                         String failureReason) {
        this.eventId = eventId;
        this.recoveryCycleId = recoveryCycleId;
        this.failureNodeId = failureNodeId;
        this.failureNodeName = failureNodeName;
        this.replacementNodeId = replacementNodeId;
        this.replacementNodeName = replacementNodeName;
        this.trigger = trigger;
        this.previousState = previousState;
        this.currentState = currentState;
        this.affectedSessionCount = affectedSessionCount;
        this.reroutedSessionCount = reroutedSessionCount;
        this.unrecoveredSessionCount = unrecoveredSessionCount;
        this.attemptNumber = attemptNumber;
        this.reason = reason;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.success = success;
        this.failureReason = failureReason;
    }

    public Long getId() {
        return id;
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
