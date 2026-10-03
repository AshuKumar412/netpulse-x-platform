package com.netpulse.heartbeat.controller;

import com.netpulse.common.ApiResponse;
import com.netpulse.exception.ResourceNotFoundException;
import com.netpulse.heartbeat.dto.HeartbeatStatusResponse;
import com.netpulse.heartbeat.service.HeartbeatService;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.repository.NodeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/heartbeat")
@Tag(name = "Heartbeat", description = "Node Heartbeat & Liveness Monitoring APIs")
public class HeartbeatController {

    private final HeartbeatService heartbeatService;
    private final NodeRepository nodeRepository;

    public HeartbeatController(HeartbeatService heartbeatService, NodeRepository nodeRepository) {
        this.heartbeatService = heartbeatService;
        this.nodeRepository = nodeRepository;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get heartbeat liveness status for all nodes")
    public ResponseEntity<ApiResponse<List<HeartbeatStatusResponse>>> getAllHeartbeatStatuses() {
        List<String> nodeIds = nodeRepository.findAll().stream()
                .map(NetworkNode::getNodeId)
                .collect(Collectors.toList());

        List<HeartbeatStatusResponse> list = heartbeatService.getAllHeartbeatStatuses(nodeIds);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/nodes/{nodeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get heartbeat liveness status for a specific node")
    public ResponseEntity<ApiResponse<HeartbeatStatusResponse>> getNodeHeartbeatStatus(@PathVariable String nodeId) {
        if (!nodeRepository.existsByNodeId(nodeId)) {
            throw new ResourceNotFoundException("NetworkNode not found with ID: " + nodeId);
        }

        HeartbeatStatusResponse response = heartbeatService.getHeartbeatStatus(nodeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/nodes/{nodeId}/suppress")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Suppress or resume heartbeat emission for controlled timeout testing")
    public ResponseEntity<ApiResponse<HeartbeatStatusResponse>> toggleHeartbeatSuppression(
            @PathVariable String nodeId,
            @RequestParam(defaultValue = "true") boolean suppress) {
        if (!nodeRepository.existsByNodeId(nodeId)) {
            throw new ResourceNotFoundException("NetworkNode not found with ID: " + nodeId);
        }

        heartbeatService.suppressHeartbeat(nodeId, suppress);
        HeartbeatStatusResponse response = heartbeatService.getHeartbeatStatus(nodeId);
        return ResponseEntity.ok(ApiResponse.success(
                suppress ? "Heartbeat emission suppressed for testing" : "Heartbeat emission resumed",
                response
        ));
    }
}
