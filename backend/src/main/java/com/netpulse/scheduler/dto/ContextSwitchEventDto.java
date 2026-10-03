package com.netpulse.scheduler.dto;

import com.netpulse.scheduler.entity.ContextSwitchEvent;
import com.netpulse.scheduler.state.ContextSwitchReason;
import com.netpulse.scheduler.state.SchedulerAlgorithm;

import java.time.Instant;

public class ContextSwitchEventDto {

    private Long id;
    private String fromProcessId;
    private String toProcessId;
    private int cpuCoreNumber;
    private String nodeId;
    private ContextSwitchReason reason;
    private double switchDurationMs;
    private SchedulerAlgorithm algorithm;
    private Instant timestamp;
    private long simulationTick;

    public ContextSwitchEventDto() {
    }

    public static ContextSwitchEventDto fromEntity(ContextSwitchEvent entity) {
        if (entity == null) return null;
        ContextSwitchEventDto dto = new ContextSwitchEventDto();
        dto.setId(entity.getId());
        dto.setFromProcessId(entity.getFromProcessId());
        dto.setToProcessId(entity.getToProcessId());
        dto.setCpuCoreNumber(entity.getCpuCoreNumber());
        dto.setNodeId(entity.getNodeId());
        dto.setReason(entity.getReason());
        dto.setSwitchDurationMs(entity.getSwitchDurationMs());
        dto.setAlgorithm(entity.getAlgorithm());
        dto.setTimestamp(entity.getTimestamp());
        dto.setSimulationTick(entity.getSimulationTick());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFromProcessId() { return fromProcessId; }
    public void setFromProcessId(String fromProcessId) { this.fromProcessId = fromProcessId; }
    public String getToProcessId() { return toProcessId; }
    public void setToProcessId(String toProcessId) { this.toProcessId = toProcessId; }
    public int getCpuCoreNumber() { return cpuCoreNumber; }
    public void setCpuCoreNumber(int cpuCoreNumber) { this.cpuCoreNumber = cpuCoreNumber; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public ContextSwitchReason getReason() { return reason; }
    public void setReason(ContextSwitchReason reason) { this.reason = reason; }
    public double getSwitchDurationMs() { return switchDurationMs; }
    public void setSwitchDurationMs(double switchDurationMs) { this.switchDurationMs = switchDurationMs; }
    public SchedulerAlgorithm getAlgorithm() { return algorithm; }
    public void setAlgorithm(SchedulerAlgorithm algorithm) { this.algorithm = algorithm; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public long getSimulationTick() { return simulationTick; }
    public void setSimulationTick(long simulationTick) { this.simulationTick = simulationTick; }
}
