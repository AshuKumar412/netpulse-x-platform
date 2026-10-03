package com.netpulse.topology.service;

import com.netpulse.exception.BadRequestException;
import com.netpulse.exception.DuplicateResourceException;
import com.netpulse.exception.ResourceNotFoundException;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.simulation.service.NetworkSimulationService;
import com.netpulse.topology.dto.*;
import com.netpulse.topology.entity.LinkStatus;
import com.netpulse.topology.entity.LinkType;
import com.netpulse.topology.entity.NetworkLink;
import com.netpulse.topology.repository.NetworkLinkRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TopologyService {

    private final NetworkLinkRepository linkRepository;
    private final NodeRepository nodeRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NetworkSimulationService simulationService;

    public TopologyService(NetworkLinkRepository linkRepository,
                           NodeRepository nodeRepository,
                           SimpMessagingTemplate messagingTemplate,
                           @Lazy NetworkSimulationService simulationService) {
        this.linkRepository = linkRepository;
        this.nodeRepository = nodeRepository;
        this.messagingTemplate = messagingTemplate;
        this.simulationService = simulationService;
    }

    @Transactional
    public TopologyLinkResponse createLink(CreateLinkRequest request) {
        String sourceId = request.getSourceNodeId().trim();
        String targetId = request.getTargetNodeId().trim();

        if (sourceId.equalsIgnoreCase(targetId)) {
            throw new BadRequestException("Self-links are not permitted: source and target node cannot be identical (" + sourceId + ")");
        }

        if (!nodeRepository.existsByNodeId(sourceId)) {
            throw new ResourceNotFoundException("Source node does not exist in cluster topology: " + sourceId);
        }

        if (!nodeRepository.existsByNodeId(targetId)) {
            throw new ResourceNotFoundException("Target node does not exist in cluster topology: " + targetId);
        }

        if (linkRepository.existsBySourceNodeIdAndTargetNodeId(sourceId, targetId) ||
            linkRepository.existsBySourceNodeIdAndTargetNodeId(targetId, sourceId)) {
            throw new DuplicateResourceException("A network link between '" + sourceId + "' and '" + targetId + "' already exists");
        }

        String linkId = (request.getLinkId() != null && !request.getLinkId().trim().isEmpty())
                ? request.getLinkId().trim()
                : "link-" + sourceId + "-" + targetId;

        if (linkRepository.existsByLinkId(linkId)) {
            throw new DuplicateResourceException("Link with ID '" + linkId + "' already exists");
        }

        NetworkLink link = new NetworkLink(
                linkId,
                sourceId,
                targetId,
                request.getLinkType() != null ? request.getLinkType() : LinkType.DIRECT,
                request.getBandwidth() != null ? request.getBandwidth() : 1000,
                request.getWeight() != null ? request.getWeight() : 1.0,
                request.getEnabled() != null ? request.getEnabled() : true,
                request.getStatus() != null ? request.getStatus() : LinkStatus.ACTIVE
        );

        NetworkLink saved = linkRepository.save(link);
        broadcastTopologyChange();
        return TopologyLinkResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<TopologyLinkResponse> getAllLinks() {
        return linkRepository.findAll().stream()
                .map(TopologyLinkResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TopologyLinkResponse getLinkById(Long id) {
        NetworkLink link = linkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Network link not found with ID: " + id));
        return TopologyLinkResponse.fromEntity(link);
    }

    @Transactional
    public TopologyLinkResponse updateLink(Long id, UpdateLinkRequest request) {
        NetworkLink link = linkRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Network link not found with ID: " + id));

        if (request.getLinkType() != null) {
            link.setLinkType(request.getLinkType());
        }
        if (request.getBandwidth() != null) {
            link.setBandwidth(request.getBandwidth());
        }
        if (request.getWeight() != null) {
            link.setWeight(request.getWeight());
        }
        if (request.getEnabled() != null) {
            link.setEnabled(request.getEnabled());
        }
        if (request.getStatus() != null) {
            link.setStatus(request.getStatus());
        }

        NetworkLink updated = linkRepository.save(link);
        broadcastTopologyChange();
        return TopologyLinkResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteLink(Long id) {
        if (!linkRepository.existsById(id)) {
            throw new ResourceNotFoundException("Network link not found with ID: " + id);
        }
        linkRepository.deleteById(id);
        broadcastTopologyChange();
    }

    @Transactional
    public void deleteLinksForNode(String nodeId) {
        linkRepository.deleteByNodeInvolvement(nodeId);
        broadcastTopologyChange();
    }

    @Transactional(readOnly = true)
    public TopologyResponse getTopologySnapshot() {
        List<NetworkNode> allNodes = nodeRepository.findAll();
        List<NetworkLink> allLinks = linkRepository.findAll();

        Map<String, Integer> degrees = new HashMap<>();
        for (NetworkNode node : allNodes) {
            degrees.put(node.getNodeId(), 0);
        }

        for (NetworkLink link : allLinks) {
            if (Boolean.TRUE.equals(link.getEnabled()) && link.getStatus() != LinkStatus.OFFLINE) {
                degrees.put(link.getSourceNodeId(), degrees.getOrDefault(link.getSourceNodeId(), 0) + 1);
                degrees.put(link.getTargetNodeId(), degrees.getOrDefault(link.getTargetNodeId(), 0) + 1);
            }
        }

        List<TopologyNodeResponse> nodeResponses = allNodes.stream()
                .map(node -> TopologyNodeResponse.fromEntity(node, degrees.getOrDefault(node.getNodeId(), 0)))
                .collect(Collectors.toList());

        List<TopologyLinkResponse> linkResponses = allLinks.stream()
                .map(TopologyLinkResponse::fromEntity)
                .collect(Collectors.toList());

        TopologySummaryResponse summary = calculateSummary(allNodes, allLinks);

        String simStatus = (simulationService != null) ? simulationService.getSimulationStatus().getStatus() : "STOPPED";

        return new TopologyResponse(
                nodeResponses,
                linkResponses,
                summary,
                simStatus,
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public List<TopologyNodeResponse> getTopologyNodes() {
        return getTopologySnapshot().getNodes();
    }

    @Transactional(readOnly = true)
    public TopologySummaryResponse getTopologySummary() {
        List<NetworkNode> allNodes = nodeRepository.findAll();
        List<NetworkLink> allLinks = linkRepository.findAll();
        return calculateSummary(allNodes, allLinks);
    }

    private TopologySummaryResponse calculateSummary(List<NetworkNode> allNodes, List<NetworkLink> allLinks) {
        long totalNodes = allNodes.size();
        long activeNodes = allNodes.stream().filter(n -> n.getStatus() == NodeStatus.HEALTHY || n.getStatus() == NodeStatus.WARNING).count();
        long degradedNodes = allNodes.stream().filter(n -> n.getStatus() == NodeStatus.CONGESTED).count();
        long failedNodes = allNodes.stream().filter(n -> n.getStatus() == NodeStatus.FAILED).count();
        long offlineNodes = allNodes.stream().filter(n -> n.getStatus() == NodeStatus.OFFLINE).count();

        long totalLinks = allLinks.size();
        long activeLinks = allLinks.stream().filter(l -> Boolean.TRUE.equals(l.getEnabled()) && l.getStatus() == LinkStatus.ACTIVE).count();
        long degradedLinks = allLinks.stream().filter(l -> Boolean.TRUE.equals(l.getEnabled()) && l.getStatus() == LinkStatus.DEGRADED).count();
        long failedLinks = allLinks.stream().filter(l -> l.getStatus() == LinkStatus.FAILED).count();
        long offlineLinks = allLinks.stream().filter(l -> !Boolean.TRUE.equals(l.getEnabled()) || l.getStatus() == LinkStatus.OFFLINE).count();

        long totalCapacity = allNodes.stream().mapToLong(n -> n.getCapacity() != null ? n.getCapacity() : 0).sum();
        long totalBandwidth = allLinks.stream()
                .filter(l -> Boolean.TRUE.equals(l.getEnabled()) && l.getStatus() != LinkStatus.OFFLINE)
                .mapToLong(l -> l.getBandwidth() != null ? l.getBandwidth() : 0)
                .sum();

        // Connected Components & Disconnected Nodes computation using BFS
        Map<String, Set<String>> adjList = new HashMap<>();
        Set<String> nodeIds = new HashSet<>();
        for (NetworkNode node : allNodes) {
            nodeIds.add(node.getNodeId());
            adjList.put(node.getNodeId(), new HashSet<>());
        }

        for (NetworkLink link : allLinks) {
            if (Boolean.TRUE.equals(link.getEnabled()) && link.getStatus() != LinkStatus.OFFLINE && link.getStatus() != LinkStatus.FAILED) {
                if (adjList.containsKey(link.getSourceNodeId()) && adjList.containsKey(link.getTargetNodeId())) {
                    adjList.get(link.getSourceNodeId()).add(link.getTargetNodeId());
                    adjList.get(link.getTargetNodeId()).add(link.getSourceNodeId());
                }
            }
        }

        long disconnectedNodes = 0;
        for (String id : nodeIds) {
            if (adjList.get(id).isEmpty()) {
                disconnectedNodes++;
            }
        }

        int connectedComponents = 0;
        Set<String> visited = new HashSet<>();
        for (String id : nodeIds) {
            if (!visited.contains(id)) {
                connectedComponents++;
                Queue<String> queue = new LinkedList<>();
                queue.add(id);
                visited.add(id);
                while (!queue.isEmpty()) {
                    String curr = queue.poll();
                    for (String neighbor : adjList.get(curr)) {
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        double density = 0.0;
        if (totalNodes >= 2) {
            density = (2.0 * activeLinks) / (totalNodes * (totalNodes - 1.0));
        }

        return new TopologySummaryResponse(
                totalNodes,
                activeNodes,
                degradedNodes,
                failedNodes,
                offlineNodes,
                totalLinks,
                activeLinks,
                degradedLinks,
                failedLinks,
                offlineLinks,
                disconnectedNodes,
                connectedComponents,
                Math.round(density * 1000.0) / 1000.0,
                totalCapacity,
                totalBandwidth
        );
    }

    public void broadcastTopologyChange() {
        try {
            TopologyResponse snapshot = getTopologySnapshot();
            messagingTemplate.convertAndSend("/topic/topology", snapshot);
        } catch (Exception e) {
            // Safe fallback if websocket client is not active
        }
    }
}
