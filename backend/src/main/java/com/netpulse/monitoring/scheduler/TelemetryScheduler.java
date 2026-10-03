package com.netpulse.monitoring.scheduler;

import com.netpulse.failover.service.FailureDetectionService;
import com.netpulse.health.dto.NodeHealthResponse;
import com.netpulse.health.service.NodeHealthService;
import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.monitoring.service.MonitoringService;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.telemetry.dto.TelemetryRecordResponse;
import com.netpulse.telemetry.entity.TelemetryRecord;
import com.netpulse.telemetry.service.TelemetryEngine;
import com.netpulse.telemetry.service.TelemetryService;
import com.netpulse.topology.repository.NetworkLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class TelemetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(TelemetryScheduler.class);

    private final MonitoringService monitoringService;
    private final NodeRepository nodeRepository;
    private final NetworkLinkRepository linkRepository;
    private final TelemetryEngine telemetryEngine;
    private final TelemetryService telemetryService;
    private final HeartbeatService heartbeatService;
    private final NodeHealthService nodeHealthService;
    private final FailureDetectionService failureDetectionService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.telemetry.retention-hours:24}")
    private long retentionHours = 24;

    public TelemetryScheduler(MonitoringService monitoringService,
                              NodeRepository nodeRepository,
                              NetworkLinkRepository linkRepository,
                              TelemetryEngine telemetryEngine,
                              TelemetryService telemetryService,
                              HeartbeatService heartbeatService,
                              NodeHealthService nodeHealthService,
                              FailureDetectionService failureDetectionService,
                              SimpMessagingTemplate messagingTemplate) {
        this.monitoringService = monitoringService;
        this.nodeRepository = nodeRepository;
        this.linkRepository = linkRepository;
        this.telemetryEngine = telemetryEngine;
        this.telemetryService = telemetryService;
        this.heartbeatService = heartbeatService;
        this.nodeHealthService = nodeHealthService;
        this.failureDetectionService = failureDetectionService;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRateString = "${app.telemetry.interval-ms:2000}")
    public void executeTelemetryCycle() {
        if (!monitoringService.isRunning()) {
            return;
        }

        List<NetworkNode> nodes = nodeRepository.findAll();
        if (nodes.isEmpty()) {
            return;
        }

        long cycle = monitoringService.incrementCycle();

        List<TelemetryRecordResponse> telemetryResponses = new ArrayList<>();
        List<NodeHealthResponse> healthResponses = new ArrayList<>();
        List<HeartbeatStatusResponse> heartbeatResponses = new ArrayList<>();

        for (NetworkNode node : nodes) {
            String nodeId = node.getNodeId();

            // 1. Emit heartbeat if node is not OFFLINE and not suppressed
            if (node.getStatus() != NodeStatus.OFFLINE) {
                heartbeatService.recordHeartbeat(nodeId);
            }

            // 2. Fetch connection degree and previous telemetry
            int linkDegree = (int) linkRepository.countByNodeInvolvement(nodeId);
            TelemetryRecord prev = telemetryService.getLatestTelemetry(nodeId);

            // 3. Compute deterministic telemetry
            TelemetryRecord record = telemetryEngine.computeTelemetry(node, linkDegree, cycle, prev);
            telemetryService.recordTelemetry(record);
            telemetryResponses.add(telemetryService.toResponse(record));

            // 4. Evaluate node health
            NodeHealthResponse health = nodeHealthService.evaluateNodeHealth(node, record);
            healthResponses.add(health);

            // 5. Gather heartbeat status
            HeartbeatStatusResponse heartbeat = heartbeatService.getHeartbeatStatus(nodeId);
            heartbeatResponses.add(heartbeat);
        }

        // 6. Evaluate node failures and process recovery progression
        try {
            failureDetectionService.processTelemetryCycle(nodes, healthResponses, heartbeatResponses);
        } catch (Exception e) {
            log.error("Error executing failure detection cycle: {}", e.getMessage());
        }

        // Broadcast updates to WebSocket subscribers
        broadcastWebSocket("/topic/telemetry", telemetryResponses);
        broadcastWebSocket("/topic/health", healthResponses);
        broadcastWebSocket("/topic/heartbeat", heartbeatResponses);
    }

    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void executeRetentionCleanup() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(retentionHours));
        telemetryService.cleanupOldRecords(cutoff);
    }

    private void broadcastWebSocket(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.debug("WebSocket broadcast to {} failed: {}", destination, e.getMessage());
        }
    }
}
