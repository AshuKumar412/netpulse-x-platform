package com.netpulse.failover.service;

import com.netpulse.failover.dto.ManualRecoveryResponse;
import com.netpulse.failover.dto.RetryFailoverResponse;
import com.netpulse.failover.entity.FailoverEvent;
import com.netpulse.failover.repository.FailoverEventRepository;
import com.netpulse.failover.state.FailoverState;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NodeRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(NodeRecoveryService.class);

    private final NodeRepository nodeRepository;
    private final HeartbeatService heartbeatService;
    private final FailoverEventRepository eventRepository;
    private final FailoverAuditService auditService;
    private final RecoveryVerificationService verificationService;
    private final FailoverPolicyService policyService;

    // nodeId -> cooldown expiration timestamp
    private final Map<String, Instant> cooldownMap = new ConcurrentHashMap<>();
    // nodeId -> attempt count
    private final Map<String, Integer> attemptMap = new ConcurrentHashMap<>();

    public NodeRecoveryService(NodeRepository nodeRepository,
                               HeartbeatService heartbeatService,
                               FailoverEventRepository eventRepository,
                               FailoverAuditService auditService,
                               RecoveryVerificationService verificationService,
                               FailoverPolicyService policyService) {
        this.nodeRepository = nodeRepository;
        this.heartbeatService = heartbeatService;
        this.eventRepository = eventRepository;
        this.auditService = auditService;
        this.verificationService = verificationService;
        this.policyService = policyService;
    }

    public boolean isInCooldown(String nodeId) {
        Instant expiry = cooldownMap.get(nodeId);
        return expiry != null && Instant.now().isBefore(expiry);
    }

    public void setCooldown(String nodeId) {
        int cooldownSec = policyService.getCooldownSeconds();
        cooldownMap.put(nodeId, Instant.now().plus(Duration.ofSeconds(cooldownSec)));
    }

    public void clearCooldown(String nodeId) {
        cooldownMap.remove(nodeId);
    }

    public int getAttemptCount(String nodeId) {
        return attemptMap.getOrDefault(nodeId, 0);
    }

    public int incrementAttempt(String nodeId) {
        return attemptMap.merge(nodeId, 1, Integer::sum);
    }

    public void resetAttempt(String nodeId) {
        attemptMap.remove(nodeId);
    }

    @Transactional
    public ManualRecoveryResponse initiateManualRecovery(String nodeId, String reason, boolean force) {
        NetworkNode node = nodeRepository.findByNodeId(nodeId).orElse(null);
        if (node == null) {
            return new ManualRecoveryResponse(nodeId, null, null, FailoverState.RECOVERY_FAILED, false, "Node not found");
        }

        if (isInCooldown(nodeId) && !force) {
            return new ManualRecoveryResponse(nodeId, null, null, FailoverState.RECOVERY_IN_PROGRESS, false,
                    "Node is currently under cooldown protection. Use force=true to override.");
        }

        clearCooldown(nodeId);
        resetAttempt(nodeId);
        verificationService.resetVerification(nodeId);

        // Un-suppress heartbeat if suppressed
        if (heartbeatService.isHeartbeatSuppressed(nodeId)) {
            heartbeatService.suppressHeartbeat(nodeId, false);
        }

        // Restore node operational status from FAILED/OFFLINE to HEALTHY
        if (node.getStatus() == NodeStatus.FAILED || node.getStatus() == NodeStatus.OFFLINE) {
            node.setStatus(NodeStatus.HEALTHY);
            nodeRepository.save(node);
        }

        heartbeatService.recordHeartbeat(nodeId);

        String eventId = auditService.generateEventId();
        String cycleId = auditService.generateRecoveryCycleId();

        FailoverEvent event = new FailoverEvent(
                eventId,
                cycleId,
                nodeId,
                node.getName(),
                null,
                null,
                com.netpulse.failover.state.FailoverTrigger.MANUAL_TRIGGER,
                FailoverState.FAILURE_CONFIRMED,
                FailoverState.RECOVERY_IN_PROGRESS,
                0, 0, 0,
                1,
                "Operator manual recovery initiated: " + (reason != null ? reason : "Standard operator command"),
                Instant.now(),
                null,
                null,
                null
        );

        auditService.recordEvent(event);
        log.info("Manual recovery initiated for node {} with eventId {} and cycleId {}", nodeId, eventId, cycleId);

        return new ManualRecoveryResponse(nodeId, eventId, cycleId, FailoverState.RECOVERY_IN_PROGRESS, true,
                "Node recovery process initiated and transitioned to RECOVERY_IN_PROGRESS");
    }

    @Transactional
    public RetryFailoverResponse retryRecovery(String eventId) {
        FailoverEvent event = eventRepository.findByEventId(eventId).orElse(null);
        if (event == null) {
            return new RetryFailoverResponse(eventId, null, null, 0, FailoverState.RECOVERY_FAILED, false, "Event not found");
        }

        String nodeId = event.getFailureNodeId();
        int attempts = incrementAttempt(nodeId);
        int maxAttempts = policyService.getMaxAttempts();

        if (attempts > maxAttempts) {
            event.setCurrentState(FailoverState.RECOVERY_FAILED);
            event.setCompletedAt(Instant.now());
            event.setSuccess(false);
            event.setFailureReason(String.format("Exceeded maximum retry attempts (%d/%d)", attempts, maxAttempts));
            auditService.recordEvent(event);
            return new RetryFailoverResponse(eventId, event.getRecoveryCycleId(), nodeId, attempts, FailoverState.RECOVERY_FAILED, false,
                    String.format("Maximum recovery attempts exceeded (%d/%d). Retry aborted.", attempts, maxAttempts));
        }

        // Remove suppression & restore node status
        heartbeatService.suppressHeartbeat(nodeId, false);
        nodeRepository.findByNodeId(nodeId).ifPresent(n -> {
            n.setStatus(NodeStatus.HEALTHY);
            nodeRepository.save(n);
        });
        heartbeatService.recordHeartbeat(nodeId);
        verificationService.resetVerification(nodeId);

        event.setAttemptNumber(attempts);
        event.setPreviousState(event.getCurrentState());
        event.setCurrentState(FailoverState.RECOVERY_IN_PROGRESS);
        event.setReason(event.getReason() + " | Retry attempt #" + attempts);
        event.setFailureReason(null);
        event.setSuccess(null);
        auditService.recordEvent(event);

        return new RetryFailoverResponse(eventId, event.getRecoveryCycleId(), nodeId, attempts, FailoverState.RECOVERY_IN_PROGRESS, true,
                "Recovery retry attempt #" + attempts + " started");
    }

    @Transactional
    public void processRecoveryTick(FailoverEvent event) {
        if (event == null || event.getCompletedAt() != null) {
            return;
        }

        String nodeId = event.getFailureNodeId();
        FailoverState state = event.getCurrentState();

        if (state != FailoverState.RECOVERY_IN_PROGRESS && state != FailoverState.RECOVERY_VERIFICATION && state != FailoverState.REROUTED) {
            return;
        }

        // Advance from REROUTED / RECOVERY_IN_PROGRESS to RECOVERY_VERIFICATION
        if (state == FailoverState.REROUTED || state == FailoverState.RECOVERY_IN_PROGRESS) {
            // Un-suppress heartbeat if auto-recover is enabled
            if (policyService.isAutoRecover() && heartbeatService.isHeartbeatSuppressed(nodeId)) {
                heartbeatService.suppressHeartbeat(nodeId, false);
            }
            nodeRepository.findByNodeId(nodeId).ifPresent(node -> {
                if (node.getStatus() == NodeStatus.FAILED) {
                    node.setStatus(NodeStatus.HEALTHY);
                    nodeRepository.save(node);
                }
            });

            event.setPreviousState(state);
            event.setCurrentState(FailoverState.RECOVERY_VERIFICATION);
            event.setReason(event.getReason() + " -> Health verification initiated");
            auditService.recordEvent(event);
            return;
        }

        // Check health verification
        if (state == FailoverState.RECOVERY_VERIFICATION) {
            RecoveryVerificationService.VerificationResult result = verificationService.checkNodeRecovery(nodeId);
            if (result.isVerified()) {
                // Recovery complete!
                event.setPreviousState(FailoverState.RECOVERY_VERIFICATION);
                event.setCurrentState(FailoverState.RESTORED);
                event.setCompletedAt(Instant.now());
                event.setSuccess(true);
                event.setReason(event.getReason() + " -> RECOVERED & RESTORED: " + result.getDetails());
                auditService.recordEvent(event);

                // Set cooldown
                setCooldown(nodeId);
                resetAttempt(nodeId);
                log.info("Node {} successfully recovered and restored. Event: {}", nodeId, event.getEventId());
            } else {
                int attempts = getAttemptCount(nodeId);
                int maxAttempts = policyService.getMaxAttempts();
                if (attempts > maxAttempts) {
                    event.setPreviousState(FailoverState.RECOVERY_VERIFICATION);
                    event.setCurrentState(FailoverState.RECOVERY_FAILED);
                    event.setCompletedAt(Instant.now());
                    event.setSuccess(false);
                    event.setFailureReason("Recovery verification failed: " + result.getDetails());
                    auditService.recordEvent(event);
                    setCooldown(nodeId);
                    log.warn("Recovery failed for node {} after exceeding max attempts: {}", nodeId, result.getDetails());
                } else {
                    event.setReason(event.getReason() + " | " + result.getDetails());
                    auditService.recordEvent(event);
                }
            }
        }
    }
}



