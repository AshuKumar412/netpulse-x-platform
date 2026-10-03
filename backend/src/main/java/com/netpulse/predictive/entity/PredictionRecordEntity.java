package com.netpulse.predictive.entity;

import com.netpulse.predictive.state.DataQualityStatus;
import com.netpulse.predictive.state.PredictedCongestionState;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "prediction_records", indexes = {
        @Index(name = "idx_pred_node_time", columnList = "nodeId, timestamp DESC"),
        @Index(name = "idx_pred_state", columnList = "predictedState"),
        @Index(name = "idx_pred_model_ver", columnList = "modelVersion"),
        @Index(name = "idx_pred_timestamp", columnList = "timestamp DESC")
})
public class PredictionRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String predictionId;

    @Column(nullable = false, length = 64)
    private String nodeId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 64)
    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PredictedCongestionState predictedState;

    @Column(nullable = false)
    private Double confidence = 0.0;

    @Column(nullable = false)
    private Integer predictionHorizonMinutes = 5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DataQualityStatus dataQuality = DataQualityStatus.SUFFICIENT;

    // Feature snapshot for transparency and explainability
    private Double cpuUsage;
    private Double memoryUsage;
    private Double latency;
    private Double packetLoss;
    private Integer activeConnections;

    private Double cpuRateOfChange;
    private Double memoryRateOfChange;
    private Double latencyRateOfChange;
    private Double packetLossRateOfChange;

    private Double rollingAvgCpu;
    private Double rollingAvgLatency;
    private Double rollingAvgConnections;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(columnDefinition = "TEXT")
    private String featureContributionsJson;

    // Outcome verification (populated when actual telemetry is evaluated later)
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private PredictedCongestionState actualState;

    private Boolean outcomeVerified = false;

    private Instant verificationTimestamp;

    public PredictionRecordEntity() {
    }

    public PredictionRecordEntity(String predictionId, String nodeId, Instant timestamp,
                                  String modelVersion, PredictedCongestionState predictedState,
                                  Double confidence, Integer predictionHorizonMinutes,
                                  DataQualityStatus dataQuality) {
        this.predictionId = predictionId;
        this.nodeId = nodeId;
        this.timestamp = timestamp;
        this.modelVersion = modelVersion;
        this.predictedState = predictedState;
        this.confidence = confidence;
        this.predictionHorizonMinutes = predictionHorizonMinutes;
        this.dataQuality = dataQuality;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPredictionId() {
        return predictionId;
    }

    public void setPredictionId(String predictionId) {
        this.predictionId = predictionId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public PredictedCongestionState getPredictedState() {
        return predictedState;
    }

    public void setPredictedState(PredictedCongestionState predictedState) {
        this.predictedState = predictedState;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Integer getPredictionHorizonMinutes() {
        return predictionHorizonMinutes;
    }

    public void setPredictionHorizonMinutes(Integer predictionHorizonMinutes) {
        this.predictionHorizonMinutes = predictionHorizonMinutes;
    }

    public DataQualityStatus getDataQuality() {
        return dataQuality;
    }

    public void setDataQuality(DataQualityStatus dataQuality) {
        this.dataQuality = dataQuality;
    }

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(Double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public Double getLatency() {
        return latency;
    }

    public void setLatency(Double latency) {
        this.latency = latency;
    }

    public Double getPacketLoss() {
        return packetLoss;
    }

    public void setPacketLoss(Double packetLoss) {
        this.packetLoss = packetLoss;
    }

    public Integer getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(Integer activeConnections) {
        this.activeConnections = activeConnections;
    }

    public Double getCpuRateOfChange() {
        return cpuRateOfChange;
    }

    public void setCpuRateOfChange(Double cpuRateOfChange) {
        this.cpuRateOfChange = cpuRateOfChange;
    }

    public Double getMemoryRateOfChange() {
        return memoryRateOfChange;
    }

    public void setMemoryRateOfChange(Double memoryRateOfChange) {
        this.memoryRateOfChange = memoryRateOfChange;
    }

    public Double getLatencyRateOfChange() {
        return latencyRateOfChange;
    }

    public void setLatencyRateOfChange(Double latencyRateOfChange) {
        this.latencyRateOfChange = latencyRateOfChange;
    }

    public Double getPacketLossRateOfChange() {
        return packetLossRateOfChange;
    }

    public void setPacketLossRateOfChange(Double packetLossRateOfChange) {
        this.packetLossRateOfChange = packetLossRateOfChange;
    }

    public Double getRollingAvgCpu() {
        return rollingAvgCpu;
    }

    public void setRollingAvgCpu(Double rollingAvgCpu) {
        this.rollingAvgCpu = rollingAvgCpu;
    }

    public Double getRollingAvgLatency() {
        return rollingAvgLatency;
    }

    public void setRollingAvgLatency(Double rollingAvgLatency) {
        this.rollingAvgLatency = rollingAvgLatency;
    }

    public Double getRollingAvgConnections() {
        return rollingAvgConnections;
    }

    public void setRollingAvgConnections(Double rollingAvgConnections) {
        this.rollingAvgConnections = rollingAvgConnections;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getFeatureContributionsJson() {
        return featureContributionsJson;
    }

    public void setFeatureContributionsJson(String featureContributionsJson) {
        this.featureContributionsJson = featureContributionsJson;
    }

    public PredictedCongestionState getActualState() {
        return actualState;
    }

    public void setActualState(PredictedCongestionState actualState) {
        this.actualState = actualState;
    }

    public Boolean getOutcomeVerified() {
        return outcomeVerified;
    }

    public void setOutcomeVerified(Boolean outcomeVerified) {
        this.outcomeVerified = outcomeVerified;
    }

    public Instant getVerificationTimestamp() {
        return verificationTimestamp;
    }

    public void setVerificationTimestamp(Instant verificationTimestamp) {
        this.verificationTimestamp = verificationTimestamp;
    }
}
