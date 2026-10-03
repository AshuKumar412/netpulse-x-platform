package com.netpulse.simulation.controller;

import com.netpulse.exception.GlobalExceptionHandler;
import com.netpulse.simulation.dto.SimulationStatusResponse;
import com.netpulse.simulation.service.NetworkSimulationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class SimulationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NetworkSimulationService simulationService;

    @InjectMocks
    private SimulationController simulationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(simulationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetStatus_Success() throws Exception {
        SimulationStatusResponse response = new SimulationStatusResponse("STOPPED", 0, 0, 0, 0, null, null);
        when(simulationService.getSimulationStatus()).thenReturn(response);

        mockMvc.perform(get("/api/simulation/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("STOPPED"));
    }

    @Test
    void testStartSimulation_Success() throws Exception {
        SimulationStatusResponse response = new SimulationStatusResponse("RUNNING", 1, 1, 2, 1, Instant.now(), Instant.now());
        when(simulationService.startSimulation()).thenReturn(response);

        mockMvc.perform(post("/api/simulation/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    void testStopSimulation_Success() throws Exception {
        SimulationStatusResponse response = new SimulationStatusResponse("STOPPED", 0, 0, 2, 1, null, null);
        when(simulationService.stopSimulation()).thenReturn(response);

        mockMvc.perform(post("/api/simulation/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("STOPPED"));
    }
}
