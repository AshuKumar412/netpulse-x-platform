package com.netpulse.predictive.dto;

import com.netpulse.predictive.state.ModelType;
import java.time.Instant;

public class ModelTrainingRequest {

    private ModelType modelType = ModelType.MULTINOMIAL_LOGISTIC_REGRESSION;
    private Instant startTime;
    private Instant endTime;
    private double testSplitRatio = 0.20;
    private double learningRate = 0.05;
    private int maxEpochs = 200;
    private double l2Regularization = 0.001;
    private int maxDepth = 6;
    private long randomSeed = 42L;
    private String labelPolicyVersion = "v1";
    private String notes;

    public ModelTrainingRequest() {
    }

    public ModelType getModelType() {
        return modelType;
    }

    public void setModelType(ModelType modelType) {
        this.modelType = modelType;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public double getTestSplitRatio() {
        return testSplitRatio;
    }

    public void setTestSplitRatio(double testSplitRatio) {
        this.testSplitRatio = testSplitRatio;
    }

    public double getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    public int getMaxEpochs() {
        return maxEpochs;
    }

    public void setMaxEpochs(int maxEpochs) {
        this.maxEpochs = maxEpochs;
    }

    public double getL2Regularization() {
        return l2Regularization;
    }

    public void setL2Regularization(double l2Regularization) {
        this.l2Regularization = l2Regularization;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public String getLabelPolicyVersion() {
        return labelPolicyVersion;
    }

    public void setLabelPolicyVersion(String labelPolicyVersion) {
        this.labelPolicyVersion = labelPolicyVersion;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
