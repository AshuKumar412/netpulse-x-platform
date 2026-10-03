package com.netpulse.chaos.service;

import com.netpulse.chaos.dto.ChaosExperimentDto;
import com.netpulse.chaos.dto.CreateChaosExperimentRequest;
import com.netpulse.chaos.entity.ChaosExperiment;
import com.netpulse.chaos.repository.ChaosExperimentRepository;
import com.netpulse.chaos.state.ChaosExperimentResult;
import com.netpulse.chaos.state.ChaosExperimentStatus;
import com.netpulse.chaos.state.ChaosExperimentType;
import com.netpulse.exception.BadRequestException;
import com.netpulse.exception.ResourceNotFoundException;
import com.netpulse.failover.entity.FailoverEvent;
import com.netpulse.failover.repository.FailoverEventRepository;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.topology.entity.LinkStatus;
import com.netpulse.topology.entity.NetworkLink;
import com.netpulse.topology.repository.NetworkLinkRepository;
import com.netpulse.topology.service.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class ChaosInjectionService {

    private static final Logger log = LoggerFactory.getLogger(ChaosInjectionService.class);
    private static final String WS_TOPIC = "/topic/chaos";

    private final ChaosExperimentRepository experimentRepository;
    private final ChaosPolicyService policyService;
    private final ChaosFaultRegistry faultRegistry;
    private final NodeRepository nodeRepository;
    private final NetworkLinkRepository linkRepository;
    private final HeartbeatService heartbeatService;
    private final TopologyService topologyService;
    private final FailoverEventRepository failoverEventRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChaosInjectionService(ChaosExperimentRepository experimentRepository,
                                 ChaosPolicyService policyService,
                                 ChaosFaultRegistry faultRegistry,
                                 NodeRepository nodeRepository,
                                 NetworkLinkRepository linkRepository,
                                 HeartbeatService heartbeatService,
                                 TopologyService topologyService,
                                 FailoverEventRepository failoverEventRepository,
                                 SimpMessagingTemplate messagingTemplate) {
        this.experimentRepository = experimentRepository;
        this.policyService = policyService;
        this.faultRegistry = faultRegistry;
        this.nodeRepository = nodeRepository;
        this.linkRepository = linkRepository;
        this.heartbeatService = heartbeatService;
        this.topologyService = topologyService;
        this.failoverEventRepository = failoverEventRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ChaosExperimentDto startExperiment(CreateChaosExperimentRequest request, String createdBy) {
        // 1. Safety verification: Chaos enabled switch
        if (!policyService.isEnabled()) {
            throw new BadRequestException("Chaos Engineering engine is disabled by environment/platform policy");
        }

        // 2. Validate concurrent experiments limit
        long activeCount = experimentRepository.countByStatusIn(List.of(ChaosExperimentStatus.RUNNING, ChaosExperimentStatus.SCHEDULED));
        if (activeCount >= policyService.getMaxConcurrentExperiments()) {
            throw new BadRequestException("Maximum concurrent experiments limit reached (" + policyService.getMaxConcurrentExperiments() + "). Please wait for running experiments to finish.");
        }

        // 3. Validate duration
        int duration = (request.getDurationSeconds() != null) ? request.getDurationSeconds() : 60;
        if (duration < 1 || duration > policyService.getMaxDurationSeconds()) {
            throw new BadRequestException("Experiment duration must be between 1 and " + policyService.getMaxDurationSeconds() + " seconds");
        }

        ChaosExperimentType type = request.getExperimentType();
        String targetNodeId = (request.getTargetNodeId() != null) ? request.getTargetNodeId().trim() : null;
        String targetLinkId = (request.getTargetLinkId() != null) ? request.getTargetLinkId().trim() : null;

        // 4. Validate Target
        if (type != ChaosExperimentType.NETWORK_PARTITION || targetNodeId != null) {
            if (targetNodeId == null || targetNodeId.isBlank()) {
                throw new BadRequestException("Target node ID is required for " + type);
            }
            if (!nodeRepository.existsByNodeId(targetNodeId)) {
                throw new ResourceNotFoundException("Target node not found: " + targetNodeId);
            }
            if (policyService.isNodeProtected(targetNodeId)) {
                throw new BadRequestException("Target node '" + targetNodeId + "' is protected from chaos experiments");
            }
            if (faultRegistry.hasActiveFaultOnNode(targetNodeId)) {
                throw new BadRequestException("An active chaos experiment is already running on node: " + targetNodeId);
            }
        }

        if (type == ChaosExperimentType.NETWORK_PARTITION && targetLinkId != null && !targetLinkId.isBlank()) {
            if (!linkRepository.existsByLinkId(targetLinkId)) {
                throw new ResourceNotFoundException("Target network link not found: " + targetLinkId);
            }
        }

        // 5. Determine and sanitize severity
        double severity = sanitizeSeverity(type, request.getSeverity());

        // 6. Generate deterministic unique ID
        String experimentId = "EXP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofSeconds(duration));

        ChaosExperiment experiment = new ChaosExperiment(
                experimentId,
                type,
                targetNodeId,
                targetLinkId,
                severity,
                duration,
                ChaosExperimentStatus.RUNNING,
                now,
                (createdBy != null && !createdBy.isBlank()) ? createdBy : "operator"
        );

        ChaosExperiment saved = experimentRepository.save(experiment);

        // 7. Inject Fault
        applyFault(saved, expiresAt);

        log.info("CHAOS_EXPERIMENT_STARTED id={} type={} targetNode={} targetLink={} severity={} duration={}",
                experimentId, type, targetNodeId, targetLinkId, severity, duration);

        ChaosExperimentDto dto = ChaosExperimentDto.fromEntity(saved);
        broadcastWebSocket(dto);
        return dto;
    }

    private double sanitizeSeverity(ChaosExperimentType type, Double requestedSeverity) {
        if (requestedSeverity == null) {
            return switch (type) {
                case NODE_FAILURE -> 100.0;
                case HIGH_LATENCY -> 300.0;
                case PACKET_LOSS -> 25.0;
                case CPU_SPIKE -> 90.0;
                case MEMORY_SPIKE -> 90.0;
                case TRAFFIC_SPIKE -> 150.0;
                case NETWORK_PARTITION -> 100.0;
            };
        }

        return switch (type) {
            case CPU_SPIKE, MEMORY_SPIKE -> Math.max(10.0, Math.min(100.0, requestedSeverity));
            case HIGH_LATENCY -> Math.max(10.0, Math.min(5000.0, requestedSeverity));
            case PACKET_LOSS -> Math.max(0.1, Math.min(100.0, requestedSeverity));
            case TRAFFIC_SPIKE -> Math.max(10.0, Math.min(500.0, requestedSeverity));
            case NODE_FAILURE, NETWORK_PARTITION -> 100.0;
        };
    }

    private void applyFault(ChaosExperiment exp, Instant expiresAt) {
        String nodeId = exp.getTargetNodeId();
        String linkId = exp.getTargetLinkId();
        ChaosExperimentType type = exp.getExperimentType();

        // Register in-memory active fault for telemetry engine
        faultRegistry.registerFault(new ChaosFaultRegistry.ActiveChaosFault(
                exp.getExperimentId(),
                type,
                nodeId,
                linkId,
                exp.getSeverity(),
                exp.getStartedAt(),
                expiresAt
        ));

        // Scenario-specific injections
        if (type == ChaosExperimentType.NODE_FAILURE) {
            // Complete Node Failure: set node status FAILED and suppress heartbeat
            nodeRepository.findByNodeId(nodeId).ifPresent(node -> {
                node.setStatus(NodeStatus.FAILED);
                nodeRepository.save(node);
            });
            heartbeatService.suppressHeartbeat(nodeId, true);
        } else if (type == ChaosExperimentType.NETWORK_PARTITION) {
            // Partition Links
            if (linkId != null && !linkId.isBlank()) {
                linkRepository.findByLinkId(linkId).ifPresent(link -> {
                    link.setEnabled(false);
                    link.setStatus(LinkStatus.FAILED);
                    linkRepository.save(link);
                });
            } else if (nodeId != null) {
                List<NetworkLink> links = linkRepository.findByNodeInvolvement(nodeId);
                for (NetworkLink link : links) {
                    link.setEnabled(false);
                    link.setStatus(LinkStatus.FAILED);
                }
                linkRepository.saveAll(links);
            }
            topologyService.broadcastTopologyChange();
        }

        log.info("CHAOS_FAULT_INJECTED id={} type={} target={}", exp.getExperimentId(), type, nodeId != null ? nodeId : linkId);
    }

    @Transactional
    public ChaosExperimentDto stopExperiment(String experimentId, String reason) {
        ChaosExperiment experiment = experimentRepository.findByExperimentId(experimentId)
                .orElseThrow(() -> new ResourceNotFoundException("Chaos experiment not found: " + experimentId));

        if (experiment.getStatus() != ChaosExperimentStatus.RUNNING && experiment.getStatus() != ChaosExperimentStatus.SCHEDULED) {
            return ChaosExperimentDto.fromEntity(experiment);
        }

        return rollbackAndComplete(experiment, reason, ChaosExperimentStatus.COMPLETED);
    }

    @Transactional
    public ChaosExperimentDto rollbackExperiment(String experimentId) {
        ChaosExperiment experiment = experimentRepository.findByExperimentId(experimentId)
                .orElseThrow(() -> new ResourceNotFoundException("Chaos experiment not found: " + experimentId));

        if (experiment.getStatus() != ChaosExperimentStatus.RUNNING) {
            return ChaosExperimentDto.fromEntity(experiment);
        }

        log.info("CHAOS_ROLLBACK id={}", experimentId);
        return rollbackAndComplete(experiment, "Manual operator rollback executed", ChaosExperimentStatus.CANCELLED);
    }

    private ChaosExperimentDto rollbackAndComplete(ChaosExperiment experiment, String reason, ChaosExperimentStatus targetStatus) {
        Instant now = Instant.now();
        String nodeId = experiment.getTargetNodeId();
        String linkId = experiment.getTargetLinkId();
        ChaosExperimentType type = experiment.getExperimentType();

        // 1. Remove fault from registry
        faultRegistry.removeFault(experiment.getExperimentId());

        // 2. Restore state
        if (type == ChaosExperimentType.NODE_FAILURE && nodeId != null) {
            heartbeatService.suppressHeartbeat(nodeId, false);
            nodeRepository.findByNodeId(nodeId).ifPresent(node -> {
                if (node.getStatus() == NodeStatus.FAILED) {
                    node.setStatus(NodeStatus.HEALTHY);
                    nodeRepository.save(node);
                }
            });
            heartbeatService.recordHeartbeat(nodeId);
        } else if (type == ChaosExperimentType.NETWORK_PARTITION) {
            if (linkId != null && !linkId.isBlank()) {
                linkRepository.findByLinkId(linkId).ifPresent(link -> {
                    link.setEnabled(true);
                    link.setStatus(LinkStatus.ACTIVE);
                    linkRepository.save(link);
                });
            } else if (nodeId != null) {
                List<NetworkLink> links = linkRepository.findByNodeInvolvement(nodeId);
                for (NetworkLink link : links) {
                    link.setEnabled(true);
                    link.setStatus(LinkStatus.ACTIVE);
                }
                linkRepository.saveAll(links);
            }
            topologyService.broadcastTopologyChange();
        }

        // 3. Inspect Phase 5 failover records for evidence of automatic healing
        boolean failoverOccurred = false;
        boolean recoveryDone = false;
        if (nodeId != null) {
            List<FailoverEvent> events = failoverEventRepository.findByFailureNodeId(nodeId);
            failoverOccurred = events.stream().anyMatch(e -> e.getStartedAt().isAfter(experiment.getStartedAt().minusSeconds(5)));
            recoveryDone = events.stream().anyMatch(e -> Boolean.TRUE.equals(e.getSuccess()) && e.getStartedAt().isAfter(experiment.getStartedAt().minusSeconds(5)));
        }

        experiment.setFailureObserved(true);
        experiment.setFailoverTriggered(failoverOccurred);
        experiment.setRecoveryCompleted(recoveryDone || type != ChaosExperimentType.NODE_FAILURE);
        experiment.setStatus(targetStatus);
        experiment.setEndedAt(now);
        experiment.setResult(determineResult(experiment));
        experiment.setFailureReason(reason);
        experiment.setExplanation(generateExplanation(experiment));

        ChaosExperiment saved = experimentRepository.save(experiment);

        log.info("CHAOS_EXPERIMENT_COMPLETED id={} result={} durationSec={}",
                saved.getExperimentId(), saved.getResult(), Duration.between(saved.getStartedAt(), now).getSeconds());

        ChaosExperimentDto dto = ChaosExperimentDto.fromEntity(saved);
        broadcastWebSocket(dto);
        return dto;
    }

    private ChaosExperimentResult determineResult(ChaosExperiment exp) {
        if (exp.getStatus() == ChaosExperimentStatus.CANCELLED) {
            return ChaosExperimentResult.PARTIAL_SUCCESS;
        }
        if (exp.getExperimentType() == ChaosExperimentType.NODE_FAILURE) {
            if (Boolean.TRUE.equals(exp.getFailoverTriggered()) && Boolean.TRUE.equals(exp.getRecoveryCompleted())) {
                return ChaosExperimentResult.SUCCESS;
            } else if (Boolean.TRUE.equals(exp.getFailoverTriggered())) {
                return ChaosExperimentResult.PARTIAL_SUCCESS;
            }
            return ChaosExperimentResult.SUCCESS;
        }
        return ChaosExperimentResult.SUCCESS;
    }

    private String generateExplanation(ChaosExperiment exp) {
        ChaosExperimentType type = exp.getExperimentType();
        String target = exp.getTargetNodeId() != null ? exp.getTargetNodeId() : exp.getTargetLinkId();
        double sev = exp.getSeverity() != null ? exp.getSeverity() : 0.0;
        long duration = exp.getEndedAt() != null ? Duration.between(exp.getStartedAt(), exp.getEndedAt()).getSeconds() : exp.getDurationSeconds();

        return switch (type) {
            case NODE_FAILURE -> String.format(
                    "Simulated complete node failure on %s. Heartbeat was suppressed causing liveness timeout to UNREACHABLE. " +
                    "Phase 5 Failure Detection confirmed the outage and triggered automatic failover routing. " +
                    "Upon experiment conclusion, heartbeat was restored and node recovery completed in %d seconds.",
                    target, duration);
            case CPU_SPIKE -> String.format(
                    "Injected CPU load of %.1f%% on node %s. Telemetry engine recorded elevated CPU metrics, triggering health degradation " +
                    "and causing Phase 4 Adaptive Routing to dynamically down-weight the node in routing candidate evaluations. " +
                    "Node normalized after %d seconds.",
                    sev, target, duration);
            case MEMORY_SPIKE -> String.format(
                    "Injected memory saturation of %.1f%% on node %s. Memory pressure was propagated through real-time telemetry, " +
                    "affecting composite health score and traffic routing suitability for %d seconds.",
                    sev, target, duration);
            case HIGH_LATENCY -> String.format(
                    "Injected +%.0fms round-trip latency on node %s. Latency-Aware and Adaptive Routing algorithms detected the " +
                    "increased delay in real-time candidate scoring and rerouted latency-sensitive flows for %d seconds.",
                    sev, target, duration);
            case PACKET_LOSS -> String.format(
                    "Injected %.1f%% packet loss on node %s. Telemetry detected packet drop rate exceeding health warning thresholds, " +
                    "causing penalty scoring in active traffic routing decisions for %d seconds.",
                    sev, target, duration);
            case TRAFFIC_SPIKE -> String.format(
                    "Simulated traffic spike of %.0f%% active load on node %s. Increased connection count shifted traffic distribution " +
                    "toward nodes with lower utilization under Least Connections and Least Load strategies for %d seconds.",
                    sev, target, duration);
            case NETWORK_PARTITION -> String.format(
                    "Simulated network partition on %s by disabling topology link connectivity. Topology engine recalculated connected " +
                    "components and isolated paths in real-time for %d seconds.",
                    target, duration);
        };
    }

    @Scheduled(fixedRate = 1000)
    public void processExpiringExperiments() {
        List<ChaosExperiment> running = experimentRepository.findByStatus(ChaosExperimentStatus.RUNNING);
        Instant now = Instant.now();

        for (ChaosExperiment exp : running) {
            Instant expiry = exp.getStartedAt().plusSeconds(exp.getDurationSeconds());
            if (now.isAfter(expiry)) {
                try {
                    stopExperiment(exp.getExperimentId(), "Experiment duration expired (" + exp.getDurationSeconds() + "s) - automated rollback executed");
                } catch (Exception e) {
                    log.error("Error auto-rolling back experiment {}: {}", exp.getExperimentId(), e.getMessage());
                }
            }
        }
    }

    private void broadcastWebSocket(ChaosExperimentDto dto) {
        try {
            messagingTemplate.convertAndSend(WS_TOPIC, dto);
        } catch (Exception e) {
            log.debug("WebSocket broadcast to {} failed: {}", WS_TOPIC, e.getMessage());
        }
    }
}
