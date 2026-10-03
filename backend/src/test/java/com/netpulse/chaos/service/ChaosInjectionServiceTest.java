package com.netpulse.chaos.service;

import com.netpulse.chaos.dto.ChaosExperimentDto;
import com.netpulse.chaos.dto.CreateChaosExperimentRequest;
import com.netpulse.chaos.entity.ChaosExperiment;
import com.netpulse.chaos.repository.ChaosExperimentRepository;
import com.netpulse.chaos.state.ChaosExperimentResult;
import com.netpulse.chaos.state.ChaosExperimentStatus;
import com.netpulse.chaos.state.ChaosExperimentType;
import com.netpulse.exception.BadRequestException;
import com.netpulse.exception.ResourceNotFoundException;
import com.netpulse.failover.repository.FailoverEventRepository;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.topology.entity.LinkStatus;
import com.netpulse.topology.entity.LinkType;
import com.netpulse.topology.entity.NetworkLink;
import com.netpulse.topology.repository.NetworkLinkRepository;
import com.netpulse.topology.service.TopologyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChaosInjectionServiceTest {

    @Mock
    private ChaosExperimentRepository experimentRepository;

    @Mock
    private ChaosPolicyService policyService;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private NetworkLinkRepository linkRepository;

    @Mock
    private HeartbeatService heartbeatService;

    @Mock
    private TopologyService topologyService;

    @Mock
    private FailoverEventRepository failoverEventRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ChaosFaultRegistry faultRegistry;
    private ChaosInjectionService chaosService;

    private NetworkNode targetNode;

    @BeforeEach
    void setUp() {
        faultRegistry = new ChaosFaultRegistry();
        chaosService = new ChaosInjectionService(
                experimentRepository,
                policyService,
                faultRegistry,
                nodeRepository,
                linkRepository,
                heartbeatService,
                topologyService,
                failoverEventRepository,
                messagingTemplate
        );

        targetNode = new NetworkNode("node-01", "Core Gateway", "10.0.0.1", 8080, NodeStatus.HEALTHY, 1000);
    }

    @Test
    @DisplayName("Should start CPU_SPIKE experiment and register in-memory fault")
    void testStartCpuSpikeExperiment() {
        when(policyService.isEnabled()).thenReturn(true);
        when(policyService.getMaxConcurrentExperiments()).thenReturn(3);
        when(policyService.getMaxDurationSeconds()).thenReturn(300);
        when(policyService.isNodeProtected("node-01")).thenReturn(false);
        when(nodeRepository.existsByNodeId("node-01")).thenReturn(true);
        when(experimentRepository.countByStatusIn(anyList())).thenReturn(0L);
        when(experimentRepository.save(any(ChaosExperiment.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateChaosExperimentRequest req = new CreateChaosExperimentRequest(
                ChaosExperimentType.CPU_SPIKE, "node-01", 95.0, 60
        );

        ChaosExperimentDto result = chaosService.startExperiment(req, "admin");

        assertNotNull(result);
        assertEquals(ChaosExperimentType.CPU_SPIKE, result.getExperimentType());
        assertEquals("node-01", result.getTargetNodeId());
        assertEquals(95.0, result.getSeverity());
        assertEquals(ChaosExperimentStatus.RUNNING, result.getStatus());

        // Verify fault registered in real-time fault registry
        assertEquals(95.0, faultRegistry.getInjectedCpu("node-01"));
        verify(experimentRepository).save(any(ChaosExperiment.class));
    }

    @Test
    @DisplayName("Should start NODE_FAILURE experiment and suppress heartbeat and mark node FAILED")
    void testStartNodeFailureExperiment() {
        when(policyService.isEnabled()).thenReturn(true);
        when(policyService.getMaxConcurrentExperiments()).thenReturn(3);
        when(policyService.getMaxDurationSeconds()).thenReturn(300);
        when(policyService.isNodeProtected("node-01")).thenReturn(false);
        when(nodeRepository.existsByNodeId("node-01")).thenReturn(true);
        when(nodeRepository.findByNodeId("node-01")).thenReturn(Optional.of(targetNode));
        when(experimentRepository.countByStatusIn(anyList())).thenReturn(0L);
        when(experimentRepository.save(any(ChaosExperiment.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateChaosExperimentRequest req = new CreateChaosExperimentRequest(
                ChaosExperimentType.NODE_FAILURE, "node-01", 100.0, 45
        );

        ChaosExperimentDto result = chaosService.startExperiment(req, "operator");

        assertNotNull(result);
        assertEquals(ChaosExperimentType.NODE_FAILURE, result.getExperimentType());
        assertEquals(NodeStatus.FAILED, targetNode.getStatus());
        verify(heartbeatService).suppressHeartbeat("node-01", true);
        verify(nodeRepository).save(targetNode);
    }

    @Test
    @DisplayName("Should start NETWORK_PARTITION experiment and disable link connectivity")
    void testStartNetworkPartitionExperiment() {
        when(policyService.isEnabled()).thenReturn(true);
        when(policyService.getMaxConcurrentExperiments()).thenReturn(3);
        when(policyService.getMaxDurationSeconds()).thenReturn(300);
        when(policyService.isNodeProtected("node-01")).thenReturn(false);
        when(nodeRepository.existsByNodeId("node-01")).thenReturn(true);
        when(experimentRepository.countByStatusIn(anyList())).thenReturn(0L);

        NetworkLink link = new NetworkLink("link-1", "node-01", "node-02", LinkType.DIRECT, 1000, 1.0, true, LinkStatus.ACTIVE);
        when(linkRepository.findByNodeInvolvement("node-01")).thenReturn(List.of(link));
        when(experimentRepository.save(any(ChaosExperiment.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateChaosExperimentRequest req = new CreateChaosExperimentRequest(
                ChaosExperimentType.NETWORK_PARTITION, "node-01", 100.0, 30
        );

        ChaosExperimentDto result = chaosService.startExperiment(req, "operator");

        assertNotNull(result);
        assertFalse(link.getEnabled());
        assertEquals(LinkStatus.FAILED, link.getStatus());
        verify(linkRepository).saveAll(anyList());
        verify(topologyService).broadcastTopologyChange();
    }

    @Test
    @DisplayName("Should reject experiment when chaos engine is disabled")
    void testChaosDisabledRejectsExperiment() {
        when(policyService.isEnabled()).thenReturn(false);

        CreateChaosExperimentRequest req = new CreateChaosExperimentRequest(
                ChaosExperimentType.CPU_SPIKE, "node-01", 90.0, 60
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> chaosService.startExperiment(req, "operator"));
        assertTrue(ex.getMessage().contains("disabled"));
    }

    @Test
    @DisplayName("Should reject experiment on protected node")
    void testProtectedNodeRejection() {
        when(policyService.isEnabled()).thenReturn(true);
        when(policyService.getMaxConcurrentExperiments()).thenReturn(3);
        when(policyService.getMaxDurationSeconds()).thenReturn(300);
        when(nodeRepository.existsByNodeId("node-01")).thenReturn(true);
        when(policyService.isNodeProtected("node-01")).thenReturn(true);

        CreateChaosExperimentRequest req = new CreateChaosExperimentRequest(
                ChaosExperimentType.HIGH_LATENCY, "node-01", 200.0, 60
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> chaosService.startExperiment(req, "operator"));
        assertTrue(ex.getMessage().contains("protected"));
    }

    @Test
    @DisplayName("Should stop and rollback experiment restoring node health and heartbeat")
    void testStopExperimentAndRollback() {
        ChaosExperiment exp = new ChaosExperiment(
                "EXP-TEST001", ChaosExperimentType.NODE_FAILURE, "node-01", null,
                100.0, 60, ChaosExperimentStatus.RUNNING, Instant.now().minusSeconds(10), "admin"
        );

        when(experimentRepository.findByExperimentId("EXP-TEST001")).thenReturn(Optional.of(exp));
        when(experimentRepository.save(any(ChaosExperiment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(nodeRepository.findByNodeId("node-01")).thenReturn(Optional.of(targetNode));
        targetNode.setStatus(NodeStatus.FAILED);

        ChaosExperimentDto result = chaosService.stopExperiment("EXP-TEST001", "Manual stop");

        assertNotNull(result);
        assertEquals(ChaosExperimentStatus.COMPLETED, result.getStatus());
        assertEquals(NodeStatus.HEALTHY, targetNode.getStatus());
        verify(heartbeatService).suppressHeartbeat("node-01", false);
        verify(heartbeatService).recordHeartbeat("node-01");
        verify(nodeRepository).save(targetNode);
    }
}
