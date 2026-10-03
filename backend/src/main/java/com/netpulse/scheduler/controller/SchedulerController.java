package com.netpulse.scheduler.controller;

import com.netpulse.scheduler.dto.*;
import com.netpulse.scheduler.service.SchedulerService;
import com.netpulse.scheduler.state.ProcessState;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    private final SchedulerService schedulerService;

    public SchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @GetMapping("/status")
    public ResponseEntity<SchedulerStatusResponse> getStatus() {
        return ResponseEntity.ok(schedulerService.getStatus());
    }

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, String>> start() {
        schedulerService.start();
        return ResponseEntity.ok(Map.of("message", "CPU Scheduler simulation started"));
    }

    @PostMapping("/pause")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, String>> pause() {
        schedulerService.pause();
        return ResponseEntity.ok(Map.of("message", "CPU Scheduler simulation paused"));
    }

    @PostMapping("/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, String>> resume() {
        schedulerService.resume();
        return ResponseEntity.ok(Map.of("message", "CPU Scheduler simulation resumed"));
    }

    @PostMapping("/stop")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, String>> stop() {
        schedulerService.stop();
        return ResponseEntity.ok(Map.of("message", "CPU Scheduler simulation stopped"));
    }

    @PostMapping("/reset")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, String>> reset() {
        schedulerService.reset();
        return ResponseEntity.ok(Map.of("message", "CPU Scheduler simulation reset complete"));
    }

    @PostMapping("/step")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, String>> step() {
        schedulerService.step();
        return ResponseEntity.ok(Map.of("message", "CPU Scheduler advanced single simulation step"));
    }

    @PostMapping("/processes")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ProcessControlBlockDto> createProcess(@Valid @RequestBody CreateProcessRequest request,
                                                                Authentication authentication) {
        String owner = authentication != null ? authentication.getName() : "OPERATOR";
        ProcessControlBlockDto created = schedulerService.createProcess(request, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/processes/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<List<ProcessControlBlockDto>> batchCreateProcesses(@Valid @RequestBody BatchCreateProcessRequest request,
                                                                             Authentication authentication) {
        String owner = authentication != null ? authentication.getName() : "OPERATOR";
        List<ProcessControlBlockDto> list = schedulerService.batchCreateProcesses(request, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(list);
    }

    @GetMapping("/processes")
    public ResponseEntity<List<ProcessControlBlockDto>> getAllProcesses(
            @RequestParam(required = false) ProcessState state,
            @RequestParam(required = false) String nodeId) {
        return ResponseEntity.ok(schedulerService.getAllProcesses(state, nodeId));
    }

    @GetMapping("/processes/{processId}")
    public ResponseEntity<ProcessControlBlockDto> getProcess(@PathVariable String processId) {
        return ResponseEntity.ok(schedulerService.getProcess(processId));
    }

    @DeleteMapping("/processes/{processId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, String>> terminateProcess(@PathVariable String processId) {
        schedulerService.terminateProcess(processId);
        return ResponseEntity.ok(Map.of("message", "Process terminated: " + processId));
    }

    @GetMapping("/policy")
    public ResponseEntity<SchedulerPolicyConfig> getPolicy() {
        return ResponseEntity.ok(schedulerService.getPolicy());
    }

    @PutMapping("/policy")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<SchedulerPolicyConfig> updatePolicy(@Valid @RequestBody SchedulerPolicyConfig config) {
        return ResponseEntity.ok(schedulerService.updatePolicy(config));
    }

    @PostMapping("/policy/algorithm")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Map<String, String>> switchAlgorithm(@RequestParam SchedulerAlgorithm algorithm) {
        schedulerService.switchAlgorithm(algorithm);
        return ResponseEntity.ok(Map.of("message", "Scheduling algorithm switched to " + algorithm.name()));
    }

    @GetMapping("/context-switches")
    public ResponseEntity<List<ContextSwitchEventDto>> getContextSwitches(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(schedulerService.getRecentContextSwitches(limit));
    }

    @PostMapping("/benchmark")
    public ResponseEntity<AlgorithmComparisonResponse> runBenchmark(
            @RequestBody(required = false) List<CreateProcessRequest> customWorkload) {
        return ResponseEntity.ok(schedulerService.runBenchmarkComparison(customWorkload));
    }
}
