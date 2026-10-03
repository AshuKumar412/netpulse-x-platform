package com.netpulse.predictive.state;

/**
 * Lifecycle status of a predictive machine learning model.
 */
public enum ModelStatus {
    TRAINING,
    READY,
    FAILED,
    RETIRED
}
