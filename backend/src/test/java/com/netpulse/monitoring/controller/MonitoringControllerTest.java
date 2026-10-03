package com.netpulse.monitoring.controller;

import com.netpulse.monitoring.dto.MonitoringStatusResponse;
import com.netpulse.monitoring.service.MonitoringService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MonitoringControllerTest {

    @Mock
    private MonitoringService monitoringService;

    @InjectMocks
    private MonitoringController monitoringController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(monitoringController).build();
    }

    @Test
    @DisplayName("GET /api/monitoring/status should return monitoring status")
    void testGetMonitoringStatus() throws Exception {
        MonitoringStatusResponse resp = new MonitoringStatusResponse("RUNNING", 100L, 200L, 4, Instant.now(), Instant.now());
        when(monitoringService.getMonitoringStatus()).thenReturn(resp);

        mockMvc.perform(get("/api/monitoring/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.cycleCount").value(100));
    }

    @Test
    @DisplayName("POST /api/monitoring/start should start monitoring")
    void testStartMonitoring() throws Exception {
        MonitoringStatusResponse resp = new MonitoringStatusResponse("RUNNING", 1L, 0L, 4, Instant.now(), Instant.now());
        when(monitoringService.startMonitoring()).thenReturn(resp);

        mockMvc.perform(post("/api/monitoring/start")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    @DisplayName("POST /api/monitoring/stop should stop monitoring")
    void testStopMonitoring() throws Exception {
        MonitoringStatusResponse resp = new MonitoringStatusResponse("STOPPED", 10L, 20L, 4, Instant.now(), Instant.now());
        when(monitoringService.stopMonitoring()).thenReturn(resp);

        mockMvc.perform(post("/api/monitoring/stop")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("STOPPED"));
    }
}
