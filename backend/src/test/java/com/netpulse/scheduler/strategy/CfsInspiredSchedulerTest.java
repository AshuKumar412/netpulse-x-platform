package com.netpulse.scheduler.strategy;

import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.state.ProcessType;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CfsInspiredSchedulerTest {

    private CfsInspiredScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new CfsInspiredScheduler();
    }

    @Test
    @DisplayName("CFS should select process with lowest virtual runtime")
    void testSelectNextLowestVruntime() {
        ProcessControlBlock p1 = new ProcessControlBlock("P1", "Proc1", "USER", "NODE-001", ProcessType.CPU_BOUND, 5, 0, 30);
        p1.setVirtualRuntime(15.5);
        ProcessControlBlock p2 = new ProcessControlBlock("P2", "Proc2", "USER", "NODE-001", ProcessType.CPU_BOUND, 5, 0, 30);
        p2.setVirtualRuntime(8.2);

        ProcessControlBlock selected = scheduler.selectNext(List.of(p1, p2), null);
        assertEquals("P2", selected.getProcessId());
        assertEquals(SchedulerAlgorithm.CFS_INSPIRED, scheduler.getAlgorithm());
        assertTrue(scheduler.isPreemptive());
    }

    @Test
    @DisplayName("CFS calculateVirtualRuntimeIncrement scales by priority weight")
    void testVirtualRuntimeWeighting() {
        double highPriorityInc = CfsInspiredScheduler.calculateVirtualRuntimeIncrement(1, 10);
        double normalPriorityInc = CfsInspiredScheduler.calculateVirtualRuntimeIncrement(5, 10);
        double lowPriorityInc = CfsInspiredScheduler.calculateVirtualRuntimeIncrement(10, 10);

        assertTrue(highPriorityInc < normalPriorityInc);
        assertTrue(normalPriorityInc < lowPriorityInc);
    }
}
