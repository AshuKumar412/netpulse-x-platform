package com.netpulse.predictive.service;

import com.netpulse.predictive.entity.PredictionRecordEntity;
import com.netpulse.predictive.repository.PredictionRecordRepository;
import com.netpulse.predictive.state.PredictedCongestionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PredictiveFailoverIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(PredictiveFailoverIntegrationService.class);

    private final PredictionRecordRepository predictionRepository;
    private final PredictiveRoutingIntegrationService configService;

    public PredictiveFailoverIntegrationService(PredictionRecordRepository predictionRepository,
                                               PredictiveRoutingIntegrationService configService) {
        this.predictionRepository = predictionRepository;
        this.configService = configService;
    }

    /**
     * Checks if a node has an active predictive failover early warning.
     * Does NOT trigger automatic premature failovers by default; provides early advisory alerts.
     */
    public boolean hasEarlyWarning(String nodeId) {
        if (!configService.getConfig().isPredictiveFailoverWarningEnabled()) {
            return false;
        }

        Optional<PredictionRecordEntity> latest = predictionRepository.findLatestByNodeId(nodeId);
        if (latest.isEmpty()) {
            return false;
        }

        PredictedCongestionState state = latest.get().getPredictedState();
        return state == PredictedCongestionState.CONGESTION_LIKELY || state == PredictedCongestionState.CONGESTED;
    }
}
