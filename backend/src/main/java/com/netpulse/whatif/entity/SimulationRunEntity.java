package com.netpulse.whatif.entity;

import com.netpulse.whatif.state.ScenarioStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "whatif_simulation_runs", indexes = {
        @Index(name = "idx_simrun_id", columnList = "run_id", unique = true),
        @Index(name = "idx_simrun_scenario_id", columnList = "scenario_id"),
        @Index(name = "idx_simrun_started_at", columnList = "started_at")
})
public class SimulationRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false, unique = true, length = 64)
    private String runId;

    @Column(name = "scenario_id", nullable = false, length = 64)
    private String scenarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ScenarioStatus status = ScenarioStatus.RUNNING;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "total_simulation_ticks", nullable = false)
    private long totalSimulationTicks = 0;

    @Column(name = "baseline_snapshot_json", columnDefinition = "TEXT")
    private String baselineSnapshotJson;

    @Column(name = "metrics_comparison_json", columnDefinition = "TEXT")
    private String metricsComparisonJson;

    @Column(name = "risk_indicators_json", columnDefinition = "TEXT")
    private String riskIndicatorsJson;

    @Column(name = "executive_summary", columnDefinition = "TEXT")
    private String executiveSummary;

    public SimulationRunEntity() {
    }

    public SimulationRunEntity(String runId, String scenarioId, Instant startedAt) {
        this.runId = runId;
        this.scenarioId = scenarioId;
        this.startedAt = startedAt;
        this.status = ScenarioStatus.RUNNING;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getScenarioId() { return scenarioId; }
    public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }
    public ScenarioStatus getStatus() { return status; }
    public void setStatus(ScenarioStatus status) { this.status = status; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public long getTotalSimulationTicks() { return totalSimulationTicks; }
    public void setTotalSimulationTicks(long totalSimulationTicks) { this.totalSimulationTicks = totalSimulationTicks; }
    public String getBaselineSnapshotJson() { return baselineSnapshotJson; }
    public void setBaselineSnapshotJson(String baselineSnapshotJson) { this.baselineSnapshotJson = baselineSnapshotJson; }
    public String getMetricsComparisonJson() { return metricsComparisonJson; }
    public void setMetricsComparisonJson(String metricsComparisonJson) { this.metricsComparisonJson = metricsComparisonJson; }
    public String getRiskIndicatorsJson() { return riskIndicatorsJson; }
    public void setRiskIndicatorsJson(String riskIndicatorsJson) { this.riskIndicatorsJson = riskIndicatorsJson; }
    public String getExecutiveSummary() { return executiveSummary; }
    public void setExecutiveSummary(String executiveSummary) { this.executiveSummary = executiveSummary; }
}
