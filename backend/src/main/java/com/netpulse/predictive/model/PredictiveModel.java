package com.netpulse.predictive.model;

import com.netpulse.predictive.dto.FeatureImportanceDto;
import com.netpulse.predictive.state.PredictedCongestionState;

import java.util.List;
import java.util.Map;

/**
 * Common interface for deterministic, explainable ML classifiers in NetPulse X.
 */
public interface PredictiveModel {

    /**
     * Trains the model on the provided feature matrix X and label vector y.
     */
    void train(double[][] X, int[] y, String[] featureNames, Map<String, Object> hyperparams);

    /**
     * Predicts the congestion state class (0: NORMAL, 1: WARNING, 2: CONGESTION_LIKELY, 3: CONGESTED).
     */
    PredictedCongestionState predict(double[] features);

    /**
     * Computes the output probability distribution over all classes [P(Normal), P(Warning), P(CongestionLikely), P(Congested)].
     */
    double[] predictProbabilities(double[] features);

    /**
     * Computes mathematical feature importances derived directly from the model parameters.
     */
    List<FeatureImportanceDto> getFeatureImportances();

    /**
     * Computes per-feature contributions/attribution for a specific single input sample.
     */
    Map<String, Double> explainPrediction(double[] features);

    /**
     * Serializes model weights / parameters to JSON.
     */
    String serializeToJson();

    /**
     * Deserializes and loads model weights from JSON.
     */
    void deserializeFromJson(String json);
}
