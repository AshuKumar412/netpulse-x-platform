package com.netpulse.whatif.dto;

import com.netpulse.scheduler.dto.AlgorithmBenchmarkResult;
import java.util.List;

public class SchedulerComparisonResponse {

    private String baselineAlgorithm;
    private List<AlgorithmBenchmarkResult> results;
    private String bestAlgorithmForWorkload;
    private String summary;

    public SchedulerComparisonResponse() {
    }

    public String getBaselineAlgorithm() { return baselineAlgorithm; }
    public void setBaselineAlgorithm(String baselineAlgorithm) { this.baselineAlgorithm = baselineAlgorithm; }
    public List<AlgorithmBenchmarkResult> getResults() { return results; }
    public void setResults(List<AlgorithmBenchmarkResult> results) { this.results = results; }
    public String getBestAlgorithmForWorkload() { return bestAlgorithmForWorkload; }
    public void setBestAlgorithmForWorkload(String bestAlgorithmForWorkload) { this.bestAlgorithmForWorkload = bestAlgorithmForWorkload; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
