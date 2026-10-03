package com.netpulse.scheduler.strategy;

import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.state.ProcessType;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoundRobinSchedulerTest {

    private RoundRobinScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new RoundRobinScheduler();
    }

    @Test
    @DisplayName("Round Robin selects head of FIFO ready queue")
    void testSelectNext() {
        ProcessControlBlock p1 = new ProcessControlBlock("P1", "Head", "USER", "NODE-001", ProcessType.CPU_BOUND, 5, 0, 30);
        ProcessControlBlock p2 = new ProcessControlBlock("P2", "Tail", "USER", "NODE-001", ProcessType.CPU_BOUND, 1, 0, 10);

        ProcessControlBlock selected = scheduler.selectNext(List.of(p1, p2), null);
        assertEquals("P1", selected.getProcessId());
        assertEquals(SchedulerAlgorithm.ROUND_ROBIN, scheduler.getAlgorithm());
        assertTrue(scheduler.isPreemptive());
    }

    @Test
    @DisplayName("Round Robin preempts when ticks ran equals or exceeds quantum")
    void testPreemptOnQuantumExpiry() {
        ProcessControlBlock running = new ProcessControlBlock("P1", "Running", "USER", "NODE-001", ProcessType.CPU_BOUND, 5, 0, 30);
        ProcessControlBlock candidate = new ProcessControlBlock("P2", "Candidate", "USER", "NODE-001", ProcessType.CPU_BOUND, 1, 0, 10);

        assertFalse(scheduler.shouldPreempt(running, candidate, 3, 4));
        assertTrue(scheduler.shouldPreempt(running, candidate, 4, 4));
        assertTrue(scheduler.shouldPreempt(running, candidate, 5, 4));
    }
}
