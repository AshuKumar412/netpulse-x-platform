package com.netpulse.simulation.service;

import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.simulation.dto.SimulationStatusResponse;
import com.netpulse.topology.entity.LinkStatus;
import com.netpulse.topology.entity.LinkType;
import com.netpulse.topology.entity.NetworkLink;
import com.netpulse.topology.repository.NetworkLinkRepository;
import com.netpulse.topology.service.TopologyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NetworkSimulationServiceTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private NetworkLinkRepository linkRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private TopologyService topologyService;

    @InjectMocks
    private NetworkSimulationService simulationService;

    private NetworkNode node1;
    private NetworkNode node2;
    private NetworkLink link12;

    @BeforeEach
    void setUp() {
        node1 = new NetworkNode("n1", "Node 1", "10.0.0.1", 8080, NodeStatus.HEALTHY, 5000);
        node2 = new NetworkNode("n2", "Node 2", "10.0.0.2", 8080, NodeStatus.HEALTHY, 5000);
        link12 = new NetworkLink("link-n1-n2", "n1", "n2", LinkType.DIRECT, 1000, 1.0, true, LinkStatus.ACTIVE);
    }

    @Test
    void testStartSimulation_Success() {
        when(nodeRepository.count()).thenReturn(2L);
        when(linkRepository.count()).thenReturn(1L);
        when(nodeRepository.findAll()).thenReturn(List.of(node1, node2));
        when(linkRepository.findAll()).thenReturn(List.of(link12));

        SimulationStatusResponse response = simulationService.startSimulation();

        assertEquals("RUNNING", response.getStatus());
        assertEquals(2L, response.getTotalNodes());
        assertEquals(1L, response.getTotalLinks());
        assertNotNull(response.getStartedAt());
    }

    @Test
    void testPauseSimulation_Success() {
        when(nodeRepository.count()).thenReturn(2L);
        when(linkRepository.count()).thenReturn(1L);
        when(nodeRepository.findAll()).thenReturn(List.of(node1, node2));
        when(linkRepository.findAll()).thenReturn(List.of(link12));

        simulationService.startSimulation();
        SimulationStatusResponse response = simulationService.pauseSimulation();

        assertEquals("PAUSED", response.getStatus());
    }

    @Test
    void testStopSimulation_Success() {
        when(nodeRepository.count()).thenReturn(2L);
        when(linkRepository.count()).thenReturn(1L);
        when(nodeRepository.findAll()).thenReturn(List.of(node1, node2));
        when(linkRepository.findAll()).thenReturn(List.of(link12));

        simulationService.startSimulation();
        SimulationStatusResponse response = simulationService.stopSimulation();

        assertEquals("STOPPED", response.getStatus());
        assertEquals(0L, response.getTickCount());
        assertNull(response.getStartedAt());
    }

    @Test
    void testEvaluateTopologyState_DeterministicLinkDegradation() {
        node1.setStatus(NodeStatus.FAILED);
        when(nodeRepository.findAll()).thenReturn(List.of(node1, node2));
        when(linkRepository.findAll()).thenReturn(List.of(link12));

        simulationService.evaluateTopologyState();

        assertEquals(LinkStatus.FAILED, link12.getStatus());
        verify(linkRepository).save(link12);
        verify(topologyService).broadcastTopologyChange();
    }
}
