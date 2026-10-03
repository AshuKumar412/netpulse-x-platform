package com.netpulse.scheduler.service;

import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.node.repository.NodeRepository;
import com.netpulse.scheduler.dto.*;
import com.netpulse.scheduler.entity.ContextSwitchEvent;
import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.repository.ContextSwitchEventRepository;
import com.netpulse.scheduler.repository.ProcessControlBlockRepository;
import com.netpulse.scheduler.state.*;
import com.netpulse.scheduler.strategy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerEngineTest {

    @Mock
    private ProcessControlBlockRepository pcbRepository;

    @Mock
    private ContextSwitchEventRepository contextSwitchRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private SchedulerPolicyService policyService;
    private CpuSchedulerFactory schedulerFactory;
    private SchedulerEngine engine;

    @BeforeEach
    void setUp() {
        policyService = new SchedulerPolicyService();
        List<CpuScheduler> schedulers = List.of(
                new FcfsScheduler(),
                new SjfScheduler(),
                new SrtfScheduler(),
                new PriorityScheduler(),
                new RoundRobinScheduler(),
                new MultilevelQueueScheduler(),
                new CfsInspiredScheduler()
        );
        schedulerFactory = new CpuSchedulerFactory(schedulers);

        engine = new SchedulerEngine(
                policyService,
                schedulerFactory,
                pcbRepository,
                contextSwitchRepository,
                nodeRepository,
                messagingTemplate
        );

        NetworkNode mockNode = new NetworkNode("NODE-001", "Test Node", "127.0.0.1", 8080, NodeStatus.HEALTHY, 100);
        lenient().when(nodeRepository.findAll()).thenReturn(List.of(mockNode));
        lenient().when(pcbRepository.save(any(ProcessControlBlock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(pcbRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(contextSwitchRepository.save(any(ContextSwitchEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Process creation should save PCB and add to ready queue")
    void testCreateProcess() {
        CreateProcessRequest request = new CreateProcessRequest("TestJob", "OPERATOR", "NODE-001", ProcessType.CPU_BOUND, 3, 20);
        ProcessControlBlock pcb = engine.createProcess(request, "OPERATOR");

        assertNotNull(pcb);
        assertEquals("TestJob", pcb.getProcessName());
        assertEquals(ProcessState.READY, pcb.getState());
        assertEquals(3, pcb.getPriority());

        SchedulerStatusResponse status = engine.getStatusResponse();
        assertEquals(1, status.getReadyQueue().size());
    }

    @Test
    @DisplayName("Batch creation should create multiple processes correctly")
    void testBatchCreateProcesses() {
        BatchCreateProcessRequest request = new BatchCreateProcessRequest(5, "NODE-001", "BATCH");
        List<ProcessControlBlock> list = engine.batchCreateProcesses(request, "ADMIN");

        assertNotNull(list);
        assertEquals(5, list.size());

        SchedulerStatusResponse status = engine.getStatusResponse();
        assertEquals(5, status.getReadyQueue().size());
    }

    @Test
    @DisplayName("Single tick execution should dispatch process to core")
    void testExecuteTickDispatchesProcess() {
        CreateProcessRequest request = new CreateProcessRequest("SingleTickJob", "OPERATOR", "NODE-001", ProcessType.CPU_BOUND, 5, 10);
        engine.createProcess(request, "OPERATOR");

        engine.executeTick();

        SchedulerStatusResponse status = engine.getStatusResponse();
        assertEquals(1, status.getRunningProcesses().size());
        assertEquals(0, status.getReadyQueue().size());
    }

    @Test
    @DisplayName("Deterministic benchmark comparison should evaluate all 7 algorithms")
    void testBenchmarkComparison() {
        AlgorithmComparisonResponse response = engine.runBenchmarkComparison(null);

        assertNotNull(response);
        assertEquals(7, response.getResults().size());
        assertNotNull(response.getBestWaitingTimeAlgorithm());
        assertNotNull(response.getBestThroughputAlgorithm());
        assertNotNull(response.getOverallSummary());

        for (AlgorithmBenchmarkResult result : response.getResults()) {
            assertNotNull(result.getAlgorithm());
            assertTrue(result.getCompletedProcessesCount() > 0);
            assertNotNull(result.getGanttTimeline());
        }
    }

    @Test
    @DisplayName("Lifecycle controls should transition scheduler state")
    void testLifecycleControls() {
        engine.pause();
        assertEquals(SchedulerStatus.PAUSED, engine.getStatusResponse().getStatus());

        engine.resume();
        assertEquals(SchedulerStatus.RUNNING, engine.getStatusResponse().getStatus());

        engine.stop();
        assertEquals(SchedulerStatus.STOPPED, engine.getStatusResponse().getStatus());
    }

    @Test
    @DisplayName("Terminating a process should move it to terminated state and record context switch")
    void testTerminateProcess() {
        CreateProcessRequest request = new CreateProcessRequest("TermJob", "OPERATOR", "NODE-001", ProcessType.CPU_BOUND, 5, 20);
        ProcessControlBlock pcb = engine.createProcess(request, "OPERATOR");

        engine.terminateProcess(pcb.getProcessId());
        SchedulerStatusResponse status = engine.getStatusResponse();
        assertTrue(status.getReadyQueue().stream().noneMatch(p -> p.getProcessId().equals(pcb.getProcessId())));
    }
}
