package com.netpulse.failover.state;

public enum FailoverTrigger {
    HEARTBEAT_UNREACHABLE,
    HEALTH_FAILED,
    NODE_OFFLINE,
    MULTI_SIGNAL_FAILURE,
    MANUAL_TRIGGER
}
