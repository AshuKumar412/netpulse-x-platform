package com.netpulse.routing.service;

import com.netpulse.failover.service.TrafficRecoveryService;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.routing.dto.*;
import com.netpulse.routing.entity.RoutingDecisionRecord;
import com.netpulse.routing.entity.RoutingStrategyType;
import com.netpulse.routing.repository.RoutingDecisionRepository;
import com.netpulse.routing.strategy.RoutingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

    private final Map<RoutingStrategyType, RoutingStrategy> strategyMap = new EnumMap<>(RoutingStrategyType.class);
    private final RoutingCandidateService candidateService;
    private final RoutingDecisionRepository decisionRepository;
    private final NodeRepository nodeRepository;
    private final TrafficRecoveryService trafficRecoveryService;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    private volatile RoutingStrategyType activeStrategy = RoutingStrategyType.ADAPTIVE;
    private final AtomicLong decisionCounter = new AtomicLong(0);
    private final Instant startedAt = Instant.now();
    private volatile Instant lastDecisionAt;

    @Value("${app.routing.weights.cpu:0.20}")
    private double weightCpu = 0.20;
    @Value("${app.routing.weights.memory:0.15}")
    private double weightMemory = 0.15;
    @Value("${app.routing.weights.latency:0.20}")
    private double weightLatency = 0.20;
    @Value("${app.routing.weights.packet-loss:0.10}")
    private double weightPacketLoss = 0.10;
    @Value("${app.routing.weights.error-rate:0.10}")
    private double weightErrorRate = 0.10;
    @Value("${app.routing.weights.connections:0.15}")
    private double weightConnections = 0.15;
    @Value("${app.routing.weights.capacity:0.10}")
    private double weightCapacity = 0.10;

    public RoutingService(List<RoutingStrategy> strategies,
                          RoutingCandidateService candidateService,
                          RoutingDecisionRepository decisionRepository,
                          NodeRepository nodeRepository,
                          TrafficRecoveryService trafficRecoveryService,
                          SimpMessagingTemplate messagingTemplate,
                          StringRedisTemplate redisTemplate,
                          @Value("${app.routing.default-strategy:ADAPTIVE}") String defaultStrategyName) {
        for (RoutingStrategy s : strategies) {
            this.strategyMap.put(s.getType(), s);
        }
        this.candidateService = candidateService;
        this.decisionRepository = decisionRepository;
        this.nodeRepository = nodeRepository;
        this.trafficRecoveryService = trafficRecoveryService;
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;

        try {
            this.activeStrategy = RoutingStrategyType.valueOf(defaultStrategyName.trim().toUpperCase());
        } catch (Exception e) {
            this.activeStrategy = RoutingStrategyType.ADAPTIVE;
        }
    }

    public RoutingDecisionResponse decideRouting(RoutingRequest request) {
        String requestId = generateRequestId();
        RoutingStrategyType strategyType = (request != null && request.getStrategy() != null)
                ? request.getStrategy()
                : this.activeStrategy;

        RoutingStrategy strategy = strategyMap.get(strategyType);
        if (strategy == null) {
            strategy = strategyMap.get(RoutingStrategyType.ADAPTIVE);
            strategyType = RoutingStrategyType.ADAPTIVE;
        }

        List<CandidateEvaluationDto> allCandidates = candidateService.evaluateAllCandidates();
        List<CandidateEvaluationDto> eligibleCandidates = allCandidates.stream()
                .filter(CandidateEvaluationDto::getEligible)
                .collect(Collectors.toList());

        return strategy.route(requestId, allCandidates, eligibleCandidates);
    }

    @Transactional
    public RoutingDecisionResponse executeAndRecordRouting(RoutingRequest request) {
        RoutingDecisionResponse decision = decideRouting(request);
        decisionCounter.incrementAndGet();
        this.lastDecisionAt = Instant.now();

        // Persist decision in PostgreSQL
        RoutingDecisionRecord record = new RoutingDecisionRecord(
                decision.getRequestId(),
                decision.getStrategy(),
                decision.getSelectedNodeId(),
                decision.getSelectedNodeName(),
                decision.getTimestamp(),
                decision.getTotalCandidates(),
                decision.getEligibleCandidates(),
                decision.getDecisionReason(),
                decision.getScore()
        );
        decisionRepository.save(record);

        // Register active traffic session for failover tracking if selected node is present
        if (decision.getSelectedNodeId() != null && trafficRecoveryService != null) {
            try {
                String reqType = (request != null && request.getServiceType() != null) ? request.getServiceType() : "STANDARD";
                trafficRecoveryService.registerSession(
                        decision.getRequestId(),
                        decision.getSelectedNodeId(),
                        reqType,
                        decision.getStrategy().name()
                );
            } catch (Exception e) {
                log.debug("Session registration skipped: {}", e.getMessage());
            }
        }

        // Sync latest decision to Redis if available
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set("routing:latest:decision", decision.getRequestId(), Duration.ofHours(1));
            } catch (Exception e) {
                log.debug("Redis decision cache skipped: {}", e.getMessage());
            }
        }

        // Broadcast decision to WebSocket /topic/routing
        broadcastDecision(decision);

        return decision;
    }

    public RoutingStrategyType getActiveStrategy() {
        return this.activeStrategy;
    }

    public synchronized void setActiveStrategy(RoutingStrategyType strategy) {
        if (strategy != null && strategyMap.containsKey(strategy)) {
            this.activeStrategy = strategy;
            log.info("Active traffic routing strategy updated to {}", strategy);
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set("routing:strategy:active", strategy.name());
                } catch (Exception e) {
                    log.debug("Redis strategy cache skipped: {}", e.getMessage());
                }
            }
        }
    }

    public RoutingStatusResponse getStatus() {
        long uptime = Duration.between(startedAt, Instant.now()).getSeconds();
        int nodeCount = (int) nodeRepository.count();
        List<RoutingStrategyType> supported = new ArrayList<>(strategyMap.keySet());

        return new RoutingStatusResponse(
                this.activeStrategy,
                this.decisionCounter.get(),
                uptime,
                nodeCount,
                supported,
                this.lastDecisionAt
        );
    }

    public RoutingConfigResponse getConfig() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("cpu", weightCpu);
        weights.put("memory", weightMemory);
        weights.put("latency", weightLatency);
        weights.put("packetLoss", weightPacketLoss);
        weights.put("errorRate", weightErrorRate);
        weights.put("connections", weightConnections);
        weights.put("capacity", weightCapacity);

        return new RoutingConfigResponse(
                this.activeStrategy,
                candidateService.isAllowWarning(),
                candidateService.isAllowDegraded(),
                candidateService.isAllowSuspected(),
                weights
        );
    }

    public List<RoutingDecisionRecord> getRecentHistory(int limit) {
        int boundedLimit = Math.max(1, Math.min(100, limit));
        return decisionRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, boundedLimit));
    }

    private String generateRequestId() {
        long count = decisionCounter.get() + 1;
        String date = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(java.time.ZoneOffset.UTC).format(Instant.now());
        return String.format("REQ-%s-%06d", date, count % 1000000);
    }

    private void broadcastDecision(RoutingDecisionResponse decision) {
        try {
            messagingTemplate.convertAndSend("/topic/routing", decision);
        } catch (Exception e) {
            log.debug("WebSocket routing broadcast skipped: {}", e.getMessage());
        }
    }
}
