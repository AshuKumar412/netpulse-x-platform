package com.netpulse.whatif.dto;

import com.netpulse.scheduler.state.ProcessState;
import com.netpulse.scheduler.state.ProcessType;

public class SnapshotProcessDto {

    private String processId;
    private String processName;
    private String targetNodeId;
    private ProcessType processType;
    private ProcessState state;
    private int priority;
    private long arrivalTime;
    private long burstTime;
    private long remainingBurstTime;

    public SnapshotProcessDto() {
    }

    public SnapshotProcessDto(String processId, String processName, String targetNodeId, ProcessType processType, ProcessState state, int priority, long arrivalTime, long burstTime, long remainingBurstTime) {
        this.processId = processId;
        this.processName = processName;
        this.targetNodeId = targetNodeId;
        this.processType = processType;
        this.state = state;
        this.priority = priority;
        this.arrivalTime = arrivalTime;
        this.burstTime = burstTime;
        this.remainingBurstTime = remainingBurstTime;
    }

    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }
    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }
    public ProcessType getProcessType() { return processType; }
    public void setProcessType(ProcessType processType) { this.processType = processType; }
    public ProcessState getState() { return state; }
    public void setState(ProcessState state) { this.state = state; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public long getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(long arrivalTime) { this.arrivalTime = arrivalTime; }
    public long getBurstTime() { return burstTime; }
    public void setBurstTime(long burstTime) { this.burstTime = burstTime; }
    public long getRemainingBurstTime() { return remainingBurstTime; }
    public void setRemainingBurstTime(long remainingBurstTime) { this.remainingBurstTime = remainingBurstTime; }
}
