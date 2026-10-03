package com.netpulse.failover.service;

import com.netpulse.health.dto.NodeHealthResponse;
import com.netpulse.health.entity.HealthClassification;
import com.netpulse.health.service.NodeHealthService;
import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.telemetry.entity.TelemetryRecord;
import com.netpulse.telemetry.service.TelemetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RecoveryVerificationService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryVerificationService.class);

    private final NodeRepository nodeRepository;
    private final HeartbeatService heartbeatService;
    private final TelemetryService telemetryService;
    private final NodeHealthService nodeHealthService;
    private final FailoverPolicyService policyService;

    // nodeId -> consecutive verified healthy cycles count
    private final Map<String, Integer> verifiedCyclesMap = new ConcurrentHashMap<>();

    public RecoveryVerificationService(NodeRepository nodeRepository,
                                       HeartbeatService heartbeatService,
                                       TelemetryService telemetryService,
                                       NodeHealthService nodeHealthService,
                                       FailoverPolicyService policyService) {
        this.nodeRepository = nodeRepository;
        this.heartbeatService = heartbeatService;
        this.telemetryService = telemetryService;
        this.nodeHealthService = nodeHealthService;
        this.policyService = policyService;
    }

    public static class VerificationResult {
        private final boolean verified;
        private final int currentCycles;
        private final int requiredCycles;
        private final String details;

        public VerificationResult(boolean verified, int currentCycles, int requiredCycles, String details) {
            this.verified = verified;
            this.currentCycles = currentCycles;
            this.requiredCycles = requiredCycles;
            this.details = details;
        }

        public boolean isVerified() {
            return verified;
        }

        public int getCurrentCycles() {
            return currentCycles;
        }

        public int getRequiredCycles() {
            return requiredCycles;
        }

        public String getDetails() {
            return details;
        }
    }

    public VerificationResult checkNodeRecovery(String nodeId) {
        NetworkNode node = nodeRepository.findByNodeId(nodeId).orElse(null);
        if (node == null) {
            verifiedCyclesMap.remove(nodeId);
            return new VerificationResult(false, 0, policyService.getHealthyCyclesRequiredForRecovery(), "Node not found in registry");
        }

        HeartbeatStatusResponse heartbeat = heartbeatService.getHeartbeatStatus(nodeId);
        TelemetryRecord telemetry = telemetryService.getLatestTelemetry(nodeId);
        NodeHealthResponse health = nodeHealthService.evaluateNodeHealth(node, telemetry);

        boolean heartbeatOk = !policyService.isHeartbeatRequiredForRecovery() || heartbeat.getLiveness() == LivenessStatus.ALIVE;
        boolean healthOk = health.getHealthStatus() == HealthClassification.HEALTHY;
        boolean telemetryOk = (telemetry != null);

        int required = policyService.getHealthyCyclesRequiredForRecovery();

        if (heartbeatOk && healthOk && telemetryOk) {
            int current = verifiedCyclesMap.merge(nodeId, 1, Integer::sum);
            log.info("Recovery verification for node {} cycle {}/{}", nodeId, current, required);
            if (current >= required) {
                return new VerificationResult(true, current, required,
                        String.format("Verified healthy for %d/%d cycles (Heartbeat: %s, Health: %s, CPU: %.1f%%, Latency: %.1f ms)",
                                current, required, heartbeat.getLiveness(), health.getHealthStatus(),
                                telemetry.getCpuUsage(), telemetry.getLatency()));
            } else {
                return new VerificationResult(false, current, required,
                        String.format("Verification in progress: %d/%d cycles satisfied (Health: %s)", current, required, health.getHealthStatus()));
            }
        } else {
            // Reset counter upon unhealthy evaluation during verification
            verifiedCyclesMap.remove(nodeId);
            String failureReason = String.format("Health check failed (Heartbeat: %s, Health: %s, Telemetry: %s, Reason: %s)",
                    heartbeat.getLiveness(), health.getHealthStatus(), telemetryOk ? "Present" : "Missing", health.getReason());
            log.warn("Recovery verification reset for node {}: {}", nodeId, failureReason);
            return new VerificationResult(false, 0, required, failureReason);
        }
    }

    public void resetVerification(String nodeId) {
        verifiedCyclesMap.remove(nodeId);
    }
}
