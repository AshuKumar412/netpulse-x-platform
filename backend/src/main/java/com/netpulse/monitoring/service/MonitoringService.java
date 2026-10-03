package com.netpulse.monitoring.service;

import com.netpulse.monitoring.dto.MonitoringStatusResponse;
import com.netpulse.node.repository.NodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private volatile String status = "RUNNING"; // Default RUNNING for automatic infrastructure monitoring
    private final AtomicLong cycleCount = new AtomicLong(0);
    private volatile Instant startedAt = Instant.now();
    private volatile Instant lastCycleAt = Instant.now();

    private final NodeRepository nodeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MonitoringService(NodeRepository nodeRepository, SimpMessagingTemplate messagingTemplate) {
        this.nodeRepository = nodeRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public synchronized MonitoringStatusResponse startMonitoring() {
        if (!"RUNNING".equals(this.status)) {
            this.status = "RUNNING";
            this.startedAt = Instant.now();
            this.lastCycleAt = Instant.now();
            log.info("Real-time telemetry and heartbeat monitoring engine STARTED");
            broadcastMonitoringStatus();
        }
        return getMonitoringStatus();
    }

    public synchronized MonitoringStatusResponse stopMonitoring() {
        if ("RUNNING".equals(this.status)) {
            this.status = "STOPPED";
            log.info("Real-time telemetry and heartbeat monitoring engine STOPPED");
            broadcastMonitoringStatus();
        }
        return getMonitoringStatus();
    }

    public boolean isRunning() {
        return "RUNNING".equals(this.status);
    }

    public long incrementCycle() {
        this.lastCycleAt = Instant.now();
        return cycleCount.incrementAndGet();
    }

    public MonitoringStatusResponse getMonitoringStatus() {
        long uptime = 0;
        if (startedAt != null && "RUNNING".equals(this.status)) {
            uptime = Duration.between(startedAt, Instant.now()).getSeconds();
        }
        int nodeCount = (int) nodeRepository.count();

        return new MonitoringStatusResponse(
                this.status,
                this.cycleCount.get(),
                uptime,
                nodeCount,
                this.startedAt,
                this.lastCycleAt
        );
    }

    public void broadcastMonitoringStatus() {
        try {
            messagingTemplate.convertAndSend("/topic/monitoring", getMonitoringStatus());
        } catch (Exception e) {
            log.debug("WebSocket monitoring broadcast skipped: {}", e.getMessage());
        }
    }
}
