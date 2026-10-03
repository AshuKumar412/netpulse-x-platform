package com.netpulse.scheduler.strategy;

import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.state.ProcessType;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SjfSchedulerTest {

    private SjfScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SjfScheduler();
    }

    @Test
    @DisplayName("SJF should select process with shortest total burst time")
    void testSelectNext() {
        ProcessControlBlock p1 = new ProcessControlBlock("P1", "Proc1", "USER", "NODE-001", ProcessType.CPU_BOUND, 5, 0, 30);
        ProcessControlBlock p2 = new ProcessControlBlock("P2", "Proc2", "USER", "NODE-001", ProcessType.CPU_BOUND, 1, 0, 10);
        ProcessControlBlock p3 = new ProcessControlBlock("P3", "Proc3", "USER", "NODE-001", ProcessType.CPU_BOUND, 3, 0, 20);

        ProcessControlBlock selected = scheduler.selectNext(List.of(p1, p2, p3), null);
        assertNotNull(selected);
        assertEquals("P2", selected.getProcessId());
        assertFalse(scheduler.isPreemptive());
    }
}
