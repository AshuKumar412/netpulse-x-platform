package com.netpulse.predictive.service;

import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.predictive.dto.PredictionForecastDto;
import com.netpulse.predictive.dto.PredictionResponse;
import com.netpulse.predictive.dto.PredictionSummaryResponse;
import com.netpulse.predictive.dto.PredictiveFeatureVector;
import com.netpulse.predictive.entity.ModelMetadataEntity;
import com.netpulse.predictive.entity.PredictionRecordEntity;
import com.netpulse.predictive.model.MultinomialLogisticRegressionModel;
import com.netpulse.predictive.repository.PredictionRecordRepository;
import com.netpulse.predictive.state.DataQualityStatus;
import com.netpulse.predictive.state.ModelStatus;
import com.netpulse.predictive.state.ModelType;
import com.netpulse.predictive.state.PredictedCongestionState;
import com.netpulse.telemetry.repository.TelemetryRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    @Mock
    private PredictiveFeatureService featureService;

    @Mock
    private ModelTrainingService trainingService;

    @Mock
    private PredictionExplainabilityService explainabilityService;

    @Mock
    private PredictionRecordRepository predictionRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private TelemetryRecordRepository telemetryRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private PredictionService predictionService;

    @BeforeEach
    void setUp() {
        MultinomialLogisticRegressionModel model = new MultinomialLogisticRegressionModel();
        double[][] X = new double[][] {
                { 20.0, 30.0, 15.0, 0.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 30.0, 15.0, 0.0, 10.0 },
                { 92.0, 88.0, 250.0, 8.0, 300.0, 2.0, 5.0, 3.0, 20.0, 1.0, 10.0, 90.0, 85.0, 240.0, 7.0, 290.0 },
        };
        int[] y = new int[] { 0, 3 };
        model.train(X, y, null, java.util.Map.of("learningRate", 0.1, "maxEpochs", 50));

        ModelMetadataEntity meta = new ModelMetadataEntity("MOD-LOG-TEST", ModelType.MULTINOMIAL_LOGISTIC_REGRESSION, ModelStatus.READY, Instant.now(), "TEST");
        meta.setAccuracy(0.95);

        lenient().when(trainingService.getActiveModel()).thenReturn(model);
        lenient().when(trainingService.getActiveMetadata()).thenReturn(meta);

        NetworkNode node = new NetworkNode("NODE-01", "Primary Node", "10.0.0.1", 8080, NodeStatus.HEALTHY, 100);
        lenient().when(nodeRepository.findAll()).thenReturn(List.of(node));
    }

    @Test
    @DisplayName("predictForNode should compute prediction, explainability, and persist record")
    void testPredictForNode() {
        PredictiveFeatureVector vector = new PredictiveFeatureVector();
        vector.setNodeId("NODE-01");
        vector.setCpuUsage(25.0);
        vector.setMemoryUsage(30.0);
        vector.setLatency(15.0);
        vector.setPacketLoss(0.0);
        vector.setActiveConnections(10);
        vector.setRollingAvgCpu(25.0);
        vector.setRollingAvgLatency(15.0);
        vector.setRollingAvgConnections(10.0);

        lenient().when(featureService.extractFeaturesForNode("NODE-01")).thenReturn(vector);
        lenient().when(featureService.evaluateNodeDataQuality("NODE-01")).thenReturn(DataQualityStatus.SUFFICIENT);
        lenient().when(explainabilityService.generateExplanation(any(), any(), any(Double.class), any()))
                .thenReturn("Operating within nominal parameters.");

        PredictionResponse response = predictionService.predictForNode("NODE-01");

        assertNotNull(response);
        assertEquals("NODE-01", response.getNodeId());
        assertEquals(PredictedCongestionState.NORMAL, response.getPredictedState());
        assertTrue(response.getConfidence() > 0.0);
        assertEquals(DataQualityStatus.SUFFICIENT, response.getDataQuality());
    }

    @Test
    @DisplayName("getSummary should return aggregated node distribution and active model metadata")
    void testGetSummary() {
        PredictiveFeatureVector vector = new PredictiveFeatureVector();
        vector.setNodeId("NODE-01");
        vector.setCpuUsage(25.0);
        vector.setRollingAvgCpu(25.0);
        vector.setRollingAvgLatency(15.0);
        vector.setRollingAvgConnections(10.0);

        lenient().when(featureService.extractFeaturesForNode("NODE-01")).thenReturn(vector);
        lenient().when(featureService.evaluateNodeDataQuality("NODE-01")).thenReturn(DataQualityStatus.SUFFICIENT);

        PredictionSummaryResponse summary = predictionService.getSummary();

        assertNotNull(summary);
        assertEquals(1, summary.getTotalNodesMonitored());
        assertEquals("MOD-LOG-TEST", summary.getActiveModelVersion());
        assertEquals(1, summary.getNormalNodes());
    }
}
