package com.netpulse.topology.service;

import com.netpulse.exception.BadRequestException;
import com.netpulse.exception.DuplicateResourceException;
import com.netpulse.exception.ResourceNotFoundException;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.simulation.dto.SimulationStatusResponse;
import com.netpulse.simulation.service.NetworkSimulationService;
import com.netpulse.topology.dto.*;
import com.netpulse.topology.entity.LinkStatus;
import com.netpulse.topology.entity.LinkType;
import com.netpulse.topology.entity.NetworkLink;
import com.netpulse.topology.repository.NetworkLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TopologyServiceTest {

    @Mock
    private NetworkLinkRepository linkRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private NetworkSimulationService simulationService;

    @InjectMocks
    private TopologyService topologyService;

    private NetworkNode nodeA;
    private NetworkNode nodeB;
    private NetworkNode nodeC;
    private NetworkLink linkAB;

    @BeforeEach
    void setUp() {
        nodeA = new NetworkNode("node-a", "Gateway 01", "10.0.0.1", 8080, NodeStatus.HEALTHY, 5000);
        nodeA.setId(1L);

        nodeB = new NetworkNode("node-b", "Edge Router", "10.0.0.2", 8080, NodeStatus.HEALTHY, 4000);
        nodeB.setId(2L);

        nodeC = new NetworkNode("node-c", "Isolated Node", "10.0.0.3", 8080, NodeStatus.HEALTHY, 3000);
        nodeC.setId(3L);

        linkAB = new NetworkLink("link-node-a-node-b", "node-a", "node-b", LinkType.DIRECT, 1000, 1.0, true, LinkStatus.ACTIVE);
        linkAB.setId(10L);
    }

    @Test
    void testCreateLink_Success() {
        CreateLinkRequest request = new CreateLinkRequest("link-node-a-node-b", "node-a", "node-b", LinkType.DIRECT, 1000, 1.0, true, LinkStatus.ACTIVE);

        when(nodeRepository.existsByNodeId("node-a")).thenReturn(true);
        when(nodeRepository.existsByNodeId("node-b")).thenReturn(true);
        when(linkRepository.existsBySourceNodeIdAndTargetNodeId("node-a", "node-b")).thenReturn(false);
        when(linkRepository.existsBySourceNodeIdAndTargetNodeId("node-b", "node-a")).thenReturn(false);
        when(linkRepository.existsByLinkId("link-node-a-node-b")).thenReturn(false);
        when(linkRepository.save(any(NetworkLink.class))).thenReturn(linkAB);
        when(nodeRepository.findAll()).thenReturn(List.of(nodeA, nodeB));
        when(linkRepository.findAll()).thenReturn(List.of(linkAB));
        when(simulationService.getSimulationStatus()).thenReturn(new SimulationStatusResponse("STOPPED", 0, 0, 2, 1, null, null));

        TopologyLinkResponse response = topologyService.createLink(request);

        assertNotNull(response);
        assertEquals("link-node-a-node-b", response.getLinkId());
        assertEquals("node-a", response.getSourceNodeId());
        assertEquals("node-b", response.getTargetNodeId());
        verify(linkRepository).save(any(NetworkLink.class));
    }

    @Test
    void testCreateLink_SelfLink_ThrowsBadRequest() {
        CreateLinkRequest request = new CreateLinkRequest("link-a-a", "node-a", "node-a", LinkType.DIRECT, 1000, 1.0, true, LinkStatus.ACTIVE);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> topologyService.createLink(request));
        assertTrue(ex.getMessage().contains("Self-links are not permitted"));
    }

    @Test
    void testCreateLink_SourceNotFound_ThrowsNotFound() {
        CreateLinkRequest request = new CreateLinkRequest("link-x-b", "node-x", "node-b", LinkType.DIRECT, 1000, 1.0, true, LinkStatus.ACTIVE);
        when(nodeRepository.existsByNodeId("node-x")).thenReturn(false);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> topologyService.createLink(request));
        assertTrue(ex.getMessage().contains("Source node does not exist"));
    }

    @Test
    void testCreateLink_Duplicate_ThrowsConflict() {
        CreateLinkRequest request = new CreateLinkRequest("link-a-b", "node-a", "node-b", LinkType.DIRECT, 1000, 1.0, true, LinkStatus.ACTIVE);
        when(nodeRepository.existsByNodeId("node-a")).thenReturn(true);
        when(nodeRepository.existsByNodeId("node-b")).thenReturn(true);
        when(linkRepository.existsBySourceNodeIdAndTargetNodeId("node-a", "node-b")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> topologyService.createLink(request));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void testGetTopologySnapshot_CalculatesGraphMetricsCorrectly() {
        when(nodeRepository.findAll()).thenReturn(List.of(nodeA, nodeB, nodeC));
        when(linkRepository.findAll()).thenReturn(List.of(linkAB));
        when(simulationService.getSimulationStatus()).thenReturn(new SimulationStatusResponse("STOPPED", 0, 0, 3, 1, null, null));

        TopologyResponse snapshot = topologyService.getTopologySnapshot();

        assertNotNull(snapshot);
        assertEquals(3, snapshot.getNodes().size());
        assertEquals(1, snapshot.getLinks().size());

        TopologySummaryResponse summary = snapshot.getSummary();
        assertNotNull(summary);
        assertEquals(3, summary.getTotalNodes());
        assertEquals(3, summary.getActiveNodes());
        assertEquals(1, summary.getTotalLinks());
        assertEquals(1, summary.getActiveLinks());
        assertEquals(1, summary.getDisconnectedNodes()); // nodeC is disconnected
        assertEquals(2, summary.getConnectedComponents()); // component 1: (nodeA, nodeB), component 2: (nodeC)
        assertEquals(12000, summary.getTotalCapacity()); // 5000 + 4000 + 3000
        assertEquals(1000, summary.getTotalBandwidth());
    }

    @Test
    void testDeleteLink_Success() {
        when(linkRepository.existsById(10L)).thenReturn(true);
        when(nodeRepository.findAll()).thenReturn(List.of(nodeA, nodeB));
        when(linkRepository.findAll()).thenReturn(List.of());
        when(simulationService.getSimulationStatus()).thenReturn(new SimulationStatusResponse("STOPPED", 0, 0, 2, 0, null, null));

        topologyService.deleteLink(10L);

        verify(linkRepository).deleteById(10L);
    }
}
