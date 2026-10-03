package com.netpulse.monitoring.controller;

import com.netpulse.common.ApiResponse;
import com.netpulse.monitoring.dto.MonitoringStatusResponse;
import com.netpulse.monitoring.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/monitoring")
@Tag(name = "Monitoring", description = "Monitoring Lifecycle & Telemetry Engine Controls")
public class MonitoringController {

    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get monitoring engine status and cycle statistics")
    public ResponseEntity<ApiResponse<MonitoringStatusResponse>> getMonitoringStatus() {
        return ResponseEntity.ok(ApiResponse.success(monitoringService.getMonitoringStatus()));
    }

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Start telemetry and heartbeat monitoring engine")
    public ResponseEntity<ApiResponse<MonitoringStatusResponse>> startMonitoring() {
        MonitoringStatusResponse response = monitoringService.startMonitoring();
        return ResponseEntity.ok(ApiResponse.success("Monitoring engine started", response));
    }

    @PostMapping("/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Stop telemetry and heartbeat monitoring engine")
    public ResponseEntity<ApiResponse<MonitoringStatusResponse>> stopMonitoring() {
        MonitoringStatusResponse response = monitoringService.stopMonitoring();
        return ResponseEntity.ok(ApiResponse.success("Monitoring engine stopped", response));
    }
}
