package com.netpulse.simulation.service;

import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.simulation.dto.SimulationStatusResponse;
import com.netpulse.topology.entity.LinkStatus;
import com.netpulse.topology.entity.NetworkLink;
import com.netpulse.topology.repository.NetworkLinkRepository;
import com.netpulse.topology.service.TopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class NetworkSimulationService {

    private static final Logger log = LoggerFactory.getLogger(NetworkSimulationService.class);

    private volatile String status = "STOPPED"; // STOPPED, RUNNING, PAUSED
    private final AtomicLong tickCount = new AtomicLong(0);
    private volatile Instant startedAt;
    private volatile Instant lastTickAt;

    private final NodeRepository nodeRepository;
    private final NetworkLinkRepository linkRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TopologyService topologyService;

    public NetworkSimulationService(NodeRepository nodeRepository,
                                    NetworkLinkRepository linkRepository,
                                    SimpMessagingTemplate messagingTemplate,
                                    @Lazy TopologyService topologyService) {
        this.nodeRepository = nodeRepository;
        this.linkRepository = linkRepository;
        this.messagingTemplate = messagingTemplate;
        this.topologyService = topologyService;
    }

    public synchronized SimulationStatusResponse startSimulation() {
        if (!"RUNNING".equals(this.status)) {
            this.status = "RUNNING";
            if (this.startedAt == null) {
                this.startedAt = Instant.now();
            }
            this.lastTickAt = Instant.now();
            log.info("Network topology simulation engine started");
            broadcastSimulationStatus();
            evaluateTopologyState();
        }
        return getSimulationStatus();
    }

    public synchronized SimulationStatusResponse pauseSimulation() {
        if ("RUNNING".equals(this.status)) {
            this.status = "PAUSED";
            log.info("Network topology simulation engine paused at tick {}", tickCount.get());
            broadcastSimulationStatus();
        }
        return getSimulationStatus();
    }

    public synchronized SimulationStatusResponse stopSimulation() {
        this.status = "STOPPED";
        this.startedAt = null;
        this.lastTickAt = null;
        this.tickCount.set(0);
        log.info("Network topology simulation engine stopped and reset");
        broadcastSimulationStatus();
        return getSimulationStatus();
    }

    public SimulationStatusResponse getSimulationStatus() {
        long uptime = 0;
        if (startedAt != null) {
            uptime = Duration.between(startedAt, Instant.now()).getSeconds();
        }
        long totalNodes = nodeRepository.count();
        long totalLinks = linkRepository.count();

        return new SimulationStatusResponse(
                this.status,
                this.tickCount.get(),
                uptime,
                totalNodes,
                totalLinks,
                this.startedAt,
                this.lastTickAt
        );
    }

    @Scheduled(fixedRate = 3000)
    public void scheduledSimulationTick() {
        if ("RUNNING".equals(this.status)) {
            tickCount.incrementAndGet();
            this.lastTickAt = Instant.now();
            evaluateTopologyState();
            broadcastSimulationStatus();
        }
    }

    @Transactional
    public void evaluateTopologyState() {
        List<NetworkNode> nodes = nodeRepository.findAll();
        List<NetworkLink> links = linkRepository.findAll();

        Map<String, NodeStatus> nodeStatusMap = nodes.stream()
                .collect(Collectors.toMap(NetworkNode::getNodeId, NetworkNode::getStatus, (s1, s2) -> s1));

        boolean stateChanged = false;

        for (NetworkLink link : links) {
            if (!Boolean.TRUE.equals(link.getEnabled())) {
                if (link.getStatus() != LinkStatus.OFFLINE) {
                    link.setStatus(LinkStatus.OFFLINE);
                    linkRepository.save(link);
                    stateChanged = true;
                }
                continue;
            }

            NodeStatus srcStatus = nodeStatusMap.get(link.getSourceNodeId());
            NodeStatus tgtStatus = nodeStatusMap.get(link.getTargetNodeId());

            LinkStatus expectedStatus;
            if (srcStatus == null || tgtStatus == null || srcStatus == NodeStatus.OFFLINE || tgtStatus == NodeStatus.OFFLINE) {
                expectedStatus = LinkStatus.OFFLINE;
            } else if (srcStatus == NodeStatus.FAILED || tgtStatus == NodeStatus.FAILED) {
                expectedStatus = LinkStatus.FAILED;
            } else if (srcStatus == NodeStatus.CONGESTED || tgtStatus == NodeStatus.CONGESTED || srcStatus == NodeStatus.WARNING || tgtStatus == NodeStatus.WARNING) {
                expectedStatus = LinkStatus.DEGRADED;
            } else {
                expectedStatus = LinkStatus.ACTIVE;
            }

            if (link.getStatus() != expectedStatus) {
                link.setStatus(expectedStatus);
                linkRepository.save(link);
                stateChanged = true;
            }
        }

        if (stateChanged && topologyService != null) {
            topologyService.broadcastTopologyChange();
        }
    }

    private void broadcastSimulationStatus() {
        try {
            messagingTemplate.convertAndSend("/topic/simulation", getSimulationStatus());
        } catch (Exception e) {
            // Safe fallback when websocket is idle
        }
    }
}
