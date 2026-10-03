package com.netpulse.failover.service;

import com.netpulse.failover.entity.ActiveSessionRecord;
import com.netpulse.failover.repository.ActiveSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrafficRecoveryServiceTest {

    @Mock
    private ActiveSessionRepository sessionRepository;

    private TrafficRecoveryService trafficRecoveryService;

    @BeforeEach
    void setUp() {
        trafficRecoveryService = new TrafficRecoveryService(sessionRepository);
    }

    @Test
    @DisplayName("Should register new active traffic session")
    void testRegisterSession() {
        when(sessionRepository.save(any(ActiveSessionRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ActiveSessionRecord record = trafficRecoveryService.registerSession("REQ-101", "node-1", "HTTP", "ADAPTIVE");

        assertNotNull(record);
        assertEquals("REQ-101", record.getSessionId());
        assertEquals("node-1", record.getNodeId());
        assertEquals("ACTIVE", record.getStatus());
        verify(sessionRepository).save(any(ActiveSessionRecord.class));
    }

    @Test
    @DisplayName("Should reroute active sessions to replacement node and track provenance")
    void testRerouteSessions() {
        ActiveSessionRecord s1 = new ActiveSessionRecord("REQ-001", "node-failed", "node-failed", "HTTP", "ADAPTIVE", "ACTIVE", Instant.now(), Instant.now(), null);
        ActiveSessionRecord s2 = new ActiveSessionRecord("REQ-002", "node-failed", "node-failed", "HTTP", "ADAPTIVE", "ACTIVE", Instant.now(), Instant.now(), null);

        when(sessionRepository.findByNodeIdAndStatus("node-failed", "ACTIVE")).thenReturn(List.of(s1, s2));

        TrafficRecoveryService.RerouteResult result = trafficRecoveryService.rerouteSessions("node-failed", "node-repl", "RC-001");

        assertNotNull(result);
        assertEquals(2, result.getAffectedCount());
        assertEquals(2, result.getReroutedCount());
        assertEquals(0, result.getUnrecoveredCount());

        assertEquals("node-repl", s1.getNodeId());
        assertEquals("node-failed", s1.getOriginalNodeId());
        assertEquals("REROUTED", s1.getStatus());
        assertEquals("RC-001", s1.getFailoverCycleId());

        verify(sessionRepository, times(2)).save(any(ActiveSessionRecord.class));
    }
}
