package com.netpulse.failover.service;

import com.netpulse.failover.dto.FailoverEventDto;
import com.netpulse.failover.entity.FailoverEvent;
import com.netpulse.failover.state.FailoverState;
import com.netpulse.failover.state.FailoverTrigger;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.routing.dto.CandidateEvaluationDto;
import com.netpulse.routing.dto.RoutingDecisionResponse;
import com.netpulse.routing.dto.RoutingRequest;
import com.netpulse.routing.entity.RoutingStrategyType;
import com.netpulse.routing.service.RoutingCandidateService;
import com.netpulse.routing.service.RoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class FailoverOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(FailoverOrchestrator.class);

    private final NodeRepository nodeRepository;
    private final RoutingService routingService;
    private final RoutingCandidateService candidateService;
    private final TrafficRecoveryService trafficRecoveryService;
    private final NodeRecoveryService nodeRecoveryService;
    private final FailoverAuditService auditService;
    private final FailoverPolicyService policyService;
    private final StringRedisTemplate redisTemplate;

    // nodeId -> FailoverState
    private final Map<String, FailoverState> nodeFailoverStateMap = new ConcurrentHashMap<>();

    public FailoverOrchestrator(NodeRepository nodeRepository,
                                RoutingService routingService,
                                RoutingCandidateService candidateService,
                                TrafficRecoveryService trafficRecoveryService,
                                NodeRecoveryService nodeRecoveryService,
                                FailoverAuditService auditService,
                                FailoverPolicyService policyService,
                                StringRedisTemplate redisTemplate) {
        this.nodeRepository = nodeRepository;
        this.routingService = routingService;
        this.candidateService = candidateService;
        this.trafficRecoveryService = trafficRecoveryService;
        this.nodeRecoveryService = nodeRecoveryService;
        this.auditService = auditService;
        this.policyService = policyService;
        this.redisTemplate = redisTemplate;
    }

    public FailoverState getNodeFailoverState(String nodeId) {
        return nodeFailoverStateMap.getOrDefault(nodeId, FailoverState.HEALTHY);
    }

    public Map<String, FailoverState> getAllNodeFailoverStates() {
        return Collections.unmodifiableMap(nodeFailoverStateMap);
    }

    public boolean isNodeIsolatedOrFailing(String nodeId) {
        FailoverState state = getNodeFailoverState(nodeId);
        return state == FailoverState.FAILURE_CONFIRMED ||
                state == FailoverState.ISOLATING ||
                state == FailoverState.FAILOVER_IN_PROGRESS ||
                state == FailoverState.REROUTED ||
                state == FailoverState.RECOVERY_IN_PROGRESS ||
                state == FailoverState.RECOVERY_VERIFICATION;
    }

    private boolean acquireLock(String nodeId) {
        if (redisTemplate != null) {
            try {
                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent("failover:lock:" + nodeId, "LOCKED", Duration.ofSeconds(30));
                return Boolean.TRUE.equals(acquired);
            } catch (Exception e) {
                log.debug("Redis lock skipped, using in-memory lock: {}", e.getMessage());
            }
        }
        return true;
    }

    private void releaseLock(String nodeId) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete("failover:lock:" + nodeId);
            } catch (Exception e) {
                log.debug("Redis lock release skipped: {}", e.getMessage());
            }
        }
    }

    @Transactional
    public synchronized FailoverEventDto handleConfirmedFailure(String failedNodeId, FailoverTrigger trigger, String detectionReason) {
        if (!policyService.isEnabled()) {
            log.info("Failover engine disabled by policy. Skipping failover for node {}", failedNodeId);
            return null;
        }

        FailoverState currentState = getNodeFailoverState(failedNodeId);
        // Idempotency: Prevent duplicate simultaneous failover executions for the same node
        if (isNodeIsolatedOrFailing(failedNodeId)) {
            log.debug("Node {} is already in failover/recovery state ({}) - duplicate trigger skipped", failedNodeId, currentState);
            return null;
        }

        if (nodeRecoveryService.isInCooldown(failedNodeId)) {
            log.info("Node {} is under cooldown protection - failover skipped", failedNodeId);
            return null;
        }

        if (!acquireLock(failedNodeId)) {
            log.warn("Could not acquire failover lock for node {}. Another process is managing failover.", failedNodeId);
            return null;
        }

        String eventId = auditService.generateEventId();
        String cycleId = auditService.generateRecoveryCycleId();
        Instant startedAt = Instant.now();

        NetworkNode failedNode = nodeRepository.findByNodeId(failedNodeId).orElse(null);
        String failedNodeName = (failedNode != null) ? failedNode.getName() : failedNodeId;

        try {
            // Step 1: Confirm failure and Isolate node
            nodeFailoverStateMap.put(failedNodeId, FailoverState.FAILURE_CONFIRMED);
            log.info("[{}] Failure confirmed on node {} ({}). Reason: {}", cycleId, failedNodeId, trigger, detectionReason);

            nodeFailoverStateMap.put(failedNodeId, FailoverState.ISOLATING);
            nodeRecoveryService.incrementAttempt(failedNodeId);

            // Step 2: Identify affected active traffic sessions
            var affectedSessions = trafficRecoveryService.getAffectedSessions(failedNodeId);
            int affectedCount = affectedSessions.size();

            // Step 3: Select replacement node using existing Phase 4 RoutingService
            List<CandidateEvaluationDto> allCandidates = candidateService.evaluateAllCandidates();
            List<CandidateEvaluationDto> eligibleCandidates = allCandidates.stream()
                    .filter(c -> !c.getNodeId().equalsIgnoreCase(failedNodeId))
                    .filter(CandidateEvaluationDto::getEligible)
                    .collect(Collectors.toList());

            if (eligibleCandidates.isEmpty()) {
                // No eligible replacement node exists
                log.warn("[{}] No eligible replacement candidate available for failover from node {}", cycleId, failedNodeId);
                nodeFailoverStateMap.put(failedNodeId, FailoverState.FAILOVER_FAILED);

                FailoverEvent failedEvent = new FailoverEvent(
                        eventId,
                        cycleId,
                        failedNodeId,
                        failedNodeName,
                        null,
                        null,
                        trigger,
                        FailoverState.ISOLATING,
                        FailoverState.FAILOVER_FAILED,
                        affectedCount,
                        0,
                        affectedCount,
                        nodeRecoveryService.getAttemptCount(failedNodeId),
                        detectionReason + " | Failover aborted: NO_ELIGIBLE_REPLACEMENT available in network",
                        startedAt,
                        Instant.now(),
                        false,
                        "NO_ELIGIBLE_REPLACEMENT"
                );

                auditService.recordEvent(failedEvent);
                nodeRecoveryService.setCooldown(failedNodeId);
                return FailoverEventDto.fromEntity(failedEvent);
            }

            // Route replacement using current active routing strategy
            RoutingRequest simRequest = new RoutingRequest(
                    routingService.getActiveStrategy(),
                    failedNodeId,
                    "TRAFFIC_MIGRATION"
            );
            RoutingDecisionResponse replacementDecision = routingService.decideRouting(simRequest);

            // If the decided node happens to be the failed node (edge case), select the best eligible from filtered list
            String replacementNodeId = replacementDecision.getSelectedNodeId();
            String replacementNodeName = replacementDecision.getSelectedNodeName();
            if (replacementNodeId == null || replacementNodeId.equalsIgnoreCase(failedNodeId)) {
                CandidateEvaluationDto fallback = eligibleCandidates.getFirst();
                replacementNodeId = fallback.getNodeId();
                replacementNodeName = fallback.getName();
            }

            // Step 4: Progress to FAILOVER_IN_PROGRESS & Reroute Traffic Sessions
            nodeFailoverStateMap.put(failedNodeId, FailoverState.FAILOVER_IN_PROGRESS);
            TrafficRecoveryService.RerouteResult rerouteResult = trafficRecoveryService.rerouteSessions(
                    failedNodeId, replacementNodeId, cycleId);

            nodeFailoverStateMap.put(failedNodeId, FailoverState.REROUTED);

            String reason = String.format("%s | Replaced by %s via %s routing (%s)",
                    detectionReason, replacementNodeName, routingService.getActiveStrategy(),
                    replacementDecision.getDecisionReason());

            FailoverEvent event = new FailoverEvent(
                    eventId,
                    cycleId,
                    failedNodeId,
                    failedNodeName,
                    replacementNodeId,
                    replacementNodeName,
                    trigger,
                    FailoverState.ISOLATING,
                    FailoverState.REROUTED,
                    rerouteResult.getAffectedCount(),
                    rerouteResult.getReroutedCount(),
                    rerouteResult.getUnrecoveredCount(),
                    nodeRecoveryService.getAttemptCount(failedNodeId),
                    reason,
                    startedAt,
                    null,
                    null,
                    null
            );

            FailoverEvent recorded = auditService.recordEvent(event);

            // Step 5: Advance state to RECOVERY_IN_PROGRESS if recovery enabled
            if (policyService.isRecoveryEnabled()) {
                nodeFailoverStateMap.put(failedNodeId, FailoverState.RECOVERY_IN_PROGRESS);
                recorded.setPreviousState(FailoverState.REROUTED);
                recorded.setCurrentState(FailoverState.RECOVERY_IN_PROGRESS);
                auditService.recordEvent(recorded);
            }

            log.info("[{}] Failover rerouted successfully: {} -> {}. Event ID: {}",
                    cycleId, failedNodeId, replacementNodeId, eventId);

            return FailoverEventDto.fromEntity(recorded);

        } finally {
            releaseLock(failedNodeId);
        }
    }

    public void updateNodeState(String nodeId, FailoverState state) {
        nodeFailoverStateMap.put(nodeId, state);
    }
}

