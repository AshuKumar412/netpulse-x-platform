package com.netpulse.predictive.controller;

import com.netpulse.predictive.dto.PredictionForecastDto;
import com.netpulse.predictive.dto.PredictionResponse;
import com.netpulse.predictive.dto.PredictionSummaryResponse;
import com.netpulse.predictive.dto.PredictiveConfigDto;
import com.netpulse.predictive.service.ModelTrainingService;
import com.netpulse.predictive.service.PredictionService;
import com.netpulse.predictive.service.PredictiveRoutingIntegrationService;
import com.netpulse.predictive.state.DataQualityStatus;
import com.netpulse.predictive.state.PredictedCongestionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PredictionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PredictionService predictionService;

    @Mock
    private ModelTrainingService trainingService;

    @Mock
    private PredictiveRoutingIntegrationService routingIntegrationService;

    @InjectMocks
    private PredictionController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/predictions/summary should return prediction distribution")
    void testGetSummary() throws Exception {
        PredictionSummaryResponse summary = new PredictionSummaryResponse();
        summary.setTotalNodesMonitored(4);
        summary.setNormalNodes(3);
        summary.setWarningNodes(1);
        summary.setCongestionLikelyNodes(0);
        summary.setCongestedNodes(0);
        summary.setActiveModelVersion("MOD-LOG-01");
        summary.setOverallDataQuality(DataQualityStatus.SUFFICIENT);

        when(predictionService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/predictions/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalNodesMonitored").value(4))
                .andExpect(jsonPath("$.data.activeModelVersion").value("MOD-LOG-01"));
    }

    @Test
    @DisplayName("GET /api/predictions/nodes/{nodeId} should return node prediction")
    void testGetPredictionForNode() throws Exception {
        PredictionResponse resp = new PredictionResponse();
        resp.setPredictionId("PRED-TEST-1");
        resp.setNodeId("NODE-01");
        resp.setTimestamp(Instant.now());
        resp.setPredictedState(PredictedCongestionState.NORMAL);
        resp.setConfidence(0.92);
        resp.setDataQuality(DataQualityStatus.SUFFICIENT);

        when(predictionService.predictForNode("NODE-01")).thenReturn(resp);

        mockMvc.perform(get("/api/predictions/nodes/NODE-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nodeId").value("NODE-01"))
                .andExpect(jsonPath("$.data.predictedState").value("NORMAL"));
    }

    @Test
    @DisplayName("GET /api/predictions/config should return predictive configuration")
    void testGetConfig() throws Exception {
        PredictiveConfigDto config = new PredictiveConfigDto();
        config.setPredictiveRoutingEnabled(false);
        config.setPredictiveRoutingPenaltyWeight(0.25);

        when(routingIntegrationService.getConfig()).thenReturn(config);

        mockMvc.perform(get("/api/predictions/config")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.predictiveRoutingEnabled").value(false));
    }
}
