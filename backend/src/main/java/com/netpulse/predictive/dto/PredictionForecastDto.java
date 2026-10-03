package com.netpulse.predictive.dto;

import com.netpulse.predictive.state.PredictedCongestionState;

public class PredictionForecastDto {

    private String nodeId;
    private PredictedCongestionState currentState;
    private PredictedCongestionState forecastState;
    private double forecastConfidence;
    private int horizonMinutes;
    private double projectedCpu;
    private double projectedLatency;
    private double riskScore; // 0 - 100
    private String rationale;

    public PredictionForecastDto() {
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public PredictedCongestionState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(PredictedCongestionState currentState) {
        this.currentState = currentState;
    }

    public PredictedCongestionState getForecastState() {
        return forecastState;
    }

    public void setForecastState(PredictedCongestionState forecastState) {
        this.forecastState = forecastState;
    }

    public double getForecastConfidence() {
        return forecastConfidence;
    }

    public void setForecastConfidence(double forecastConfidence) {
        this.forecastConfidence = forecastConfidence;
    }

    public int getHorizonMinutes() {
        return horizonMinutes;
    }

    public void setHorizonMinutes(int horizonMinutes) {
        this.horizonMinutes = horizonMinutes;
    }

    public double getProjectedCpu() {
        return projectedCpu;
    }

    public void setProjectedCpu(double projectedCpu) {
        this.projectedCpu = projectedCpu;
    }

    public double getProjectedLatency() {
        return projectedLatency;
    }

    public void setProjectedLatency(double projectedLatency) {
        this.projectedLatency = projectedLatency;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }
}
