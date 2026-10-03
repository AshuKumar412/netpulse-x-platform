package com.netpulse.predictive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.predictive.dto.*;
import com.netpulse.predictive.entity.ModelMetadataEntity;
import com.netpulse.predictive.entity.PredictionRecordEntity;
import com.netpulse.predictive.model.PredictiveModel;
import com.netpulse.predictive.repository.PredictionRecordRepository;
import com.netpulse.predictive.state.DataQualityStatus;
import com.netpulse.predictive.state.PredictedCongestionState;
import com.netpulse.telemetry.entity.TelemetryRecord;
import com.netpulse.telemetry.repository.TelemetryRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PredictiveFeatureService featureService;
    private final ModelTrainingService trainingService;
    private final PredictionExplainabilityService explainabilityService;
    private final PredictionRecordRepository predictionRepository;
    private final NodeRepository nodeRepository;
    private final TelemetryRecordRepository telemetryRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public PredictionService(PredictiveFeatureService featureService,
                             ModelTrainingService trainingService,
                             PredictionExplainabilityService explainabilityService,
                             PredictionRecordRepository predictionRepository,
                             NodeRepository nodeRepository,
                             TelemetryRecordRepository telemetryRepository,
                             SimpMessagingTemplate messagingTemplate) {
        this.featureService = featureService;
        this.trainingService = trainingService;
        this.explainabilityService = explainabilityService;
        this.predictionRepository = predictionRepository;
        this.nodeRepository = nodeRepository;
        this.telemetryRepository = telemetryRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Executes predictive congestion analysis for a single node.
     */
    @Transactional
    public PredictionResponse predictForNode(String nodeId) {
        PredictiveModel model = trainingService.getActiveModel();
        ModelMetadataEntity metadata = trainingService.getActiveMetadata();

        Instant now = Instant.now();
        String predictionId = "PRED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        if (model == null || metadata == null) {
            PredictionResponse resp = new PredictionResponse();
            resp.setPredictionId(predictionId);
            resp.setNodeId(nodeId);
            resp.setTimestamp(now);
            resp.setModelVersion("NONE");
            resp.setPredictedState(PredictedCongestionState.MODEL_UNAVAILABLE);
            resp.setConfidence(0.0);
            resp.setPredictionHorizonMinutes(5);
            resp.setDataQuality(DataQualityStatus.INSUFFICIENT);
            resp.setExplanation("Predictive model unavailable. Train a model from the Intelligence Console first.");
            return resp;
        }

        PredictiveFeatureVector vector = featureService.extractFeaturesForNode(nodeId);
        DataQualityStatus quality = featureService.evaluateNodeDataQuality(nodeId);

        if (vector == null || quality == DataQualityStatus.INSUFFICIENT) {
            PredictionResponse resp = new PredictionResponse();
            resp.setPredictionId(predictionId);
            resp.setNodeId(nodeId);
            resp.setTimestamp(now);
            resp.setModelVersion(metadata.getModelVersion());
            resp.setPredictedState(PredictedCongestionState.INSUFFICIENT_DATA);
            resp.setConfidence(0.0);
            resp.setPredictionHorizonMinutes(5);
            resp.setDataQuality(DataQualityStatus.INSUFFICIENT);
            resp.setExplanation("Insufficient historical telemetry points for node " + nodeId + " to generate reliable forecast.");
            return resp;
        }

        double[] featureArr = vector.toFeatureArray();
        PredictedCongestionState state = model.predict(featureArr);
        double[] probs = model.predictProbabilities(featureArr);

        // Confidence is the probability of the predicted class
        int stateIdx = mapStateToIndex(state);
        double confidence = (stateIdx >= 0 && stateIdx < probs.length) ? probs[stateIdx] : 0.50;
        confidence = Math.round(confidence * 1000.0) / 1000.0;

        Map<String, Double> contributions = model.explainPrediction(featureArr);
        String explanation = explainabilityService.generateExplanation(state, vector, confidence, contributions);

        // Persist prediction record
        PredictionRecordEntity entity = new PredictionRecordEntity();
        entity.setPredictionId(predictionId);
        entity.setNodeId(nodeId);
        entity.setTimestamp(now);
        entity.setModelVersion(metadata.getModelVersion());
        entity.setPredictedState(state);
        entity.setConfidence(confidence);
        entity.setPredictionHorizonMinutes(5);
        entity.setDataQuality(quality);

        entity.setCpuUsage(vector.getCpuUsage());
        entity.setMemoryUsage(vector.getMemoryUsage());
        entity.setLatency(vector.getLatency());
        entity.setPacketLoss(vector.getPacketLoss());
        entity.setActiveConnections(vector.getActiveConnections());

        entity.setCpuRateOfChange(vector.getCpuRateOfChange());
        entity.setMemoryRateOfChange(vector.getMemoryRateOfChange());
        entity.setLatencyRateOfChange(vector.getLatencyRateOfChange());
        entity.setPacketLossRateOfChange(vector.getPacketLossRateOfChange());

        entity.setRollingAvgCpu(vector.getRollingAvgCpu());
        entity.setRollingAvgLatency(vector.getRollingAvgLatency());
        entity.setRollingAvgConnections(vector.getRollingAvgConnections());

        entity.setExplanation(explanation);

        try {
            entity.setFeatureContributionsJson(MAPPER.writeValueAsString(contributions));
        } catch (Exception e) {
            log.warn("Failed to serialize feature contributions", e);
        }

        predictionRepository.save(entity);

        PredictionResponse response = mapToResponse(entity, vector, contributions);

        // Broadcast to WebSocket
        try {
            messagingTemplate.convertAndSend("/topic/predictions", response);
        } catch (Exception e) {
            log.debug("WebSocket broadcast skipped: {}", e.getMessage());
        }

        return response;
    }

    /**
     * Executes predictive analysis across all registered nodes in the topology.
     */
    @Transactional
    public List<PredictionResponse> runAllPredictions() {
        List<NetworkNode> nodes = nodeRepository.findAll();
        List<PredictionResponse> list = new ArrayList<>();
        for (NetworkNode n : nodes) {
            list.add(predictForNode(n.getNodeId()));
        }
        return list;
    }

    /**
     * Retrieves the overall prediction summary across all network nodes.
     */
    public PredictionSummaryResponse getSummary() {
        PredictionSummaryResponse summary = new PredictionSummaryResponse();
        List<NetworkNode> nodes = nodeRepository.findAll();
        summary.setTotalNodesMonitored(nodes.size());

        ModelMetadataEntity activeMetadata = trainingService.getActiveMetadata();
        if (activeMetadata != null) {
            summary.setActiveModelVersion(activeMetadata.getModelVersion());
            summary.setActiveModelType(activeMetadata.getModelType().name());
            summary.setActiveModelAccuracy(activeMetadata.getAccuracy());
        } else {
            summary.setActiveModelVersion("NONE");
            summary.setActiveModelType("NONE");
            summary.setActiveModelAccuracy(0.0);
        }

        List<PredictionResponse> latestList = new ArrayList<>();
        int normal = 0, warning = 0, likely = 0, congested = 0, insufficient = 0;
        Map<String, Integer> dist = new LinkedHashMap<>();

        for (NetworkNode n : nodes) {
            Optional<PredictionRecordEntity> opt = predictionRepository.findLatestByNodeId(n.getNodeId());
            if (opt.isPresent()) {
                PredictionRecordEntity ent = opt.get();
                PredictiveFeatureVector v = featureService.extractFeaturesForNode(n.getNodeId());
                Map<String, Double> contribs = parseContributions(ent.getFeatureContributionsJson());
                PredictionResponse resp = mapToResponse(ent, v, contribs);
                latestList.add(resp);

                switch (ent.getPredictedState()) {
                    case NORMAL -> normal++;
                    case WARNING -> warning++;
                    case CONGESTION_LIKELY -> likely++;
                    case CONGESTED -> congested++;
                    case INSUFFICIENT_DATA -> insufficient++;
                    default -> normal++;
                }
            } else {
                // If no record saved yet, generate on-the-fly
                PredictionResponse resp = predictForNode(n.getNodeId());
                latestList.add(resp);
                if (resp.getPredictedState() == PredictedCongestionState.NORMAL) normal++;
                else if (resp.getPredictedState() == PredictedCongestionState.WARNING) warning++;
                else if (resp.getPredictedState() == PredictedCongestionState.CONGESTION_LIKELY) likely++;
                else if (resp.getPredictedState() == PredictedCongestionState.CONGESTED) congested++;
                else insufficient++;
            }
        }

        summary.setNormalNodes(normal);
        summary.setWarningNodes(warning);
        summary.setCongestionLikelyNodes(likely);
        summary.setCongestedNodes(congested);
        summary.setInsufficientDataNodes(insufficient);
        summary.setLatestNodePredictions(latestList);
        summary.setLatestPredictionTimestamp(Instant.now());

        dist.put("NORMAL", normal);
        dist.put("WARNING", warning);
        dist.put("CONGESTION_LIKELY", likely);
        dist.put("CONGESTED", congested);
        dist.put("INSUFFICIENT_DATA", insufficient);
        summary.setPredictionDistribution(dist);

        summary.setOverallDataQuality(insufficient > (nodes.size() / 2) ? DataQualityStatus.LIMITED : DataQualityStatus.SUFFICIENT);
        return summary;
    }

    /**
     * Generates a 5-minute forecast for a specific node.
     */
    public PredictionForecastDto getForecastForNode(String nodeId) {
        PredictionResponse pred = predictForNode(nodeId);
        PredictionForecastDto dto = new PredictionForecastDto();
        dto.setNodeId(nodeId);
        dto.setHorizonMinutes(5);
        dto.setForecastState(pred.getPredictedState());
        dto.setForecastConfidence(pred.getConfidence());

        double cpuTrendPerMin = pred.getCpuTrendPercentPerMin();
        double latTrendPerMin = pred.getLatencyTrendMsPerMin();

        dto.setProjectedCpu(Math.max(0.0, Math.min(100.0, pred.getCurrentCpu() + (cpuTrendPerMin * 5.0))));
        dto.setProjectedLatency(Math.max(1.0, pred.getCurrentLatency() + (latTrendPerMin * 5.0)));

        double riskScore = (pred.getPredictedState() == PredictedCongestionState.CONGESTED) ? 95.0 :
                (pred.getPredictedState() == PredictedCongestionState.CONGESTION_LIKELY) ? 75.0 :
                (pred.getPredictedState() == PredictedCongestionState.WARNING) ? 45.0 : 15.0;
        dto.setRiskScore(riskScore);
        dto.setRationale(pred.getExplanation());
        return dto;
    }

    /**
     * Evaluates past unverified predictions against subsequent real telemetry that arrived after the forecast horizon.
     */
    @Transactional
    public PredictionOutcomeVerificationDto verifyHistoricalPredictions() {
        Instant fiveMinAgo = Instant.now().minus(Duration.ofMinutes(5));
        List<PredictionRecordEntity> unverified = predictionRepository.findByOutcomeVerifiedFalseAndTimestampBefore(fiveMinAgo);

        long tp = 0, tn = 0, fp = 0, fn = 0;

        for (PredictionRecordEntity p : unverified) {
            // Find telemetry 5 minutes after prediction
            List<TelemetryRecord> subsequent = telemetryRepository.findByNodeIdAndTimestampBetweenOrderByTimestampAsc(
                    p.getNodeId(), p.getTimestamp().plus(Duration.ofMinutes(4)), p.getTimestamp().plus(Duration.ofMinutes(7)));

            if (!subsequent.isEmpty()) {
                TelemetryRecord actualRec = subsequent.get(subsequent.size() - 1);
                PredictedCongestionState actualState = mapTelemetryToState(actualRec);

                p.setActualState(actualState);
                p.setOutcomeVerified(true);
                p.setVerificationTimestamp(Instant.now());
                predictionRepository.save(p);

                boolean predictedCongestedOrLikely = (p.getPredictedState() == PredictedCongestionState.CONGESTION_LIKELY || p.getPredictedState() == PredictedCongestionState.CONGESTED);
                boolean actualCongestedOrLikely = (actualState == PredictedCongestionState.CONGESTION_LIKELY || actualState == PredictedCongestionState.CONGESTED);

                if (predictedCongestedOrLikely && actualCongestedOrLikely) tp++;
                else if (!predictedCongestedOrLikely && !actualCongestedOrLikely) tn++;
                else if (predictedCongestedOrLikely && !actualCongestedOrLikely) fp++;
                else fn++;
            }
        }

        long total = tp + tn + fp + fn;
        PredictionOutcomeVerificationDto res = new PredictionOutcomeVerificationDto();
        res.setTotalPredictionsEvaluated(total);
        res.setTruePositiveCount(tp);
        res.setTrueNegativeCount(tn);
        res.setFalsePositiveCount(fp);
        res.setFalseNegativeCount(fn);

        double acc = total > 0 ? (double) (tp + tn) / total : 1.0;
        double prec = (tp + fp) > 0 ? (double) tp / (tp + fp) : 1.0;
        double rec = (tp + fn) > 0 ? (double) tp / (tp + fn) : 1.0;

        res.setVerificationAccuracy(Math.round(acc * 1000.0) / 1000.0);
        res.setEmpiricalPrecision(Math.round(prec * 1000.0) / 1000.0);
        res.setEmpiricalRecall(Math.round(rec * 1000.0) / 1000.0);

        return res;
    }

    public List<PredictionRecordEntity> getRecentHistory() {
        return predictionRepository.findTop100ByOrderByTimestampDesc();
    }

    public List<PredictionRecordEntity> getNodeHistory(String nodeId) {
        return predictionRepository.findByNodeIdOrderByTimestampDesc(nodeId, PageRequest.of(0, 50));
    }

    private PredictionResponse mapToResponse(PredictionRecordEntity ent, PredictiveFeatureVector v, Map<String, Double> contribs) {
        PredictionResponse r = new PredictionResponse();
        r.setPredictionId(ent.getPredictionId());
        r.setNodeId(ent.getNodeId());
        r.setTimestamp(ent.getTimestamp());
        r.setModelVersion(ent.getModelVersion());
        r.setPredictedState(ent.getPredictedState());
        r.setConfidence(ent.getConfidence());
        r.setPredictionHorizonMinutes(ent.getPredictionHorizonMinutes());
        r.setDataQuality(ent.getDataQuality());

        r.setCurrentCpu(ent.getCpuUsage() != null ? ent.getCpuUsage() : (v != null ? v.getCpuUsage() : 0.0));
        r.setCurrentMemory(ent.getMemoryUsage() != null ? ent.getMemoryUsage() : (v != null ? v.getMemoryUsage() : 0.0));
        r.setCurrentLatency(ent.getLatency() != null ? ent.getLatency() : (v != null ? v.getLatency() : 0.0));
        r.setCurrentPacketLoss(ent.getPacketLoss() != null ? ent.getPacketLoss() : (v != null ? v.getPacketLoss() : 0.0));
        r.setCurrentConnections(ent.getActiveConnections() != null ? ent.getActiveConnections() : (v != null ? v.getActiveConnections() : 0));

        double cpuR = ent.getCpuRateOfChange() != null ? ent.getCpuRateOfChange() : (v != null ? v.getCpuRateOfChange() : 0.0);
        double latR = ent.getLatencyRateOfChange() != null ? ent.getLatencyRateOfChange() : (v != null ? v.getLatencyRateOfChange() : 0.0);
        r.setCpuTrendPercentPerMin(Math.round(cpuR * 60.0 * 100.0) / 100.0);
        r.setLatencyTrendMsPerMin(Math.round(latR * 60.0 * 100.0) / 100.0);

        r.setExplanation(ent.getExplanation());
        r.setFeatureContributions(contribs);
        return r;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> parseContributions(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return MAPPER.readValue(json, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private PredictedCongestionState mapTelemetryToState(TelemetryRecord t) {
        if (t.getCpuUsage() >= 85.0 || t.getLatency() >= 200.0 || t.getPacketLoss() >= 5.0) return PredictedCongestionState.CONGESTED;
        if (t.getCpuUsage() >= 75.0 || t.getLatency() >= 120.0 || t.getPacketLoss() >= 3.0) return PredictedCongestionState.CONGESTION_LIKELY;
        if (t.getCpuUsage() >= 60.0 || t.getLatency() >= 60.0 || t.getPacketLoss() >= 1.0) return PredictedCongestionState.WARNING;
        return PredictedCongestionState.NORMAL;
    }

    private int mapStateToIndex(PredictedCongestionState s) {
        return switch (s) {
            case NORMAL -> 0;
            case WARNING -> 1;
            case CONGESTION_LIKELY -> 2;
            case CONGESTED -> 3;
            default -> 0;
        };
    }
}
