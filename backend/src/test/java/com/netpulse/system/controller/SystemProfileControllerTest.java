package com.netpulse.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netpulse.exception.GlobalExceptionHandler;
import com.netpulse.system.dto.*;
import com.netpulse.system.service.SystemDiscoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SystemProfileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SystemDiscoveryService discoveryService;

    @InjectMocks
    private SystemProfileController controller;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/system/profile should return dynamic host profile")
    void testGetSystemProfile() throws Exception {
        LocalSystemIdentityDto identity = new LocalSystemIdentityDto("NPX-A1B2C3", "TestHost", "HIGH_END", 2000L, 16, 8, 16000000000L);
        OsProfileDto os = new OsProfileDto("Windows 11", "22631", "amd64", "10.0", Instant.now(), 3600L);
        CpuProfileDto cpu = new CpuProfileDto("Intel", "Core i7", 8, 16, 25.0, List.of(25.0), 3200000000L, 1.2);
        MemoryProfileDto mem = new MemoryProfileDto(16000000000L, 8000000000L, 8000000000L, 50.0, 0L, 0L);

        HostSystemProfileDto profile = new HostSystemProfileDto(identity, os, cpu, mem, List.of(), List.of(), "25", "Oracle", Instant.now());
        when(discoveryService.discoverSystemProfile()).thenReturn(profile);

        mockMvc.perform(get("/api/system/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identity.safeMachineId").value("NPX-A1B2C3"))
                .andExpect(jsonPath("$.os.osName").value("Windows 11"))
                .andExpect(jsonPath("$.cpu.physicalCores").value(8));
    }

    @Test
    @DisplayName("GET /api/system/metrics should return live metrics payload")
    void testGetLiveMetrics() throws Exception {
        HostLiveMetricsDto metrics = new HostLiveMetricsDto(18.5, List.of(18.5), 8000000000L, 16000000000L, 50.0, 3600L, List.of(), List.of(), 500000000L, 1000000000L, Instant.now());
        when(discoveryService.collectLiveMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/api/system/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpuLoadPercent").value(18.5))
                .andExpect(jsonPath("$.memoryUtilizationPercent").value(50.0));
    }

    @Test
    @DisplayName("GET /api/system/identity should return safe machine identity")
    void testGetSystemIdentity() throws Exception {
        LocalSystemIdentityDto identity = new LocalSystemIdentityDto("NPX-998877", "TestHost", "ENTERPRISE_GRADE", 1000L, 32, 16, 32000000000L);
        when(discoveryService.getLocalSystemIdentity()).thenReturn(identity);

        mockMvc.perform(get("/api/system/identity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.safeMachineId").value("NPX-998877"))
                .andExpect(jsonPath("$.capabilityTier").value("ENTERPRISE_GRADE"));
    }
}
