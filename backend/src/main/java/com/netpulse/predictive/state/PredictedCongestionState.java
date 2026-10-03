package com.netpulse.predictive.state;

/**
 * Represents the predicted operational congestion state of a network node.
 */
public enum PredictedCongestionState {
    NORMAL,
    WARNING,
    CONGESTION_LIKELY,
    CONGESTED,
    INSUFFICIENT_DATA,
    MODEL_UNAVAILABLE,
    PREDICTION_ERROR
}
