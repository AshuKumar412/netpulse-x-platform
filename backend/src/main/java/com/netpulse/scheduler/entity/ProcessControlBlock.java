package com.netpulse.scheduler.entity;

import com.netpulse.scheduler.state.ProcessState;
import com.netpulse.scheduler.state.ProcessType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "process_control_blocks", indexes = {
        @Index(name = "idx_pcb_process_id", columnList = "process_id", unique = true),
        @Index(name = "idx_pcb_state", columnList = "state"),
        @Index(name = "idx_pcb_target_node", columnList = "target_node_id")
})
public class ProcessControlBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "process_id", nullable = false, unique = true, length = 64)
    private String processId;

    @Column(name = "process_name", nullable = false, length = 100)
    private String processName;

    @Column(nullable = false, length = 100)
    private String owner;

    @Column(name = "target_node_id", nullable = false, length = 64)
    private String targetNodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_type", nullable = false, length = 32)
    private ProcessType processType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProcessState state;

    @Column(nullable = false)
    private int priority = 5; // 1 = Highest, 10 = Lowest

    @Column(name = "effective_priority", nullable = false)
    private int effectivePriority = 5;

    @Column(name = "arrival_time", nullable = false)
    private long arrivalTime = 0;

    @Column(name = "burst_time", nullable = false)
    private long burstTime = 20;

    @Column(name = "remaining_burst_time", nullable = false)
    private long remainingBurstTime = 20;

    @Column(name = "waiting_time", nullable = false)
    private long waitingTime = 0;

    @Column(name = "turnaround_time", nullable = false)
    private long turnaroundTime = 0;

    @Column(name = "response_time", nullable = false)
    private long responseTime = -1;

    @Column(name = "completion_time", nullable = false)
    private long completionTime = -1;

    @Column(name = "start_time", nullable = false)
    private long startTime = -1;

    @Column(name = "cpu_time_consumed", nullable = false)
    private long cpuTimeConsumed = 0;

    @Column(name = "context_switch_count", nullable = false)
    private int contextSwitchCount = 0;

    @Column(name = "virtual_runtime", nullable = false)
    private double virtualRuntime = 0.0;

    @Column(name = "allocated_core_id")
    private Integer allocatedCoreId;

    @Column(name = "starvation_risk", nullable = false)
    private boolean starvationRisk = false;

    @Column(name = "io_wait_remaining", nullable = false)
    private int ioWaitRemaining = 0;

    @Column(name = "io_frequency", nullable = false)
    private int ioFrequency = 0;

    @Column(name = "io_duration", nullable = false)
    private int ioDuration = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ProcessControlBlock() {
    }

    public ProcessControlBlock(String processId,
                               String processName,
                               String owner,
                               String targetNodeId,
                               ProcessType processType,
                               int priority,
                               long arrivalTime,
                               long burstTime) {
        this.processId = processId;
        this.processName = processName;
        this.owner = owner;
        this.targetNodeId = targetNodeId;
        this.processType = processType != null ? processType : ProcessType.CPU_BOUND;
        this.priority = Math.max(1, Math.min(10, priority));
        this.effectivePriority = this.priority;
        this.arrivalTime = Math.max(0, arrivalTime);
        this.burstTime = Math.max(1, burstTime);
        this.remainingBurstTime = this.burstTime;
        this.state = ProcessState.NEW;
        this.virtualRuntime = 0.0;
        this.starvationRisk = false;

        if (this.processType == ProcessType.IO_BOUND) {
            this.ioFrequency = 4;
            this.ioDuration = 2;
        } else if (this.processType == ProcessType.NETWORK_BOUND) {
            this.ioFrequency = 6;
            this.ioDuration = 3;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public ProcessType getProcessType() {
        return processType;
    }

    public void setProcessType(ProcessType processType) {
        this.processType = processType;
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getEffectivePriority() {
        return effectivePriority;
    }

    public void setEffectivePriority(int effectivePriority) {
        this.effectivePriority = effectivePriority;
    }

    public long getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(long arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public long getBurstTime() {
        return burstTime;
    }

    public void setBurstTime(long burstTime) {
        this.burstTime = burstTime;
    }

    public long getRemainingBurstTime() {
        return remainingBurstTime;
    }

    public void setRemainingBurstTime(long remainingBurstTime) {
        this.remainingBurstTime = remainingBurstTime;
    }

    public long getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(long waitingTime) {
        this.waitingTime = waitingTime;
    }

    public long getTurnaroundTime() {
        return turnaroundTime;
    }

    public void setTurnaroundTime(long turnaroundTime) {
        this.turnaroundTime = turnaroundTime;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    public long getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(long completionTime) {
        this.completionTime = completionTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getCpuTimeConsumed() {
        return cpuTimeConsumed;
    }

    public void setCpuTimeConsumed(long cpuTimeConsumed) {
        this.cpuTimeConsumed = cpuTimeConsumed;
    }

    public int getContextSwitchCount() {
        return contextSwitchCount;
    }

    public void setContextSwitchCount(int contextSwitchCount) {
        this.contextSwitchCount = contextSwitchCount;
    }

    public void incrementContextSwitchCount() {
        this.contextSwitchCount++;
    }

    public double getVirtualRuntime() {
        return virtualRuntime;
    }

    public void setVirtualRuntime(double virtualRuntime) {
        this.virtualRuntime = virtualRuntime;
    }

    public Integer getAllocatedCoreId() {
        return allocatedCoreId;
    }

    public void setAllocatedCoreId(Integer allocatedCoreId) {
        this.allocatedCoreId = allocatedCoreId;
    }

    public boolean isStarvationRisk() {
        return starvationRisk;
    }

    public void setStarvationRisk(boolean starvationRisk) {
        this.starvationRisk = starvationRisk;
    }

    public int getIoWaitRemaining() {
        return ioWaitRemaining;
    }

    public void setIoWaitRemaining(int ioWaitRemaining) {
        this.ioWaitRemaining = ioWaitRemaining;
    }

    public int getIoFrequency() {
        return ioFrequency;
    }

    public void setIoFrequency(int ioFrequency) {
        this.ioFrequency = ioFrequency;
    }

    public int getIoDuration() {
        return ioDuration;
    }

    public void setIoDuration(int ioDuration) {
        this.ioDuration = ioDuration;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
