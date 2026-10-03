package com.netpulse.predictive.dto;

public class PredictiveConfigDto {

    private boolean predictiveRoutingEnabled = false;
    private double predictiveRoutingPenaltyWeight = 0.25;
    private boolean predictiveFailoverWarningEnabled = true;
    private int predictionHorizonMinutes = 5;
    private double warningThresholdProbability = 0.65;
    private double congestionLikelyThresholdProbability = 0.80;

    public PredictiveConfigDto() {
    }

    public boolean isPredictiveRoutingEnabled() {
        return predictiveRoutingEnabled;
    }

    public void setPredictiveRoutingEnabled(boolean predictiveRoutingEnabled) {
        this.predictiveRoutingEnabled = predictiveRoutingEnabled;
    }

    public double getPredictiveRoutingPenaltyWeight() {
        return predictiveRoutingPenaltyWeight;
    }

    public void setPredictiveRoutingPenaltyWeight(double predictiveRoutingPenaltyWeight) {
        this.predictiveRoutingPenaltyWeight = predictiveRoutingPenaltyWeight;
    }

    public boolean isPredictiveFailoverWarningEnabled() {
        return predictiveFailoverWarningEnabled;
    }

    public void setPredictiveFailoverWarningEnabled(boolean predictiveFailoverWarningEnabled) {
        this.predictiveFailoverWarningEnabled = predictiveFailoverWarningEnabled;
    }

    public int getPredictionHorizonMinutes() {
        return predictionHorizonMinutes;
    }

    public void setPredictionHorizonMinutes(int predictionHorizonMinutes) {
        this.predictionHorizonMinutes = predictionHorizonMinutes;
    }

    public double getWarningThresholdProbability() {
        return warningThresholdProbability;
    }

    public void setWarningThresholdProbability(double warningThresholdProbability) {
        this.warningThresholdProbability = warningThresholdProbability;
    }

    public double getCongestionLikelyThresholdProbability() {
        return congestionLikelyThresholdProbability;
    }

    public void setCongestionLikelyThresholdProbability(double congestionLikelyThresholdProbability) {
        this.congestionLikelyThresholdProbability = congestionLikelyThresholdProbability;
    }
}
