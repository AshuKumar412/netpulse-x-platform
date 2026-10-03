package com.netpulse.telemetry.controller;

import com.netpulse.common.ApiResponse;
import com.netpulse.exception.ResourceNotFoundException;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.telemetry.dto.TelemetryRecordResponse;
import com.netpulse.telemetry.entity.TelemetryRecord;
import com.netpulse.telemetry.service.TelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/telemetry")
@Tag(name = "Telemetry", description = "Real-Time Infrastructure Telemetry APIs")
public class TelemetryController {

    private final TelemetryService telemetryService;
    private final NodeRepository nodeRepository;

    public TelemetryController(TelemetryService telemetryService, NodeRepository nodeRepository) {
        this.telemetryService = telemetryService;
        this.nodeRepository = nodeRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get current telemetry records for all monitored nodes")
    public ResponseEntity<ApiResponse<List<TelemetryRecordResponse>>> getAllCurrentTelemetry() {
        List<String> nodeIds = nodeRepository.findAll().stream()
                .map(NetworkNode::getNodeId)
                .collect(Collectors.toList());

        List<TelemetryRecordResponse> responses = telemetryService.getAllLatestTelemetry(nodeIds).stream()
                .map(telemetryService::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/nodes/{nodeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get latest telemetry record for a specific node")
    public ResponseEntity<ApiResponse<TelemetryRecordResponse>> getNodeTelemetry(@PathVariable String nodeId) {
        if (!nodeRepository.existsByNodeId(nodeId)) {
            throw new ResourceNotFoundException("NetworkNode not found with ID: " + nodeId);
        }

        TelemetryRecord record = telemetryService.getLatestTelemetry(nodeId);
        if (record == null) {
            return ResponseEntity.ok(ApiResponse.success("Telemetry not yet initialized for node", null));
        }

        return ResponseEntity.ok(ApiResponse.success(telemetryService.toResponse(record)));
    }

    @GetMapping("/nodes/{nodeId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get bounded historical telemetry records for a node")
    public ResponseEntity<ApiResponse<List<TelemetryRecordResponse>>> getNodeTelemetryHistory(
            @PathVariable String nodeId,
            @RequestParam(defaultValue = "30") int limit) {
        if (!nodeRepository.existsByNodeId(nodeId)) {
            throw new ResourceNotFoundException("NetworkNode not found with ID: " + nodeId);
        }

        List<TelemetryRecordResponse> history = telemetryService.getRecentHistory(nodeId, limit);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
