package com.netpulse.scheduler.strategy;

import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class PriorityScheduler implements CpuScheduler {

    @Override
    public SchedulerAlgorithm getAlgorithm() {
        return SchedulerAlgorithm.PRIORITY;
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
        // Lower number = higher priority
        return readyQueue.stream()
                .min(Comparator.comparingInt(ProcessControlBlock::getEffectivePriority)
                        .thenComparingLong(ProcessControlBlock::getArrivalTime)
                        .thenComparing(ProcessControlBlock::getProcessId))
                .orElse(null);
    }

    @Override
    public boolean shouldPreempt(ProcessControlBlock currentlyRunning, ProcessControlBlock candidate, int ticksRanInCurrentBurst, int timeQuantum) {
        if (currentlyRunning == null || candidate == null) {
            return false;
        }
        // Preempt if candidate has higher priority (smaller number)
        return candidate.getEffectivePriority() < currentlyRunning.getEffectivePriority();
    }

    @Override
    public String getDecisionReason(ProcessControlBlock selected, List<ProcessControlBlock> readyQueue) {
        if (selected == null) return "No process available in ready queue";
        return String.format("Priority: Process [%s] selected with highest priority (%d, base: %d)",
                selected.getProcessName(), selected.getEffectivePriority(), selected.getPriority());
    }
}
