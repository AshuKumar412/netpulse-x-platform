package com.netpulse.failover.service;

import com.netpulse.failover.dto.ManualRecoveryResponse;
import com.netpulse.failover.dto.RetryFailoverResponse;
import com.netpulse.failover.entity.FailoverEvent;
import com.netpulse.failover.repository.FailoverEventRepository;
import com.netpulse.failover.state.FailoverState;
import com.netpulse.failover.state.FailoverTrigger;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NodeRecoveryServiceTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private HeartbeatService heartbeatService;

    @Mock
    private FailoverEventRepository eventRepository;

    @Mock
    private FailoverAuditService auditService;

    @Mock
    private RecoveryVerificationService verificationService;

    @Mock
    private FailoverPolicyService policyService;

    private NodeRecoveryService nodeRecoveryService;

    @BeforeEach
    void setUp() {
        nodeRecoveryService = new NodeRecoveryService(
                nodeRepository,
                heartbeatService,
                eventRepository,
                auditService,
                verificationService,
                policyService
        );
    }

    @Test
    @DisplayName("Should successfully initiate manual recovery for an existing node")
    void testInitiateManualRecoverySuccess() {
        NetworkNode node = new NetworkNode("node-1", "Router 1", "192.168.1.1", 8080, NodeStatus.FAILED, 2000);
        when(nodeRepository.findByNodeId("node-1")).thenReturn(Optional.of(node));
        when(heartbeatService.isHeartbeatSuppressed("node-1")).thenReturn(true);
        when(auditService.generateEventId()).thenReturn("FO-20260930-100001");
        when(auditService.generateRecoveryCycleId()).thenReturn("RC-20260930-100001");

        ManualRecoveryResponse response = nodeRecoveryService.initiateManualRecovery("node-1", "Operator recovery test", false);

        assertNotNull(response);
        assertTrue(response.isInitiated());
        assertEquals("node-1", response.getNodeId());
        assertEquals(FailoverState.RECOVERY_IN_PROGRESS, response.getState());

        verify(heartbeatService).suppressHeartbeat("node-1", false);
        verify(heartbeatService).recordHeartbeat("node-1");
        verify(auditService).recordEvent(any(FailoverEvent.class));
    }

    @Test
    @DisplayName("Should reject retry when max attempts exceeded")
    void testRetryExceedsMaxAttempts() {
        FailoverEvent event = new FailoverEvent(
                "FO-001", "RC-001", "node-1", "Router 1", "node-2", "Router 2",
                FailoverTrigger.HEARTBEAT_UNREACHABLE, FailoverState.RECOVERY_VERIFICATION, FailoverState.RECOVERY_FAILED,
                1, 1, 0, 3, "Initial failure", Instant.now(), null, null, null
        );
        when(eventRepository.findByEventId("FO-001")).thenReturn(Optional.of(event));
        when(policyService.getMaxAttempts()).thenReturn(3);

        // Advance attempt count to 4 (exceeds max of 3)
        nodeRecoveryService.incrementAttempt("node-1");
        nodeRecoveryService.incrementAttempt("node-1");
        nodeRecoveryService.incrementAttempt("node-1");

        RetryFailoverResponse response = nodeRecoveryService.retryRecovery("FO-001");

        assertNotNull(response);
        assertFalse(response.isRetried());
        assertEquals(FailoverState.RECOVERY_FAILED, response.getState());
        assertTrue(response.getMessage().contains("Maximum recovery attempts exceeded"));
    }

    @Test
    @DisplayName("Should transition node to RESTORED when recovery verification passes")
    void testProcessRecoveryTickSuccess() {
        FailoverEvent event = new FailoverEvent(
                "FO-002", "RC-002", "node-1", "Router 1", "node-2", "Router 2",
                FailoverTrigger.HEARTBEAT_UNREACHABLE, FailoverState.RECOVERY_IN_PROGRESS, FailoverState.RECOVERY_VERIFICATION,
                0, 0, 0, 1, "Testing recovery tick", Instant.now(), null, null, null
        );

        when(policyService.getCooldownSeconds()).thenReturn(15);
        when(verificationService.checkNodeRecovery("node-1"))
                .thenReturn(new RecoveryVerificationService.VerificationResult(true, 2, 2, "2/2 cycles verified healthy"));

        nodeRecoveryService.processRecoveryTick(event);

        assertEquals(FailoverState.RESTORED, event.getCurrentState());
        assertTrue(event.getSuccess());
        assertNotNull(event.getCompletedAt());
        verify(auditService).recordEvent(event);
    }
}
