package com.netpulse.system.controller;

import com.netpulse.system.dto.HostLiveMetricsDto;
import com.netpulse.system.dto.HostSystemProfileDto;
import com.netpulse.system.dto.LocalSystemIdentityDto;
import com.netpulse.system.service.SystemDiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing real-time host machine discovery, profile, and performance metrics.
 */
@RestController
@RequestMapping("/api/system")
@Tag(name = "Host System Discovery", description = "Dynamic hardware and operating system profile discovered at runtime")
public class SystemProfileController {

    private final SystemDiscoveryService discoveryService;

    public SystemProfileController(SystemDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get host machine system profile", description = "Discovers and returns operating system, processor, memory, storage, and network topology of current machine")
    public ResponseEntity<HostSystemProfileDto> getSystemProfile() {
        return ResponseEntity.ok(discoveryService.discoverSystemProfile());
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get live host performance metrics", description = "Returns actual current CPU utilization, memory usage, and storage stats")
    public ResponseEntity<HostLiveMetricsDto> getLiveMetrics() {
        return ResponseEntity.ok(discoveryService.collectLiveMetrics());
    }

    @GetMapping("/identity")
    @Operation(summary = "Get safe local system identity", description = "Returns safe machine fingerprint and adaptive hardware capability tier")
    public ResponseEntity<LocalSystemIdentityDto> getSystemIdentity() {
        return ResponseEntity.ok(discoveryService.getLocalSystemIdentity());
    }
}
