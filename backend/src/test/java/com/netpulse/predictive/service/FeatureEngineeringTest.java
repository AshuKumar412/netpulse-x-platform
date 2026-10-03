package com.netpulse.predictive.service;

import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.predictive.dto.PredictiveFeatureVector;
import com.netpulse.predictive.state.DataQualityStatus;
import com.netpulse.telemetry.entity.TelemetryRecord;
import com.netpulse.telemetry.repository.TelemetryRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureEngineeringTest {

    @Mock
    private TelemetryRecordRepository telemetryRepository;

    @Mock
    private HeartbeatService heartbeatService;

    @InjectMocks
    private PredictiveFeatureService featureService;

    @Test
    @DisplayName("Feature extraction should calculate valid 1st derivatives and rolling averages")
    void testFeatureExtractionDerivativesAndAverages() {
        Instant t0 = Instant.now().minus(10, ChronoUnit.SECONDS);
        Instant t1 = Instant.now();

        TelemetryRecord r0 = new TelemetryRecord("NODE-01", t0, 50.0, 40.0, 20.0, 0.0, 50, 0.0);
        TelemetryRecord r1 = new TelemetryRecord("NODE-01", t1, 70.0, 45.0, 40.0, 1.0, 70, 0.0);

        when(telemetryRepository.findByNodeIdOrderByTimestampDesc(eq("NODE-01"), any(PageRequest.class)))
                .thenReturn(List.of(r1, r0));

        when(heartbeatService.getHeartbeatStatus("NODE-01"))
                .thenReturn(new HeartbeatStatusResponse("NODE-01", t1, 0L, LivenessStatus.ALIVE, false));

        PredictiveFeatureVector vector = featureService.extractFeaturesForNode("NODE-01");

        assertNotNull(vector);
        assertEquals("NODE-01", vector.getNodeId());
        assertEquals(70.0, vector.getCpuUsage());

        // CPU rose from 50 to 70 over 10 seconds -> +2.0 %/sec
        assertEquals(2.0, vector.getCpuRateOfChange(), 0.01);

        // Latency rose from 20 to 40 over 10 seconds -> +2.0 ms/sec
        assertEquals(2.0, vector.getLatencyRateOfChange(), 0.01);

        // Rolling average CPU: (70 + 50) / 2 = 60.0
        assertEquals(60.0, vector.getRollingAvgCpu(), 0.01);

        // Feature vector length check
        double[] array = vector.toFeatureArray();
        assertEquals(16, array.length);
    }

    @Test
    @DisplayName("Data quality evaluation should report INSUFFICIENT on empty records")
    void testDataQualityEvaluation() {
        when(telemetryRepository.findByNodeIdOrderByTimestampDesc(eq("NODE-02"), any(PageRequest.class)))
                .thenReturn(List.of());

        DataQualityStatus status = featureService.evaluateNodeDataQuality("NODE-02");
        assertEquals(DataQualityStatus.INSUFFICIENT, status);
    }
}
