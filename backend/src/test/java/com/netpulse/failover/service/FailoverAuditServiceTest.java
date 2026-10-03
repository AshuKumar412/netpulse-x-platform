package com.netpulse.failover.service;

import com.netpulse.failover.dto.FailoverEventDto;
import com.netpulse.failover.entity.FailoverEvent;
import com.netpulse.failover.repository.FailoverEventRepository;
import com.netpulse.failover.state.FailoverState;
import com.netpulse.failover.state.FailoverTrigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FailoverAuditServiceTest {

    @Mock
    private FailoverEventRepository eventRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private FailoverAuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new FailoverAuditService(eventRepository, messagingTemplate);
    }

    @Test
    @DisplayName("Should generate unique event IDs with FE- prefix and cycle IDs with RC- prefix")
    void testIdGeneration() {
        String eventId1 = auditService.generateEventId();
        String eventId2 = auditService.generateEventId();
        assertNotNull(eventId1);
        assertNotNull(eventId2);
        assertNotEquals(eventId1, eventId2);
        assertTrue(eventId1.startsWith("FE-"), "Event ID should start with FE-");

        String cycleId1 = auditService.generateRecoveryCycleId();
        String cycleId2 = auditService.generateRecoveryCycleId();
        assertNotNull(cycleId1);
        assertNotNull(cycleId2);
        assertNotEquals(cycleId1, cycleId2);
        assertTrue(cycleId1.startsWith("RC-"), "Cycle ID should start with RC-");
    }

    @Test
    @DisplayName("Should persist failover event and broadcast to WebSocket topic")
    void testRecordEventAndBroadcast() {
        FailoverEvent event = new FailoverEvent(
                "FE-001", "RC-001", "node-1", "Router 1", "node-2", "Router 2",
                FailoverTrigger.HEARTBEAT_UNREACHABLE, FailoverState.ISOLATING, FailoverState.REROUTED,
                2, 2, 0, 1, "Rerouted", Instant.now(), null, null, null
        );

        when(eventRepository.save(any(FailoverEvent.class))).thenReturn(event);

        FailoverEvent saved = auditService.recordEvent(event);

        assertNotNull(saved);
        assertEquals("FE-001", saved.getEventId());
        verify(eventRepository).save(event);
        verify(messagingTemplate).convertAndSend(eq("/topic/failover"), any(FailoverEventDto.class));
    }

    @Test
    @DisplayName("Should log a warning and not rethrow when WebSocket broadcast fails")
    void testBroadcastFailureDoesNotPropagateException() {
        FailoverEvent event = new FailoverEvent(
                "FE-002", "RC-002", "node-1", "Router 1", null, null,
                FailoverTrigger.NODE_OFFLINE, FailoverState.FAILURE_CONFIRMED, FailoverState.FAILOVER_FAILED,
                0, 0, 0, 1, "Node offline", Instant.now(), Instant.now(), false, "NO_REPLACEMENT"
        );

        when(eventRepository.save(any(FailoverEvent.class))).thenReturn(event);
        doThrow(new RuntimeException("WS error"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // Should NOT throw — broadcast failures are caught internally
        FailoverEvent result = auditService.recordEvent(event);
        assertNotNull(result);
        assertEquals("FE-002", result.getEventId());
    }
}
