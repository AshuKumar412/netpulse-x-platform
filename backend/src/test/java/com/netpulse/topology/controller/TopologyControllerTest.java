package com.netpulse.topology.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netpulse.exception.GlobalExceptionHandler;
import com.netpulse.topology.dto.*;
import com.netpulse.topology.entity.LinkStatus;
import com.netpulse.topology.entity.LinkType;
import com.netpulse.topology.service.TopologyService;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class TopologyControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TopologyService topologyService;

    @InjectMocks
    private TopologyController topologyController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(topologyController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetTopology_Success() throws Exception {
        TopologySummaryResponse summary = new TopologySummaryResponse(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0, 0, 0);
        TopologyResponse response = new TopologyResponse(List.of(), List.of(), summary, "STOPPED", Instant.now());

        when(topologyService.getTopologySnapshot()).thenReturn(response);

        mockMvc.perform(get("/api/topology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.simulationStatus").value("STOPPED"));
    }

    @Test
    void testCreateLink_Success() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest("link-1-2", "node-1", "node-2", LinkType.DIRECT, 1000, 1.0, true, LinkStatus.ACTIVE);
        TopologyLinkResponse response = new TopologyLinkResponse(1L, "link-1-2", "node-1", "node-2", LinkType.DIRECT, 1000, 1.0, true, LinkStatus.ACTIVE, Instant.now(), Instant.now());

        when(topologyService.createLink(any(CreateLinkRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/topology/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.linkId").value("link-1-2"));
    }

    @Test
    void testDeleteLink_Success() throws Exception {
        mockMvc.perform(delete("/api/topology/links/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(topologyService).deleteLink(1L);
    }
}
