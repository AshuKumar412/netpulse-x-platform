package com.netpulse.telemetry.service;

import com.netpulse.telemetry.dto.TelemetryRecordResponse;
import com.netpulse.telemetry.entity.TelemetryRecord;
import com.netpulse.telemetry.repository.TelemetryRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);

    private final TelemetryRecordRepository telemetryRepository;
    private final StringRedisTemplate redisTemplate;

    private final Map<String, TelemetryRecord> latestTelemetryCache = new ConcurrentHashMap<>();

    @Value("${app.telemetry.history-retention-limit:50}")
    private int historyRetentionLimit = 50;

    public TelemetryService(TelemetryRecordRepository telemetryRepository,
                            StringRedisTemplate redisTemplate) {
        this.telemetryRepository = telemetryRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public TelemetryRecord recordTelemetry(TelemetryRecord record) {
        latestTelemetryCache.put(record.getNodeId(), record);

        // Sync with Redis if available
        if (redisTemplate != null) {
            try {
                String key = "telemetry:latest:" + record.getNodeId();
                redisTemplate.opsForHash().put(key, "cpu", String.valueOf(record.getCpuUsage()));
                redisTemplate.opsForHash().put(key, "memory", String.valueOf(record.getMemoryUsage()));
                redisTemplate.opsForHash().put(key, "latency", String.valueOf(record.getLatency()));
                redisTemplate.opsForHash().put(key, "packetLoss", String.valueOf(record.getPacketLoss()));
                redisTemplate.opsForHash().put(key, "connections", String.valueOf(record.getActiveConnections()));
                redisTemplate.opsForHash().put(key, "errorRate", String.valueOf(record.getErrorRate()));
                redisTemplate.opsForHash().put(key, "timestamp", String.valueOf(record.getTimestamp().toEpochMilli()));
            } catch (Exception e) {
                log.debug("Redis telemetry write skipped: {}", e.getMessage());
            }
        }

        return telemetryRepository.save(record);
    }

    public TelemetryRecord getLatestTelemetry(String nodeId) {
        TelemetryRecord cached = latestTelemetryCache.get(nodeId);
        if (cached != null) {
            return cached;
        }

        Optional<TelemetryRecord> opt = telemetryRepository.findLatestByNodeId(nodeId);
        opt.ifPresent(record -> latestTelemetryCache.put(nodeId, record));
        return opt.orElse(null);
    }

    public List<TelemetryRecord> getAllLatestTelemetry(List<String> nodeIds) {
        List<TelemetryRecord> result = new ArrayList<>();
        for (String nodeId : nodeIds) {
            TelemetryRecord rec = getLatestTelemetry(nodeId);
            if (rec != null) {
                result.add(rec);
            }
        }
        return result;
    }

    public List<TelemetryRecordResponse> getRecentHistory(String nodeId, int limit) {
        int boundedLimit = Math.max(1, Math.min(100, limit));
        Pageable pageable = PageRequest.of(0, boundedLimit);
        List<TelemetryRecord> list = telemetryRepository.findByNodeIdOrderByTimestampDesc(nodeId, pageable);

        // Return chronological order (oldest to newest) for charting
        List<TelemetryRecordResponse> responses = list.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        Collections.reverse(responses);
        return responses;
    }

    @Transactional
    public void cleanupOldRecords(Instant cutoff) {
        int deleted = telemetryRepository.deleteByTimestampBefore(cutoff);
        if (deleted > 0) {
            log.info("Pruned {} expired telemetry records prior to {}", deleted, cutoff);
        }
    }

    @Transactional
    public void removeNodeTelemetry(String nodeId) {
        latestTelemetryCache.remove(nodeId);
        telemetryRepository.deleteByNodeId(nodeId);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete("telemetry:latest:" + nodeId);
            } catch (Exception e) {
                log.debug("Redis telemetry delete skipped: {}", e.getMessage());
            }
        }
    }

    public TelemetryRecordResponse toResponse(TelemetryRecord record) {
        if (record == null) return null;
        return new TelemetryRecordResponse(
                record.getId(),
                record.getNodeId(),
                record.getTimestamp(),
                record.getCpuUsage(),
                record.getMemoryUsage(),
                record.getLatency(),
                record.getPacketLoss(),
                record.getActiveConnections(),
                record.getErrorRate()
        );
    }
}
