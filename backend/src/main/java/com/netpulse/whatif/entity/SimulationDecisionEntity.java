package com.netpulse.whatif.entity;

import com.netpulse.whatif.state.SimulationDecisionType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "whatif_simulation_decisions", indexes = {
        @Index(name = "idx_simdec_run_id", columnList = "run_id"),
        @Index(name = "idx_simdec_decision_id", columnList = "decision_id", unique = true)
})
public class SimulationDecisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "decision_id", nullable = false, unique = true, length = 64)
    private String decisionId;

    @Column(name = "run_id", nullable = false, length = 64)
    private String runId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, length = 32)
    private SimulationDecisionType decisionType;

    @Column(name = "simulation_tick", nullable = false)
    private long simulationTick;

    @Column(name = "selected_node_id", length = 64)
    private String selectedNodeId;

    @Column(name = "source_node_id", length = 64)
    private String sourceNodeId;

    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;

    @Column(name = "candidate_evaluations_json", columnDefinition = "TEXT")
    private String candidateEvaluationsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public SimulationDecisionEntity() {
    }

    public SimulationDecisionEntity(String decisionId, String runId, SimulationDecisionType decisionType, long simulationTick, String selectedNodeId, String sourceNodeId, String decisionReason, String candidateEvaluationsJson) {
        this.decisionId = decisionId;
        this.runId = runId;
        this.decisionType = decisionType;
        this.simulationTick = simulationTick;
        this.selectedNodeId = selectedNodeId;
        this.sourceNodeId = sourceNodeId;
        this.decisionReason = decisionReason;
        this.candidateEvaluationsJson = candidateEvaluationsJson;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDecisionId() { return decisionId; }
    public void setDecisionId(String decisionId) { this.decisionId = decisionId; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public SimulationDecisionType getDecisionType() { return decisionType; }
    public void setDecisionType(SimulationDecisionType decisionType) { this.decisionType = decisionType; }
    public long getSimulationTick() { return simulationTick; }
    public void setSimulationTick(long simulationTick) { this.simulationTick = simulationTick; }
    public String getSelectedNodeId() { return selectedNodeId; }
    public void setSelectedNodeId(String selectedNodeId) { this.selectedNodeId = selectedNodeId; }
    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }
    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }
    public String getCandidateEvaluationsJson() { return candidateEvaluationsJson; }
    public void setCandidateEvaluationsJson(String candidateEvaluationsJson) { this.candidateEvaluationsJson = candidateEvaluationsJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
