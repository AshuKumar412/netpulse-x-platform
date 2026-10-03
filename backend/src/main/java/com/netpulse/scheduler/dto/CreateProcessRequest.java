package com.netpulse.scheduler.dto;

import com.netpulse.scheduler.state.ProcessType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateProcessRequest {

    @NotBlank(message = "Process name is required")
    private String processName;

    private String owner;

    private String targetNodeId;

    @NotNull(message = "Process type is required")
    private ProcessType processType = ProcessType.CPU_BOUND;

    @Min(value = 1, message = "Priority must be between 1 (highest) and 10 (lowest)")
    @Max(value = 10, message = "Priority must be between 1 (highest) and 10 (lowest)")
    private int priority = 5;

    @Min(value = 1, message = "Burst time must be at least 1")
    private long burstTime = 20;

    public CreateProcessRequest() {
    }

    public CreateProcessRequest(String processName, String owner, String targetNodeId, ProcessType processType, int priority, long burstTime) {
        this.processName = processName;
        this.owner = owner;
        this.targetNodeId = targetNodeId;
        this.processType = processType;
        this.priority = priority;
        this.burstTime = burstTime;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public ProcessType getProcessType() {
        return processType;
    }

    public void setProcessType(ProcessType processType) {
        this.processType = processType;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getBurstTime() {
        return burstTime;
    }

    public void setBurstTime(long burstTime) {
        this.burstTime = burstTime;
    }
}
