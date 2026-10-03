package com.netpulse.scheduler.strategy;

import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class CfsInspiredScheduler implements CpuScheduler {

    private static final double MIN_GRANULARITY = 1.0;

    @Override
    public SchedulerAlgorithm getAlgorithm() {
        return SchedulerAlgorithm.CFS_INSPIRED;
    }

    @Override
    public boolean isPreemptive() {
        return true;
    }

    @Override
    public ProcessControlBlock selectNext(List<ProcessControlBlock> readyQueue, ProcessControlBlock currentlyRunning) {
        if (readyQueue == null || readyQueue.isEmpty()) {
            return null;
        }
        // Smallest virtual runtime gets scheduled first
        return readyQueue.stream()
                .min(Comparator.comparingDouble(ProcessControlBlock::getVirtualRuntime)
                        .thenComparingLong(ProcessControlBlock::getArrivalTime)
                        .thenComparing(ProcessControlBlock::getProcessId))
                .orElse(null);
    }

    @Override
    public boolean shouldPreempt(ProcessControlBlock currentlyRunning, ProcessControlBlock candidate, int ticksRanInCurrentBurst, int timeQuantum) {
        if (currentlyRunning == null || candidate == null) {
            return false;
        }
        // Preempt if candidate virtual runtime is significantly lower than running process virtual runtime
        return (currentlyRunning.getVirtualRuntime() - candidate.getVirtualRuntime()) >= MIN_GRANULARITY;
    }

    @Override
    public String getDecisionReason(ProcessControlBlock selected, List<ProcessControlBlock> readyQueue) {
        if (selected == null) return "No process available in ready queue";
        return String.format("CFS-Inspired: Process [%s] selected with lowest virtual runtime (%.2f vruntime)",
                selected.getProcessName(), selected.getVirtualRuntime());
    }

    public static double calculateVirtualRuntimeIncrement(int priority, long ticks) {
        // Priority 1 (highest) -> weight factor 0.5 (slow vruntime growth)
        // Priority 5 (normal)  -> weight factor 1.0
        // Priority 10 (lowest) -> weight factor 2.0 (fast vruntime growth)
        int clamped = Math.max(1, Math.min(10, priority));
        double weightFactor = 0.5 + ((clamped - 1.0) * 1.5 / 9.0);
        return ticks * weightFactor;
    }
}
