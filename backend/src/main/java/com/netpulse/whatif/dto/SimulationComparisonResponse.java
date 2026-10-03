package com.netpulse.whatif.dto;

import com.netpulse.whatif.state.RiskIndicator;

import java.util.List;

public class SimulationComparisonResponse {

    private String scenarioId;
    private String scenarioName;
    private SystemSnapshotDto baselineSnapshot;
    private BaselineVsSimulatedMetricsDto metrics;
    private List<SimulationDecisionDto> keyDecisions;
    private List<RiskIndicator> riskIndicators;
    private String impactAnalysisSummary;

    public SimulationComparisonResponse() {
    }

    public String getScenarioId() { return scenarioId; }
    public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }
    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    public SystemSnapshotDto getBaselineSnapshot() { return baselineSnapshot; }
    public void setBaselineSnapshot(SystemSnapshotDto baselineSnapshot) { this.baselineSnapshot = baselineSnapshot; }
    public BaselineVsSimulatedMetricsDto getMetrics() { return metrics; }
    public void setMetrics(BaselineVsSimulatedMetricsDto metrics) { this.metrics = metrics; }
    public List<SimulationDecisionDto> getKeyDecisions() { return keyDecisions; }
    public void setKeyDecisions(List<SimulationDecisionDto> keyDecisions) { this.keyDecisions = keyDecisions; }
    public List<RiskIndicator> getRiskIndicators() { return riskIndicators; }
    public void setRiskIndicators(List<RiskIndicator> riskIndicators) { this.riskIndicators = riskIndicators; }
    public String getImpactAnalysisSummary() { return impactAnalysisSummary; }
    public void setImpactAnalysisSummary(String impactAnalysisSummary) { this.impactAnalysisSummary = impactAnalysisSummary; }
}
