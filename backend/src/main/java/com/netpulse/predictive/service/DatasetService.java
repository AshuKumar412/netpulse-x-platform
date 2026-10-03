package com.netpulse.predictive.service;

import com.netpulse.predictive.dto.DatasetExtractionResult;
import com.netpulse.predictive.dto.PredictiveFeatureVector;
import com.netpulse.predictive.state.DataQualityStatus;
import com.netpulse.telemetry.entity.TelemetryRecord;
import com.netpulse.telemetry.repository.TelemetryRecordRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class DatasetService {

    private final TelemetryRecordRepository telemetryRepository;
    private final PredictiveFeatureService featureService;

    public DatasetService(TelemetryRecordRepository telemetryRepository,
                          PredictiveFeatureService featureService) {
        this.telemetryRepository = telemetryRepository;
        this.featureService = featureService;
    }

    /**
     * Extracts training dataset from persisted historical telemetry within the given time window.
     * ZERO synthetic rows are generated. If data is sparse, quality is reported honestly as INSUFFICIENT.
     */
    public DatasetExtractionResult extractDataset(Instant start, Instant end) {
        DatasetExtractionResult result = new DatasetExtractionResult();
        result.setRangeStart(start);
        result.setRangeEnd(end);

        List<TelemetryRecord> records;
        if (start != null && end != null) {
            records = telemetryRepository.findByTimestampBetweenOrderByTimestampAsc(start, end);
        } else {
            records = telemetryRepository.findAllByOrderByTimestampAsc();
        }

        if (records.isEmpty()) {
            result.setQualityStatus(DataQualityStatus.INSUFFICIENT);
            result.setQualityReason("No historical telemetry records found in database.");
            return result;
        }

        // Group records chronologically by nodeId
        Map<String, List<TelemetryRecord>> nodeRecords = new LinkedHashMap<>();
        for (TelemetryRecord r : records) {
            nodeRecords.computeIfAbsent(r.getNodeId(), k -> new ArrayList<>()).add(r);
        }

        List<PredictiveFeatureVector> allVectors = new ArrayList<>();
        List<Integer> allLabels = new ArrayList<>();

        int countNormal = 0;
        int countWarning = 0;
        int countLikely = 0;
        int countCongested = 0;

        for (Map.Entry<String, List<TelemetryRecord>> entry : nodeRecords.entrySet()) {
            String nodeId = entry.getKey();
            List<TelemetryRecord> list = entry.getValue();

            // Needs at least 2 points for derivative extraction
            for (int i = 0; i < list.size(); i++) {
                // Take window up to 5 points backwards
                List<TelemetryRecord> windowDesc = new ArrayList<>();
                for (int w = i; w >= Math.max(0, i - 4); w--) {
                    windowDesc.add(list.get(w));
                }

                PredictiveFeatureVector vector = featureService.buildFeatureVectorFromSequence(nodeId, windowDesc);
                if (vector != null) {
                    int label = computeGroundTruthLabel(vector);
                    allVectors.add(vector);
                    allLabels.add(label);

                    switch (label) {
                        case 0 -> countNormal++;
                        case 1 -> countWarning++;
                        case 2 -> countLikely++;
                        case 3 -> countCongested++;
                    }
                }
            }
        }

        result.setFeatures(allVectors);
        result.setLabels(allLabels);
        result.setTotalSamples(allVectors.size());
        result.setClassDistributionNormal(countNormal);
        result.setClassDistributionWarning(countWarning);
        result.setClassDistributionCongestionLikely(countLikely);
        result.setClassDistributionCongested(countCongested);

        if (allVectors.size() < 10) {
            result.setQualityStatus(DataQualityStatus.INSUFFICIENT);
            result.setQualityReason("Historical dataset contains fewer than 10 samples (Found: " + allVectors.size() + "). Minimum 10 samples required.");
        } else if (allVectors.size() < 30) {
            result.setQualityStatus(DataQualityStatus.LIMITED);
            result.setQualityReason("Historical dataset has limited sample volume (" + allVectors.size() + " samples).");
        } else {
            result.setQualityStatus(DataQualityStatus.SUFFICIENT);
            result.setQualityReason("Historical dataset contains " + allVectors.size() + " validated telemetry feature vectors.");
        }

        return result;
    }

    /**
     * Deterministic operational ground truth label mapping based on Phase 3 / Phase 4 thresholds:
     * 0: NORMAL
     * 1: WARNING
     * 2: CONGESTION_LIKELY
     * 3: CONGESTED
     */
    public int computeGroundTruthLabel(PredictiveFeatureVector v) {
        // Severe congestion condition
        if (v.getCpuUsage() >= 85.0 || v.getLatency() >= 200.0 || v.getPacketLoss() >= 5.0 || v.getMemoryUsage() >= 90.0) {
            return 3; // CONGESTED
        }

        // Imminent congestion trajectory condition
        if ((v.getCpuUsage() >= 75.0 && v.getCpuRateOfChange() > 0.05) ||
                (v.getLatency() >= 120.0 && v.getLatencyRateOfChange() > 2.0) ||
                (v.getPacketLoss() >= 3.0) ||
                (v.getCpuUsage() >= 80.0) ||
                (v.getActiveConnections() >= 250 && v.getConnectionGrowthRate() > 5.0)) {
            return 2; // CONGESTION_LIKELY
        }

        // Warning elevation condition
        if (v.getCpuUsage() >= 60.0 || v.getLatency() >= 60.0 || v.getPacketLoss() >= 1.0 || v.getMemoryUsage() >= 75.0) {
            return 1; // WARNING
        }

        return 0; // NORMAL
    }
}
