package com.netpulse.scheduler.dto;

import com.netpulse.scheduler.state.CpuCoreState;

public class CpuCoreDto {

    private int coreId;
    private String nodeId;
    private CpuCoreState state;
    private String currentProcessId;
    private String currentProcessName;
    private int currentProcessPriority;
    private long currentProcessRemainingBurst;
    private long currentBurstTicksRan;
    private double coreUtilizationPercent;
    private long totalTicksBusy;
    private long totalTicksIdle;

    public CpuCoreDto() {
    }

    public CpuCoreDto(int coreId, String nodeId, CpuCoreState state) {
        this.coreId = coreId;
        this.nodeId = nodeId;
        this.state = state;
    }

    public int getCoreId() { return coreId; }
    public void setCoreId(int coreId) { this.coreId = coreId; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public CpuCoreState getState() { return state; }
    public void setState(CpuCoreState state) { this.state = state; }
    public String getCurrentProcessId() { return currentProcessId; }
    public void setCurrentProcessId(String currentProcessId) { this.currentProcessId = currentProcessId; }
    public String getCurrentProcessName() { return currentProcessName; }
    public void setCurrentProcessName(String currentProcessName) { this.currentProcessName = currentProcessName; }
    public int getCurrentProcessPriority() { return currentProcessPriority; }
    public void setCurrentProcessPriority(int currentProcessPriority) { this.currentProcessPriority = currentProcessPriority; }
    public long getCurrentProcessRemainingBurst() { return currentProcessRemainingBurst; }
    public void setCurrentProcessRemainingBurst(long currentProcessRemainingBurst) { this.currentProcessRemainingBurst = currentProcessRemainingBurst; }
    public long getCurrentBurstTicksRan() { return currentBurstTicksRan; }
    public void setCurrentBurstTicksRan(long currentBurstTicksRan) { this.currentBurstTicksRan = currentBurstTicksRan; }
    public double getCoreUtilizationPercent() { return coreUtilizationPercent; }
    public void setCoreUtilizationPercent(double coreUtilizationPercent) { this.coreUtilizationPercent = coreUtilizationPercent; }
    public long getTotalTicksBusy() { return totalTicksBusy; }
    public void setTotalTicksBusy(long totalTicksBusy) { this.totalTicksBusy = totalTicksBusy; }
    public long getTotalTicksIdle() { return totalTicksIdle; }
    public void setTotalTicksIdle(long totalTicksIdle) { this.totalTicksIdle = totalTicksIdle; }
}
