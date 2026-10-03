package com.netpulse.scheduler.strategy;

import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoundRobinScheduler implements CpuScheduler {

    @Override
    public SchedulerAlgorithm getAlgorithm() {
        return SchedulerAlgorithm.ROUND_ROBIN;
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
        // Round robin picks head of FIFO ready queue
        return readyQueue.get(0);
    }

    @Override
    public boolean shouldPreempt(ProcessControlBlock currentlyRunning, ProcessControlBlock candidate, int ticksRanInCurrentBurst, int timeQuantum) {
        if (currentlyRunning == null) {
            return false;
        }
        // Preempt when time quantum expired
        return ticksRanInCurrentBurst >= timeQuantum;
    }

    @Override
    public String getDecisionReason(ProcessControlBlock selected, List<ProcessControlBlock> readyQueue) {
        if (selected == null) return "No process available in ready queue";
        return String.format("Round Robin: Process [%s] dispatched from head of FIFO queue",
                selected.getProcessName());
    }
}
