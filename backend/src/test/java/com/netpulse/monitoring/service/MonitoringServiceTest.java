package com.netpulse.monitoring.service;

import com.netpulse.monitoring.dto.MonitoringStatusResponse;
import com.netpulse.node.repository.NodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        monitoringService = new MonitoringService(nodeRepository, messagingTemplate);
    }

    @Test
    @DisplayName("Should initialize in RUNNING state and report status")
    void testInitialStatus() {
        when(nodeRepository.count()).thenReturn(5L);

        MonitoringStatusResponse status = monitoringService.getMonitoringStatus();

        assertNotNull(status);
        assertEquals("RUNNING", status.getStatus());
        assertEquals(5, status.getMonitoredNodeCount());
        assertTrue(monitoringService.isRunning());
    }

    @Test
    @DisplayName("Should transition cleanly between STOPPED and RUNNING")
    void testStopAndStartLifecycle() {
        when(nodeRepository.count()).thenReturn(3L);

        MonitoringStatusResponse stopResp = monitoringService.stopMonitoring();
        assertEquals("STOPPED", stopResp.getStatus());
        assertFalse(monitoringService.isRunning());

        MonitoringStatusResponse startResp = monitoringService.startMonitoring();
        assertEquals("RUNNING", startResp.getStatus());
        assertTrue(monitoringService.isRunning());
    }

    @Test
    @DisplayName("Should increment cycle count on each execution tick")
    void testIncrementCycle() {
        long c1 = monitoringService.incrementCycle();
        long c2 = monitoringService.incrementCycle();

        assertEquals(1L, c1);
        assertEquals(2L, c2);
        assertEquals(2L, monitoringService.getMonitoringStatus().getCycleCount());
    }
}
