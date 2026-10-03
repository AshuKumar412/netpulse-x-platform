package com.netpulse.scheduler.strategy;

import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.state.ProcessType;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultilevelQueueSchedulerTest {

    private MultilevelQueueScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new MultilevelQueueScheduler();
    }

    @Test
    @DisplayName("MLQ should prioritize Queue 1 (priority 1-3) over Queue 2 and Queue 3")
    void testQueuePrioritization() {
        ProcessControlBlock pLow = new ProcessControlBlock("P1", "Low", "USER", "NODE-001", ProcessType.CPU_BOUND, 9, 0, 30);
        ProcessControlBlock pMed = new ProcessControlBlock("P2", "Med", "USER", "NODE-001", ProcessType.CPU_BOUND, 5, 0, 20);
        ProcessControlBlock pHigh = new ProcessControlBlock("P3", "High", "USER", "NODE-001", ProcessType.CPU_BOUND, 2, 0, 10);

        ProcessControlBlock selected = scheduler.selectNext(List.of(pLow, pMed, pHigh), null);
        assertEquals("P3", selected.getProcessId());

        ProcessControlBlock selected2 = scheduler.selectNext(List.of(pLow, pMed), null);
        assertEquals("P2", selected2.getProcessId());
    }

    @Test
    @DisplayName("MLQ should preempt if candidate is in higher priority queue level")
    void testPreemptionHigherQueue() {
        ProcessControlBlock runningMed = new ProcessControlBlock("P2", "Med", "USER", "NODE-001", ProcessType.CPU_BOUND, 5, 0, 20);
        ProcessControlBlock candidateHigh = new ProcessControlBlock("P3", "High", "USER", "NODE-001", ProcessType.CPU_BOUND, 2, 0, 10);

        assertTrue(scheduler.shouldPreempt(runningMed, candidateHigh, 1, 4));
    }
}
