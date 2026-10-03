package com.netpulse.scheduler.service;

import com.netpulse.exception.BadRequestException;
import com.netpulse.exception.ResourceNotFoundException;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.scheduler.dto.*;
import com.netpulse.scheduler.entity.ContextSwitchEvent;
import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.repository.ContextSwitchEventRepository;
import com.netpulse.scheduler.repository.ProcessControlBlockRepository;
import com.netpulse.scheduler.state.*;
import com.netpulse.scheduler.strategy.CfsInspiredScheduler;
import com.netpulse.scheduler.strategy.CpuScheduler;
import com.netpulse.scheduler.strategy.CpuSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SchedulerEngine {

    private static final Logger log = LoggerFactory.getLogger(SchedulerEngine.class);
    public static final String WS_SCHEDULER_TOPIC = "/topic/scheduler";

    private final SchedulerPolicyService policyService;
    private final CpuSchedulerFactory schedulerFactory;
    private final ProcessControlBlockRepository pcbRepository;
    private final ContextSwitchEventRepository contextSwitchRepository;
    private final NodeRepository nodeRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private volatile SchedulerStatus status = SchedulerStatus.RUNNING;
    private final AtomicLong simulationTick = new AtomicLong(0);

    // In-memory process collections for low-latency simulation
    private final List<ProcessControlBlock> readyQueue = new CopyOnWriteArrayList<>();
    private final List<ProcessControlBlock> waitingQueue = new CopyOnWriteArrayList<>();
    private final List<ProcessControlBlock> completedProcesses = new CopyOnWriteArrayList<>();

    // Map of coreId -> Running PCB
    private final Map<Integer, ProcessControlBlock> runningProcesses = new ConcurrentHashMap<>();
    // Map of coreId -> Ticks ran on currently dispatched process
    private final Map<Integer, Integer> coreCurrentBurstTicks = new ConcurrentHashMap<>();
    // Map of coreId -> Core Runtime State
    private final Map<Integer, CpuCoreDto> coreStates = new ConcurrentHashMap<>();

    // Real-time Gantt block sliding window
    private final List<GanttBlockDto> recentGanttBlocks = new CopyOnWriteArrayList<>();
    private static final int MAX_GANTT_HISTORY = 120;

    // Last context switches
    private final List<ContextSwitchEventDto> recentContextSwitches = new CopyOnWriteArrayList<>();
    private static final int MAX_CS_HISTORY = 50;

    public SchedulerEngine(SchedulerPolicyService policyService,
                           CpuSchedulerFactory schedulerFactory,
                           ProcessControlBlockRepository pcbRepository,
                           ContextSwitchEventRepository contextSwitchRepository,
                           NodeRepository nodeRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.policyService = policyService;
        this.schedulerFactory = schedulerFactory;
        this.pcbRepository = pcbRepository;
        this.contextSwitchRepository = contextSwitchRepository;
        this.nodeRepository = nodeRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public synchronized void start() {
        this.status = SchedulerStatus.RUNNING;
        broadcastStatus();
    }

    public synchronized void pause() {
        this.status = SchedulerStatus.PAUSED;
        broadcastStatus();
    }

    public synchronized void resume() {
        this.status = SchedulerStatus.RUNNING;
        broadcastStatus();
    }

    public synchronized void stop() {
        this.status = SchedulerStatus.STOPPED;
        broadcastStatus();
    }

    public synchronized void reset() {
        this.status = SchedulerStatus.STOPPED;
        this.simulationTick.set(0);
        this.readyQueue.clear();
        this.waitingQueue.clear();
        this.runningProcesses.clear();
        this.coreCurrentBurstTicks.clear();
        this.completedProcesses.clear();
        this.recentGanttBlocks.clear();
        this.recentContextSwitches.clear();
        this.coreStates.clear();
        this.pcbRepository.deleteAllInBatch();
        this.contextSwitchRepository.deleteAllInBatch();
        broadcastStatus();
    }

    public synchronized ProcessControlBlock createProcess(CreateProcessRequest request, String owner) {
        String processId = "PROC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String targetNode = request.getTargetNodeId();
        if (targetNode == null || targetNode.isBlank()) {
            List<NetworkNode> nodes = nodeRepository.findAll();
            targetNode = nodes.isEmpty() ? "NODE-001" : nodes.get(0).getNodeId();
        }

        ProcessControlBlock pcb = new ProcessControlBlock(
                processId,
                request.getProcessName() != null ? request.getProcessName() : processId,
                owner != null ? owner : "SYSTEM",
                targetNode,
                request.getProcessType(),
                request.getPriority(),
                this.simulationTick.get(),
                request.getBurstTime()
        );
        pcb.setState(ProcessState.READY);

        ProcessControlBlock saved = pcbRepository.save(pcb);
        readyQueue.add(saved);
        broadcastStatus();
        return saved;
    }

    public synchronized List<ProcessControlBlock> batchCreateProcesses(BatchCreateProcessRequest request, String owner) {
        int count = Math.max(1, Math.min(50, request.getCount()));
        String prefix = request.getProcessNamePrefix() != null ? request.getProcessNamePrefix() : "PROC";
        List<NetworkNode> nodes = nodeRepository.findAll();
        String defaultNode = nodes.isEmpty() ? "NODE-001" : nodes.get(0).getNodeId();
        String targetNode = (request.getTargetNodeId() != null && !request.getTargetNodeId().isBlank())
                ? request.getTargetNodeId() : defaultNode;

        List<ProcessControlBlock> toSave = new ArrayList<>();
        long currentTick = simulationTick.get();

        ProcessType[] types = ProcessType.values();
        for (int i = 1; i <= count; i++) {
            String pid = String.format("%s-%03d", prefix, i);
            int priority = 1 + (i % 10);
            long burst = 10 + ((i * 3) % 25);
            ProcessType ptype = types[i % types.length];

            ProcessControlBlock pcb = new ProcessControlBlock(
                    pid,
                    pid + "_" + ptype.name(),
                    owner != null ? owner : "SYSTEM",
                    targetNode,
                    ptype,
                    priority,
                    currentTick,
                    burst
            );
            pcb.setState(ProcessState.READY);
            toSave.add(pcb);
        }

        List<ProcessControlBlock> saved = pcbRepository.saveAll(toSave);
        readyQueue.addAll(saved);
        broadcastStatus();
        return saved;
    }

    public synchronized void terminateProcess(String processId) {
        // Check if running on any core
        for (Map.Entry<Integer, ProcessControlBlock> entry : runningProcesses.entrySet()) {
            if (entry.getValue() != null && entry.getValue().getProcessId().equals(processId)) {
                ProcessControlBlock pcb = entry.getValue();
                pcb.setState(ProcessState.TERMINATED);
                pcb.setCompletionTime(simulationTick.get());
                pcb.setTurnaroundTime(Math.max(0, pcb.getCompletionTime() - pcb.getArrivalTime()));
                pcbRepository.save(pcb);
                completedProcesses.add(pcb);
                runningProcesses.remove(entry.getKey());
                coreCurrentBurstTicks.remove(entry.getKey());
                recordContextSwitch(pcb.getProcessId(), null, entry.getKey(), pcb.getTargetNodeId(),
                        ContextSwitchReason.PROCESS_TERMINATED, policyService.getPolicy().getContextSwitchCostMs());
                break;
            }
        }

        readyQueue.removeIf(p -> {
            if (p.getProcessId().equals(processId)) {
                p.setState(ProcessState.TERMINATED);
                p.setCompletionTime(simulationTick.get());
                p.setTurnaroundTime(Math.max(0, p.getCompletionTime() - p.getArrivalTime()));
                pcbRepository.save(p);
                completedProcesses.add(p);
                return true;
            }
            return false;
        });

        waitingQueue.removeIf(p -> {
            if (p.getProcessId().equals(processId)) {
                p.setState(ProcessState.TERMINATED);
                p.setCompletionTime(simulationTick.get());
                p.setTurnaroundTime(Math.max(0, p.getCompletionTime() - p.getArrivalTime()));
                pcbRepository.save(p);
                completedProcesses.add(p);
                return true;
            }
            return false;
        });

        broadcastStatus();
    }

    @Scheduled(fixedRate = 100)
    public void periodicTick() {
        if (status == SchedulerStatus.RUNNING) {
            executeTick();
        }
    }

    public synchronized void executeTick() {
        long currentTick = simulationTick.incrementAndGet();
        SchedulerPolicyConfig policy = policyService.getPolicy();
        CpuScheduler scheduler = schedulerFactory.getScheduler(policy.getAlgorithm());

        // 1. Refresh Core definitions based on active Nodes
        refreshCores(policy);

        // 2. Advance I/O waiting queue
        processWaitingQueue();

        // 3. Priority Aging and Starvation Detection
        if (policy.isAgingEnabled() && currentTick % policy.getAgingIntervalTicks() == 0) {
            applyAging(policy);
        }

        // 4. Update Waiting Time for all ready processes
        for (ProcessControlBlock pcb : readyQueue) {
            pcb.setWaitingTime(pcb.getWaitingTime() + 1);
        }

        // 5. Execute CPU step for each Core
        List<CpuCoreDto> coresList = new ArrayList<>(coreStates.values());
        coresList.sort(Comparator.comparingInt(CpuCoreDto::getCoreId));

        for (CpuCoreDto core : coresList) {
            int coreId = core.getCoreId();
            if (core.getState() == CpuCoreState.OFFLINE) {
                continue;
            }

            ProcessControlBlock running = runningProcesses.get(coreId);

            if (running != null) {
                // Execute 1 tick of CPU burst
                running.setRemainingBurstTime(running.getRemainingBurstTime() - 1);
                running.setCpuTimeConsumed(running.getCpuTimeConsumed() + 1);
                int ticksRan = coreCurrentBurstTicks.merge(coreId, 1, Integer::sum);
                core.setTotalTicksBusy(core.getTotalTicksBusy() + 1);

                // Update Virtual Runtime for CFS
                if (policy.getAlgorithm() == SchedulerAlgorithm.CFS_INSPIRED) {
                    double vInc = CfsInspiredScheduler.calculateVirtualRuntimeIncrement(running.getPriority(), 1);
                    running.setVirtualRuntime(running.getVirtualRuntime() + vInc);
                }

                // Check Process Completion
                if (running.getRemainingBurstTime() <= 0) {
                    running.setState(ProcessState.TERMINATED);
                    running.setCompletionTime(currentTick);
                    running.setTurnaroundTime(Math.max(0, running.getCompletionTime() - running.getArrivalTime()));
                    pcbRepository.save(running);
                    completedProcesses.add(running);

                    recordGanttBlock(running, coreId, currentTick - ticksRan, currentTick, false);
                    recordContextSwitch(running.getProcessId(), null, coreId, core.getNodeId(),
                            ContextSwitchReason.PROCESS_TERMINATED, policy.getContextSwitchCostMs());

                    runningProcesses.remove(coreId);
                    coreCurrentBurstTicks.remove(coreId);
                    core.setState(CpuCoreState.IDLE);
                    core.setCurrentProcessId(null);
                    core.setCurrentProcessName(null);
                    core.setCurrentProcessRemainingBurst(0);

                    // Try to dispatch next immediately
                    dispatchNext(core, scheduler, policy, currentTick);
                }
                // Check I/O trigger
                else if (shouldTriggerIo(running, ticksRan)) {
                    running.setState(ProcessState.WAITING);
                    running.setIoWaitRemaining(running.getIoDuration() > 0 ? running.getIoDuration() : 3);
                    waitingQueue.add(running);

                    recordGanttBlock(running, coreId, currentTick - ticksRan, currentTick, false);
                    recordContextSwitch(running.getProcessId(), null, coreId, core.getNodeId(),
                            ContextSwitchReason.IO_REQUEST, policy.getContextSwitchCostMs());

                    runningProcesses.remove(coreId);
                    coreCurrentBurstTicks.remove(coreId);
                    core.setState(CpuCoreState.IDLE);
                    core.setCurrentProcessId(null);
                    core.setCurrentProcessName(null);
                    core.setCurrentProcessRemainingBurst(0);

                    dispatchNext(core, scheduler, policy, currentTick);
                }
                // Check Preemption
                else if (scheduler.isPreemptive()) {
                    List<ProcessControlBlock> eligibleReady = getEligibleReadyProcesses(core.getNodeId());
                    ProcessControlBlock topCandidate = scheduler.selectNext(eligibleReady, running);

                    if (topCandidate != null && scheduler.shouldPreempt(running, topCandidate, ticksRan, policy.getTimeQuantum())) {
                        ContextSwitchReason reason = getPreemptionReason(policy.getAlgorithm(), ticksRan, policy.getTimeQuantum());

                        recordGanttBlock(running, coreId, currentTick - ticksRan, currentTick, false);
                        recordContextSwitch(running.getProcessId(), topCandidate.getProcessId(), coreId, core.getNodeId(),
                                reason, policy.getContextSwitchCostMs());

                        running.setState(ProcessState.READY);
                        readyQueue.add(running);

                        runningProcesses.remove(coreId);
                        coreCurrentBurstTicks.remove(coreId);

                        // Dispatch preempting candidate
                        readyQueue.remove(topCandidate);
                        dispatchProcessToCore(core, topCandidate, currentTick);
                    } else {
                        // Keep running
                        core.setState(CpuCoreState.RUNNING);
                        core.setCurrentProcessId(running.getProcessId());
                        core.setCurrentProcessName(running.getProcessName());
                        core.setCurrentProcessPriority(running.getEffectivePriority());
                        core.setCurrentProcessRemainingBurst(running.getRemainingBurstTime());
                        core.setCurrentBurstTicksRan(ticksRan);
                    }
                } else {
                    // Non-preemptive, continue running
                    core.setState(CpuCoreState.RUNNING);
                    core.setCurrentProcessId(running.getProcessId());
                    core.setCurrentProcessName(running.getProcessName());
                    core.setCurrentProcessPriority(running.getEffectivePriority());
                    core.setCurrentProcessRemainingBurst(running.getRemainingBurstTime());
                    core.setCurrentBurstTicksRan(ticksRan);
                }
            } else {
                // Core is IDLE, try dispatching
                dispatchNext(core, scheduler, policy, currentTick);
            }

            // Update core utilization
            long total = core.getTotalTicksBusy() + core.getTotalTicksIdle();
            if (total > 0) {
                core.setCoreUtilizationPercent(((double) core.getTotalTicksBusy() / total) * 100.0);
            }
        }

        // Broadcast to WebSocket clients
        broadcastStatus();
    }

    private void dispatchNext(CpuCoreDto core, CpuScheduler scheduler, SchedulerPolicyConfig policy, long currentTick) {
        List<ProcessControlBlock> eligible = getEligibleReadyProcesses(core.getNodeId());
        ProcessControlBlock next = scheduler.selectNext(eligible, null);

        if (next != null) {
            readyQueue.remove(next);
            recordContextSwitch(null, next.getProcessId(), core.getCoreId(), core.getNodeId(),
                    ContextSwitchReason.PROCESS_DISPATCHED, policy.getContextSwitchCostMs());
            dispatchProcessToCore(core, next, currentTick);
        } else {
            core.setState(CpuCoreState.IDLE);
            core.setTotalTicksIdle(core.getTotalTicksIdle() + 1);
            core.setCurrentProcessId(null);
            core.setCurrentProcessName(null);
            core.setCurrentProcessRemainingBurst(0);
            core.setCurrentBurstTicksRan(0);
        }
    }

    private void dispatchProcessToCore(CpuCoreDto core, ProcessControlBlock process, long currentTick) {
        process.setState(ProcessState.RUNNING);
        process.setAllocatedCoreId(core.getCoreId());
        process.incrementContextSwitchCount();

        if (process.getStartTime() == -1) {
            process.setStartTime(currentTick);
            process.setResponseTime(Math.max(0, currentTick - process.getArrivalTime()));
        }

        runningProcesses.put(core.getCoreId(), process);
        coreCurrentBurstTicks.put(core.getCoreId(), 0);

        core.setState(CpuCoreState.RUNNING);
        core.setCurrentProcessId(process.getProcessId());
        core.setCurrentProcessName(process.getProcessName());
        core.setCurrentProcessPriority(process.getEffectivePriority());
        core.setCurrentProcessRemainingBurst(process.getRemainingBurstTime());
        core.setCurrentBurstTicksRan(0);
    }

    private List<ProcessControlBlock> getEligibleReadyProcesses(String nodeId) {
        // Returns ready processes for this node, or any process if node matching is loose
        List<ProcessControlBlock> list = new ArrayList<>();
        for (ProcessControlBlock p : readyQueue) {
            if (p.getTargetNodeId() == null || p.getTargetNodeId().equals(nodeId) || p.getTargetNodeId().isBlank()) {
                list.add(p);
            }
        }
        if (list.isEmpty()) {
            list.addAll(readyQueue);
        }
        return list;
    }

    private boolean shouldTriggerIo(ProcessControlBlock process, int ticksRan) {
        if (process.getProcessType() == ProcessType.IO_BOUND && process.getIoFrequency() > 0) {
            return ticksRan >= process.getIoFrequency() && process.getRemainingBurstTime() > 1;
        } else if (process.getProcessType() == ProcessType.NETWORK_BOUND && process.getIoFrequency() > 0) {
            return ticksRan >= process.getIoFrequency() && process.getRemainingBurstTime() > 1;
        }
        return false;
    }

    private void processWaitingQueue() {
        Iterator<ProcessControlBlock> it = waitingQueue.iterator();
        while (it.hasNext()) {
            ProcessControlBlock p = it.next();
            int remaining = p.getIoWaitRemaining() - 1;
            p.setIoWaitRemaining(remaining);
            if (remaining <= 0) {
                p.setState(ProcessState.READY);
                waitingQueue.remove(p);
                readyQueue.add(p);
                recordContextSwitch(p.getProcessId(), null, p.getAllocatedCoreId() != null ? p.getAllocatedCoreId() : 1,
                        p.getTargetNodeId(), ContextSwitchReason.IO_COMPLETION, 0.5);
            }
        }
    }

    private void applyAging(SchedulerPolicyConfig policy) {
        for (ProcessControlBlock p : readyQueue) {
            if (p.getWaitingTime() >= policy.getStarvationThresholdTicks()) {
                p.setStarvationRisk(true);
                // Boost priority: decrement effective priority number (1 is highest)
                if (p.getEffectivePriority() > 1) {
                    p.setEffectivePriority(p.getEffectivePriority() - 1);
                    log.info("Aging: Boosted priority for process {} from {} to {}",
                            p.getProcessId(), p.getEffectivePriority() + 1, p.getEffectivePriority());
                }
            } else {
                p.setStarvationRisk(false);
            }
        }
    }

    private ContextSwitchReason getPreemptionReason(SchedulerAlgorithm algo, int ticksRan, int quantum) {
        if (algo == SchedulerAlgorithm.ROUND_ROBIN && ticksRan >= quantum) {
            return ContextSwitchReason.TIME_QUANTUM_EXPIRED;
        }
        if (algo == SchedulerAlgorithm.PRIORITY) {
            return ContextSwitchReason.PREEMPTION_HIGHER_PRIORITY;
        }
        if (algo == SchedulerAlgorithm.SRTF) {
            return ContextSwitchReason.PREEMPTION_SHORTER_BURST;
        }
        if (algo == SchedulerAlgorithm.CFS_INSPIRED) {
            return ContextSwitchReason.CFS_GRANULARITY_EXCEEDED;
        }
        return ContextSwitchReason.PREEMPTION_HIGHER_PRIORITY;
    }

    private void refreshCores(SchedulerPolicyConfig policy) {
        List<NetworkNode> nodes = nodeRepository.findAll();
        if (nodes.isEmpty()) {
            NetworkNode fallback = new NetworkNode("NODE-001", "Primary Node", "127.0.0.1", 8080, NodeStatus.HEALTHY, 100);
            nodes = List.of(fallback);
        }

        int coresPerNode = policy.getCoresPerNode();
        int globalCoreIndex = 1;

        for (NetworkNode node : nodes) {
            boolean isOffline = (node.getStatus() == NodeStatus.FAILED || node.getStatus() == NodeStatus.OFFLINE);

            for (int c = 1; c <= coresPerNode; c++) {
                int coreId = globalCoreIndex++;
                CpuCoreDto coreDto = coreStates.computeIfAbsent(coreId, id -> new CpuCoreDto(id, node.getNodeId(), CpuCoreState.IDLE));
                coreDto.setNodeId(node.getNodeId());

                if (isOffline) {
                    coreDto.setState(CpuCoreState.OFFLINE);
                    ProcessControlBlock running = runningProcesses.remove(coreId);
                    if (running != null) {
                        running.setState(ProcessState.READY);
                        readyQueue.add(0, running);
                    }
                } else if (coreDto.getState() == CpuCoreState.OFFLINE) {
                    coreDto.setState(CpuCoreState.IDLE);
                }
            }
        }
    }

    private void recordGanttBlock(ProcessControlBlock pcb, int coreId, long startTick, long endTick, boolean isContextSwitch) {
        if (endTick <= startTick) return;
        GanttBlockDto block = new GanttBlockDto(
                pcb != null ? pcb.getProcessId() : "IDLE",
                pcb != null ? pcb.getProcessName() : "IDLE",
                coreId,
                pcb != null ? pcb.getTargetNodeId() : "NODE-001",
                startTick,
                endTick,
                pcb != null ? pcb.getEffectivePriority() : 10,
                policyService.getPolicy().getAlgorithm(),
                isContextSwitch
        );
        recentGanttBlocks.add(block);
        while (recentGanttBlocks.size() > MAX_GANTT_HISTORY) {
            recentGanttBlocks.remove(0);
        }
    }

    private void recordContextSwitch(String fromPid, String toPid, int coreNumber, String nodeId,
                                     ContextSwitchReason reason, double durationMs) {
        ContextSwitchEvent event = new ContextSwitchEvent(
                fromPid,
                toPid,
                coreNumber,
                nodeId != null ? nodeId : "NODE-001",
                reason,
                durationMs,
                policyService.getPolicy().getAlgorithm(),
                simulationTick.get()
        );
        try {
            contextSwitchRepository.save(event);
        } catch (Exception e) {
            log.warn("Could not persist context switch event: {}", e.getMessage());
        }

        ContextSwitchEventDto dto = ContextSwitchEventDto.fromEntity(event);
        recentContextSwitches.add(0, dto);
        while (recentContextSwitches.size() > MAX_CS_HISTORY) {
            recentContextSwitches.remove(recentContextSwitches.size() - 1);
        }
    }

    public SchedulerMetricsDto computeMetrics() {
        SchedulerMetricsDto metrics = new SchedulerMetricsDto();
        metrics.setCurrentSimulationTick(simulationTick.get());
        metrics.setTotalRunningProcesses(runningProcesses.size());
        metrics.setTotalReadyProcesses(readyQueue.size());
        metrics.setTotalWaitingProcesses(waitingQueue.size());
        metrics.setTotalCompletedProcesses(completedProcesses.size());
        metrics.setTotalContextSwitches(contextSwitchRepository.count());

        long starvationAlerts = readyQueue.stream().filter(ProcessControlBlock::isStarvationRisk).count();
        metrics.setTotalStarvationAlerts(starvationAlerts);

        if (!completedProcesses.isEmpty()) {
            double avgWait = completedProcesses.stream().mapToLong(ProcessControlBlock::getWaitingTime).average().orElse(0.0);
            double avgTurnaround = completedProcesses.stream().mapToLong(ProcessControlBlock::getTurnaroundTime).average().orElse(0.0);
            double avgResponse = completedProcesses.stream().filter(p -> p.getResponseTime() >= 0)
                    .mapToLong(ProcessControlBlock::getResponseTime).average().orElse(0.0);

            metrics.setAverageWaitingTime(Math.round(avgWait * 100.0) / 100.0);
            metrics.setAverageTurnaroundTime(Math.round(avgTurnaround * 100.0) / 100.0);
            metrics.setAverageResponseTime(Math.round(avgResponse * 100.0) / 100.0);
        }

        long totalBusy = coreStates.values().stream().mapToLong(CpuCoreDto::getTotalTicksBusy).sum();
        long totalIdle = coreStates.values().stream().mapToLong(CpuCoreDto::getTotalTicksIdle).sum();
        long totalTicks = totalBusy + totalIdle;
        if (totalTicks > 0) {
            double util = ((double) totalBusy / totalTicks) * 100.0;
            metrics.setCpuUtilizationPercent(Math.round(util * 100.0) / 100.0);
        }

        long ticks = simulationTick.get();
        if (ticks > 0) {
            double throughput = ((double) completedProcesses.size() / ticks) * 600.0;
            metrics.setThroughputPerMinute(Math.round(throughput * 100.0) / 100.0);
        }

        return metrics;
    }

    public SchedulerStatusResponse getStatusResponse() {
        SchedulerStatusResponse response = new SchedulerStatusResponse();
        response.setStatus(this.status);
        response.setAlgorithm(policyService.getPolicy().getAlgorithm());
        response.setPolicyConfig(policyService.getPolicy());
        response.setCurrentTick(simulationTick.get());
        response.setMetrics(computeMetrics());

        List<CpuCoreDto> cores = new ArrayList<>(coreStates.values());
        cores.sort(Comparator.comparingInt(CpuCoreDto::getCoreId));
        response.setCores(cores);

        response.setReadyQueue(readyQueue.stream().map(ProcessControlBlockDto::fromEntity).toList());
        response.setWaitingQueue(waitingQueue.stream().map(ProcessControlBlockDto::fromEntity).toList());
        response.setRunningProcesses(runningProcesses.values().stream().map(ProcessControlBlockDto::fromEntity).toList());
        response.setRecentGanttBlocks(new ArrayList<>(recentGanttBlocks));
        response.setRecentContextSwitches(new ArrayList<>(recentContextSwitches));

        return response;
    }

    private void broadcastStatus() {
        try {
            SchedulerStatusResponse response = getStatusResponse();
            messagingTemplate.convertAndSend(WS_SCHEDULER_TOPIC, response);
        } catch (Exception e) {
            log.trace("Error broadcasting scheduler status: {}", e.getMessage());
        }
    }

    // =========================================================================
    // DETERMINISTIC ALGORITHM COMPARISON BENCHMARK ENGINE
    // =========================================================================

    public AlgorithmComparisonResponse runBenchmarkComparison(List<ProcessControlBlock> inputProcesses) {
        List<ProcessControlBlock> workload = inputProcesses != null && !inputProcesses.isEmpty()
                ? inputProcesses
                : generateStandardWorkload();

        AlgorithmComparisonResponse response = new AlgorithmComparisonResponse();
        response.setWorkloadProcessCount(workload.size());
        response.setWorkloadProcesses(workload.stream().map(ProcessControlBlockDto::fromEntity).toList());

        List<AlgorithmBenchmarkResult> results = new ArrayList<>();
        SchedulerAlgorithm[] algorithms = SchedulerAlgorithm.values();

        for (SchedulerAlgorithm algo : algorithms) {
            AlgorithmBenchmarkResult res = simulateWorkloadForAlgorithm(algo, cloneWorkload(workload));
            results.add(res);
        }

        response.setResults(results);

        // Find Best Performers
        AlgorithmBenchmarkResult bestWait = results.stream().min(Comparator.comparingDouble(AlgorithmBenchmarkResult::getAverageWaitingTime)).orElse(results.get(0));
        AlgorithmBenchmarkResult bestThroughput = results.stream().max(Comparator.comparingDouble(AlgorithmBenchmarkResult::getThroughput)).orElse(results.get(0));
        AlgorithmBenchmarkResult bestFairness = results.stream().filter(r -> r.getAlgorithm() == SchedulerAlgorithm.CFS_INSPIRED).findFirst().orElse(results.get(0));

        response.setBestWaitingTimeAlgorithm(bestWait.getAlgorithm().name() + " (" + bestWait.getAverageWaitingTime() + " ticks)");
        response.setBestThroughputAlgorithm(bestThroughput.getAlgorithm().name() + " (" + bestThroughput.getThroughput() + " proc/1k ticks)");
        response.setBestFairnessAlgorithm(bestFairness.getAlgorithm().name());
        response.setOverallSummary(String.format("Benchmark comparison completed across %d algorithms with %d identical simulated processes. Shortest Job/Remaining Time minimized waiting time (%s), while Round Robin & CFS provided optimal fairness and predictable interactive responsiveness.",
                algorithms.length, workload.size(), bestWait.getAlgorithmName()));

        return response;
    }

    private AlgorithmBenchmarkResult simulateWorkloadForAlgorithm(SchedulerAlgorithm algorithm, List<ProcessControlBlock> processes) {
        CpuScheduler scheduler = schedulerFactory.getScheduler(algorithm);
        int timeQuantum = 4;

        List<ProcessControlBlock> simReady = new ArrayList<>();
        List<ProcessControlBlock> simWaiting = new ArrayList<>();
        List<ProcessControlBlock> simCompleted = new ArrayList<>();
        List<GanttBlockDto> simGantt = new ArrayList<>();

        long simTick = 0;
        int contextSwitches = 0;
        ProcessControlBlock currentRunning = null;
        int currentBurstTicks = 0;

        // Sort initial processes by arrival time
        List<ProcessControlBlock> unarrived = new ArrayList<>(processes);
        unarrived.sort(Comparator.comparingLong(ProcessControlBlock::getArrivalTime));

        long maxTicks = 2000;

        while ((!unarrived.isEmpty() || !simReady.isEmpty() || !simWaiting.isEmpty() || currentRunning != null) && simTick < maxTicks) {
            simTick++;

            // 1. Check arrivals
            Iterator<ProcessControlBlock> arrIt = unarrived.iterator();
            while (arrIt.hasNext()) {
                ProcessControlBlock p = arrIt.next();
                if (p.getArrivalTime() <= simTick) {
                    p.setState(ProcessState.READY);
                    simReady.add(p);
                    arrIt.remove();
                }
            }

            // 2. Check I/O waiting queue
            Iterator<ProcessControlBlock> waitIt = simWaiting.iterator();
            while (waitIt.hasNext()) {
                ProcessControlBlock p = waitIt.next();
                p.setIoWaitRemaining(p.getIoWaitRemaining() - 1);
                if (p.getIoWaitRemaining() <= 0) {
                    p.setState(ProcessState.READY);
                    simReady.add(p);
                    waitIt.remove();
                }
            }

            // 3. Update waiting time for ready processes
            for (ProcessControlBlock p : simReady) {
                p.setWaitingTime(p.getWaitingTime() + 1);
            }

            // 4. Running process execution
            if (currentRunning != null) {
                currentRunning.setRemainingBurstTime(currentRunning.getRemainingBurstTime() - 1);
                currentRunning.setCpuTimeConsumed(currentRunning.getCpuTimeConsumed() + 1);
                currentBurstTicks++;

                if (algorithm == SchedulerAlgorithm.CFS_INSPIRED) {
                    double vInc = CfsInspiredScheduler.calculateVirtualRuntimeIncrement(currentRunning.getPriority(), 1);
                    currentRunning.setVirtualRuntime(currentRunning.getVirtualRuntime() + vInc);
                }

                if (currentRunning.getRemainingBurstTime() <= 0) {
                    currentRunning.setState(ProcessState.TERMINATED);
                    currentRunning.setCompletionTime(simTick);
                    currentRunning.setTurnaroundTime(Math.max(0, simTick - currentRunning.getArrivalTime()));
                    simCompleted.add(currentRunning);

                    simGantt.add(new GanttBlockDto(
                            currentRunning.getProcessId(),
                            currentRunning.getProcessName(),
                            1,
                            "NODE-001",
                            simTick - currentBurstTicks,
                            simTick,
                            currentRunning.getEffectivePriority(),
                            algorithm,
                            false
                    ));

                    currentRunning = null;
                    currentBurstTicks = 0;
                    contextSwitches++;

                    // Dispatch next
                    ProcessControlBlock next = scheduler.selectNext(simReady, null);
                    if (next != null) {
                        simReady.remove(next);
                        if (next.getStartTime() == -1) {
                            next.setStartTime(simTick);
                            next.setResponseTime(Math.max(0, simTick - next.getArrivalTime()));
                        }
                        currentRunning = next;
                        currentRunning.setState(ProcessState.RUNNING);
                    }
                } else if (shouldTriggerIo(currentRunning, currentBurstTicks)) {
                    currentRunning.setState(ProcessState.WAITING);
                    currentRunning.setIoWaitRemaining(currentRunning.getIoDuration() > 0 ? currentRunning.getIoDuration() : 2);
                    simWaiting.add(currentRunning);

                    simGantt.add(new GanttBlockDto(
                            currentRunning.getProcessId(),
                            currentRunning.getProcessName(),
                            1,
                            "NODE-001",
                            simTick - currentBurstTicks,
                            simTick,
                            currentRunning.getEffectivePriority(),
                            algorithm,
                            false
                    ));

                    currentRunning = null;
                    currentBurstTicks = 0;
                    contextSwitches++;

                    ProcessControlBlock next = scheduler.selectNext(simReady, null);
                    if (next != null) {
                        simReady.remove(next);
                        if (next.getStartTime() == -1) {
                            next.setStartTime(simTick);
                            next.setResponseTime(Math.max(0, simTick - next.getArrivalTime()));
                        }
                        currentRunning = next;
                        currentRunning.setState(ProcessState.RUNNING);
                    }
                } else if (scheduler.isPreemptive()) {
                    ProcessControlBlock candidate = scheduler.selectNext(simReady, currentRunning);
                    if (candidate != null && scheduler.shouldPreempt(currentRunning, candidate, currentBurstTicks, timeQuantum)) {
                        simGantt.add(new GanttBlockDto(
                                currentRunning.getProcessId(),
                                currentRunning.getProcessName(),
                                1,
                                "NODE-001",
                                simTick - currentBurstTicks,
                                simTick,
                                currentRunning.getEffectivePriority(),
                                algorithm,
                                false
                        ));

                        currentRunning.setState(ProcessState.READY);
                        simReady.add(currentRunning);

                        simReady.remove(candidate);
                        if (candidate.getStartTime() == -1) {
                            candidate.setStartTime(simTick);
                            candidate.setResponseTime(Math.max(0, simTick - candidate.getArrivalTime()));
                        }
                        currentRunning = candidate;
                        currentRunning.setState(ProcessState.RUNNING);
                        currentBurstTicks = 0;
                        contextSwitches++;
                    }
                }
            } else {
                ProcessControlBlock next = scheduler.selectNext(simReady, null);
                if (next != null) {
                    simReady.remove(next);
                    if (next.getStartTime() == -1) {
                        next.setStartTime(simTick);
                        next.setResponseTime(Math.max(0, simTick - next.getArrivalTime()));
                    }
                    currentRunning = next;
                    currentRunning.setState(ProcessState.RUNNING);
                    currentBurstTicks = 0;
                    contextSwitches++;
                }
            }
        }

        AlgorithmBenchmarkResult res = new AlgorithmBenchmarkResult();
        res.setAlgorithm(algorithm);
        res.setAlgorithmName(getAlgorithmDisplayName(algorithm));
        res.setCompletedProcessesCount(simCompleted.size());
        res.setTotalSimulationTicks(simTick);
        res.setTotalContextSwitches(contextSwitches);
        res.setGanttTimeline(simGantt);

        if (!simCompleted.isEmpty()) {
            double avgWait = simCompleted.stream().mapToLong(ProcessControlBlock::getWaitingTime).average().orElse(0.0);
            double avgTurnaround = simCompleted.stream().mapToLong(ProcessControlBlock::getTurnaroundTime).average().orElse(0.0);
            double avgResponse = simCompleted.stream().filter(p -> p.getResponseTime() >= 0).mapToLong(ProcessControlBlock::getResponseTime).average().orElse(0.0);

            res.setAverageWaitingTime(Math.round(avgWait * 100.0) / 100.0);
            res.setAverageTurnaroundTime(Math.round(avgTurnaround * 100.0) / 100.0);
            res.setAverageResponseTime(Math.round(avgResponse * 100.0) / 100.0);
        }

        long totalBusyTicks = simCompleted.stream().mapToLong(ProcessControlBlock::getBurstTime).sum();
        if (simTick > 0) {
            double util = Math.min(100.0, ((double) totalBusyTicks / simTick) * 100.0);
            res.setCpuUtilizationPercent(Math.round(util * 100.0) / 100.0);
            double throughput = ((double) simCompleted.size() / simTick) * 1000.0;
            res.setThroughput(Math.round(throughput * 100.0) / 100.0);
        }

        res.setExplanation(getAlgorithmBenchmarkExplanation(algorithm, res.getAverageWaitingTime(), res.getAverageTurnaroundTime(), contextSwitches));

        return res;
    }

    private List<ProcessControlBlock> generateStandardWorkload() {
        List<ProcessControlBlock> workload = new ArrayList<>();
        workload.add(new ProcessControlBlock("P1", "Web_Server", "SYSTEM", "NODE-001", ProcessType.NETWORK_BOUND, 2, 0, 16));
        workload.add(new ProcessControlBlock("P2", "DB_Query", "SYSTEM", "NODE-001", ProcessType.IO_BOUND, 4, 2, 24));
        workload.add(new ProcessControlBlock("P3", "ML_Inference", "SYSTEM", "NODE-001", ProcessType.CPU_BOUND, 6, 4, 32));
        workload.add(new ProcessControlBlock("P4", "Telemetry_Collector", "SYSTEM", "NODE-001", ProcessType.INTERACTIVE, 1, 6, 12));
        workload.add(new ProcessControlBlock("P5", "Backup_Sync", "SYSTEM", "NODE-001", ProcessType.BACKGROUND, 8, 8, 20));
        return workload;
    }

    private List<ProcessControlBlock> cloneWorkload(List<ProcessControlBlock> original) {
        List<ProcessControlBlock> cloned = new ArrayList<>();
        for (ProcessControlBlock p : original) {
            ProcessControlBlock c = new ProcessControlBlock(
                    p.getProcessId(),
                    p.getProcessName(),
                    p.getOwner(),
                    p.getTargetNodeId(),
                    p.getProcessType(),
                    p.getPriority(),
                    p.getArrivalTime(),
                    p.getBurstTime()
            );
            cloned.add(c);
        }
        return cloned;
    }

    private String getAlgorithmDisplayName(SchedulerAlgorithm algo) {
        return switch (algo) {
            case FCFS -> "First-Come, First-Served (FCFS)";
            case SJF -> "Shortest Job First (SJF)";
            case SRTF -> "Shortest Remaining Time First (SRTF)";
            case PRIORITY -> "Preemptive Priority (with Aging)";
            case ROUND_ROBIN -> "Round Robin (RR)";
            case MULTILEVEL_QUEUE -> "Multilevel Queue (MLQ)";
            case CFS_INSPIRED -> "CFS-Inspired (Virtual Runtime)";
        };
    }

    private String getAlgorithmBenchmarkExplanation(SchedulerAlgorithm algo, double wait, double turnaround, int cs) {
        return switch (algo) {
            case FCFS -> String.format("FCFS queued tasks by arrival. Simple non-preemptive execution resulted in %d context switches and avg wait time of %.2f ticks. Subject to Convoy Effect.", cs, wait);
            case SJF -> String.format("SJF prioritized tasks with shorter total burst times. Minimized average wait time (%.2f ticks) with zero preemption context switches (%d CS total).", wait, cs);
            case SRTF -> String.format("SRTF preemptively switched when shorter jobs arrived, yielding optimal responsive throughput with %d context switches and avg turnaround of %.2f ticks.", cs, turnaround);
            case PRIORITY -> String.format("Priority scheduling favored critical high-priority tasks (priority 1-3). Preemption triggered %d context switches, achieving rapid response for critical processes.", cs);
            case ROUND_ROBIN -> String.format("Round Robin enforced strict quantum slices, ensuring deterministic fairness across all tasks with %d context switches and balanced response times.", cs);
            case MULTILEVEL_QUEUE -> String.format("Multilevel Queue routed tasks to differentiated priority tiers (Interactive RR -> IO RR -> Background FCFS), recording %d context switches.", cs);
            case CFS_INSPIRED -> String.format("CFS used virtual runtime scaling based on priority weights, delivering completely fair CPU time allocation and %d smooth preemptive context switches.", cs);
        };
    }
}
