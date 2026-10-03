package com.netpulse.scheduler.dto;

import com.netpulse.scheduler.state.SchedulerAlgorithm;
import com.netpulse.scheduler.state.SchedulerStatus;

import java.util.List;

public class SchedulerStatusResponse {

    private SchedulerStatus status;
    private SchedulerAlgorithm algorithm;
    private SchedulerPolicyConfig policyConfig;
    private SchedulerMetricsDto metrics;
    private List<CpuCoreDto> cores;
    private List<ProcessControlBlockDto> readyQueue;
    private List<ProcessControlBlockDto> runningProcesses;
    private List<ProcessControlBlockDto> waitingQueue;
    private List<GanttBlockDto> recentGanttBlocks;
    private List<ContextSwitchEventDto> recentContextSwitches;
    private long currentTick;

    public SchedulerStatusResponse() {
    }

    public SchedulerStatus getStatus() { return status; }
    public void setStatus(SchedulerStatus status) { this.status = status; }
    public SchedulerAlgorithm getAlgorithm() { return algorithm; }
    public void setAlgorithm(SchedulerAlgorithm algorithm) { this.algorithm = algorithm; }
    public SchedulerPolicyConfig getPolicyConfig() { return policyConfig; }
    public void setPolicyConfig(SchedulerPolicyConfig policyConfig) { this.policyConfig = policyConfig; }
    public SchedulerMetricsDto getMetrics() { return metrics; }
    public void setMetrics(SchedulerMetricsDto metrics) { this.metrics = metrics; }
    public List<CpuCoreDto> getCores() { return cores; }
    public void setCores(List<CpuCoreDto> cores) { this.cores = cores; }
    public List<ProcessControlBlockDto> getReadyQueue() { return readyQueue; }
    public void setReadyQueue(List<ProcessControlBlockDto> readyQueue) { this.readyQueue = readyQueue; }
    public List<ProcessControlBlockDto> getRunningProcesses() { return runningProcesses; }
    public void setRunningProcesses(List<ProcessControlBlockDto> runningProcesses) { this.runningProcesses = runningProcesses; }
    public List<ProcessControlBlockDto> getWaitingQueue() { return waitingQueue; }
    public void setWaitingQueue(List<ProcessControlBlockDto> waitingQueue) { this.waitingQueue = waitingQueue; }
    public List<GanttBlockDto> getRecentGanttBlocks() { return recentGanttBlocks; }
    public void setRecentGanttBlocks(List<GanttBlockDto> recentGanttBlocks) { this.recentGanttBlocks = recentGanttBlocks; }
    public List<ContextSwitchEventDto> getRecentContextSwitches() { return recentContextSwitches; }
    public void setRecentContextSwitches(List<ContextSwitchEventDto> recentContextSwitches) { this.recentContextSwitches = recentContextSwitches; }
    public long getCurrentTick() { return currentTick; }
    public void setCurrentTick(long currentTick) { this.currentTick = currentTick; }
}
