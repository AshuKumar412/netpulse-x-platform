package com.netpulse.scheduler.strategy;

import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class FcfsScheduler implements CpuScheduler {

    @Override
    public SchedulerAlgorithm getAlgorithm() {
        return SchedulerAlgorithm.FCFS;
    }

    @Override
    public boolean isPreemptive() {
        return false;
    }

    @Override
    public ProcessControlBlock selectNext(List<ProcessControlBlock> readyQueue, ProcessControlBlock currentlyRunning) {
        if (readyQueue == null || readyQueue.isEmpty()) {
            return null;
        }
        return readyQueue.stream()
                .min(Comparator.comparingLong(ProcessControlBlock::getArrivalTime)
                        .thenComparing(ProcessControlBlock::getProcessId))
                .orElse(null);
    }

    @Override
    public boolean shouldPreempt(ProcessControlBlock currentlyRunning, ProcessControlBlock candidate, int ticksRanInCurrentBurst, int timeQuantum) {
        // Non-preemptive
        return false;
    }

    @Override
    public String getDecisionReason(ProcessControlBlock selected, List<ProcessControlBlock> readyQueue) {
        if (selected == null) return "No process available in ready queue";
        return String.format("FCFS: Process [%s] selected with earliest arrival time (%d ms)",
                selected.getProcessName(), selected.getArrivalTime());
    }
}
