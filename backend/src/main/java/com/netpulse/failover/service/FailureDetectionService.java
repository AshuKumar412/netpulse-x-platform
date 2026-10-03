package com.netpulse.failover.service;

import com.netpulse.failover.entity.FailoverEvent;
import com.netpulse.failover.repository.FailoverEventRepository;
import com.netpulse.failover.state.FailoverState;
import com.netpulse.failover.state.FailoverTrigger;
import com.netpulse.health.dto.NodeHealthResponse;
import com.netpulse.health.entity.HealthClassification;
import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FailureDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FailureDetectionService.class);

    private final FailoverOrchestrator orchestrator;
    private final NodeRecoveryService nodeRecoveryService;
    private final FailoverAuditService auditService;
    private final FailoverPolicyService policyService;
    private final FailoverEventRepository eventRepository;

    // nodeId -> consecutive unhealthy count
    private final Map<String, Integer> consecutiveFailuresMap = new ConcurrentHashMap<>();

    public FailureDetectionService(FailoverOrchestrator orchestrator,
                                   NodeRecoveryService nodeRecoveryService,
                                   FailoverAuditService auditService,
                                   FailoverPolicyService policyService,
                                   FailoverEventRepository eventRepository) {
        this.orchestrator = orchestrator;
        this.nodeRecoveryService = nodeRecoveryService;
        this.auditService = auditService;
        this.policyService = policyService;
        this.eventRepository = eventRepository;
    }

    public void processTelemetryCycle(List<NetworkNode> nodes,
                                      List<NodeHealthResponse> healthList,
                                      List<HeartbeatStatusResponse> heartbeatList) {
        if (!policyService.isEnabled() || nodes == null || nodes.isEmpty()) {
            return;
        }

        Map<String, NodeHealthResponse> healthMap = new ConcurrentHashMap<>();
        if (healthList != null) {
            for (NodeHealthResponse h : healthList) {
                healthMap.put(h.getNodeId(), h);
            }
        }

        Map<String, HeartbeatStatusResponse> heartbeatMap = new ConcurrentHashMap<>();
        if (heartbeatList != null) {
            for (HeartbeatStatusResponse hb : heartbeatList) {
                heartbeatMap.put(hb.getNodeId(), hb);
            }
        }

        for (NetworkNode node : nodes) {
            String nodeId = node.getNodeId();
            NodeHealthResponse health = healthMap.get(nodeId);
            HeartbeatStatusResponse heartbeat = heartbeatMap.get(nodeId);

            evaluateNode(node, health, heartbeat);
        }

        // Process active recovery progressions
        List<FailoverEvent> activeEvents = eventRepository.findByCurrentStateIn(List.of(
                FailoverState.REROUTED,
                FailoverState.RECOVERY_IN_PROGRESS,
                FailoverState.RECOVERY_VERIFICATION
        ));

        for (FailoverEvent event : activeEvents) {
            try {
                nodeRecoveryService.processRecoveryTick(event);
            } catch (Exception e) {
                log.error("Error processing recovery tick for event {}: {}", event.getEventId(), e.getMessage());
            }
        }
    }

    public void evaluateNode(NetworkNode node, NodeHealthResponse health, HeartbeatStatusResponse heartbeat) {
        String nodeId = node.getNodeId();
        boolean isOffline = (node.getStatus() == NodeStatus.OFFLINE);
        boolean isFailedStatus = (node.getStatus() == NodeStatus.FAILED);
        boolean isUnreachable = (heartbeat != null && heartbeat.getLiveness() == LivenessStatus.UNREACHABLE);
        boolean isHealthFailed = (health != null && health.getHealthStatus() == HealthClassification.FAILED);

        // If node is undergoing active recovery or already isolated, skip duplicate detection
        if (orchestrator.isNodeIsolatedOrFailing(nodeId)) {
            return;
        }

        if (isOffline || isFailedStatus || isUnreachable || isHealthFailed) {
            int count = consecutiveFailuresMap.merge(nodeId, 1, Integer::sum);
            int required = policyService.getConsecutiveFailuresRequired();

            FailoverTrigger trigger;
            String reason;

            if (isOffline) {
                trigger = FailoverTrigger.NODE_OFFLINE;
                reason = "Node administratively OFFLINE";
            } else if (isUnreachable && isHealthFailed) {
                trigger = FailoverTrigger.MULTI_SIGNAL_FAILURE;
                reason = String.format("Multi-signal failure: Heartbeat UNREACHABLE and Health FAILED (%s)",
                        health != null ? health.getReason() : "No telemetry");
            } else if (isUnreachable) {
                trigger = FailoverTrigger.HEARTBEAT_UNREACHABLE;
                reason = "Heartbeat timeout exceeded - node unreachable";
            } else {
                trigger = FailoverTrigger.HEALTH_FAILED;
                reason = "Critical health degradation: " + (health != null ? health.getReason() : "Health FAILED");
            }

            log.warn("Failure signal detected on node {} (Count: {}/{}, Trigger: {})", nodeId, count, required, trigger);

            // Immediate trigger for OFFLINE or when consecutive failures reach threshold
            if (isOffline || count >= required) {
                consecutiveFailuresMap.remove(nodeId);
                orchestrator.handleConfirmedFailure(nodeId, trigger, reason);
            }
        } else {
            // Node is healthy - reset failure count
            consecutiveFailuresMap.remove(nodeId);
            if (orchestrator.getNodeFailoverState(nodeId) != FailoverState.HEALTHY &&
                    !orchestrator.isNodeIsolatedOrFailing(nodeId)) {
                orchestrator.updateNodeState(nodeId, FailoverState.HEALTHY);
            }
        }
    }

    public int getConsecutiveFailures(String nodeId) {
        return consecutiveFailuresMap.getOrDefault(nodeId, 0);
    }

    public void resetConsecutiveFailures(String nodeId) {
        consecutiveFailuresMap.remove(nodeId);
    }
}



