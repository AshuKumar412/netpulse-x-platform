package com.netpulse.topology.controller;

import com.netpulse.common.ApiResponse;
import com.netpulse.topology.dto.*;
import com.netpulse.topology.service.TopologyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topology")
@Tag(name = "Network Topology", description = "Endpoints for graph topology computation and link management")
@SecurityRequirement(name = "bearerAuth")
public class TopologyController {

    private final TopologyService topologyService;

    public TopologyController(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get complete network topology snapshot", description = "Calculates in-memory graph representation with nodes, links, and graph theory summary metrics.")
    public ResponseEntity<ApiResponse<TopologyResponse>> getTopology() {
        TopologyResponse response = topologyService.getTopologySnapshot();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/nodes")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get topology nodes with connectivity degrees", description = "Retrieves all topology nodes with computed node degrees.")
    public ResponseEntity<ApiResponse<List<TopologyNodeResponse>>> getTopologyNodes() {
        List<TopologyNodeResponse> nodes = topologyService.getTopologyNodes();
        return ResponseEntity.ok(ApiResponse.success(nodes));
    }

    @GetMapping("/links")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "List all network links", description = "Retrieves all directional and backbone connections between infrastructure nodes.")
    public ResponseEntity<ApiResponse<List<TopologyLinkResponse>>> getAllLinks() {
        List<TopologyLinkResponse> links = topologyService.getAllLinks();
        return ResponseEntity.ok(ApiResponse.success(links));
    }

    @GetMapping("/links/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get network link details", description = "Retrieves configuration details of a single network link.")
    public ResponseEntity<ApiResponse<TopologyLinkResponse>> getLinkById(@PathVariable Long id) {
        TopologyLinkResponse link = topologyService.getLinkById(id);
        return ResponseEntity.ok(ApiResponse.success(link));
    }

    @PostMapping("/links")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Create network link", description = "Establishes a new link between two existing topology nodes. Allowed for ADMIN and OPERATOR.")
    public ResponseEntity<ApiResponse<TopologyLinkResponse>> createLink(@Valid @RequestBody CreateLinkRequest request) {
        TopologyLinkResponse response = topologyService.createLink(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Network link established successfully", response));
    }

    @PutMapping("/links/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Update network link", description = "Updates bandwidth, weight, or state of an existing link. Allowed for ADMIN and OPERATOR.")
    public ResponseEntity<ApiResponse<TopologyLinkResponse>> updateLink(@PathVariable Long id,
                                                                        @Valid @RequestBody UpdateLinkRequest request) {
        TopologyLinkResponse response = topologyService.updateLink(id, request);
        return ResponseEntity.ok(ApiResponse.success("Network link updated successfully", response));
    }

    @DeleteMapping("/links/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete network link", description = "Decommissions a link between two nodes. Allowed for ADMIN only.")
    public ResponseEntity<ApiResponse<Void>> deleteLink(@PathVariable Long id) {
        topologyService.deleteLink(id);
        return ResponseEntity.ok(ApiResponse.success("Network link removed successfully", null));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get computed topology graph summary", description = "Returns computed graph properties including connected components, density, and bandwidth.")
    public ResponseEntity<ApiResponse<TopologySummaryResponse>> getTopologySummary() {
        TopologySummaryResponse summary = topologyService.getTopologySummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
