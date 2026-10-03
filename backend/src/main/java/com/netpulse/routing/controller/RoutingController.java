package com.netpulse.routing.controller;

import com.netpulse.common.ApiResponse;
import com.netpulse.routing.dto.*;
import com.netpulse.routing.entity.RoutingDecisionRecord;
import com.netpulse.routing.entity.RoutingStrategyType;
import com.netpulse.routing.service.RoutingCandidateService;
import com.netpulse.routing.service.RoutingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routing")
@Tag(name = "Routing", description = "Adaptive Traffic Routing Engine APIs")
public class RoutingController {

    private final RoutingService routingService;
    private final RoutingCandidateService candidateService;

    public RoutingController(RoutingService routingService, RoutingCandidateService candidateService) {
        this.routingService = routingService;
        this.candidateService = candidateService;
    }

    @PostMapping("/decide")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Evaluate and return a routing decision without persisting state")
    public ResponseEntity<ApiResponse<RoutingDecisionResponse>> decideRouting(
            @RequestBody(required = false) RoutingRequest request) {
        RoutingDecisionResponse decision = routingService.decideRouting(request);
        return ResponseEntity.ok(ApiResponse.success(decision));
    }

    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Execute, persist, and broadcast an adaptive traffic routing decision")
    public ResponseEntity<ApiResponse<RoutingDecisionResponse>> executeRoutingRequest(
            @RequestBody(required = false) RoutingRequest request) {
        RoutingDecisionResponse decision = routingService.executeAndRecordRouting(request);
        return ResponseEntity.ok(ApiResponse.success("Traffic routing decision executed", decision));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get routing engine status and statistics")
    public ResponseEntity<ApiResponse<RoutingStatusResponse>> getStatus() {
        return ResponseEntity.ok(ApiResponse.success(routingService.getStatus()));
    }

    @GetMapping("/strategy")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get current active default routing strategy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveStrategy() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "activeStrategy", routingService.getActiveStrategy(),
                "supportedStrategies", RoutingStrategyType.values()
        )));
    }

    @PutMapping("/strategy")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Update active default routing strategy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStrategy(
            @Valid @RequestBody UpdateStrategyRequest request) {
        routingService.setActiveStrategy(request.getStrategy());
        return ResponseEntity.ok(ApiResponse.success("Active routing strategy updated", Map.of(
                "activeStrategy", routingService.getActiveStrategy()
        )));
    }

    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get routing engine weights and eligibility configuration")
    public ResponseEntity<ApiResponse<RoutingConfigResponse>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success(routingService.getConfig()));
    }

    @GetMapping("/candidates")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get live candidate evaluation breakdown across all nodes")
    public ResponseEntity<ApiResponse<List<CandidateEvaluationDto>>> getCandidates() {
        return ResponseEntity.ok(ApiResponse.success(candidateService.evaluateAllCandidates()));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get bounded recent routing decisions history")
    public ResponseEntity<ApiResponse<List<RoutingDecisionRecord>>> getRecentHistory(
            @RequestParam(defaultValue = "30") int limit) {
        return ResponseEntity.ok(ApiResponse.success(routingService.getRecentHistory(limit)));
    }
}
