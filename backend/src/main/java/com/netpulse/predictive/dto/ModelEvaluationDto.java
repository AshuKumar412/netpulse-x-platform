package com.netpulse.predictive.dto;

import java.util.Map;

public class ModelEvaluationDto {

    private double accuracy;
    private double precisionScore;
    private double recallScore;
    private double f1Score;
    private ConfusionMatrixDto confusionMatrix;
    private Map<String, Double> perClassPrecision;
    private Map<String, Double> perClassRecall;
    private Map<String, Double> perClassF1;
    private int evaluationSamples;

    public ModelEvaluationDto() {
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public double getPrecisionScore() {
        return precisionScore;
    }

    public void setPrecisionScore(double precisionScore) {
        this.precisionScore = precisionScore;
    }

    public double getRecallScore() {
        return recallScore;
    }

    public void setRecallScore(double recallScore) {
        this.recallScore = recallScore;
    }

    public double getF1Score() {
        return f1Score;
    }

    public void setF1Score(double f1Score) {
        this.f1Score = f1Score;
    }

    public ConfusionMatrixDto getConfusionMatrix() {
        return confusionMatrix;
    }

    public void setConfusionMatrix(ConfusionMatrixDto confusionMatrix) {
        this.confusionMatrix = confusionMatrix;
    }

    public Map<String, Double> getPerClassPrecision() {
        return perClassPrecision;
    }

    public void setPerClassPrecision(Map<String, Double> perClassPrecision) {
        this.perClassPrecision = perClassPrecision;
    }

    public Map<String, Double> getPerClassRecall() {
        return perClassRecall;
    }

    public void setPerClassRecall(Map<String, Double> perClassRecall) {
        this.perClassRecall = perClassRecall;
    }

    public Map<String, Double> getPerClassF1() {
        return perClassF1;
    }

    public void setPerClassF1(Map<String, Double> perClassF1) {
        this.perClassF1 = perClassF1;
    }

    public int getEvaluationSamples() {
        return evaluationSamples;
    }

    public void setEvaluationSamples(int evaluationSamples) {
        this.evaluationSamples = evaluationSamples;
    }
}
