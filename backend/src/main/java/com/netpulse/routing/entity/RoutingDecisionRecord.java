package com.netpulse.routing.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "routing_decisions", indexes = {
        @Index(name = "idx_routing_request_id", columnList = "requestId", unique = true),
        @Index(name = "idx_routing_timestamp", columnList = "timestamp DESC"),
        @Index(name = "idx_routing_strategy", columnList = "strategy"),
        @Index(name = "idx_routing_selected_node", columnList = "selectedNodeId")
})
public class RoutingDecisionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String requestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RoutingStrategyType strategy;

    @Column(length = 64)
    private String selectedNodeId;

    @Column(length = 128)
    private String selectedNodeName;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private Integer totalCandidates;

    @Column(nullable = false)
    private Integer eligibleCandidates;

    @Column(nullable = false, length = 512)
    private String decisionReason;

    @Column
    private Double score;

    public RoutingDecisionRecord() {
    }

    public RoutingDecisionRecord(String requestId, RoutingStrategyType strategy, String selectedNodeId,
                                 String selectedNodeName, Instant timestamp, Integer totalCandidates,
                                 Integer eligibleCandidates, String decisionReason, Double score) {
        this.requestId = requestId;
        this.strategy = strategy;
        this.selectedNodeId = selectedNodeId;
        this.selectedNodeName = selectedNodeName;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.totalCandidates = totalCandidates;
        this.eligibleCandidates = eligibleCandidates;
        this.decisionReason = decisionReason;
        this.score = score;
    }

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
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

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
