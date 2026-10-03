package com.netpulse.scheduler.dto;

import java.util.List;

public class AlgorithmComparisonResponse {

    private int workloadProcessCount;
    private List<ProcessControlBlockDto> workloadProcesses;
    private List<AlgorithmBenchmarkResult> results;
    private String bestWaitingTimeAlgorithm;
    private String bestThroughputAlgorithm;
    private String bestFairnessAlgorithm;
    private String overallSummary;

    public AlgorithmComparisonResponse() {
    }

    public int getWorkloadProcessCount() { return workloadProcessCount; }
    public void setWorkloadProcessCount(int workloadProcessCount) { this.workloadProcessCount = workloadProcessCount; }
    public List<ProcessControlBlockDto> getWorkloadProcesses() { return workloadProcesses; }
    public void setWorkloadProcesses(List<ProcessControlBlockDto> workloadProcesses) { this.workloadProcesses = workloadProcesses; }
    public List<AlgorithmBenchmarkResult> getResults() { return results; }
    public void setResults(List<AlgorithmBenchmarkResult> results) { this.results = results; }
    public String getBestWaitingTimeAlgorithm() { return bestWaitingTimeAlgorithm; }
    public void setBestWaitingTimeAlgorithm(String bestWaitingTimeAlgorithm) { this.bestWaitingTimeAlgorithm = bestWaitingTimeAlgorithm; }
    public String getBestThroughputAlgorithm() { return bestThroughputAlgorithm; }
    public void setBestThroughputAlgorithm(String bestThroughputAlgorithm) { this.bestThroughputAlgorithm = bestThroughputAlgorithm; }
    public String getBestFairnessAlgorithm() { return bestFairnessAlgorithm; }
    public void setBestFairnessAlgorithm(String bestFairnessAlgorithm) { this.bestFairnessAlgorithm = bestFairnessAlgorithm; }
    public String getOverallSummary() { return overallSummary; }
    public void setOverallSummary(String overallSummary) { this.overallSummary = overallSummary; }
}
