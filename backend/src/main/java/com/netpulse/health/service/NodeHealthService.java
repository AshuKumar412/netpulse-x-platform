package com.netpulse.health.service;

import com.netpulse.health.dto.NodeHealthResponse;
import com.netpulse.health.entity.HealthClassification;
import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.telemetry.entity.TelemetryRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class NodeHealthService {

    private final HeartbeatService heartbeatService;

    @Value("${app.health.cpu-warning:70.0}")
    private double cpuWarning = 70.0;

    @Value("${app.health.cpu-critical:90.0}")
    private double cpuCritical = 90.0;

    @Value("${app.health.memory-warning:75.0}")
    private double memoryWarning = 75.0;

    @Value("${app.health.memory-critical:90.0}")
    private double memoryCritical = 90.0;

    @Value("${app.health.latency-warning:100.0}")
    private double latencyWarning = 100.0;

    @Value("${app.health.latency-critical:250.0}")
    private double latencyCritical = 250.0;

    @Value("${app.health.packet-loss-warning:2.0}")
    private double packetLossWarning = 2.0;

    @Value("${app.health.packet-loss-critical:5.0}")
    private double packetLossCritical = 5.0;

    @Value("${app.health.error-rate-warning:1.0}")
    private double errorRateWarning = 1.0;

    @Value("${app.health.error-rate-critical:5.0}")
    private double errorRateCritical = 5.0;

    public NodeHealthService(HeartbeatService heartbeatService) {
        this.heartbeatService = heartbeatService;
    }

    public NodeHealthResponse evaluateNodeHealth(NetworkNode node, TelemetryRecord telemetry) {
        String nodeId = node.getNodeId();
        HeartbeatStatusResponse heartbeat = heartbeatService.getHeartbeatStatus(nodeId);
        LivenessStatus liveness = heartbeat.getLiveness();
        Instant lastHeartbeat = heartbeat.getLastHeartbeat();
        Instant now = Instant.now();

        // 1. Check administrative OFFLINE status
        if (node.getStatus() == NodeStatus.OFFLINE) {
            return new NodeHealthResponse(
                    nodeId,
                    HealthClassification.OFFLINE,
                    liveness,
                    lastHeartbeat,
                    now,
                    "Node administratively OFFLINE",
                    telemetry != null ? telemetry.getCpuUsage() : 0.0,
                    telemetry != null ? telemetry.getMemoryUsage() : 0.0,
                    telemetry != null ? telemetry.getLatency() : 0.0,
                    telemetry != null ? telemetry.getPacketLoss() : 0.0,
                    telemetry != null ? telemetry.getErrorRate() : 0.0
            );
        }

        // 2. Check Heartbeat Unreachable (timeout)
        if (liveness == LivenessStatus.UNREACHABLE) {
            return new NodeHealthResponse(
                    nodeId,
                    HealthClassification.FAILED,
                    liveness,
                    lastHeartbeat,
                    now,
                    "Heartbeat timeout exceeded - node unreachable",
                    telemetry != null ? telemetry.getCpuUsage() : 0.0,
                    telemetry != null ? telemetry.getMemoryUsage() : 0.0,
                    telemetry != null ? telemetry.getLatency() : 0.0,
                    telemetry != null ? telemetry.getPacketLoss() : 100.0,
                    telemetry != null ? telemetry.getErrorRate() : 100.0
            );
        }

        // Extract telemetry metrics
        double cpu = telemetry != null ? telemetry.getCpuUsage() : 0.0;
        double mem = telemetry != null ? telemetry.getMemoryUsage() : 0.0;
        double lat = telemetry != null ? telemetry.getLatency() : 0.0;
        double loss = telemetry != null ? telemetry.getPacketLoss() : 0.0;
        double err = telemetry != null ? telemetry.getErrorRate() : 0.0;

        // 3. Node FAILED state
        if (node.getStatus() == NodeStatus.FAILED || loss >= packetLossCritical || err >= errorRateCritical) {
            String reason = node.getStatus() == NodeStatus.FAILED
                    ? "Node operational status reported FAILED"
                    : String.format("Critical telemetry failure (Packet Loss: %.1f%%, Error Rate: %.1f%%)", loss, err);
            return new NodeHealthResponse(
                    nodeId,
                    HealthClassification.FAILED,
                    liveness,
                    lastHeartbeat,
                    now,
                    reason,
                    cpu, mem, lat, loss, err
            );
        }

        // 4. Node DEGRADED state
        if (node.getStatus() == NodeStatus.CONGESTED || lat >= latencyCritical || cpu >= cpuCritical || mem >= memoryCritical) {
            List<String> reasons = new ArrayList<>();
            if (node.getStatus() == NodeStatus.CONGESTED) reasons.add("Congestion detected");
            if (cpu >= cpuCritical) reasons.add(String.format("Critical CPU (%.1f%%)", cpu));
            if (mem >= memoryCritical) reasons.add(String.format("Critical Memory (%.1f%%)", mem));
            if (lat >= latencyCritical) reasons.add(String.format("Critical Latency (%.1f ms)", lat));

            return new NodeHealthResponse(
                    nodeId,
                    HealthClassification.DEGRADED,
                    liveness,
                    lastHeartbeat,
                    now,
                    String.join(", ", reasons),
                    cpu, mem, lat, loss, err
            );
        }

        // 5. Node WARNING state
        if (node.getStatus() == NodeStatus.WARNING || liveness == LivenessStatus.SUSPECTED
                || cpu >= cpuWarning || mem >= memoryWarning || lat >= latencyWarning
                || loss >= packetLossWarning || err >= errorRateWarning) {
            List<String> reasons = new ArrayList<>();
            if (liveness == LivenessStatus.SUSPECTED) reasons.add("Heartbeat delayed (SUSPECTED)");
            if (cpu >= cpuWarning) reasons.add(String.format("High CPU (%.1f%%)", cpu));
            if (mem >= memoryWarning) reasons.add(String.format("High Memory (%.1f%%)", mem));
            if (lat >= latencyWarning) reasons.add(String.format("Elevated Latency (%.1f ms)", lat));
            if (loss >= packetLossWarning) reasons.add(String.format("Packet Loss (%.1f%%)", loss));
            if (err >= errorRateWarning) reasons.add(String.format("Error Rate (%.1f%%)", err));
            if (reasons.isEmpty()) reasons.add("Node warning flag active");

            return new NodeHealthResponse(
                    nodeId,
                    HealthClassification.WARNING,
                    liveness,
                    lastHeartbeat,
                    now,
                    String.join(", ", reasons),
                    cpu, mem, lat, loss, err
            );
        }

        // 6. HEALTHY state
        return new NodeHealthResponse(
                nodeId,
                HealthClassification.HEALTHY,
                liveness,
                lastHeartbeat,
                now,
                "Optimal operating conditions and active heartbeat",
                cpu, mem, lat, loss, err
        );
    }

    public void setThresholds(double cpuWarning, double cpuCritical,
                              double memoryWarning, double memoryCritical,
                              double latencyWarning, double latencyCritical,
                              double packetLossWarning, double packetLossCritical,
                              double errorRateWarning, double errorRateCritical) {
        this.cpuWarning = cpuWarning;
        this.cpuCritical = cpuCritical;
        this.memoryWarning = memoryWarning;
        this.memoryCritical = memoryCritical;
        this.latencyWarning = latencyWarning;
        this.latencyCritical = latencyCritical;
        this.packetLossWarning = packetLossWarning;
        this.packetLossCritical = packetLossCritical;
        this.errorRateWarning = errorRateWarning;
        this.errorRateCritical = errorRateCritical;
    }
}
