package com.netpulse.predictive.dto;

public class PredictionOutcomeVerificationDto {

    private long totalPredictionsEvaluated;
    private long truePositiveCount;
    private long trueNegativeCount;
    private long falsePositiveCount;
    private long falseNegativeCount;
    private double verificationAccuracy;
    private double empiricalPrecision;
    private double empiricalRecall;

    public PredictionOutcomeVerificationDto() {
    }

    public long getTotalPredictionsEvaluated() {
        return totalPredictionsEvaluated;
    }

    public void setTotalPredictionsEvaluated(long totalPredictionsEvaluated) {
        this.totalPredictionsEvaluated = totalPredictionsEvaluated;
    }

    public long getTruePositiveCount() {
        return truePositiveCount;
    }

    public void setTruePositiveCount(long truePositiveCount) {
        this.truePositiveCount = truePositiveCount;
    }

    public long getTrueNegativeCount() {
        return trueNegativeCount;
    }

    public void setTrueNegativeCount(long trueNegativeCount) {
        this.trueNegativeCount = trueNegativeCount;
    }

    public long getFalsePositiveCount() {
        return falsePositiveCount;
    }

    public void setFalsePositiveCount(long falsePositiveCount) {
        this.falsePositiveCount = falsePositiveCount;
    }

    public long getFalseNegativeCount() {
        return falseNegativeCount;
    }

    public void setFalseNegativeCount(long falseNegativeCount) {
        this.falseNegativeCount = falseNegativeCount;
    }

    public double getVerificationAccuracy() {
        return verificationAccuracy;
    }

    public void setVerificationAccuracy(double verificationAccuracy) {
        this.verificationAccuracy = verificationAccuracy;
    }

    public double getEmpiricalPrecision() {
        return empiricalPrecision;
    }

    public void setEmpiricalPrecision(double empiricalPrecision) {
        this.empiricalPrecision = empiricalPrecision;
    }

    public double getEmpiricalRecall() {
        return empiricalRecall;
    }

    public void setEmpiricalRecall(double empiricalRecall) {
        this.empiricalRecall = empiricalRecall;
    }
}
