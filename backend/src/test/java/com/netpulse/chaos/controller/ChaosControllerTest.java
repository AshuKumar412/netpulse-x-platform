package com.netpulse.chaos.controller;

import com.netpulse.chaos.dto.ChaosExperimentDto;
import com.netpulse.chaos.dto.ChaosPolicyConfig;
import com.netpulse.chaos.dto.CreateChaosExperimentRequest;
import com.netpulse.chaos.repository.ChaosExperimentRepository;
import com.netpulse.chaos.service.ChaosInjectionService;
import com.netpulse.chaos.service.ChaosPolicyService;
import com.netpulse.chaos.state.ChaosExperimentResult;
import com.netpulse.chaos.state.ChaosExperimentStatus;
import com.netpulse.chaos.state.ChaosExperimentType;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChaosControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChaosInjectionService chaosService;

    @Mock
    private ChaosPolicyService policyService;

    @Mock
    private ChaosExperimentRepository experimentRepository;

    @InjectMocks
    private ChaosController chaosController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chaosController).build();
    }

    @Test
    @DisplayName("GET /api/chaos/status should return platform chaos metrics")
    void testGetStatus() throws Exception {
        ChaosPolicyConfig policy = new ChaosPolicyConfig(true, 300, 3, 30, List.of("node-protected"));
        when(policyService.isEnabled()).thenReturn(true);
        when(policyService.getPolicyConfig()).thenReturn(policy);
        when(experimentRepository.count()).thenReturn(10L);
        when(experimentRepository.countByStatusIn(anyList())).thenReturn(1L);
        when(experimentRepository.countByResult(ChaosExperimentResult.SUCCESS)).thenReturn(8L);
        when(experimentRepository.countByResult(ChaosExperimentResult.FAILED)).thenReturn(1L);
        when(experimentRepository.findByStatus(ChaosExperimentStatus.RUNNING)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/chaos/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.totalExperimentsCount").value(10))
                .andExpect(jsonPath("$.data.successfulCount").value(8));
    }

    @Test
    @DisplayName("POST /api/chaos/experiments should start experiment")
    void testStartExperiment() throws Exception {
        ChaosExperimentDto dto = new ChaosExperimentDto(
                1L, "EXP-12345", ChaosExperimentType.CPU_SPIKE, "node-01", null,
                90.0, 60, ChaosExperimentStatus.RUNNING, Instant.now(), null, "admin",
                false, false, false, null, null, null, null, Instant.now()
        );

        when(chaosService.startExperiment(any(CreateChaosExperimentRequest.class), any())).thenReturn(dto);

        mockMvc.perform(post("/api/chaos/experiments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "experimentType": "CPU_SPIKE",
                                  "targetNodeId": "node-01",
                                  "severity": 90.0,
                                  "durationSeconds": 60
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.experimentId").value("EXP-12345"))
                .andExpect(jsonPath("$.data.experimentType").value("CPU_SPIKE"))
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    @DisplayName("POST /api/chaos/experiments/{id}/stop should stop experiment")
    void testStopExperiment() throws Exception {
        ChaosExperimentDto dto = new ChaosExperimentDto(
                1L, "EXP-12345", ChaosExperimentType.CPU_SPIKE, "node-01", null,
                90.0, 60, ChaosExperimentStatus.COMPLETED, Instant.now().minusSeconds(10), Instant.now(), "admin",
                true, false, true, ChaosExperimentResult.SUCCESS, "Operator stop", "Explained", null, Instant.now()
        );

        when(chaosService.stopExperiment(eq("EXP-12345"), anyString())).thenReturn(dto);

        mockMvc.perform(post("/api/chaos/experiments/EXP-12345/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.experimentId").value("EXP-12345"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/chaos/policies should return policy configuration")
    void testGetPolicies() throws Exception {
        ChaosPolicyConfig policy = new ChaosPolicyConfig(true, 300, 3, 30, List.of("node-protected"));
        when(policyService.getPolicyConfig()).thenReturn(policy);

        mockMvc.perform(get("/api/chaos/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.maxDurationSeconds").value(300))
                .andExpect(jsonPath("$.data.maxConcurrentExperiments").value(3));
    }
}
