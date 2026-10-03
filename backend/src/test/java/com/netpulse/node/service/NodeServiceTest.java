package com.netpulse.node.service;

import com.netpulse.exception.DuplicateResourceException;
import com.netpulse.exception.ResourceNotFoundException;
import com.netpulse.node.dto.CreateNodeRequest;
import com.netpulse.node.dto.NodeResponse;
import com.netpulse.node.dto.NodeSummaryResponse;
import com.netpulse.node.dto.UpdateNodeRequest;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NodeServiceTest {

    @Mock
    private NodeRepository nodeRepository;

    @InjectMocks
    private NodeService nodeService;

    private NetworkNode sampleNode;
    private CreateNodeRequest createRequest;

    @BeforeEach
    void setUp() {
        sampleNode = new NetworkNode("node-us-east-1", "Edge Router 01", "192.168.1.10", 8080, NodeStatus.HEALTHY, 5000);
        sampleNode.setId(1L);

        createRequest = new CreateNodeRequest("node-us-east-1", "Edge Router 01", "192.168.1.10", 8080, NodeStatus.HEALTHY, 5000);
    }

    @Test
    @DisplayName("Should successfully create a new network node")
    void testCreateNodeSuccess() {
        when(nodeRepository.existsByNodeId("node-us-east-1")).thenReturn(false);
        when(nodeRepository.save(any(NetworkNode.class))).thenReturn(sampleNode);

        NodeResponse response = nodeService.createNode(createRequest);

        assertNotNull(response);
        assertEquals("node-us-east-1", response.getNodeId());
        assertEquals("Edge Router 01", response.getName());
        assertEquals("192.168.1.10", response.getHost());
        assertEquals(8080, response.getPort());
        assertEquals(NodeStatus.HEALTHY, response.getStatus());
        assertEquals(5000, response.getCapacity());

        verify(nodeRepository).save(any(NetworkNode.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when nodeId already exists")
    void testCreateNodeDuplicateThrowsException() {
        when(nodeRepository.existsByNodeId("node-us-east-1")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> nodeService.createNode(createRequest));
        verify(nodeRepository, never()).save(any(NetworkNode.class));
    }

    @Test
    @DisplayName("Should retrieve node by database ID")
    void testGetNodeByIdSuccess() {
        when(nodeRepository.findById(1L)).thenReturn(Optional.of(sampleNode));

        NodeResponse response = nodeService.getNodeById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("node-us-east-1", response.getNodeId());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when node ID does not exist")
    void testGetNodeByIdNotFound() {
        when(nodeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> nodeService.getNodeById(99L));
    }

    @Test
    @DisplayName("Should update existing node configuration")
    void testUpdateNodeSuccess() {
        when(nodeRepository.findById(1L)).thenReturn(Optional.of(sampleNode));
        when(nodeRepository.save(any(NetworkNode.class))).thenReturn(sampleNode);

        UpdateNodeRequest updateRequest = new UpdateNodeRequest("Edge Core Updated", "192.168.1.20", 9000, NodeStatus.WARNING, 10000);

        NodeResponse response = nodeService.updateNode(1L, updateRequest);

        assertNotNull(response);
        verify(nodeRepository).save(sampleNode);
    }

    @Test
    @DisplayName("Should delete node by ID successfully")
    void testDeleteNodeSuccess() {
        when(nodeRepository.findById(1L)).thenReturn(Optional.of(sampleNode));

        nodeService.deleteNode(1L);

        verify(nodeRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent node")
    void testDeleteNodeNotFoundThrowsException() {
        when(nodeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> nodeService.deleteNode(99L));
        verify(nodeRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should calculate real summary metrics accurately")
    void testGetNodeSummary() {
        NetworkNode node1 = new NetworkNode("n1", "Node 1", "10.0.0.1", 8080, NodeStatus.HEALTHY, 2000);
        NetworkNode node2 = new NetworkNode("n2", "Node 2", "10.0.0.2", 8080, NodeStatus.WARNING, 3000);
        NetworkNode node3 = new NetworkNode("n3", "Node 3", "10.0.0.3", 8080, NodeStatus.FAILED, 1000);

        when(nodeRepository.findAll()).thenReturn(List.of(node1, node2, node3));

        NodeSummaryResponse summary = nodeService.getNodeSummary();

        assertNotNull(summary);
        assertEquals(3, summary.getTotalNodes());
        assertEquals(1, summary.getHealthyNodes());
        assertEquals(1, summary.getWarningNodes());
        assertEquals(1, summary.getFailedNodes());
        assertEquals(0, summary.getCongestedNodes());
        assertEquals(6000, summary.getTotalCapacity());
    }
}
