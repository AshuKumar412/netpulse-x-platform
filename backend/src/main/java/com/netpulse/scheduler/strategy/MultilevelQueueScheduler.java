package com.netpulse.scheduler.strategy;

import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class MultilevelQueueScheduler implements CpuScheduler {

    @Override
    public SchedulerAlgorithm getAlgorithm() {
        return SchedulerAlgorithm.MULTILEVEL_QUEUE;
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

        // Queue 1: High priority (1 to 3)
        List<ProcessControlBlock> q1 = readyQueue.stream()
                .filter(p -> p.getEffectivePriority() <= 3)
                .toList();
        if (!q1.isEmpty()) {
            return q1.get(0);
        }

        // Queue 2: Medium priority (4 to 7)
        List<ProcessControlBlock> q2 = readyQueue.stream()
                .filter(p -> p.getEffectivePriority() > 3 && p.getEffectivePriority() <= 7)
                .toList();
        if (!q2.isEmpty()) {
            return q2.get(0);
        }

        // Queue 3: Low priority (8 to 10) - FCFS
        return readyQueue.stream()
                .filter(p -> p.getEffectivePriority() > 7)
                .min(Comparator.comparingLong(ProcessControlBlock::getArrivalTime)
                        .thenComparing(ProcessControlBlock::getProcessId))
                .orElse(readyQueue.get(0));
    }

    @Override
    public boolean shouldPreempt(ProcessControlBlock currentlyRunning, ProcessControlBlock candidate, int ticksRanInCurrentBurst, int timeQuantum) {
        if (currentlyRunning == null) {
            return false;
        }

        // Strict priority between queues: higher queue always preempts lower queue
        if (candidate != null) {
            int runningQueueLevel = getQueueLevel(currentlyRunning.getEffectivePriority());
            int candidateQueueLevel = getQueueLevel(candidate.getEffectivePriority());
            if (candidateQueueLevel < runningQueueLevel) {
                return true;
            }
        }

        // Within same queue level, check time quantum for Q1 (quantum=2) and Q2 (quantum=4)
        int queueLevel = getQueueLevel(currentlyRunning.getEffectivePriority());
        if (queueLevel == 1) {
            return ticksRanInCurrentBurst >= 2;
        } else if (queueLevel == 2) {
            return ticksRanInCurrentBurst >= 4;
        }
        // Queue 3 is FCFS (non-preemptive within queue)
        return false;
    }

    private int getQueueLevel(int priority) {
        if (priority <= 3) return 1;
        if (priority <= 7) return 2;
        return 3;
    }

    @Override
    public String getDecisionReason(ProcessControlBlock selected, List<ProcessControlBlock> readyQueue) {
        if (selected == null) return "No process available in ready queue";
        int level = getQueueLevel(selected.getEffectivePriority());
        return String.format("MLQ: Process [%s] selected from Queue %d (Priority: %d)",
                selected.getProcessName(), level, selected.getEffectivePriority());
    }
}
