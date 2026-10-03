package com.netpulse.predictive.service;

import com.netpulse.predictive.dto.PredictiveFeatureVector;
import com.netpulse.predictive.model.PredictiveModel;
import com.netpulse.predictive.state.PredictedCongestionState;
import org.springframework.stereotype.Service;

@Service
public class PredictiveWhatIfIntegrationService {

    private final ModelTrainingService trainingService;

    public PredictiveWhatIfIntegrationService(ModelTrainingService trainingService) {
        this.trainingService = trainingService;
    }

    /**
     * Evaluates the hypothetical predicted congestion state on simulated node metrics within Phase 8 What-If.
     * Operates purely in-memory on isolated cloned models without persisting live prediction records.
     */
    public PredictedCongestionState evaluateHypotheticalPrediction(String nodeId, double cpu, double memory,
                                                                   double latency, double packetLoss, int connections) {
        PredictiveModel model = trainingService.getActiveModel();
        if (model == null) {
            return PredictedCongestionState.MODEL_UNAVAILABLE;
        }

        PredictiveFeatureVector vector = new PredictiveFeatureVector();
        vector.setNodeId(nodeId);
        vector.setCpuUsage(cpu);
        vector.setMemoryUsage(memory);
        vector.setLatency(latency);
        vector.setPacketLoss(packetLoss);
        vector.setActiveConnections(connections);
        vector.setErrorRate(0.0);

        vector.setCpuRateOfChange(0.0);
        vector.setMemoryRateOfChange(0.0);
        vector.setLatencyRateOfChange(0.0);
        vector.setPacketLossRateOfChange(0.0);
        vector.setConnectionGrowthRate(0.0);

        vector.setRollingAvgCpu(cpu);
        vector.setRollingAvgMemory(memory);
        vector.setRollingAvgLatency(latency);
        vector.setRollingAvgPacketLoss(packetLoss);
        vector.setRollingAvgConnections(connections);

        return model.predict(vector.toFeatureArray());
    }
}
