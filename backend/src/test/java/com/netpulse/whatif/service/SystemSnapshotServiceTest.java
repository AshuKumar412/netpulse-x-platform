package com.netpulse.whatif.service;

import com.netpulse.health.dto.NodeHealthResponse;
import com.netpulse.health.entity.HealthClassification;
import com.netpulse.health.service.NodeHealthService;
import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.routing.dto.RoutingStatusResponse;
import com.netpulse.routing.entity.RoutingStrategyType;
import com.netpulse.routing.service.RoutingService;
import com.netpulse.scheduler.dto.SchedulerPolicyConfig;
import com.netpulse.scheduler.dto.SchedulerStatusResponse;
import com.netpulse.scheduler.service.SchedulerService;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import com.netpulse.scheduler.state.SchedulerStatus;
import com.netpulse.telemetry.entity.TelemetryRecord;
import com.netpulse.telemetry.service.TelemetryService;
import com.netpulse.topology.entity.LinkStatus;
import com.netpulse.topology.entity.NetworkLink;
import com.netpulse.topology.repository.NetworkLinkRepository;
import com.netpulse.whatif.dto.SystemSnapshotDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SystemSnapshotServiceTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private NetworkLinkRepository linkRepository;

    @Mock
    private TelemetryService telemetryService;

    @Mock
    private HeartbeatService heartbeatService;

    @Mock
    private NodeHealthService nodeHealthService;

    @Mock
    private RoutingService routingService;

    @Mock
    private SchedulerService schedulerService;

    @InjectMocks
    private SystemSnapshotService snapshotService;

    @BeforeEach
    void setUp() {
        NetworkNode node1 = new NetworkNode("NODE-001", "Primary Node", "10.0.0.1", 8080, NodeStatus.HEALTHY, 100);
        NetworkNode node2 = new NetworkNode("NODE-002", "Secondary Node", "10.0.0.2", 8080, NodeStatus.HEALTHY, 100);
        lenient().when(nodeRepository.findAll()).thenReturn(List.of(node1, node2));

        NetworkLink link = new NetworkLink("LINK-001", "NODE-001", "NODE-002", com.netpulse.topology.entity.LinkType.DIRECT, 1000, 1.0, true, LinkStatus.ACTIVE);
        lenient().when(linkRepository.findAll()).thenReturn(List.of(link));

        TelemetryRecord tel = new TelemetryRecord("NODE-001", Instant.now(), 35.0, 40.0, 25.0, 0.0, 15, 0.0);
        lenient().when(telemetryService.getLatestTelemetry(anyString())).thenReturn(tel);

        HeartbeatStatusResponse hb = new HeartbeatStatusResponse("NODE-001", Instant.now(), 0L, LivenessStatus.ALIVE, false);
        lenient().when(heartbeatService.getHeartbeatStatus(anyString())).thenReturn(hb);

        NodeHealthResponse health = new NodeHealthResponse("NODE-001", HealthClassification.HEALTHY, LivenessStatus.ALIVE, Instant.now(), Instant.now(), "Healthy", 35.0, 40.0, 25.0, 0.0, 0.0);
        lenient().when(nodeHealthService.evaluateNodeHealth(any(), any())).thenReturn(health);

        com.netpulse.routing.dto.RoutingConfigResponse routingConfig = new com.netpulse.routing.dto.RoutingConfigResponse(
                RoutingStrategyType.ADAPTIVE, true, false, false,
                Map.of("cpu", 0.20, "memory", 0.15, "latency", 0.20, "packetLoss", 0.10, "connections", 0.15)
        );
        lenient().when(routingService.getConfig()).thenReturn(routingConfig);

        SchedulerStatusResponse sched = new SchedulerStatusResponse();
        sched.setStatus(SchedulerStatus.RUNNING);
        sched.setAlgorithm(SchedulerAlgorithm.ROUND_ROBIN);
        sched.setCurrentTick(50);
        lenient().when(schedulerService.getStatus()).thenReturn(sched);
        lenient().when(schedulerService.getPolicy()).thenReturn(new SchedulerPolicyConfig(SchedulerAlgorithm.ROUND_ROBIN, 4, 0.5, 4, true, 10, 25, 100));
    }

    @Test
    @DisplayName("Snapshot capture should produce a consistent, complete point-in-time state")
    void testCaptureSnapshot() {
        SystemSnapshotDto snapshot = snapshotService.captureSnapshot();

        assertNotNull(snapshot);
        assertNotNull(snapshot.getSnapshotId());
        assertTrue(snapshot.getSnapshotId().startsWith("SNAP-"));
        assertEquals(2, snapshot.getTotalNodes());
        assertEquals(2, snapshot.getHealthyNodes());
        assertEquals(0, snapshot.getFailedNodes());
        assertEquals(1, snapshot.getLinks().size());
        assertEquals(35.0, snapshot.getAverageCpu());
        assertEquals(RoutingStrategyType.ADAPTIVE, snapshot.getRoutingState().getActiveStrategy());
        assertEquals(SchedulerAlgorithm.ROUND_ROBIN, snapshot.getSchedulerState().getActiveAlgorithm());
    }
}
