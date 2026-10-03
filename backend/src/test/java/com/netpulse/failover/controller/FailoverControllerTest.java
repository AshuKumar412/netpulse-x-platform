package com.netpulse.failover.controller;

import com.netpulse.failover.dto.FailoverPolicyConfig;
import com.netpulse.failover.dto.FailoverStatusResponse;
import com.netpulse.failover.dto.ManualRecoveryResponse;
import com.netpulse.failover.dto.RetryFailoverResponse;
import com.netpulse.failover.entity.FailoverEvent;
import com.netpulse.failover.repository.FailoverEventRepository;
import com.netpulse.failover.service.FailoverOrchestrator;
import com.netpulse.failover.service.FailoverPolicyService;
import com.netpulse.failover.service.NodeRecoveryService;
import com.netpulse.failover.state.FailoverState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FailoverControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FailoverEventRepository eventRepository;

    @Mock
    private FailoverPolicyService policyService;

    @Mock
    private FailoverOrchestrator orchestrator;

    @Mock
    private NodeRecoveryService nodeRecoveryService;

    @InjectMocks
    private FailoverController failoverController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(failoverController).build();
    }

    @Test
    @DisplayName("GET /api/failover/status should return platform status with correct metrics")
    void testGetStatus() throws Exception {
        when(policyService.isEnabled()).thenReturn(true);
        when(orchestrator.getAllNodeFailoverStates()).thenReturn(Collections.emptyMap());
        when(eventRepository.count()).thenReturn(6L);
        when(eventRepository.countBySuccessTrue()).thenReturn(5L);
        when(eventRepository.countBySuccessFalse()).thenReturn(1L);
        when(eventRepository.countByCurrentStateIn(anyList())).thenReturn(0L);
        when(eventRepository.findCompletedSuccessfulEvents()).thenReturn(Collections.emptyList());
        when(eventRepository.findByCurrentStateIn(anyList())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/failover/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.failoverEnabled").value(true))
                .andExpect(jsonPath("$.data.totalFailoverEvents").value(6))
                .andExpect(jsonPath("$.data.recoveredNodesCount").value(5));
    }

    @Test
    @DisplayName("GET /api/failover/policies should return policy configuration")
    void testGetPolicies() throws Exception {
        FailoverPolicyConfig config = new FailoverPolicyConfig(true, 2, true, true, true, 2, 3, 15);
        when(policyService.getPolicyConfig()).thenReturn(config);

        mockMvc.perform(get("/api/failover/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.consecutiveFailuresRequired").value(2))
                .andExpect(jsonPath("$.data.maxAttempts").value(3));
    }

    @Test
    @DisplayName("POST /api/failover/recovery/{nodeId} should initiate node recovery")
    void testInitiateRecovery() throws Exception {
        ManualRecoveryResponse response = new ManualRecoveryResponse(
                "node-1", "FE-001", "RC-001", FailoverState.RECOVERY_IN_PROGRESS, true, "Recovery initiated"
        );

        when(nodeRecoveryService.initiateManualRecovery(eq("node-1"), any(), anyBoolean())).thenReturn(response);

        mockMvc.perform(post("/api/failover/recovery/node-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Test recovery\",\"force\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodeId").value("node-1"))
                .andExpect(jsonPath("$.data.initiated").value(true))
                .andExpect(jsonPath("$.data.state").value("RECOVERY_IN_PROGRESS"));
    }

    @Test
    @DisplayName("POST /api/failover/retry/{eventId} should retry failed recovery")
    void testRetryRecovery() throws Exception {
        RetryFailoverResponse response = new RetryFailoverResponse(
                "FE-001", "RC-001", "node-1", 2, FailoverState.RECOVERY_IN_PROGRESS, true, "Retry started"
        );

        when(nodeRecoveryService.retryRecovery("FE-001")).thenReturn(response);

        mockMvc.perform(post("/api/failover/retry/FE-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventId").value("FE-001"))
                .andExpect(jsonPath("$.data.retried").value(true))
                .andExpect(jsonPath("$.data.attemptNumber").value(2));
    }
}
