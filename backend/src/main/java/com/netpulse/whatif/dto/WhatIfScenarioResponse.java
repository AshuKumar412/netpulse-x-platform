package com.netpulse.whatif.dto;

import com.netpulse.whatif.state.ScenarioStatus;
import com.netpulse.whatif.state.ScenarioType;

import java.time.Instant;
import java.util.List;

public class WhatIfScenarioResponse {

    private String scenarioId;
    private String name;
    private String description;
    private String createdBy;
    private ScenarioType type;
    private ScenarioStatus status;
    private List<WhatIfScenarioChangeDto> changes;
    private int durationSeconds;
    private Instant createdAt;
    private Instant completedAt;
    private SimulationRunResponse latestRun;

    public WhatIfScenarioResponse() {
    }

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
    public List<WhatIfScenarioChangeDto> getChanges() { return changes; }
    public void setChanges(List<WhatIfScenarioChangeDto> changes) { this.changes = changes; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public SimulationRunResponse getLatestRun() { return latestRun; }
    public void setLatestRun(SimulationRunResponse latestRun) { this.latestRun = latestRun; }
}
