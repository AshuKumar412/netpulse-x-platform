package com.netpulse.routing.dto;

import com.netpulse.routing.entity.RoutingStrategyType;

import java.time.Instant;
import java.util.List;

public class RoutingDecisionResponse {

    private String requestId;
    private RoutingStrategyType strategy;
    private String status; // SUCCESS, NO_ELIGIBLE_NODE
    private String selectedNodeId;
    private String selectedNodeName;
    private Integer totalCandidates;
    private Integer eligibleCandidates;
    private Double score;
    private String decisionReason;
    private Instant timestamp;
    private List<CandidateEvaluationDto> evaluatedCandidates;

    public RoutingDecisionResponse() {
    }

    public RoutingDecisionResponse(String requestId, RoutingStrategyType strategy, String status,
                                   String selectedNodeId, String selectedNodeName, Integer totalCandidates,
                                   Integer eligibleCandidates, Double score, String decisionReason,
                                   Instant timestamp, List<CandidateEvaluationDto> evaluatedCandidates) {
        this.requestId = requestId;
        this.strategy = strategy;
        this.status = status;
        this.selectedNodeId = selectedNodeId;
        this.selectedNodeName = selectedNodeName;
        this.totalCandidates = totalCandidates;
        this.eligibleCandidates = eligibleCandidates;
        this.score = score;
        this.decisionReason = decisionReason;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.evaluatedCandidates = evaluatedCandidates;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public RoutingStrategyType getStrategy() {
        return strategy;
    }

    public void setStrategy(RoutingStrategyType strategy) {
        this.strategy = strategy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSelectedNodeId() {
        return selectedNodeId;
    }

    public void setSelectedNodeId(String selectedNodeId) {
        this.selectedNodeId = selectedNodeId;
    }

    public String getSelectedNodeName() {
        return selectedNodeName;
    }

    public void setSelectedNodeName(String selectedNodeName) {
        this.selectedNodeName = selectedNodeName;
    }

    public Integer getTotalCandidates() {
        return totalCandidates;
    }

    public void setTotalCandidates(Integer totalCandidates) {
        this.totalCandidates = totalCandidates;
    }

    public Integer getEligibleCandidates() {
        return eligibleCandidates;
    }

    public void setEligibleCandidates(Integer eligibleCandidates) {
        this.eligibleCandidates = eligibleCandidates;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<CandidateEvaluationDto> getEvaluatedCandidates() {
        return evaluatedCandidates;
    }

    public void setEvaluatedCandidates(List<CandidateEvaluationDto> evaluatedCandidates) {
        this.evaluatedCandidates = evaluatedCandidates;
    }
}
