package com.netpulse.whatif.dto;

import com.netpulse.scheduler.dto.GanttBlockDto;
import com.netpulse.whatif.state.RiskIndicator;
import com.netpulse.whatif.state.ScenarioStatus;
import com.netpulse.whatif.state.ScenarioType;

import java.time.Instant;
import java.util.List;

public class SimulationRunResponse {

    private String runId;
    private String scenarioId;
    private String scenarioName;
    private ScenarioType scenarioType;
    private ScenarioStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private long totalSimulationTicks;
    private SystemSnapshotDto baselineSnapshot;
    private BaselineVsSimulatedMetricsDto comparisonMetrics;
    private List<SimulationDecisionDto> decisions;
    private List<SimulationEventDto> events;
    private List<RiskIndicator> riskIndicators;
    private List<GanttBlockDto> simulatedGanttTimeline;
    private String executiveSummary;
    private String decisionExplainabilitySummary;

    public SimulationRunResponse() {
    }

    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getScenarioId() { return scenarioId; }
    public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }
    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    public ScenarioType getScenarioType() { return scenarioType; }
    public void setScenarioType(ScenarioType scenarioType) { this.scenarioType = scenarioType; }
    public ScenarioStatus getStatus() { return status; }
    public void setStatus(ScenarioStatus status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public long getTotalSimulationTicks() { return totalSimulationTicks; }
    public void setTotalSimulationTicks(long totalSimulationTicks) { this.totalSimulationTicks = totalSimulationTicks; }
    public SystemSnapshotDto getBaselineSnapshot() { return baselineSnapshot; }
    public void setBaselineSnapshot(SystemSnapshotDto baselineSnapshot) { this.baselineSnapshot = baselineSnapshot; }
    public BaselineVsSimulatedMetricsDto getComparisonMetrics() { return comparisonMetrics; }
    public void setComparisonMetrics(BaselineVsSimulatedMetricsDto comparisonMetrics) { this.comparisonMetrics = comparisonMetrics; }
    public List<SimulationDecisionDto> getDecisions() { return decisions; }
    public void setDecisions(List<SimulationDecisionDto> decisions) { this.decisions = decisions; }
    public List<SimulationEventDto> getEvents() { return events; }
    public void setEvents(List<SimulationEventDto> events) { this.events = events; }
    public List<RiskIndicator> getRiskIndicators() { return riskIndicators; }
    public void setRiskIndicators(List<RiskIndicator> riskIndicators) { this.riskIndicators = riskIndicators; }
    public List<GanttBlockDto> getSimulatedGanttTimeline() { return simulatedGanttTimeline; }
    public void setSimulatedGanttTimeline(List<GanttBlockDto> simulatedGanttTimeline) { this.simulatedGanttTimeline = simulatedGanttTimeline; }
    public String getExecutiveSummary() { return executiveSummary; }
    public void setExecutiveSummary(String executiveSummary) { this.executiveSummary = executiveSummary; }
    public String getDecisionExplainabilitySummary() { return decisionExplainabilitySummary; }
    public void setDecisionExplainabilitySummary(String decisionExplainabilitySummary) { this.decisionExplainabilitySummary = decisionExplainabilitySummary; }
}
