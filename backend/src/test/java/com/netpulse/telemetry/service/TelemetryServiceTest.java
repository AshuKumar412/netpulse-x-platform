package com.netpulse.telemetry.service;

import com.netpulse.telemetry.dto.TelemetryRecordResponse;
import com.netpulse.telemetry.entity.TelemetryRecord;
import com.netpulse.telemetry.repository.TelemetryRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryServiceTest {

    @Mock
    private TelemetryRecordRepository telemetryRepository;

    private TelemetryService telemetryService;

    @BeforeEach
    void setUp() {
        telemetryService = new TelemetryService(telemetryRepository, null);
    }

    @Test
    @DisplayName("Should save telemetry record and update internal cache")
    void testRecordTelemetrySuccess() {
        TelemetryRecord record = new TelemetryRecord("node-1", Instant.now(), 45.0, 55.0, 20.0, 0.0, 100, 0.0);
        when(telemetryRepository.save(any(TelemetryRecord.class))).thenReturn(record);

        TelemetryRecord saved = telemetryService.recordTelemetry(record);

        assertNotNull(saved);
        assertEquals("node-1", saved.getNodeId());
        assertEquals(45.0, saved.getCpuUsage());
        verify(telemetryRepository).save(record);

        // Verify cache hit
        TelemetryRecord cached = telemetryService.getLatestTelemetry("node-1");
        assertNotNull(cached);
        assertEquals("node-1", cached.getNodeId());
    }

    @Test
    @DisplayName("Should return recent chronological history for charting")
    void testGetRecentHistory() {
        Instant t1 = Instant.now().minusSeconds(20);
        Instant t2 = Instant.now().minusSeconds(10);
        TelemetryRecord r1 = new TelemetryRecord("node-1", t1, 40.0, 50.0, 20.0, 0.0, 80, 0.0);
        TelemetryRecord r2 = new TelemetryRecord("node-1", t2, 45.0, 52.0, 22.0, 0.0, 85, 0.0);

        // Repository returns desc (r2 then r1)
        when(telemetryRepository.findByNodeIdOrderByTimestampDesc(eq("node-1"), any(Pageable.class)))
                .thenReturn(List.of(r2, r1));

        List<TelemetryRecordResponse> history = telemetryService.getRecentHistory("node-1", 10);

        assertNotNull(history);
        assertEquals(2, history.size());
        // Should be reversed into chronological order (r1 then r2)
        assertEquals(t1, history.get(0).getTimestamp());
        assertEquals(t2, history.get(1).getTimestamp());
    }

    @Test
    @DisplayName("Should cleanup old telemetry records")
    void testCleanupOldRecords() {
        Instant cutoff = Instant.now().minusSeconds(3600);
        when(telemetryRepository.deleteByTimestampBefore(cutoff)).thenReturn(25);

        telemetryService.cleanupOldRecords(cutoff);

        verify(telemetryRepository).deleteByTimestampBefore(cutoff);
    }

    @Test
    @DisplayName("Should remove all telemetry for specific node on decommissioning")
    void testRemoveNodeTelemetry() {
        telemetryService.removeNodeTelemetry("node-del");

        verify(telemetryRepository).deleteByNodeId("node-del");
        assertNull(telemetryService.getLatestTelemetry("node-del"));
    }
}
