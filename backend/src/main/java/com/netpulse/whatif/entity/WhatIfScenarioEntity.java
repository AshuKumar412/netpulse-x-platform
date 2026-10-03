package com.netpulse.whatif.entity;

import com.netpulse.whatif.state.ScenarioStatus;
import com.netpulse.whatif.state.ScenarioType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "whatif_scenarios", indexes = {
        @Index(name = "idx_whatif_scenario_id", columnList = "scenario_id", unique = true),
        @Index(name = "idx_whatif_status", columnList = "status"),
        @Index(name = "idx_whatif_created_at", columnList = "created_at")
})
public class WhatIfScenarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false, unique = true, length = 64)
    private String scenarioId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ScenarioType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ScenarioStatus status = ScenarioStatus.DRAFT;

    @Column(name = "changes_json", columnDefinition = "TEXT")
    private String changesJson;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds = 60;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public WhatIfScenarioEntity() {
    }

    public WhatIfScenarioEntity(String scenarioId, String name, String description, String createdBy, ScenarioType type, String changesJson, int durationSeconds) {
        this.scenarioId = scenarioId;
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.type = type;
        this.status = ScenarioStatus.READY;
        this.changesJson = changesJson;
        this.durationSeconds = durationSeconds;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getScenarioId() { return scenarioId; }
    public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public ScenarioType getType() { return type; }
    public void setType(ScenarioType type) { this.type = type; }
    public ScenarioStatus getStatus() { return status; }
    public void setStatus(ScenarioStatus status) { this.status = status; }
    public String getChangesJson() { return changesJson; }
    public void setChangesJson(String changesJson) { this.changesJson = changesJson; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
