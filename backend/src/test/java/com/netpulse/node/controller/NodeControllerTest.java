package com.netpulse.node.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.netpulse.exception.GlobalExceptionHandler;
import com.netpulse.node.dto.CreateNodeRequest;
import com.netpulse.node.dto.NodeResponse;
import com.netpulse.node.dto.NodeSummaryResponse;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.service.NodeService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NodeControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private NodeService nodeService;

    @InjectMocks
    private NodeController nodeController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(nodeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Should retrieve all nodes successfully")
    void testGetAllNodes() throws Exception {
        NodeResponse node = new NodeResponse(1L, "node-us-east-1", "Edge Router 01", "192.168.1.10", 8080, NodeStatus.HEALTHY, 5000, Instant.now(), Instant.now());
        when(nodeService.getAllNodes(null, null)).thenReturn(List.of(node));

        mockMvc.perform(get("/api/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].nodeId").value("node-us-east-1"));
    }

    @Test
    @DisplayName("Should retrieve node summary successfully")
    void testGetNodeSummary() throws Exception {
        NodeSummaryResponse summary = new NodeSummaryResponse(3, 2, 1, 0, 0, 0, 15000);
        when(nodeService.getNodeSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/nodes/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalNodes").value(3))
                .andExpect(jsonPath("$.data.healthyNodes").value(2));
    }

    @Test
    @DisplayName("Should create node successfully")
    void testCreateNode() throws Exception {
        CreateNodeRequest request = new CreateNodeRequest("node-us-west-1", "Edge West", "192.168.2.1", 8080, NodeStatus.HEALTHY, 8000);
        NodeResponse response = new NodeResponse(2L, "node-us-west-1", "Edge West", "192.168.2.1", 8080, NodeStatus.HEALTHY, 8000, Instant.now(), Instant.now());

        when(nodeService.createNode(any(CreateNodeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nodeId").value("node-us-west-1"));
    }

    @Test
    @DisplayName("Should delete node successfully")
    void testDeleteNode() throws Exception {
        mockMvc.perform(delete("/api/nodes/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(nodeService).deleteNode(1L);
    }
}
