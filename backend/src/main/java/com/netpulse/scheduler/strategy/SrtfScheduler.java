package com.netpulse.scheduler.strategy;

import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class SrtfScheduler implements CpuScheduler {

    @Override
    public SchedulerAlgorithm getAlgorithm() {
        return SchedulerAlgorithm.SRTF;
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
        return readyQueue.stream()
                .min(Comparator.comparingLong(ProcessControlBlock::getRemainingBurstTime)
                        .thenComparingLong(ProcessControlBlock::getArrivalTime)
                        .thenComparing(ProcessControlBlock::getProcessId))
                .orElse(null);
    }

    @Override
    public boolean shouldPreempt(ProcessControlBlock currentlyRunning, ProcessControlBlock candidate, int ticksRanInCurrentBurst, int timeQuantum) {
        if (currentlyRunning == null || candidate == null) {
            return false;
        }
        // Preempt if candidate in ready queue has strictly less remaining burst time than running process
        return candidate.getRemainingBurstTime() < currentlyRunning.getRemainingBurstTime();
    }

    @Override
    public String getDecisionReason(ProcessControlBlock selected, List<ProcessControlBlock> readyQueue) {
        if (selected == null) return "No process available in ready queue";
        return String.format("SRTF: Process [%s] selected with shortest remaining burst time (%d ms)",
                selected.getProcessName(), selected.getRemainingBurstTime());
    }
}
