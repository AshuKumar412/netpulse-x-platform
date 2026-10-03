package com.netpulse.predictive.dto;

public class FeatureImportanceDto {

    private String featureName;
    private double importanceScore;
    private double relativePercentage;

    public FeatureImportanceDto() {
    }

    public FeatureImportanceDto(String featureName, double importanceScore, double relativePercentage) {
        this.featureName = featureName;
        this.importanceScore = importanceScore;
        this.relativePercentage = relativePercentage;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public double getImportanceScore() {
        return importanceScore;
    }

    public void setImportanceScore(double importanceScore) {
        this.importanceScore = importanceScore;
    }

    public double getRelativePercentage() {
        return relativePercentage;
    }

    public void setRelativePercentage(double relativePercentage) {
        this.relativePercentage = relativePercentage;
    }
}
