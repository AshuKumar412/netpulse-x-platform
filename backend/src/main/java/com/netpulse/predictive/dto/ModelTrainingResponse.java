package com.netpulse.predictive.dto;

import com.netpulse.predictive.state.ModelStatus;
import com.netpulse.predictive.state.ModelType;
import java.time.Instant;
import java.util.List;

public class ModelTrainingResponse {

    private String modelVersion;
    private ModelType modelType;
    private ModelStatus status;
    private Instant trainingStartedAt;
    private Instant trainingCompletedAt;
    private int numberOfSamples;
    private int featureCount;
    private ModelEvaluationDto evaluation;
    private List<FeatureImportanceDto> featureImportances;
    private String message;

    public ModelTrainingResponse() {
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public ModelType getModelType() {
        return modelType;
    }

    public void setModelType(ModelType modelType) {
        this.modelType = modelType;
    }

    public ModelStatus getStatus() {
        return status;
    }

    public void setStatus(ModelStatus status) {
        this.status = status;
    }

    public Instant getTrainingStartedAt() {
        return trainingStartedAt;
    }

    public void setTrainingStartedAt(Instant trainingStartedAt) {
        this.trainingStartedAt = trainingStartedAt;
    }

    public Instant getTrainingCompletedAt() {
        return trainingCompletedAt;
    }

    public void setTrainingCompletedAt(Instant trainingCompletedAt) {
        this.trainingCompletedAt = trainingCompletedAt;
    }

    public int getNumberOfSamples() {
        return numberOfSamples;
    }

    public void setNumberOfSamples(int numberOfSamples) {
        this.numberOfSamples = numberOfSamples;
    }

    public int getFeatureCount() {
        return featureCount;
    }

    public void setFeatureCount(int featureCount) {
        this.featureCount = featureCount;
    }

    public ModelEvaluationDto getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(ModelEvaluationDto evaluation) {
        this.evaluation = evaluation;
    }

    public List<FeatureImportanceDto> getFeatureImportances() {
        return featureImportances;
    }

    public void setFeatureImportances(List<FeatureImportanceDto> featureImportances) {
        this.featureImportances = featureImportances;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
