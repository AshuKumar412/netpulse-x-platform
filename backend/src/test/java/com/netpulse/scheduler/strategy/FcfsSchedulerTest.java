package com.netpulse.scheduler.strategy;

import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.state.ProcessType;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FcfsSchedulerTest {

    private FcfsScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new FcfsScheduler();
    }

    @Test
    @DisplayName("FCFS should return FCFS algorithm enum and non-preemptive")
    void testAlgorithmAndPreemption() {
        assertEquals(SchedulerAlgorithm.FCFS, scheduler.getAlgorithm());
        assertFalse(scheduler.isPreemptive());
    }

    @Test
    @DisplayName("FCFS should select process with earliest arrival time")
    void testSelectNext() {
        ProcessControlBlock p1 = new ProcessControlBlock("P1", "Proc1", "USER", "NODE-001", ProcessType.CPU_BOUND, 5, 10, 20);
        ProcessControlBlock p2 = new ProcessControlBlock("P2", "Proc2", "USER", "NODE-001", ProcessType.CPU_BOUND, 1, 5, 20);
        ProcessControlBlock p3 = new ProcessControlBlock("P3", "Proc3", "USER", "NODE-001", ProcessType.CPU_BOUND, 3, 8, 20);

        ProcessControlBlock selected = scheduler.selectNext(List.of(p1, p2, p3), null);
        assertNotNull(selected);
        assertEquals("P2", selected.getProcessId());
    }

    @Test
    @DisplayName("FCFS should never preempt")
    void testShouldPreempt() {
        ProcessControlBlock p1 = new ProcessControlBlock("P1", "Proc1", "USER", "NODE-001", ProcessType.CPU_BOUND, 5, 10, 20);
        ProcessControlBlock p2 = new ProcessControlBlock("P2", "Proc2", "USER", "NODE-001", ProcessType.CPU_BOUND, 1, 5, 5);

        assertFalse(scheduler.shouldPreempt(p1, p2, 10, 4));
    }
}
