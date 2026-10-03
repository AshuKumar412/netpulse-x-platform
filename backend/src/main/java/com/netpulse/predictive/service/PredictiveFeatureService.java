package com.netpulse.predictive.service;

import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.predictive.dto.PredictiveFeatureVector;
import com.netpulse.predictive.state.DataQualityStatus;
import com.netpulse.telemetry.entity.TelemetryRecord;
import com.netpulse.telemetry.repository.TelemetryRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class PredictiveFeatureService {

    private final TelemetryRecordRepository telemetryRepository;
    private final HeartbeatService heartbeatService;

    public PredictiveFeatureService(TelemetryRecordRepository telemetryRepository,
                                    HeartbeatService heartbeatService) {
        this.telemetryRepository = telemetryRepository;
        this.heartbeatService = heartbeatService;
    }

    /**
     * Extracts feature vector for a specific node based on its latest telemetry and immediate preceding records.
     */
    public PredictiveFeatureVector extractFeaturesForNode(String nodeId) {
        List<TelemetryRecord> recent = telemetryRepository.findByNodeIdOrderByTimestampDesc(nodeId, PageRequest.of(0, 5));
        if (recent.isEmpty()) {
            return null;
        }

        return buildFeatureVectorFromSequence(nodeId, recent);
    }

    /**
     * Constructs a normalized feature vector from a sequence of telemetry records sorted newest-to-oldest.
     */
    public PredictiveFeatureVector buildFeatureVectorFromSequence(String nodeId, List<TelemetryRecord> seqDesc) {
        if (seqDesc == null || seqDesc.isEmpty()) {
            return null;
        }

        TelemetryRecord current = seqDesc.get(0);
        PredictiveFeatureVector vector = new PredictiveFeatureVector();
        vector.setNodeId(nodeId);
        vector.setTimestamp(current.getTimestamp());

        // Base telemetry metrics
        vector.setCpuUsage(current.getCpuUsage());
        vector.setMemoryUsage(current.getMemoryUsage());
        vector.setLatency(current.getLatency());
        vector.setPacketLoss(current.getPacketLoss());
        vector.setActiveConnections(current.getActiveConnections());
        vector.setErrorRate(current.getErrorRate() != null ? current.getErrorRate() : 0.0);

        // 1st Derivatives (Rate of Change Δ/Δt per second)
        if (seqDesc.size() > 1) {
            TelemetryRecord prev = seqDesc.get(1);
            long elapsedSeconds = Math.max(1, Duration.between(prev.getTimestamp(), current.getTimestamp()).toSeconds());

            vector.setCpuRateOfChange((current.getCpuUsage() - prev.getCpuUsage()) / elapsedSeconds);
            vector.setMemoryRateOfChange((current.getMemoryUsage() - prev.getMemoryUsage()) / elapsedSeconds);
            vector.setLatencyRateOfChange((current.getLatency() - prev.getLatency()) / elapsedSeconds);
            vector.setPacketLossRateOfChange((current.getPacketLoss() - prev.getPacketLoss()) / elapsedSeconds);
            vector.setConnectionGrowthRate(((double) (current.getActiveConnections() - prev.getActiveConnections())) / elapsedSeconds);
        } else {
            vector.setCpuRateOfChange(0.0);
            vector.setMemoryRateOfChange(0.0);
            vector.setLatencyRateOfChange(0.0);
            vector.setPacketLossRateOfChange(0.0);
            vector.setConnectionGrowthRate(0.0);
        }

        // Rolling Averages (across up to 5 historical points)
        double sumCpu = 0.0, sumMem = 0.0, sumLat = 0.0, sumLoss = 0.0, sumConn = 0.0;
        int count = seqDesc.size();
        for (TelemetryRecord r : seqDesc) {
            sumCpu += r.getCpuUsage();
            sumMem += r.getMemoryUsage();
            sumLat += r.getLatency();
            sumLoss += r.getPacketLoss();
            sumConn += r.getActiveConnections();
        }

        vector.setRollingAvgCpu(sumCpu / count);
        vector.setRollingAvgMemory(sumMem / count);
        vector.setRollingAvgLatency(sumLat / count);
        vector.setRollingAvgPacketLoss(sumLoss / count);
        vector.setRollingAvgConnections(sumConn / count);

        // Heartbeat age and base health score
        HeartbeatStatusResponse hb = heartbeatService.getHeartbeatStatus(nodeId);
        if (hb != null) {
            vector.setHeartbeatAgeSeconds(hb.getAgeSeconds() != null ? hb.getAgeSeconds() : 0L);
            vector.setHealthScore(hb.getLiveness() == com.netpulse.heartbeat.entity.LivenessStatus.ALIVE ? 100.0 : 0.0);
        } else {
            vector.setHeartbeatAgeSeconds(0L);
            vector.setHealthScore(100.0);
        }

        return vector;
    }

    /**
     * Assesses data quality for a node based on recent telemetry points count and continuity.
     */
    public DataQualityStatus evaluateNodeDataQuality(String nodeId) {
        List<TelemetryRecord> recent = telemetryRepository.findByNodeIdOrderByTimestampDesc(nodeId, PageRequest.of(0, 5));
        if (recent.isEmpty()) {
            return DataQualityStatus.INSUFFICIENT;
        }
        if (recent.size() < 3) {
            return DataQualityStatus.LIMITED;
        }
        return DataQualityStatus.SUFFICIENT;
    }
}
