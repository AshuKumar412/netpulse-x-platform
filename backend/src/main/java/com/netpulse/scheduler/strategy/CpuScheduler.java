package com.netpulse.scheduler.strategy;

import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.state.SchedulerAlgorithm;

import java.util.List;

public interface CpuScheduler {

    SchedulerAlgorithm getAlgorithm();

    boolean isPreemptive();

    /**
     * Selects the next process to execute from the ready queue.
     * Returns null if ready queue is empty.
     */
    ProcessControlBlock selectNext(List<ProcessControlBlock> readyQueue, ProcessControlBlock currentlyRunning);

    /**
     * Checks whether the currently running process on a core should be preempted.
     */
    boolean shouldPreempt(ProcessControlBlock currentlyRunning, ProcessControlBlock candidate, int ticksRanInCurrentBurst, int timeQuantum);

    /**
     * Provides an explainable human-readable reason for why the scheduler selected the process.
     */
    String getDecisionReason(ProcessControlBlock selected, List<ProcessControlBlock> readyQueue);
}
