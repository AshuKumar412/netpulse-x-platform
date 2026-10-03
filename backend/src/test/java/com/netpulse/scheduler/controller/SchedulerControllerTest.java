package com.netpulse.scheduler.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netpulse.scheduler.dto.*;
import com.netpulse.scheduler.service.SchedulerService;
import com.netpulse.scheduler.state.ProcessState;
import com.netpulse.scheduler.state.ProcessType;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import com.netpulse.scheduler.state.SchedulerStatus;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SchedulerControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SchedulerService schedulerService;

    @InjectMocks
    private SchedulerController schedulerController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(schedulerController).build();
    }

    @Test
    @DisplayName("GET /api/scheduler/status should return current status")
    void testGetStatus() throws Exception {
        SchedulerStatusResponse response = new SchedulerStatusResponse();
        response.setStatus(SchedulerStatus.RUNNING);
        response.setAlgorithm(SchedulerAlgorithm.ROUND_ROBIN);
        response.setCurrentTick(42);

        when(schedulerService.getStatus()).thenReturn(response);

        mockMvc.perform(get("/api/scheduler/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.algorithm").value("ROUND_ROBIN"))
                .andExpect(jsonPath("$.currentTick").value(42));
    }

    @Test
    @DisplayName("POST /api/scheduler/start should start simulation")
    void testStart() throws Exception {
        doNothing().when(schedulerService).start();

        mockMvc.perform(post("/api/scheduler/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("CPU Scheduler simulation started"));

        verify(schedulerService, times(1)).start();
    }

    @Test
    @DisplayName("POST /api/scheduler/processes should create process")
    void testCreateProcess() throws Exception {
        CreateProcessRequest request = new CreateProcessRequest("TestApp", "USER", "NODE-001", ProcessType.CPU_BOUND, 3, 20);
        ProcessControlBlockDto dto = new ProcessControlBlockDto();
        dto.setProcessId("PROC-100");
        dto.setProcessName("TestApp");
        dto.setState(ProcessState.READY);

        when(schedulerService.createProcess(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/scheduler/processes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.processId").value("PROC-100"))
                .andExpect(jsonPath("$.processName").value("TestApp"));
    }

    @Test
    @DisplayName("POST /api/scheduler/benchmark should return comparison results")
    void testRunBenchmark() throws Exception {
        AlgorithmComparisonResponse response = new AlgorithmComparisonResponse();
        response.setWorkloadProcessCount(5);
        response.setBestWaitingTimeAlgorithm("SRTF (12.4 ticks)");
        response.setResults(Collections.emptyList());

        when(schedulerService.runBenchmarkComparison(any())).thenReturn(response);

        mockMvc.perform(post("/api/scheduler/benchmark")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workloadProcessCount").value(5))
                .andExpect(jsonPath("$.bestWaitingTimeAlgorithm").value("SRTF (12.4 ticks)"));
    }
}
