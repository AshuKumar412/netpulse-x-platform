package com.netpulse.scheduler.dto;

public class SchedulerMetricsDto {

    private double averageWaitingTime;
    private double averageTurnaroundTime;
    private double averageResponseTime;
    private double cpuUtilizationPercent;
    private double throughputPerMinute;
    private long totalContextSwitches;
    private long totalCompletedProcesses;
    private long totalRunningProcesses;
    private long totalReadyProcesses;
    private long totalWaitingProcesses;
    private long totalStarvationAlerts;
    private long currentSimulationTick;

    public SchedulerMetricsDto() {
    }

    public double getAverageWaitingTime() { return averageWaitingTime; }
    public void setAverageWaitingTime(double averageWaitingTime) { this.averageWaitingTime = averageWaitingTime; }
    public double getAverageTurnaroundTime() { return averageTurnaroundTime; }
    public void setAverageTurnaroundTime(double averageTurnaroundTime) { this.averageTurnaroundTime = averageTurnaroundTime; }
    public double getAverageResponseTime() { return averageResponseTime; }
    public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
    public double getCpuUtilizationPercent() { return cpuUtilizationPercent; }
    public void setCpuUtilizationPercent(double cpuUtilizationPercent) { this.cpuUtilizationPercent = cpuUtilizationPercent; }
    public double getThroughputPerMinute() { return throughputPerMinute; }
    public void setThroughputPerMinute(double throughputPerMinute) { this.throughputPerMinute = throughputPerMinute; }
    public long getTotalContextSwitches() { return totalContextSwitches; }
    public void setTotalContextSwitches(long totalContextSwitches) { this.totalContextSwitches = totalContextSwitches; }
    public long getTotalCompletedProcesses() { return totalCompletedProcesses; }
    public void setTotalCompletedProcesses(long totalCompletedProcesses) { this.totalCompletedProcesses = totalCompletedProcesses; }
    public long getTotalRunningProcesses() { return totalRunningProcesses; }
    public void setTotalRunningProcesses(long totalRunningProcesses) { this.totalRunningProcesses = totalRunningProcesses; }
    public long getTotalReadyProcesses() { return totalReadyProcesses; }
    public void setTotalReadyProcesses(long totalReadyProcesses) { this.totalReadyProcesses = totalReadyProcesses; }
    public long getTotalWaitingProcesses() { return totalWaitingProcesses; }
    public void setTotalWaitingProcesses(long totalWaitingProcesses) { this.totalWaitingProcesses = totalWaitingProcesses; }
    public long getTotalStarvationAlerts() { return totalStarvationAlerts; }
    public void setTotalStarvationAlerts(long totalStarvationAlerts) { this.totalStarvationAlerts = totalStarvationAlerts; }
    public long getCurrentSimulationTick() { return currentSimulationTick; }
    public void setCurrentSimulationTick(long currentSimulationTick) { this.currentSimulationTick = currentSimulationTick; }
}
