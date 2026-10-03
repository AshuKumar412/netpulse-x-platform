package com.netpulse.whatif.controller;

import com.netpulse.whatif.dto.*;
import com.netpulse.whatif.service.WhatIfScenarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/what-if")
public class WhatIfController {

    private final WhatIfScenarioService scenarioService;

    public WhatIfController(WhatIfScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @GetMapping("/snapshot")
    public ResponseEntity<SystemSnapshotDto> getSnapshot() {
        return ResponseEntity.ok(scenarioService.captureSystemSnapshot());
    }

    @PostMapping("/scenarios")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<WhatIfScenarioResponse> createScenario(@Valid @RequestBody WhatIfScenarioRequest request,
                                                                 Authentication authentication) {
        String owner = authentication != null ? authentication.getName() : "OPERATOR";
        WhatIfScenarioResponse created = scenarioService.createScenario(request, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/scenarios")
    public ResponseEntity<List<WhatIfScenarioResponse>> getAllScenarios() {
        return ResponseEntity.ok(scenarioService.getAllScenarios());
    }

    @GetMapping("/scenarios/{id}")
    public ResponseEntity<WhatIfScenarioResponse> getScenarioById(@PathVariable String id) {
        return ResponseEntity.ok(scenarioService.getScenarioById(id));
    }

    @PostMapping("/scenarios/{id}/run")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<SimulationRunResponse> runSimulation(@PathVariable String id) {
        return ResponseEntity.ok(scenarioService.runSimulation(id));
    }

    @PostMapping("/scenarios/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, String>> cancelSimulation(@PathVariable String id) {
        scenarioService.cancelSimulation(id);
        return ResponseEntity.ok(Map.of("message", "Simulation cancelled for scenario: " + id));
    }

    @GetMapping("/scenarios/{id}/timeline")
    public ResponseEntity<List<SimulationEventDto>> getTimeline(@PathVariable String id) {
        return ResponseEntity.ok(scenarioService.getTimeline(id));
    }

    @GetMapping("/scenarios/{id}/decisions")
    public ResponseEntity<List<SimulationDecisionDto>> getDecisions(@PathVariable String id) {
        return ResponseEntity.ok(scenarioService.getDecisions(id));
    }

    @GetMapping("/scenarios/{id}/comparison")
    public ResponseEntity<SimulationComparisonResponse> getComparison(@PathVariable String id) {
        return ResponseEntity.ok(scenarioService.getComparison(id));
    }

    @PostMapping("/compare/routing")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<RoutingComparisonResponse> compareRouting(@RequestBody RoutingComparisonRequest request) {
        return ResponseEntity.ok(scenarioService.compareRouting(request));
    }

    @PostMapping("/compare/scheduler")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<SchedulerComparisonResponse> compareScheduler(@RequestBody SchedulerComparisonRequest request) {
        return ResponseEntity.ok(scenarioService.compareScheduler(request));
    }

    @GetMapping("/templates")
    public ResponseEntity<List<WhatIfTemplateDto>> getTemplates() {
        return ResponseEntity.ok(scenarioService.getTemplates());
    }

    @GetMapping("/scenarios/{id}/export")
    public ResponseEntity<String> exportReport(@PathVariable String id,
                                               @RequestParam(defaultValue = "json") String format) {
        String data = scenarioService.exportReport(id, format);
        HttpHeaders headers = new HttpHeaders();
        if ("csv".equalsIgnoreCase(format)) {
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", "scenario_" + id + "_report.csv");
        } else {
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", "scenario_" + id + "_report.json");
        }
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
