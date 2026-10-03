package com.netpulse.whatif.dto;

import com.netpulse.scheduler.state.SchedulerAlgorithm;
import com.netpulse.scheduler.state.SchedulerStatus;

import java.util.List;

public class SnapshotSchedulerStateDto {

    private SchedulerStatus status;
    private SchedulerAlgorithm activeAlgorithm;
    private Integer timeQuantum;
    private Integer coresPerNode;
    private Long currentTick;
    private List<SnapshotProcessDto> activeWorkload;

    public SnapshotSchedulerStateDto() {
    }

    public SnapshotSchedulerStateDto(SchedulerStatus status, SchedulerAlgorithm activeAlgorithm, Integer timeQuantum, Integer coresPerNode, Long currentTick, List<SnapshotProcessDto> activeWorkload) {
        this.status = status;
        this.activeAlgorithm = activeAlgorithm;
        this.timeQuantum = timeQuantum;
        this.coresPerNode = coresPerNode;
        this.currentTick = currentTick;
        this.activeWorkload = activeWorkload;
    }

    public SchedulerStatus getStatus() { return status; }
    public void setStatus(SchedulerStatus status) { this.status = status; }
    public SchedulerAlgorithm getActiveAlgorithm() { return activeAlgorithm; }
    public void setActiveAlgorithm(SchedulerAlgorithm activeAlgorithm) { this.activeAlgorithm = activeAlgorithm; }
    public Integer getTimeQuantum() { return timeQuantum; }
    public void setTimeQuantum(Integer timeQuantum) { this.timeQuantum = timeQuantum; }
    public Integer getCoresPerNode() { return coresPerNode; }
    public void setCoresPerNode(Integer coresPerNode) { this.coresPerNode = coresPerNode; }
    public Long getCurrentTick() { return currentTick; }
    public void setCurrentTick(Long currentTick) { this.currentTick = currentTick; }
    public List<SnapshotProcessDto> getActiveWorkload() { return activeWorkload; }
    public void setActiveWorkload(List<SnapshotProcessDto> activeWorkload) { this.activeWorkload = activeWorkload; }
}
