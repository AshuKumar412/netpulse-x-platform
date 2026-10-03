package com.netpulse.scheduler.strategy;

import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.state.ProcessType;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SrtfSchedulerTest {

    private SrtfScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SrtfScheduler();
    }

    @Test
    @DisplayName("SRTF should select process with shortest remaining burst time")
    void testSelectNext() {
        ProcessControlBlock p1 = new ProcessControlBlock("P1", "Proc1", "USER", "NODE-001", ProcessType.CPU_BOUND, 5, 0, 30);
        p1.setRemainingBurstTime(15);
        ProcessControlBlock p2 = new ProcessControlBlock("P2", "Proc2", "USER", "NODE-001", ProcessType.CPU_BOUND, 1, 0, 10);
        p2.setRemainingBurstTime(8);

        ProcessControlBlock selected = scheduler.selectNext(List.of(p1, p2), null);
        assertNotNull(selected);
        assertEquals("P2", selected.getProcessId());
        assertTrue(scheduler.isPreemptive());
    }

    @Test
    @DisplayName("SRTF should preempt when candidate has strictly shorter remaining burst")
    void testShouldPreempt() {
        ProcessControlBlock running = new ProcessControlBlock("P1", "Proc1", "USER", "NODE-001", ProcessType.CPU_BOUND, 5, 0, 30);
        running.setRemainingBurstTime(12);

        ProcessControlBlock shorter = new ProcessControlBlock("P2", "Proc2", "USER", "NODE-001", ProcessType.CPU_BOUND, 1, 0, 10);
        shorter.setRemainingBurstTime(5);

        ProcessControlBlock longer = new ProcessControlBlock("P3", "Proc3", "USER", "NODE-001", ProcessType.CPU_BOUND, 1, 0, 20);
        longer.setRemainingBurstTime(15);

        assertTrue(scheduler.shouldPreempt(running, shorter, 2, 4));
        assertFalse(scheduler.shouldPreempt(running, longer, 2, 4));
    }
}
