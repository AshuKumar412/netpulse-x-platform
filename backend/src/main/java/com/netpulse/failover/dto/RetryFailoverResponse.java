package com.netpulse.failover.dto;

import com.netpulse.failover.state.FailoverState;

public class RetryFailoverResponse {

    private String eventId;
    private String recoveryCycleId;
    private String nodeId;
    private int attemptNumber;
    private FailoverState state;
    private boolean retried;
    private String message;

    public RetryFailoverResponse() {
    }

    public RetryFailoverResponse(String eventId, String recoveryCycleId, String nodeId, int attemptNumber, FailoverState state, boolean retried, String message) {
        this.eventId = eventId;
        this.recoveryCycleId = recoveryCycleId;
        this.nodeId = nodeId;
        this.attemptNumber = attemptNumber;
        this.state = state;
        this.retried = retried;
        this.message = message;
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

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public FailoverState getState() {
        return state;
    }

    public void setState(FailoverState state) {
        this.state = state;
    }

    public boolean isRetried() {
        return retried;
    }

    public void setRetried(boolean retried) {
        this.retried = retried;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
