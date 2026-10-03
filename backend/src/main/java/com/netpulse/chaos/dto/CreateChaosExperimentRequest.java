package com.netpulse.chaos.dto;

import com.netpulse.chaos.state.ChaosExperimentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateChaosExperimentRequest {

    @NotNull(message = "Experiment type is required")
    private ChaosExperimentType experimentType;

    private String targetNodeId;

    private String targetLinkId;

    private Double severity;

    @NotNull(message = "Duration in seconds is required")
    @Min(value = 1, message = "Duration must be at least 1 second")
    @Max(value = 300, message = "Duration cannot exceed 300 seconds")
    private Integer durationSeconds;

    public CreateChaosExperimentRequest() {
    }

    public CreateChaosExperimentRequest(ChaosExperimentType experimentType, String targetNodeId, Double severity, Integer durationSeconds) {
        this.experimentType = experimentType;
        this.targetNodeId = targetNodeId;
        this.severity = severity;
        this.durationSeconds = durationSeconds;
    }

    public CreateChaosExperimentRequest(ChaosExperimentType experimentType, String targetNodeId, String targetLinkId, Double severity, Integer durationSeconds) {
        this.experimentType = experimentType;
        this.targetNodeId = targetNodeId;
        this.targetLinkId = targetLinkId;
        this.severity = severity;
        this.durationSeconds = durationSeconds;
    }

    public ChaosExperimentType getExperimentType() {
        return experimentType;
    }

    public void setExperimentType(ChaosExperimentType experimentType) {
        this.experimentType = experimentType;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public String getTargetLinkId() {
        return targetLinkId;
    }

    public void setTargetLinkId(String targetLinkId) {
        this.targetLinkId = targetLinkId;
    }

    public Double getSeverity() {
        return severity;
    }

    public void setSeverity(Double severity) {
        this.severity = severity;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
