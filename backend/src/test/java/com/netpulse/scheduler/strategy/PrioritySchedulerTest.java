package com.netpulse.scheduler.strategy;

import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.state.ProcessType;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrioritySchedulerTest {

    private PriorityScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PriorityScheduler();
    }

    @Test
    @DisplayName("Priority scheduler should select smallest priority number (highest priority)")
    void testSelectNext() {
        ProcessControlBlock p1 = new ProcessControlBlock("P1", "Low", "USER", "NODE-001", ProcessType.CPU_BOUND, 8, 0, 30);
        ProcessControlBlock p2 = new ProcessControlBlock("P2", "High", "USER", "NODE-001", ProcessType.CPU_BOUND, 2, 0, 10);
        ProcessControlBlock p3 = new ProcessControlBlock("P3", "Medium", "USER", "NODE-001", ProcessType.CPU_BOUND, 5, 0, 20);

        ProcessControlBlock selected = scheduler.selectNext(List.of(p1, p2, p3), null);
        assertNotNull(selected);
        assertEquals("P2", selected.getProcessId());
        assertTrue(scheduler.isPreemptive());
    }

    @Test
    @DisplayName("Priority scheduler should preempt when candidate priority is higher (smaller number)")
    void testPreemption() {
        ProcessControlBlock running = new ProcessControlBlock("P1", "Running", "USER", "NODE-001", ProcessType.CPU_BOUND, 5, 0, 30);
        ProcessControlBlock higher = new ProcessControlBlock("P2", "Higher", "USER", "NODE-001", ProcessType.CPU_BOUND, 2, 0, 10);
        ProcessControlBlock lower = new ProcessControlBlock("P3", "Lower", "USER", "NODE-001", ProcessType.CPU_BOUND, 7, 0, 20);

        assertTrue(scheduler.shouldPreempt(running, higher, 1, 4));
        assertFalse(scheduler.shouldPreempt(running, lower, 1, 4));
    }
}
