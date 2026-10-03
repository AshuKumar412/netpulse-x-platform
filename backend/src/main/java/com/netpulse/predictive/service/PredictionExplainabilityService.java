package com.netpulse.predictive.service;

import com.netpulse.predictive.dto.PredictiveFeatureVector;
import com.netpulse.predictive.state.PredictedCongestionState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PredictionExplainabilityService {

    /**
     * Generates a deterministic, explainable rationale for a prediction based on actual feature values and rates of change.
     */
    public String generateExplanation(PredictedCongestionState state, PredictiveFeatureVector vector,
                                      double confidence, Map<String, Double> featureContributions) {
        if (vector == null) {
            return "Insufficient telemetry feature data available for explanation.";
        }

        List<String> signals = new ArrayList<>();

        // CPU evaluation
        if (vector.getCpuUsage() >= 85.0) {
            signals.add(String.format("Critical CPU saturation at %.1f%%", vector.getCpuUsage()));
        } else if (vector.getCpuUsage() >= 70.0) {
            if (vector.getCpuRateOfChange() > 0.02) {
                signals.add(String.format("Elevated CPU at %.1f%% with upward trajectory (+%.1f%%/sec)",
                        vector.getCpuUsage(), vector.getCpuRateOfChange()));
            } else {
                signals.add(String.format("High steady CPU load at %.1f%%", vector.getCpuUsage()));
            }
        }

        // Latency evaluation
        if (vector.getLatency() >= 200.0) {
            signals.add(String.format("Severe transit latency at %.1f ms", vector.getLatency()));
        } else if (vector.getLatency() >= 80.0) {
            if (vector.getLatencyRateOfChange() > 1.0) {
                signals.add(String.format("Rising round-trip latency at %.1f ms (+%.1f ms/sec)",
                        vector.getLatency(), vector.getLatencyRateOfChange()));
            } else {
                signals.add(String.format("Elevated latency at %.1f ms", vector.getLatency()));
            }
        }

        // Packet loss evaluation
        if (vector.getPacketLoss() >= 5.0) {
            signals.add(String.format("Critical packet loss at %.1f%%", vector.getPacketLoss()));
        } else if (vector.getPacketLoss() >= 1.0) {
            signals.add(String.format("Packet drop rate elevated at %.1f%%", vector.getPacketLoss()));
        }

        // Active connections evaluation
        if (vector.getActiveConnections() >= 200) {
            if (vector.getConnectionGrowthRate() > 2.0) {
                signals.add(String.format("High connection surge at %d active sockets (+%.1f/sec)",
                        vector.getActiveConnections(), vector.getConnectionGrowthRate()));
            } else {
                signals.add(String.format("Heavy socket volume at %d active connections", vector.getActiveConnections()));
            }
        }

        if (signals.isEmpty()) {
            return String.format("Node operating within nominal parameters (CPU: %.1f%%, Latency: %.1f ms, Loss: %.1f%%). Low congestion risk forecast (Confidence: %.0f%%).",
                    vector.getCpuUsage(), vector.getLatency(), vector.getPacketLoss(), confidence * 100.0);
        }

        String joinedSignals = String.join("; ", signals);
        return switch (state) {
            case CONGESTED -> String.format("Immediate congestion observed: %s. Health classification degraded (Confidence: %.0f%%).",
                    joinedSignals, confidence * 100.0);
            case CONGESTION_LIKELY -> String.format("Congestion likely within 5-minute horizon driven by %s (Confidence: %.0f%%).",
                    joinedSignals, confidence * 100.0);
            case WARNING -> String.format("Telemetry approaching operational warning limits: %s (Confidence: %.0f%%).",
                    joinedSignals, confidence * 100.0);
            default -> String.format("Operating nominal with minor signal variations: %s.", joinedSignals);
        };
    }
}
