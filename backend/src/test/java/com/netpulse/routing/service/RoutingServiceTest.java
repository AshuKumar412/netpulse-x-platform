package com.netpulse.routing.service;

import com.netpulse.health.entity.HealthClassification;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.routing.dto.CandidateEvaluationDto;
import com.netpulse.routing.dto.RoutingConfigResponse;
import com.netpulse.routing.dto.RoutingDecisionResponse;
import com.netpulse.routing.dto.RoutingRequest;
import com.netpulse.routing.entity.RoutingDecisionRecord;
import com.netpulse.routing.entity.RoutingStrategyType;
import com.netpulse.routing.repository.RoutingDecisionRepository;
import com.netpulse.routing.strategy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoutingServiceTest {

    @Mock
    private RoutingCandidateService candidateService;

    @Mock
    private RoutingDecisionRepository decisionRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private com.netpulse.failover.service.TrafficRecoveryService trafficRecoveryService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private RoutingService routingService;

    @BeforeEach
    void setUp() {
        List<RoutingStrategy> strategies = List.of(
                new RoundRobinRoutingStrategy(),
                new LeastConnectionsRoutingStrategy(),
                new LeastLoadRoutingStrategy(),
                new LatencyAwareRoutingStrategy(),
                new WeightedRoutingStrategy(),
                new AdaptiveRoutingStrategy()
        );

        routingService = new RoutingService(
                strategies,
                candidateService,
                decisionRepository,
                nodeRepository,
                trafficRecoveryService,
                messagingTemplate,
                null,
                "ADAPTIVE"
        );
    }

    @Test
    @DisplayName("Should execute routing decision and persist record")
    void testExecuteAndRecordRouting() {
        CandidateEvaluationDto c1 = new CandidateEvaluationDto("node-1", "Node 1", 2000, HealthClassification.HEALTHY,
                LivenessStatus.ALIVE, 20.0, 30.0, 15.0, 0.0, 10, 0.0, true, "OK", null);
        when(candidateService.evaluateAllCandidates()).thenReturn(List.of(c1));

        RoutingDecisionResponse resp = routingService.executeAndRecordRouting(new RoutingRequest(RoutingStrategyType.ADAPTIVE));

        assertNotNull(resp);
        assertEquals("SUCCESS", resp.getStatus());
        assertEquals("node-1", resp.getSelectedNodeId());
        verify(decisionRepository).save(any(RoutingDecisionRecord.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/routing"), any(RoutingDecisionResponse.class));
    }

    @Test
    @DisplayName("Should allow updating active routing strategy")
    void testUpdateActiveStrategy() {
        assertEquals(RoutingStrategyType.ADAPTIVE, routingService.getActiveStrategy());

        routingService.setActiveStrategy(RoutingStrategyType.LEAST_CONNECTIONS);
        assertEquals(RoutingStrategyType.LEAST_CONNECTIONS, routingService.getActiveStrategy());
    }

    @Test
    @DisplayName("Should retrieve routing configuration properties")
    void testGetConfig() {
        when(candidateService.isAllowWarning()).thenReturn(true);
        when(candidateService.isAllowDegraded()).thenReturn(false);
        when(candidateService.isAllowSuspected()).thenReturn(false);

        RoutingConfigResponse config = routingService.getConfig();

        assertNotNull(config);
        assertEquals(RoutingStrategyType.ADAPTIVE, config.getDefaultStrategy());
        assertTrue(config.getAllowWarning());
        assertFalse(config.getAllowDegraded());
        assertNotNull(config.getWeights());
    }
}
