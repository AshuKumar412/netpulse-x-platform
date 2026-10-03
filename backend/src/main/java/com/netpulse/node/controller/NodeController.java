package com.netpulse.node.controller;

import com.netpulse.common.ApiResponse;
import com.netpulse.node.dto.CreateNodeRequest;
import com.netpulse.node.dto.NodeResponse;
import com.netpulse.node.dto.NodeSummaryResponse;
import com.netpulse.node.dto.UpdateNodeRequest;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.service.NodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nodes")
@Tag(name = "Node Management", description = "Endpoints for configuring and managing network nodes")
@SecurityRequirement(name = "bearerAuth")
public class NodeController {

    private final NodeService nodeService;

    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Register a new network node", description = "Creates a new network node configuration. Allowed for ADMIN and OPERATOR.")
    public ResponseEntity<ApiResponse<NodeResponse>> createNode(@Valid @RequestBody CreateNodeRequest request) {
        NodeResponse response = nodeService.createNode(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Node created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "List all network nodes", description = "Retrieves all registered nodes with optional status and text search filtering.")
    public ResponseEntity<ApiResponse<List<NodeResponse>>> getAllNodes(
            @Parameter(description = "Filter by node operational status")
            @RequestParam(required = false) NodeStatus status,
            @Parameter(description = "Search term for nodeId, name, or host")
            @RequestParam(required = false) String search) {
        List<NodeResponse> nodes = nodeService.getAllNodes(status, search);
        return ResponseEntity.ok(ApiResponse.success(nodes));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get network node status summary", description = "Aggregates real-time count of nodes by status for the infrastructure dashboard.")
    public ResponseEntity<ApiResponse<NodeSummaryResponse>> getNodeSummary() {
        NodeSummaryResponse summary = nodeService.getNodeSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get node details by ID", description = "Retrieves configuration details of a single network node.")
    public ResponseEntity<ApiResponse<NodeResponse>> getNodeById(@PathVariable Long id) {
        NodeResponse node = nodeService.getNodeById(id);
        return ResponseEntity.ok(ApiResponse.success(node));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Update network node", description = "Updates configuration or status of an existing network node. Allowed for ADMIN and OPERATOR.")
    public ResponseEntity<ApiResponse<NodeResponse>> updateNode(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNodeRequest request) {
        NodeResponse updated = nodeService.updateNode(id, request);
        return ResponseEntity.ok(ApiResponse.success("Node updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete network node", description = "Deletes a network node from the topology. Allowed for ADMIN only.")
    public ResponseEntity<ApiResponse<Void>> deleteNode(@PathVariable Long id) {
        nodeService.deleteNode(id);
        return ResponseEntity.ok(ApiResponse.success("Node deleted successfully", null));
    }
}
