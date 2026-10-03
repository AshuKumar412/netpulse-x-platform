package com.netpulse.predictive.dto;

import com.netpulse.predictive.state.DataQualityStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of the historical telemetry extraction and feature engineering pipeline.
 */
public class DatasetExtractionResult {

    private List<PredictiveFeatureVector> features = new ArrayList<>();
    private List<Integer> labels = new ArrayList<>(); // 0: NORMAL, 1: WARNING, 2: CONGESTION_LIKELY, 3: CONGESTED

    private int totalSamples = 0;
    private Instant rangeStart;
    private Instant rangeEnd;
    private DataQualityStatus qualityStatus = DataQualityStatus.SUFFICIENT;
    private String qualityReason = "Dataset window contains sufficient telemetry points.";
    private int classDistributionNormal = 0;
    private int classDistributionWarning = 0;
    private int classDistributionCongestionLikely = 0;
    private int classDistributionCongested = 0;

    public DatasetExtractionResult() {
    }

    public List<PredictiveFeatureVector> getFeatures() {
        return features;
    }

    public void setFeatures(List<PredictiveFeatureVector> features) {
        this.features = features;
    }

    public List<Integer> getLabels() {
        return labels;
    }

    public void setLabels(List<Integer> labels) {
        this.labels = labels;
    }

    public int getTotalSamples() {
        return totalSamples;
    }

    public void setTotalSamples(int totalSamples) {
        this.totalSamples = totalSamples;
    }

    public Instant getRangeStart() {
        return rangeStart;
    }

    public void setRangeStart(Instant rangeStart) {
        this.rangeStart = rangeStart;
    }

    public Instant getRangeEnd() {
        return rangeEnd;
    }

    public void setRangeEnd(Instant rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    public DataQualityStatus getQualityStatus() {
        return qualityStatus;
    }

    public void setQualityStatus(DataQualityStatus qualityStatus) {
        this.qualityStatus = qualityStatus;
    }

    public String getQualityReason() {
        return qualityReason;
    }

    public void setQualityReason(String qualityReason) {
        this.qualityReason = qualityReason;
    }

    public int getClassDistributionNormal() {
        return classDistributionNormal;
    }

    public void setClassDistributionNormal(int classDistributionNormal) {
        this.classDistributionNormal = classDistributionNormal;
    }

    public int getClassDistributionWarning() {
        return classDistributionWarning;
    }

    public void setClassDistributionWarning(int classDistributionWarning) {
        this.classDistributionWarning = classDistributionWarning;
    }

    public int getClassDistributionCongestionLikely() {
        return classDistributionCongestionLikely;
    }

    public void setClassDistributionCongestionLikely(int classDistributionCongestionLikely) {
        this.classDistributionCongestionLikely = classDistributionCongestionLikely;
    }

    public int getClassDistributionCongested() {
        return classDistributionCongested;
    }

    public void setClassDistributionCongested(int classDistributionCongested) {
        this.classDistributionCongested = classDistributionCongested;
    }
}
