package com.netpulse.failover.controller;

import com.netpulse.common.ApiResponse;
import com.netpulse.failover.dto.FailoverEventDto;
import com.netpulse.failover.dto.FailoverPolicyConfig;
import com.netpulse.failover.dto.FailoverStatusResponse;
import com.netpulse.failover.dto.ManualRecoveryRequest;
import com.netpulse.failover.dto.ManualRecoveryResponse;
import com.netpulse.failover.dto.RetryFailoverResponse;
import com.netpulse.failover.entity.FailoverEvent;
import com.netpulse.failover.repository.FailoverEventRepository;
import com.netpulse.failover.service.FailoverOrchestrator;
import com.netpulse.failover.service.FailoverPolicyService;
import com.netpulse.failover.service.NodeRecoveryService;
import com.netpulse.failover.state.FailoverState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/failover")
@Tag(
        name = "Self-Healing & Failover",
        description = "Automatic failover, health verification, and node recovery orchestration"
)
public class FailoverController {

    private static final List<FailoverState> ACTIVE_STATES = List.of(
            FailoverState.FAILURE_CONFIRMED,
            FailoverState.ISOLATING,
            FailoverState.FAILOVER_IN_PROGRESS,
            FailoverState.REROUTED,
            FailoverState.RECOVERY_IN_PROGRESS,
            FailoverState.RECOVERY_VERIFICATION
    );

    private final FailoverEventRepository eventRepository;
    private final FailoverPolicyService policyService;
    private final FailoverOrchestrator orchestrator;
    private final NodeRecoveryService nodeRecoveryService;

    public FailoverController(
            FailoverEventRepository eventRepository,
            FailoverPolicyService policyService,
            FailoverOrchestrator orchestrator,
            NodeRecoveryService nodeRecoveryService) {

        this.eventRepository = eventRepository;
        this.policyService = policyService;
        this.orchestrator = orchestrator;
        this.nodeRecoveryService = nodeRecoveryService;
    }

    @GetMapping("/status")
    @Operation(summary = "Get self-healing platform status and metrics")
    public ResponseEntity<ApiResponse<FailoverStatusResponse>> getStatus() {

        long totalEvents = eventRepository.count();
        long successfulEvents = eventRepository.countBySuccessTrue();
        long failedEvents = eventRepository.countBySuccessFalse();
        long activeFailoversCount = eventRepository.countByCurrentStateIn(ACTIVE_STATES);

        // Compute average recovery duration in Java — Hibernate 6.6 does not support
        // EXTRACT(EPOCH FROM duration) for Instant subtraction in JPQL
        Double averageRecoveryDurationMs = null;
        try {
            List<FailoverEvent> successfulCompleted =
                    eventRepository.findCompletedSuccessfulEvents();
            if (!successfulCompleted.isEmpty()) {
                double avgMs = successfulCompleted.stream()
                        .filter(e -> e.getCompletedAt() != null && e.getStartedAt() != null)
                        .mapToLong(e -> Duration.between(e.getStartedAt(), e.getCompletedAt()).toMillis())
                        .filter(ms -> ms >= 0)
                        .average()
                        .orElse(0.0);
                averageRecoveryDurationMs = avgMs > 0 ? avgMs : null;
            }
        } catch (Exception ignored) {
            // Leave null — never produce a fake metric
        }

        // Recovery success rate: null when no completed attempts yet
        Double recoverySuccessRate = null;
        long completedAttempts = successfulEvents + failedEvents;
        if (completedAttempts > 0) {
            recoverySuccessRate = (double) successfulEvents / (double) completedAttempts * 100.0;
        }

        List<FailoverEventDto> activeEventDtos = eventRepository
                .findByCurrentStateIn(ACTIVE_STATES)
                .stream()
                .map(FailoverEventDto::fromEntity)
                .collect(Collectors.toList());

        FailoverStatusResponse status = new FailoverStatusResponse(
                policyService.isEnabled(),
                activeFailoversCount,
                successfulEvents,
                failedEvents,
                totalEvents,
                recoverySuccessRate,
                averageRecoveryDurationMs,
                activeEventDtos,
                orchestrator.getAllNodeFailoverStates()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Self-healing status retrieved successfully", status)
        );
    }

    @GetMapping("/events")
    @Operation(summary = "Get paginated failover audit events history")
    public ResponseEntity<ApiResponse<List<FailoverEventDto>>> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 0) page = 0;
        if (size <= 0) size = 20;

        List<FailoverEventDto> events = eventRepository
                .findAllByOrderByStartedAtDesc(PageRequest.of(page, size))
                .stream()
                .map(FailoverEventDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success("Failover events history retrieved successfully", events)
        );
    }

    @GetMapping("/events/{eventId}")
    @Operation(summary = "Get detailed failover event by ID")
    public ResponseEntity<ApiResponse<FailoverEventDto>> getEventById(
            @PathVariable String eventId) {

        return eventRepository
                .findByEventId(eventId)
                .map(event -> ResponseEntity.ok(
                        ApiResponse.success("Failover event retrieved", FailoverEventDto.fromEntity(event))
                ))
                .orElseGet(() -> ResponseEntity
                        .status(404)
                        .body(ApiResponse.error("Failover event not found")));
    }

    @GetMapping("/active")
    @Operation(summary = "Get currently active failover and recovery operations")
    public ResponseEntity<ApiResponse<List<FailoverEventDto>>> getActiveEvents() {

        List<FailoverEventDto> active = eventRepository
                .findByCurrentStateIn(ACTIVE_STATES)
                .stream()
                .map(FailoverEventDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success("Active failover operations retrieved successfully", active)
        );
    }

    @GetMapping("/policies")
    @Operation(summary = "Get failover policy configuration")
    public ResponseEntity<ApiResponse<FailoverPolicyConfig>> getPolicies() {

        return ResponseEntity.ok(
                ApiResponse.success("Failover policies retrieved", policyService.getPolicyConfig())
        );
    }

    @PostMapping("/recovery/{nodeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Initiate controlled manual node recovery")
    public ResponseEntity<ApiResponse<ManualRecoveryResponse>> initiateRecovery(
            @PathVariable String nodeId,
            @RequestBody(required = false) ManualRecoveryRequest request) {

        String reason = request != null ? request.getReason() : null;
        boolean force = request != null && request.isForce();

        ManualRecoveryResponse response = nodeRecoveryService.initiateManualRecovery(nodeId, reason, force);

        return ResponseEntity.ok(
                ApiResponse.success(response.getMessage(), response)
        );
    }

    @PostMapping("/retry/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Retry a failed recovery operation")
    public ResponseEntity<ApiResponse<RetryFailoverResponse>> retryRecovery(
            @PathVariable String eventId) {

        RetryFailoverResponse response = nodeRecoveryService.retryRecovery(eventId);

        return ResponseEntity.ok(
                ApiResponse.success(response.getMessage(), response)
        );
    }
}