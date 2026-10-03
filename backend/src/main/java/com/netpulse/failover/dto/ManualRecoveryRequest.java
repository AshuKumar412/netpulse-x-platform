package com.netpulse.failover.dto;

public class ManualRecoveryRequest {

    private String reason;
    private boolean force;

    public ManualRecoveryRequest() {
    }

    public ManualRecoveryRequest(String reason, boolean force) {
        this.reason = reason;
        this.force = force;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }
}
