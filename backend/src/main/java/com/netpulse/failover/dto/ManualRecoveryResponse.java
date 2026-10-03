package com.netpulse.failover.dto;

import com.netpulse.failover.state.FailoverState;

public class ManualRecoveryResponse {

    private String nodeId;
    private String eventId;
    private String recoveryCycleId;
    private FailoverState state;
    private boolean initiated;
    private String message;

    public ManualRecoveryResponse() {
    }

    public ManualRecoveryResponse(String nodeId, String eventId, String recoveryCycleId, FailoverState state, boolean initiated, String message) {
        this.nodeId = nodeId;
        this.eventId = eventId;
        this.recoveryCycleId = recoveryCycleId;
        this.state = state;
        this.initiated = initiated;
        this.message = message;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
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

    public FailoverState getState() {
        return state;
    }

    public void setState(FailoverState state) {
        this.state = state;
    }

    public boolean isInitiated() {
        return initiated;
    }

    public void setInitiated(boolean initiated) {
        this.initiated = initiated;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
