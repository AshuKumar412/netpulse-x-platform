package com.netpulse.scheduler.dto;

import com.netpulse.scheduler.state.SchedulerAlgorithm;
import java.util.List;

public class AlgorithmBenchmarkResult {

    private SchedulerAlgorithm algorithm;
    private String algorithmName;
    private double averageWaitingTime;
    private double averageTurnaroundTime;
    private double averageResponseTime;
    private double cpuUtilizationPercent;
    private double throughput;
    private int totalContextSwitches;
    private long totalSimulationTicks;
    private int completedProcessesCount;
    private List<GanttBlockDto> ganttTimeline;
    private String explanation;

    public AlgorithmBenchmarkResult() {
    }

    public SchedulerAlgorithm getAlgorithm() { return algorithm; }
    public void setAlgorithm(SchedulerAlgorithm algorithm) { this.algorithm = algorithm; }
    public String getAlgorithmName() { return algorithmName; }
    public void setAlgorithmName(String algorithmName) { this.algorithmName = algorithmName; }
    public double getAverageWaitingTime() { return averageWaitingTime; }
    public void setAverageWaitingTime(double averageWaitingTime) { this.averageWaitingTime = averageWaitingTime; }
    public double getAverageTurnaroundTime() { return averageTurnaroundTime; }
    public void setAverageTurnaroundTime(double averageTurnaroundTime) { this.averageTurnaroundTime = averageTurnaroundTime; }
    public double getAverageResponseTime() { return averageResponseTime; }
    public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
    public double getCpuUtilizationPercent() { return cpuUtilizationPercent; }
    public void setCpuUtilizationPercent(double cpuUtilizationPercent) { this.cpuUtilizationPercent = cpuUtilizationPercent; }
    public double getThroughput() { return throughput; }
    public void setThroughput(double throughput) { this.throughput = throughput; }
    public int getTotalContextSwitches() { return totalContextSwitches; }
    public void setTotalContextSwitches(int totalContextSwitches) { this.totalContextSwitches = totalContextSwitches; }
    public long getTotalSimulationTicks() { return totalSimulationTicks; }
    public void setTotalSimulationTicks(long totalSimulationTicks) { this.totalSimulationTicks = totalSimulationTicks; }
    public int getCompletedProcessesCount() { return completedProcessesCount; }
    public void setCompletedProcessesCount(int completedProcessesCount) { this.completedProcessesCount = completedProcessesCount; }
    public List<GanttBlockDto> getGanttTimeline() { return ganttTimeline; }
    public void setGanttTimeline(List<GanttBlockDto> ganttTimeline) { this.ganttTimeline = ganttTimeline; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
