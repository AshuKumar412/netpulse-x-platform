package com.netpulse.predictive.dto;

import com.netpulse.predictive.state.DataQualityStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class PredictionSummaryResponse {

    private int totalNodesMonitored;
    private int normalNodes;
    private int warningNodes;
    private int congestionLikelyNodes;
    private int congestedNodes;
    private int insufficientDataNodes;

    private String activeModelVersion;
    private String activeModelType;
    private Double activeModelAccuracy;
    private DataQualityStatus overallDataQuality;

    private Instant latestPredictionTimestamp;
    private List<PredictionResponse> latestNodePredictions;
    private Map<String, Integer> predictionDistribution;

    public PredictionSummaryResponse() {
    }

    public int getTotalNodesMonitored() {
        return totalNodesMonitored;
    }

    public void setTotalNodesMonitored(int totalNodesMonitored) {
        this.totalNodesMonitored = totalNodesMonitored;
    }

    public int getNormalNodes() {
        return normalNodes;
    }

    public void setNormalNodes(int normalNodes) {
        this.normalNodes = normalNodes;
    }

    public int getWarningNodes() {
        return warningNodes;
    }

    public void setWarningNodes(int warningNodes) {
        this.warningNodes = warningNodes;
    }

    public int getCongestionLikelyNodes() {
        return congestionLikelyNodes;
    }

    public void setCongestionLikelyNodes(int congestionLikelyNodes) {
        this.congestionLikelyNodes = congestionLikelyNodes;
    }

    public int getCongestedNodes() {
        return congestedNodes;
    }

    public void setCongestedNodes(int congestedNodes) {
        this.congestedNodes = congestedNodes;
    }

    public int getInsufficientDataNodes() {
        return insufficientDataNodes;
    }

    public void setInsufficientDataNodes(int insufficientDataNodes) {
        this.insufficientDataNodes = insufficientDataNodes;
    }

    public String getActiveModelVersion() {
        return activeModelVersion;
    }

    public void setActiveModelVersion(String activeModelVersion) {
        this.activeModelVersion = activeModelVersion;
    }

    public String getActiveModelType() {
        return activeModelType;
    }

    public void setActiveModelType(String activeModelType) {
        this.activeModelType = activeModelType;
    }

    public Double getActiveModelAccuracy() {
        return activeModelAccuracy;
    }

    public void setActiveModelAccuracy(Double activeModelAccuracy) {
        this.activeModelAccuracy = activeModelAccuracy;
    }

    public DataQualityStatus getOverallDataQuality() {
        return overallDataQuality;
    }

    public void setOverallDataQuality(DataQualityStatus overallDataQuality) {
        this.overallDataQuality = overallDataQuality;
    }

    public Instant getLatestPredictionTimestamp() {
        return latestPredictionTimestamp;
    }

    public void setLatestPredictionTimestamp(Instant latestPredictionTimestamp) {
        this.latestPredictionTimestamp = latestPredictionTimestamp;
    }

    public List<PredictionResponse> getLatestNodePredictions() {
        return latestNodePredictions;
    }

    public void setLatestNodePredictions(List<PredictionResponse> latestNodePredictions) {
        this.latestNodePredictions = latestNodePredictions;
    }

    public Map<String, Integer> getPredictionDistribution() {
        return predictionDistribution;
    }

    public void setPredictionDistribution(Map<String, Integer> predictionDistribution) {
        this.predictionDistribution = predictionDistribution;
    }
}
