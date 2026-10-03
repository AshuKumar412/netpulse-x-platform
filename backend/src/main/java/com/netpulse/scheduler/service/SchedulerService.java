package com.netpulse.scheduler.service;

import com.netpulse.exception.ResourceNotFoundException;
import com.netpulse.scheduler.dto.*;
import com.netpulse.scheduler.entity.ContextSwitchEvent;
import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.repository.ContextSwitchEventRepository;
import com.netpulse.scheduler.repository.ProcessControlBlockRepository;
import com.netpulse.scheduler.state.ProcessState;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SchedulerService {

    private final SchedulerEngine engine;
    private final SchedulerPolicyService policyService;
    private final ProcessControlBlockRepository pcbRepository;
    private final ContextSwitchEventRepository contextSwitchRepository;

    public SchedulerService(SchedulerEngine engine,
                            SchedulerPolicyService policyService,
                            ProcessControlBlockRepository pcbRepository,
                            ContextSwitchEventRepository contextSwitchRepository) {
        this.engine = engine;
        this.policyService = policyService;
        this.pcbRepository = pcbRepository;
        this.contextSwitchRepository = contextSwitchRepository;
    }

    public SchedulerStatusResponse getStatus() {
        return engine.getStatusResponse();
    }

    public void start() {
        engine.start();
    }

    public void pause() {
        engine.pause();
    }

    public void resume() {
        engine.resume();
    }

    public void stop() {
        engine.stop();
    }

    public void reset() {
        engine.reset();
    }

    public void step() {
        engine.executeTick();
    }

    @Transactional
    public ProcessControlBlockDto createProcess(CreateProcessRequest request, String owner) {
        ProcessControlBlock pcb = engine.createProcess(request, owner);
        return ProcessControlBlockDto.fromEntity(pcb);
    }

    @Transactional
    public List<ProcessControlBlockDto> batchCreateProcesses(BatchCreateProcessRequest request, String owner) {
        List<ProcessControlBlock> list = engine.batchCreateProcesses(request, owner);
        return list.stream().map(ProcessControlBlockDto::fromEntity).toList();
    }

    public ProcessControlBlockDto getProcess(String processId) {
        ProcessControlBlock pcb = pcbRepository.findByProcessId(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Process not found: " + processId));
        return ProcessControlBlockDto.fromEntity(pcb);
    }

    public List<ProcessControlBlockDto> getAllProcesses(ProcessState state, String nodeId) {
        List<ProcessControlBlock> list;
        if (state != null && nodeId != null) {
            list = pcbRepository.findByTargetNodeIdAndState(nodeId, state);
        } else if (state != null) {
            list = pcbRepository.findByState(state);
        } else if (nodeId != null) {
            list = pcbRepository.findByTargetNodeId(nodeId);
        } else {
            list = pcbRepository.findRecentProcesses(PageRequest.of(0, 100));
        }
        return list.stream().map(ProcessControlBlockDto::fromEntity).toList();
    }

    public void terminateProcess(String processId) {
        engine.terminateProcess(processId);
    }

    public SchedulerPolicyConfig getPolicy() {
        return policyService.getPolicy();
    }

    public SchedulerPolicyConfig updatePolicy(SchedulerPolicyConfig newPolicy) {
        return policyService.updatePolicy(newPolicy);
    }

    public void switchAlgorithm(SchedulerAlgorithm algorithm) {
        policyService.setAlgorithm(algorithm);
    }

    public List<ContextSwitchEventDto> getRecentContextSwitches(int limit) {
        int bounded = Math.max(1, Math.min(100, limit));
        List<ContextSwitchEvent> events = contextSwitchRepository.findRecentEvents(PageRequest.of(0, bounded));
        return events.stream().map(ContextSwitchEventDto::fromEntity).toList();
    }

    public AlgorithmComparisonResponse runBenchmarkComparison(List<CreateProcessRequest> customWorkload) {
        List<ProcessControlBlock> workload = null;
        if (customWorkload != null && !customWorkload.isEmpty()) {
            workload = customWorkload.stream().map(req -> new ProcessControlBlock(
                    "P-" + UUID.randomUUID().toString().substring(0, 6),
                    req.getProcessName(),
                    req.getOwner() != null ? req.getOwner() : "BENCHMARK",
                    req.getTargetNodeId() != null ? req.getTargetNodeId() : "NODE-001",
                    req.getProcessType(),
                    req.getPriority(),
                    0,
                    req.getBurstTime()
            )).toList();
        }
        return engine.runBenchmarkComparison(workload);
    }
}
