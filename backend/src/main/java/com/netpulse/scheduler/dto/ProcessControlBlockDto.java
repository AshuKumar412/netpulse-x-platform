package com.netpulse.scheduler.dto;

import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.state.ProcessState;
import com.netpulse.scheduler.state.ProcessType;

import java.time.Instant;

public class ProcessControlBlockDto {

    private Long id;
    private String processId;
    private String processName;
    private String owner;
    private String targetNodeId;
    private ProcessType processType;
    private ProcessState state;
    private int priority;
    private int effectivePriority;
    private long arrivalTime;
    private long burstTime;
    private long remainingBurstTime;
    private long waitingTime;
    private long turnaroundTime;
    private long responseTime;
    private long completionTime;
    private long startTime;
    private long cpuTimeConsumed;
    private int contextSwitchCount;
    private double virtualRuntime;
    private Integer allocatedCoreId;
    private boolean starvationRisk;
    private int ioWaitRemaining;
    private Instant createdAt;
    private Instant updatedAt;

    public ProcessControlBlockDto() {
    }

    public static ProcessControlBlockDto fromEntity(ProcessControlBlock entity) {
        if (entity == null) return null;
        ProcessControlBlockDto dto = new ProcessControlBlockDto();
        dto.setId(entity.getId());
        dto.setProcessId(entity.getProcessId());
        dto.setProcessName(entity.getProcessName());
        dto.setOwner(entity.getOwner());
        dto.setTargetNodeId(entity.getTargetNodeId());
        dto.setProcessType(entity.getProcessType());
        dto.setState(entity.getState());
        dto.setPriority(entity.getPriority());
        dto.setEffectivePriority(entity.getEffectivePriority());
        dto.setArrivalTime(entity.getArrivalTime());
        dto.setBurstTime(entity.getBurstTime());
        dto.setRemainingBurstTime(entity.getRemainingBurstTime());
        dto.setWaitingTime(entity.getWaitingTime());
        dto.setTurnaroundTime(entity.getTurnaroundTime());
        dto.setResponseTime(entity.getResponseTime());
        dto.setCompletionTime(entity.getCompletionTime());
        dto.setStartTime(entity.getStartTime());
        dto.setCpuTimeConsumed(entity.getCpuTimeConsumed());
        dto.setContextSwitchCount(entity.getContextSwitchCount());
        dto.setVirtualRuntime(entity.getVirtualRuntime());
        dto.setAllocatedCoreId(entity.getAllocatedCoreId());
        dto.setStarvationRisk(entity.isStarvationRisk());
        dto.setIoWaitRemaining(entity.getIoWaitRemaining());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }
    public ProcessType getProcessType() { return processType; }
    public void setProcessType(ProcessType processType) { this.processType = processType; }
    public ProcessState getState() { return state; }
    public void setState(ProcessState state) { this.state = state; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public int getEffectivePriority() { return effectivePriority; }
    public void setEffectivePriority(int effectivePriority) { this.effectivePriority = effectivePriority; }
    public long getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(long arrivalTime) { this.arrivalTime = arrivalTime; }
    public long getBurstTime() { return burstTime; }
    public void setBurstTime(long burstTime) { this.burstTime = burstTime; }
    public long getRemainingBurstTime() { return remainingBurstTime; }
    public void setRemainingBurstTime(long remainingBurstTime) { this.remainingBurstTime = remainingBurstTime; }
    public long getWaitingTime() { return waitingTime; }
    public void setWaitingTime(long waitingTime) { this.waitingTime = waitingTime; }
    public long getTurnaroundTime() { return turnaroundTime; }
    public void setTurnaroundTime(long turnaroundTime) { this.turnaroundTime = turnaroundTime; }
    public long getResponseTime() { return responseTime; }
    public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
    public long getCompletionTime() { return completionTime; }
    public void setCompletionTime(long completionTime) { this.completionTime = completionTime; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getCpuTimeConsumed() { return cpuTimeConsumed; }
    public void setCpuTimeConsumed(long cpuTimeConsumed) { this.cpuTimeConsumed = cpuTimeConsumed; }
    public int getContextSwitchCount() { return contextSwitchCount; }
    public void setContextSwitchCount(int contextSwitchCount) { this.contextSwitchCount = contextSwitchCount; }
    public double getVirtualRuntime() { return virtualRuntime; }
    public void setVirtualRuntime(double virtualRuntime) { this.virtualRuntime = virtualRuntime; }
    public Integer getAllocatedCoreId() { return allocatedCoreId; }
    public void setAllocatedCoreId(Integer allocatedCoreId) { this.allocatedCoreId = allocatedCoreId; }
    public boolean isStarvationRisk() { return starvationRisk; }
    public void setStarvationRisk(boolean starvationRisk) { this.starvationRisk = starvationRisk; }
    public int getIoWaitRemaining() { return ioWaitRemaining; }
    public void setIoWaitRemaining(int ioWaitRemaining) { this.ioWaitRemaining = ioWaitRemaining; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
