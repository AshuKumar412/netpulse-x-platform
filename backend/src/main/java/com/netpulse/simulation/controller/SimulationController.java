package com.netpulse.simulation.controller;

import com.netpulse.common.ApiResponse;
import com.netpulse.simulation.dto.SimulationStatusResponse;
import com.netpulse.simulation.service.NetworkSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulation")
@Tag(name = "Network Simulation", description = "Endpoints for managing deterministic network simulation lifecycle")
@SecurityRequirement(name = "bearerAuth")
public class SimulationController {

    private final NetworkSimulationService simulationService;

    public SimulationController(NetworkSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    @Operation(summary = "Get simulation engine state", description = "Returns active simulation status, uptime, tick count, and evaluated nodes/links count.")
    public ResponseEntity<ApiResponse<SimulationStatusResponse>> getStatus() {
        SimulationStatusResponse response = simulationService.getSimulationStatus();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Start network simulation", description = "Activates deterministic simulation ticks and topology evaluation. Allowed for ADMIN and OPERATOR.")
    public ResponseEntity<ApiResponse<SimulationStatusResponse>> startSimulation() {
        SimulationStatusResponse response = simulationService.startSimulation();
        return ResponseEntity.ok(ApiResponse.success("Simulation started", response));
    }

    @PostMapping("/pause")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Pause network simulation", description = "Freezes simulation ticks while preserving current topology state. Allowed for ADMIN and OPERATOR.")
    public ResponseEntity<ApiResponse<SimulationStatusResponse>> pauseSimulation() {
        SimulationStatusResponse response = simulationService.pauseSimulation();
        return ResponseEntity.ok(ApiResponse.success("Simulation paused", response));
    }

    @PostMapping("/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Stop and reset network simulation", description = "Stops simulation ticks and resets uptime counter. Allowed for ADMIN and OPERATOR.")
    public ResponseEntity<ApiResponse<SimulationStatusResponse>> stopSimulation() {
        SimulationStatusResponse response = simulationService.stopSimulation();
        return ResponseEntity.ok(ApiResponse.success("Simulation stopped", response));
    }
}
