package com.netpulse.whatif.service;

import com.netpulse.health.dto.NodeHealthResponse;
import com.netpulse.health.service.NodeHealthService;
import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.routing.dto.RoutingConfigResponse;
import com.netpulse.routing.dto.RoutingStatusResponse;
import com.netpulse.routing.entity.RoutingStrategyType;
import com.netpulse.routing.service.RoutingService;
import com.netpulse.scheduler.dto.ProcessControlBlockDto;
import com.netpulse.scheduler.dto.SchedulerPolicyConfig;
import com.netpulse.scheduler.dto.SchedulerStatusResponse;
import com.netpulse.scheduler.service.SchedulerService;
import com.netpulse.telemetry.entity.TelemetryRecord;
import com.netpulse.telemetry.service.TelemetryService;
import com.netpulse.topology.entity.NetworkLink;
import com.netpulse.topology.repository.NetworkLinkRepository;
import com.netpulse.whatif.dto.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SystemSnapshotService {

    private final NodeRepository nodeRepository;
    private final NetworkLinkRepository linkRepository;
    private final TelemetryService telemetryService;
    private final HeartbeatService heartbeatService;
    private final NodeHealthService nodeHealthService;
    private final RoutingService routingService;
    private final SchedulerService schedulerService;

    public SystemSnapshotService(NodeRepository nodeRepository,
                                 NetworkLinkRepository linkRepository,
                                 TelemetryService telemetryService,
                                 HeartbeatService heartbeatService,
                                 NodeHealthService nodeHealthService,
                                 RoutingService routingService,
                                 SchedulerService schedulerService) {
        this.nodeRepository = nodeRepository;
        this.linkRepository = linkRepository;
        this.telemetryService = telemetryService;
        this.heartbeatService = heartbeatService;
        this.nodeHealthService = nodeHealthService;
        this.routingService = routingService;
        this.schedulerService = schedulerService;
    }

    /**
     * Captures a read-only, point-in-time domain snapshot of the entire operational system.
     * Contains zero sensitive credentials, secrets, or OTPs.
     */
    public SystemSnapshotDto captureSnapshot() {
        SystemSnapshotDto snapshot = new SystemSnapshotDto();
        snapshot.setSnapshotId("SNAP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        snapshot.setCapturedAt(Instant.now());

        // 1. Capture Nodes
        List<NetworkNode> nodes = nodeRepository.findAll();
        List<SnapshotNodeDto> snapshotNodes = new ArrayList<>();
        int healthyCount = 0;
        int failedCount = 0;

        for (NetworkNode node : nodes) {
            snapshotNodes.add(new SnapshotNodeDto(
                    node.getNodeId(),
                    node.getName(),
                    node.getHost(),
                    node.getPort(),
                    node.getStatus(),
                    node.getCapacity()
            ));
            if (node.getStatus() == NodeStatus.HEALTHY) {
                healthyCount++;
            } else if (node.getStatus() == NodeStatus.FAILED || node.getStatus() == NodeStatus.OFFLINE) {
                failedCount++;
            }
        }
        snapshot.setNodes(snapshotNodes);
        snapshot.setTotalNodes(nodes.size());
        snapshot.setHealthyNodes(healthyCount);
        snapshot.setFailedNodes(failedCount);

        // 2. Capture Links
        List<NetworkLink> links = linkRepository.findAll();
        List<SnapshotLinkDto> snapshotLinks = new ArrayList<>();
        for (NetworkLink link : links) {
            int linkLat = (int) Math.round(link.getWeight() * 15.0);
            snapshotLinks.add(new SnapshotLinkDto(
                    link.getLinkId(),
                    link.getSourceNodeId(),
                    link.getTargetNodeId(),
                    link.getStatus(),
                    link.getBandwidth(),
                    linkLat,
                    0.0
            ));
        }
        snapshot.setLinks(snapshotLinks);

        // 3. Capture Telemetry & Aggregate Averages
        List<SnapshotTelemetryDto> snapshotTelemetry = new ArrayList<>();
        double totalCpu = 0.0;
        double totalMemory = 0.0;
        double totalLatency = 0.0;
        double totalPacketLoss = 0.0;
        int totalConnections = 0;

        for (NetworkNode node : nodes) {
            TelemetryRecord rec = telemetryService.getLatestTelemetry(node.getNodeId());
            if (rec != null) {
                snapshotTelemetry.add(new SnapshotTelemetryDto(
                        node.getNodeId(),
                        rec.getCpuUsage(),
                        rec.getMemoryUsage(),
                        rec.getLatency(),
                        rec.getPacketLoss(),
                        rec.getActiveConnections(),
                        rec.getErrorRate(),
                        1000L
                ));
                totalCpu += rec.getCpuUsage();
                totalMemory += rec.getMemoryUsage();
                totalLatency += rec.getLatency();
                totalPacketLoss += rec.getPacketLoss();
                totalConnections += rec.getActiveConnections();
            } else {
                snapshotTelemetry.add(new SnapshotTelemetryDto(
                        node.getNodeId(), 25.0, 30.0, 20.0, 0.0, 10, 0.0, 1000L
                ));
                totalCpu += 25.0;
                totalMemory += 30.0;
                totalLatency += 20.0;
                totalConnections += 10;
            }
        }
        snapshot.setTelemetry(snapshotTelemetry);
        int nodeCount = Math.max(1, nodes.size());
        snapshot.setAverageCpu(Math.round((totalCpu / nodeCount) * 100.0) / 100.0);
        snapshot.setAverageMemory(Math.round((totalMemory / nodeCount) * 100.0) / 100.0);
        snapshot.setAverageLatency(Math.round((totalLatency / nodeCount) * 100.0) / 100.0);
        snapshot.setAveragePacketLoss(Math.round((totalPacketLoss / nodeCount) * 100.0) / 100.0);
        snapshot.setTotalActiveConnections(totalConnections);

        // 4. Capture Health & Heartbeat
        List<SnapshotHealthDto> snapshotHealth = new ArrayList<>();
        for (NetworkNode node : nodes) {
            TelemetryRecord rec = telemetryService.getLatestTelemetry(node.getNodeId());
            HeartbeatStatusResponse hb = heartbeatService.getHeartbeatStatus(node.getNodeId());
            NodeHealthResponse health = nodeHealthService.evaluateNodeHealth(node, rec);

            double hScore = (health.getHealthStatus() == com.netpulse.health.entity.HealthClassification.HEALTHY) ? 100.0 :
                    (health.getHealthStatus() == com.netpulse.health.entity.HealthClassification.WARNING) ? 75.0 :
                    (health.getHealthStatus() == com.netpulse.health.entity.HealthClassification.DEGRADED) ? 50.0 : 0.0;

            snapshotHealth.add(new SnapshotHealthDto(
                    node.getNodeId(),
                    health.getHealthStatus(),
                    hb.getLiveness(),
                    hScore,
                    hb.getLastHeartbeat() != null ? hb.getLastHeartbeat().toEpochMilli() : 0L
            ));
        }
        snapshot.setHealth(snapshotHealth);

        // 5. Capture Routing Configuration
        RoutingConfigResponse routingConfig = routingService.getConfig();
        Map<String, Double> weights = routingConfig.getWeights();
        snapshot.setRoutingState(new SnapshotRoutingStateDto(
                routingConfig.getDefaultStrategy() != null ? routingConfig.getDefaultStrategy() : RoutingStrategyType.ADAPTIVE,
                100L,
                weights.getOrDefault("cpu", 0.20),
                weights.getOrDefault("memory", 0.15),
                weights.getOrDefault("latency", 0.20),
                weights.getOrDefault("packetLoss", 0.10),
                weights.getOrDefault("connections", 0.15)
        ));

        // 6. Capture Scheduler Configuration & Active Workload
        SchedulerStatusResponse schedStatus = schedulerService.getStatus();
        SchedulerPolicyConfig schedPolicy = schedulerService.getPolicy();
        List<SnapshotProcessDto> processes = new ArrayList<>();

        if (schedStatus.getReadyQueue() != null) {
            for (ProcessControlBlockDto pcb : schedStatus.getReadyQueue()) {
                processes.add(new SnapshotProcessDto(
                        pcb.getProcessId(), pcb.getProcessName(), pcb.getTargetNodeId(),
                        pcb.getProcessType(), pcb.getState(), pcb.getPriority(),
                        pcb.getArrivalTime(), pcb.getBurstTime(), pcb.getRemainingBurstTime()
                ));
            }
        }
        if (schedStatus.getRunningProcesses() != null) {
            for (ProcessControlBlockDto pcb : schedStatus.getRunningProcesses()) {
                processes.add(new SnapshotProcessDto(
                        pcb.getProcessId(), pcb.getProcessName(), pcb.getTargetNodeId(),
                        pcb.getProcessType(), pcb.getState(), pcb.getPriority(),
                        pcb.getArrivalTime(), pcb.getBurstTime(), pcb.getRemainingBurstTime()
                ));
            }
        }

        snapshot.setSchedulerState(new SnapshotSchedulerStateDto(
                schedStatus.getStatus(),
                schedStatus.getAlgorithm(),
                schedPolicy.getTimeQuantum(),
                schedPolicy.getCoresPerNode(),
                schedStatus.getCurrentTick(),
                processes
        ));

        return snapshot;
    }
}
