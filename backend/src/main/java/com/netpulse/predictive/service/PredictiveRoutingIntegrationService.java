package com.netpulse.predictive.service;

import com.netpulse.predictive.dto.PredictiveConfigDto;
import com.netpulse.predictive.entity.PredictionRecordEntity;
import com.netpulse.predictive.repository.PredictionRecordRepository;
import com.netpulse.predictive.state.PredictedCongestionState;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PredictiveRoutingIntegrationService {

    private final PredictionRecordRepository predictionRepository;
    private final PredictiveConfigDto config = new PredictiveConfigDto();

    public PredictiveRoutingIntegrationService(PredictionRecordRepository predictionRepository) {
        this.predictionRepository = predictionRepository;
    }

    public PredictiveConfigDto getConfig() {
        return config;
    }

    public void updateConfig(PredictiveConfigDto newConfig) {
        if (newConfig != null) {
            this.config.setPredictiveRoutingEnabled(newConfig.isPredictiveRoutingEnabled());
            this.config.setPredictiveRoutingPenaltyWeight(newConfig.getPredictiveRoutingPenaltyWeight());
            this.config.setPredictiveFailoverWarningEnabled(newConfig.isPredictiveFailoverWarningEnabled());
            this.config.setPredictionHorizonMinutes(newConfig.getPredictionHorizonMinutes());
            this.config.setWarningThresholdProbability(newConfig.getWarningThresholdProbability());
            this.config.setCongestionLikelyThresholdProbability(newConfig.getCongestionLikelyThresholdProbability());
        }
    }

    /**
     * Calculates the predictive penalty to apply to a routing candidate node score.
     * If predictive routing is disabled, returns 0.0, keeping Phase 4 baseline behavior exact.
     */
    public double calculatePredictivePenalty(String nodeId) {
        if (!config.isPredictiveRoutingEnabled()) {
            return 0.0;
        }

        Optional<PredictionRecordEntity> latest = predictionRepository.findLatestByNodeId(nodeId);
        if (latest.isEmpty()) {
            return 0.0;
        }

        PredictedCongestionState state = latest.get().getPredictedState();
        double weight = config.getPredictiveRoutingPenaltyWeight();

        return switch (state) {
            case CONGESTED -> 40.0 * weight;
            case CONGESTION_LIKELY -> 25.0 * weight;
            case WARNING -> 10.0 * weight;
            default -> 0.0;
        };
    }
}
