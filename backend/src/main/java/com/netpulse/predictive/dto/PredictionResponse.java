package com.netpulse.predictive.dto;

import com.netpulse.predictive.state.DataQualityStatus;
import com.netpulse.predictive.state.PredictedCongestionState;
import java.time.Instant;
import java.util.Map;

public class PredictionResponse {

    private String predictionId;
    private String nodeId;
    private Instant timestamp;
    private String modelVersion;
    private PredictedCongestionState predictedState;
    private double confidence;
    private int predictionHorizonMinutes;
    private DataQualityStatus dataQuality;

    private double currentCpu;
    private double currentMemory;
    private double currentLatency;
    private double currentPacketLoss;
    private int currentConnections;

    private double cpuTrendPercentPerMin;
    private double latencyTrendMsPerMin;

    private String explanation;
    private Map<String, Double> featureContributions;

    public PredictionResponse() {
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

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public int getPredictionHorizonMinutes() {
        return predictionHorizonMinutes;
    }

    public void setPredictionHorizonMinutes(int predictionHorizonMinutes) {
        this.predictionHorizonMinutes = predictionHorizonMinutes;
    }

    public DataQualityStatus getDataQuality() {
        return dataQuality;
    }

    public void setDataQuality(DataQualityStatus dataQuality) {
        this.dataQuality = dataQuality;
    }

    public double getCurrentCpu() {
        return currentCpu;
    }

    public void setCurrentCpu(double currentCpu) {
        this.currentCpu = currentCpu;
    }

    public double getCurrentMemory() {
        return currentMemory;
    }

    public void setCurrentMemory(double currentMemory) {
        this.currentMemory = currentMemory;
    }

    public double getCurrentLatency() {
        return currentLatency;
    }

    public void setCurrentLatency(double currentLatency) {
        this.currentLatency = currentLatency;
    }

    public double getCurrentPacketLoss() {
        return currentPacketLoss;
    }

    public void setCurrentPacketLoss(double currentPacketLoss) {
        this.currentPacketLoss = currentPacketLoss;
    }

    public int getCurrentConnections() {
        return currentConnections;
    }

    public void setCurrentConnections(int currentConnections) {
        this.currentConnections = currentConnections;
    }

    public double getCpuTrendPercentPerMin() {
        return cpuTrendPercentPerMin;
    }

    public void setCpuTrendPercentPerMin(double cpuTrendPercentPerMin) {
        this.cpuTrendPercentPerMin = cpuTrendPercentPerMin;
    }

    public double getLatencyTrendMsPerMin() {
        return latencyTrendMsPerMin;
    }

    public void setLatencyTrendMsPerMin(double latencyTrendMsPerMin) {
        this.latencyTrendMsPerMin = latencyTrendMsPerMin;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Map<String, Double> getFeatureContributions() {
        return featureContributions;
    }

    public void setFeatureContributions(Map<String, Double> featureContributions) {
        this.featureContributions = featureContributions;
    }
}
