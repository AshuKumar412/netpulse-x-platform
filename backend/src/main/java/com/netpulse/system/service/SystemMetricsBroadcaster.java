package com.netpulse.system.service;

import com.netpulse.system.dto.HostLiveMetricsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Periodically broadcasts live host machine performance telemetry over WebSocket.
 */
@Service
public class SystemMetricsBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(SystemMetricsBroadcaster.class);

    private final SystemDiscoveryService discoveryService;
    private final SimpMessagingTemplate messagingTemplate;

    public SystemMetricsBroadcaster(SystemDiscoveryService discoveryService, SimpMessagingTemplate messagingTemplate) {
        this.discoveryService = discoveryService;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRateString = "${app.system.metrics-broadcast-interval-ms:2000}")
    public void broadcastLiveMetrics() {
        try {
            HostLiveMetricsDto metrics = discoveryService.collectLiveMetrics();
            messagingTemplate.convertAndSend("/topic/system-metrics", metrics);
        } catch (Throwable t) {
            log.trace("Error broadcasting host live system metrics: {}", t.getMessage());
        }
    }
}
