package com.netpulse.health.controller;

import com.netpulse.common.ApiResponse;
import com.netpulse.exception.ResourceNotFoundException;
import com.netpulse.health.dto.NodeHealthResponse;
import com.netpulse.health.service.NodeHealthService;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.telemetry.entity.TelemetryRecord;
import com.netpulse.telemetry.service.TelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/health/nodes")
@Tag(name = "Health", description = "Node Health Evaluation APIs")
public class HealthController {

    private final NodeHealthService nodeHealthService;
    private final NodeRepository nodeRepository;
    private final TelemetryService telemetryService;

    public HealthController(NodeHealthService nodeHealthService,
                            NodeRepository nodeRepository,
                            TelemetryService telemetryService) {
        this.nodeHealthService = nodeHealthService;
        this.nodeRepository = nodeRepository;
        this.telemetryService = telemetryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get evaluated health status for all nodes")
    public ResponseEntity<ApiResponse<List<NodeHealthResponse>>> getAllNodeHealth() {
        List<NetworkNode> nodes = nodeRepository.findAll();
        List<NodeHealthResponse> list = new ArrayList<>();

        for (NetworkNode node : nodes) {
            TelemetryRecord telemetry = telemetryService.getLatestTelemetry(node.getNodeId());
            list.add(nodeHealthService.evaluateNodeHealth(node, telemetry));
        }

        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{nodeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get evaluated health status for a specific node")
    public ResponseEntity<ApiResponse<NodeHealthResponse>> getNodeHealth(@PathVariable String nodeId) {
        NetworkNode node = nodeRepository.findByNodeId(nodeId)
                .orElseThrow(() -> new ResourceNotFoundException("NetworkNode not found with ID: " + nodeId));

        TelemetryRecord telemetry = telemetryService.getLatestTelemetry(nodeId);
        NodeHealthResponse health = nodeHealthService.evaluateNodeHealth(node, telemetry);

        return ResponseEntity.ok(ApiResponse.success(health));
    }
}
