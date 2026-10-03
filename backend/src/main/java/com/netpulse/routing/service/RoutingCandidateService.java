package com.netpulse.routing.service;

import com.netpulse.health.dto.NodeHealthResponse;
import com.netpulse.health.entity.HealthClassification;
import com.netpulse.health.service.NodeHealthService;
import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.routing.dto.CandidateEvaluationDto;
import com.netpulse.telemetry.entity.TelemetryRecord;
import com.netpulse.telemetry.service.TelemetryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoutingCandidateService {

    private final NodeRepository nodeRepository;
    private final TelemetryService telemetryService;
    private final HeartbeatService heartbeatService;
    private final NodeHealthService nodeHealthService;

    @Value("${app.routing.eligibility.allow-warning:true}")
    private boolean allowWarning = true;

    @Value("${app.routing.eligibility.allow-degraded:false}")
    private boolean allowDegraded = false;

    @Value("${app.routing.eligibility.allow-suspected:false}")
    private boolean allowSuspected = false;

    public RoutingCandidateService(NodeRepository nodeRepository,
                                   TelemetryService telemetryService,
                                   HeartbeatService heartbeatService,
                                   NodeHealthService nodeHealthService) {
        this.nodeRepository = nodeRepository;
        this.telemetryService = telemetryService;
        this.heartbeatService = heartbeatService;
        this.nodeHealthService = nodeHealthService;
    }

    public List<CandidateEvaluationDto> evaluateAllCandidates() {
        List<NetworkNode> nodes = nodeRepository.findAll();
        List<CandidateEvaluationDto> candidates = new ArrayList<>();

        for (NetworkNode node : nodes) {
            String nodeId = node.getNodeId();
            TelemetryRecord telemetry = telemetryService.getLatestTelemetry(nodeId);
            HeartbeatStatusResponse heartbeat = heartbeatService.getHeartbeatStatus(nodeId);
            NodeHealthResponse health = nodeHealthService.evaluateNodeHealth(node, telemetry);

            LivenessStatus liveness = heartbeat.getLiveness();
            HealthClassification healthStatus = health.getHealthStatus();

            boolean eligible = true;
            String reason;

            if (node.getStatus() == NodeStatus.OFFLINE) {
                eligible = false;
                reason = "Node administratively OFFLINE";
            } else if (node.getStatus() == NodeStatus.FAILED) {
                eligible = false;
                reason = "Node operational status reported FAILED";
            } else if (liveness == LivenessStatus.UNREACHABLE) {
                eligible = false;
                reason = "Heartbeat timeout exceeded (UNREACHABLE)";
            } else if (liveness == LivenessStatus.SUSPECTED && !allowSuspected) {
                eligible = false;
                reason = "Heartbeat delayed (SUSPECTED) - excluded by policy";
            } else if (healthStatus == HealthClassification.FAILED || healthStatus == HealthClassification.OFFLINE) {
                eligible = false;
                reason = "Node health evaluated as " + healthStatus;
            } else if (healthStatus == HealthClassification.DEGRADED && !allowDegraded) {
                eligible = false;
                reason = "Node health DEGRADED - excluded by policy";
            } else if (healthStatus == HealthClassification.WARNING && !allowWarning) {
                eligible = false;
                reason = "Node health WARNING - excluded by policy";
            } else {
                reason = "Eligible candidate (" + healthStatus + ")";
            }

            candidates.add(new CandidateEvaluationDto(
                    nodeId,
                    node.getName(),
                    node.getCapacity(),
                    healthStatus,
                    liveness,
                    telemetry != null ? telemetry.getCpuUsage() : 0.0,
                    telemetry != null ? telemetry.getMemoryUsage() : 0.0,
                    telemetry != null ? telemetry.getLatency() : 0.0,
                    telemetry != null ? telemetry.getPacketLoss() : 0.0,
                    telemetry != null ? telemetry.getActiveConnections() : 0,
                    telemetry != null ? telemetry.getErrorRate() : 0.0,
                    eligible,
                    reason,
                    null
            ));
        }

        return candidates;
    }

    public void setEligibilityPolicy(boolean allowWarning, boolean allowDegraded, boolean allowSuspected) {
        this.allowWarning = allowWarning;
        this.allowDegraded = allowDegraded;
        this.allowSuspected = allowSuspected;
    }

    public boolean isAllowWarning() {
        return allowWarning;
    }

    public boolean isAllowDegraded() {
        return allowDegraded;
    }

    public boolean isAllowSuspected() {
        return allowSuspected;
    }
}
