package com.netpulse.chaos.controller;

import com.netpulse.chaos.dto.*;
import com.netpulse.chaos.entity.ChaosExperiment;
import com.netpulse.chaos.repository.ChaosExperimentRepository;
import com.netpulse.chaos.service.ChaosInjectionService;
import com.netpulse.chaos.service.ChaosPolicyService;
import com.netpulse.chaos.state.ChaosExperimentResult;
import com.netpulse.chaos.state.ChaosExperimentStatus;
import com.netpulse.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chaos")
@Tag(name = "Chaos Engineering", description = "Controlled failure injection, resilience verification, and safety policies")
public class ChaosController {

    private final ChaosInjectionService chaosService;
    private final ChaosPolicyService policyService;
    private final ChaosExperimentRepository experimentRepository;

    public ChaosController(ChaosInjectionService chaosService,
                           ChaosPolicyService policyService,
                           ChaosExperimentRepository experimentRepository) {
        this.chaosService = chaosService;
        this.policyService = policyService;
        this.experimentRepository = experimentRepository;
    }

    @GetMapping("/status")
    @Operation(summary = "Get chaos engineering platform status and active experiments")
    public ResponseEntity<ApiResponse<ChaosStatusResponse>> getStatus() {
        long total = experimentRepository.count();
        long active = experimentRepository.countByStatusIn(List.of(ChaosExperimentStatus.RUNNING, ChaosExperimentStatus.SCHEDULED));
        long success = experimentRepository.countByResult(ChaosExperimentResult.SUCCESS);
        long failed = experimentRepository.countByResult(ChaosExperimentResult.FAILED);

        List<ChaosExperimentDto> activeList = experimentRepository.findByStatus(ChaosExperimentStatus.RUNNING).stream()
                .map(ChaosExperimentDto::fromEntity)
                .collect(Collectors.toList());

        ChaosStatusResponse response = new ChaosStatusResponse(
                policyService.isEnabled(),
                active,
                total,
                success,
                failed,
                activeList,
                policyService.getPolicyConfig()
        );

        return ResponseEntity.ok(ApiResponse.success("Chaos status retrieved successfully", response));
    }

    @GetMapping("/experiments")
    @Operation(summary = "Get paginated history of chaos experiments")
    public ResponseEntity<ApiResponse<List<ChaosExperimentDto>>> getExperiments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;

        List<ChaosExperimentDto> experiments = experimentRepository
                .findAllByOrderByStartedAtDesc(PageRequest.of(page, size))
                .stream()
                .map(ChaosExperimentDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Chaos experiments history retrieved", experiments));
    }

    @GetMapping("/experiments/{id}")
    @Operation(summary = "Get detailed chaos experiment by ID")
    public ResponseEntity<ApiResponse<ChaosExperimentDto>> getExperimentById(@PathVariable String id) {
        return experimentRepository.findByExperimentId(id)
                .map(exp -> ResponseEntity.ok(ApiResponse.success("Chaos experiment retrieved", ChaosExperimentDto.fromEntity(exp))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Chaos experiment not found: " + id)));
    }

    @GetMapping("/experiments/active")
    @Operation(summary = "Get currently active running chaos experiments")
    public ResponseEntity<ApiResponse<List<ChaosExperimentDto>>> getActiveExperiments() {
        List<ChaosExperimentDto> active = experimentRepository.findByStatus(ChaosExperimentStatus.RUNNING).stream()
                .map(ChaosExperimentDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Active chaos experiments retrieved", active));
    }

    @PostMapping("/experiments")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Start a new controlled chaos experiment")
    public ResponseEntity<ApiResponse<ChaosExperimentDto>> startExperiment(
            @Valid @RequestBody CreateChaosExperimentRequest request,
            Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : "operator";
        ChaosExperimentDto response = chaosService.startExperiment(request, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Chaos experiment started successfully", response));
    }

    @PostMapping("/experiments/{id}/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Stop a running chaos experiment")
    public ResponseEntity<ApiResponse<ChaosExperimentDto>> stopExperiment(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "Operator requested stop") String reason) {
        ChaosExperimentDto response = chaosService.stopExperiment(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Chaos experiment stopped", response));
    }

    @PostMapping("/experiments/{id}/rollback")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Immediately rollback a chaos experiment and restore infrastructure state")
    public ResponseEntity<ApiResponse<ChaosExperimentDto>> rollbackExperiment(@PathVariable String id) {
        ChaosExperimentDto response = chaosService.rollbackExperiment(id);
        return ResponseEntity.ok(ApiResponse.success("Chaos experiment rolled back and cancelled", response));
    }

    @GetMapping("/policies")
    @Operation(summary = "Get active chaos policy limits and configuration")
    public ResponseEntity<ApiResponse<ChaosPolicyConfig>> getPolicies() {
        return ResponseEntity.ok(ApiResponse.success("Chaos policies retrieved", policyService.getPolicyConfig()));
    }
}
