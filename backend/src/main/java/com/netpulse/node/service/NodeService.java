package com.netpulse.node.service;

import com.netpulse.exception.DuplicateResourceException;
import com.netpulse.exception.ResourceNotFoundException;
import com.netpulse.node.dto.CreateNodeRequest;
import com.netpulse.node.dto.NodeResponse;
import com.netpulse.node.dto.NodeSummaryResponse;
import com.netpulse.node.dto.UpdateNodeRequest;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.telemetry.service.TelemetryService;
import com.netpulse.topology.repository.NetworkLinkRepository;
import com.netpulse.topology.service.TopologyService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NodeService {

    private final NodeRepository nodeRepository;
    private final NetworkLinkRepository linkRepository;
    private final TopologyService topologyService;
    private final TelemetryService telemetryService;
    private final HeartbeatService heartbeatService;

    public NodeService(NodeRepository nodeRepository,
                       NetworkLinkRepository linkRepository,
                       @Lazy TopologyService topologyService,
                       @Lazy TelemetryService telemetryService,
                       @Lazy HeartbeatService heartbeatService) {
        this.nodeRepository = nodeRepository;
        this.linkRepository = linkRepository;
        this.topologyService = topologyService;
        this.telemetryService = telemetryService;
        this.heartbeatService = heartbeatService;
    }

    @Transactional
    public NodeResponse createNode(CreateNodeRequest request) {
        String nodeId = request.getNodeId().trim();

        if (nodeRepository.existsByNodeId(nodeId)) {
            throw new DuplicateResourceException("Node with ID '" + nodeId + "' already exists");
        }

        NetworkNode node = new NetworkNode(
                nodeId,
                request.getName().trim(),
                request.getHost().trim(),
                request.getPort(),
                request.getStatus() != null ? request.getStatus() : NodeStatus.HEALTHY,
                request.getCapacity()
        );

        NetworkNode savedNode = nodeRepository.save(node);
        if (topologyService != null) {
            topologyService.broadcastTopologyChange();
        }
        return NodeResponse.fromEntity(savedNode);
    }

    @Transactional(readOnly = true)
    public List<NodeResponse> getAllNodes(NodeStatus status, String search) {
        List<NetworkNode> nodes;

        if (status != null) {
            nodes = nodeRepository.findByStatus(status);
        } else {
            nodes = nodeRepository.findAll();
        }

        if (search != null && !search.trim().isEmpty()) {
            String lowerSearch = search.trim().toLowerCase();
            nodes = nodes.stream()
                    .filter(n -> n.getNodeId().toLowerCase().contains(lowerSearch)
                            || n.getName().toLowerCase().contains(lowerSearch)
                            || n.getHost().toLowerCase().contains(lowerSearch))
                    .collect(Collectors.toList());
        }

        return nodes.stream()
                .map(NodeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NodeResponse getNodeById(Long id) {
        NetworkNode node = nodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Node not found with ID: " + id));
        return NodeResponse.fromEntity(node);
    }

    @Transactional(readOnly = true)
    public NodeResponse getNodeByNodeId(String nodeId) {
        NetworkNode node = nodeRepository.findByNodeId(nodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Node not found with nodeId: " + nodeId));
        return NodeResponse.fromEntity(node);
    }

    @Transactional
    public NodeResponse updateNode(Long id, UpdateNodeRequest request) {
        NetworkNode node = nodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Node not found with ID: " + id));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            node.setName(request.getName().trim());
        }
        if (request.getHost() != null && !request.getHost().trim().isEmpty()) {
            node.setHost(request.getHost().trim());
        }
        if (request.getPort() != null) {
            node.setPort(request.getPort());
        }
        if (request.getStatus() != null) {
            node.setStatus(request.getStatus());
        }
        if (request.getCapacity() != null) {
            node.setCapacity(request.getCapacity());
        }

        NetworkNode updated = nodeRepository.save(node);
        if (topologyService != null) {
            topologyService.broadcastTopologyChange();
        }
        return NodeResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteNode(Long id) {
        NetworkNode node = nodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Node not found with ID: " + id));

        if (linkRepository != null) {
            linkRepository.deleteByNodeInvolvement(node.getNodeId());
        }

        if (telemetryService != null) {
            telemetryService.removeNodeTelemetry(node.getNodeId());
        }

        if (heartbeatService != null) {
            heartbeatService.removeNode(node.getNodeId());
        }

        nodeRepository.deleteById(id);

        if (topologyService != null) {
            topologyService.broadcastTopologyChange();
        }
    }

    @Transactional(readOnly = true)
    public NodeSummaryResponse getNodeSummary() {
        List<NetworkNode> allNodes = nodeRepository.findAll();

        long total = allNodes.size();
        long healthy = allNodes.stream().filter(n -> n.getStatus() == NodeStatus.HEALTHY).count();
        long warning = allNodes.stream().filter(n -> n.getStatus() == NodeStatus.WARNING).count();
        long congested = allNodes.stream().filter(n -> n.getStatus() == NodeStatus.CONGESTED).count();
        long failed = allNodes.stream().filter(n -> n.getStatus() == NodeStatus.FAILED).count();
        long offline = allNodes.stream().filter(n -> n.getStatus() == NodeStatus.OFFLINE).count();
        long totalCapacity = allNodes.stream().mapToLong(n -> n.getCapacity() != null ? n.getCapacity() : 0).sum();

        return new NodeSummaryResponse(total, healthy, warning, congested, failed, offline, totalCapacity);
    }
}
