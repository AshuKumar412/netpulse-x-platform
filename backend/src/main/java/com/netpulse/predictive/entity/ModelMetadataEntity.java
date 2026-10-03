package com.netpulse.predictive.entity;

import com.netpulse.predictive.state.ModelStatus;
import com.netpulse.predictive.state.ModelType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "model_metadata", indexes = {
        @Index(name = "idx_model_version", columnList = "modelVersion", unique = true),
        @Index(name = "idx_model_status", columnList = "status"),
        @Index(name = "idx_model_completed_at", columnList = "trainingCompletedAt DESC")
})
public class ModelMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private ModelType modelType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ModelStatus status;

    @Column(nullable = false)
    private Instant trainingStartedAt;

    private Instant trainingCompletedAt;

    private Instant trainingDataStart;

    private Instant trainingDataEnd;

    @Column(nullable = false)
    private Integer numberOfSamples = 0;

    @Column(nullable = false)
    private Integer featureCount = 0;

    @Column(nullable = false, length = 32)
    private String featureSchemaVersion = "v1";

    @Column(nullable = false, length = 32)
    private String labelPolicyVersion = "v1";

    private Double accuracy;

    private Double precisionScore;

    private Double recallScore;

    private Double f1Score;

    @Column(columnDefinition = "TEXT")
    private String confusionMatrixJson;

    @Column(columnDefinition = "TEXT")
    private String weightsJson;

    @Column(columnDefinition = "TEXT")
    private String hyperparamsJson;

    @Column(length = 64)
    private String createdBy;

    @Column(length = 255)
    private String notes;

    public ModelMetadataEntity() {
    }

    public ModelMetadataEntity(String modelVersion, ModelType modelType, ModelStatus status,
                               Instant trainingStartedAt, String createdBy) {
        this.modelVersion = modelVersion;
        this.modelType = modelType;
        this.status = status;
        this.trainingStartedAt = trainingStartedAt;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Instant getTrainingDataStart() {
        return trainingDataStart;
    }

    public void setTrainingDataStart(Instant trainingDataStart) {
        this.trainingDataStart = trainingDataStart;
    }

    public Instant getTrainingDataEnd() {
        return trainingDataEnd;
    }

    public void setTrainingDataEnd(Instant trainingDataEnd) {
        this.trainingDataEnd = trainingDataEnd;
    }

    public Integer getNumberOfSamples() {
        return numberOfSamples;
    }

    public void setNumberOfSamples(Integer numberOfSamples) {
        this.numberOfSamples = numberOfSamples;
    }

    public Integer getFeatureCount() {
        return featureCount;
    }

    public void setFeatureCount(Integer featureCount) {
        this.featureCount = featureCount;
    }

    public String getFeatureSchemaVersion() {
        return featureSchemaVersion;
    }

    public void setFeatureSchemaVersion(String featureSchemaVersion) {
        this.featureSchemaVersion = featureSchemaVersion;
    }

    public String getLabelPolicyVersion() {
        return labelPolicyVersion;
    }

    public void setLabelPolicyVersion(String labelPolicyVersion) {
        this.labelPolicyVersion = labelPolicyVersion;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public Double getPrecisionScore() {
        return precisionScore;
    }

    public void setPrecisionScore(Double precisionScore) {
        this.precisionScore = precisionScore;
    }

    public Double getRecallScore() {
        return recallScore;
    }

    public void setRecallScore(Double recallScore) {
        this.recallScore = recallScore;
    }

    public Double getF1Score() {
        return f1Score;
    }

    public void setF1Score(Double f1Score) {
        this.f1Score = f1Score;
    }

    public String getConfusionMatrixJson() {
        return confusionMatrixJson;
    }

    public void setConfusionMatrixJson(String confusionMatrixJson) {
        this.confusionMatrixJson = confusionMatrixJson;
    }

    public String getWeightsJson() {
        return weightsJson;
    }

    public void setWeightsJson(String weightsJson) {
        this.weightsJson = weightsJson;
    }

    public String getHyperparamsJson() {
        return hyperparamsJson;
    }

    public void setHyperparamsJson(String hyperparamsJson) {
        this.hyperparamsJson = hyperparamsJson;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
