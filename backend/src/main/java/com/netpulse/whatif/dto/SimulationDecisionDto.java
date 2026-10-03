package com.netpulse.whatif.dto;

import com.netpulse.whatif.state.SimulationDecisionType;

import java.time.Instant;
import java.util.List;

public class SimulationDecisionDto {

    private String decisionId;
    private SimulationDecisionType decisionType;
    private long simulationTick;
    private String selectedNodeId;
    private String sourceNodeId;
    private String decisionReason;
    private List<SimulationCandidateEvaluationDto> candidateEvaluations;
    private Instant timestamp;

    public SimulationDecisionDto() {
    }

    public SimulationDecisionDto(String decisionId, SimulationDecisionType decisionType, long simulationTick, String selectedNodeId, String sourceNodeId, String decisionReason, List<SimulationCandidateEvaluationDto> candidateEvaluations) {
        this.decisionId = decisionId;
        this.decisionType = decisionType;
        this.simulationTick = simulationTick;
        this.selectedNodeId = selectedNodeId;
        this.sourceNodeId = sourceNodeId;
        this.decisionReason = decisionReason;
        this.candidateEvaluations = candidateEvaluations;
        this.timestamp = Instant.now();
    }

    public String getDecisionId() { return decisionId; }
    public void setDecisionId(String decisionId) { this.decisionId = decisionId; }
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
    public List<SimulationCandidateEvaluationDto> getCandidateEvaluations() { return candidateEvaluations; }
    public void setCandidateEvaluations(List<SimulationCandidateEvaluationDto> candidateEvaluations) { this.candidateEvaluations = candidateEvaluations; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
