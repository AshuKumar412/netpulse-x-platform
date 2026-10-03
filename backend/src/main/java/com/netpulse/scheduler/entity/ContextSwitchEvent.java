package com.netpulse.scheduler.entity;

import com.netpulse.scheduler.state.ContextSwitchReason;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "context_switch_events", indexes = {
        @Index(name = "idx_cs_timestamp", columnList = "timestamp"),
        @Index(name = "idx_cs_node_id", columnList = "node_id"),
        @Index(name = "idx_cs_sim_tick", columnList = "simulation_tick")
})
public class ContextSwitchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_process_id", length = 64)
    private String fromProcessId;

    @Column(name = "to_process_id", length = 64)
    private String toProcessId;

    @Column(name = "cpu_core_number", nullable = false)
    private int cpuCoreNumber;

    @Column(name = "node_id", nullable = false, length = 64)
    private String nodeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ContextSwitchReason reason;

    @Column(name = "switch_duration_ms", nullable = false)
    private double switchDurationMs = 0.5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SchedulerAlgorithm algorithm;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "simulation_tick", nullable = false)
    private long simulationTick;

    public ContextSwitchEvent() {
    }

    public ContextSwitchEvent(String fromProcessId,
                              String toProcessId,
                              int cpuCoreNumber,
                              String nodeId,
                              ContextSwitchReason reason,
                              double switchDurationMs,
                              SchedulerAlgorithm algorithm,
                              long simulationTick) {
        this.fromProcessId = fromProcessId;
        this.toProcessId = toProcessId;
        this.cpuCoreNumber = cpuCoreNumber;
        this.nodeId = nodeId;
        this.reason = reason;
        this.switchDurationMs = switchDurationMs;
        this.algorithm = algorithm;
        this.timestamp = Instant.now();
        this.simulationTick = simulationTick;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFromProcessId() {
        return fromProcessId;
    }

    public void setFromProcessId(String fromProcessId) {
        this.fromProcessId = fromProcessId;
    }

    public String getToProcessId() {
        return toProcessId;
    }

    public void setToProcessId(String toProcessId) {
        this.toProcessId = toProcessId;
    }

    public int getCpuCoreNumber() {
        return cpuCoreNumber;
    }

    public void setCpuCoreNumber(int cpuCoreNumber) {
        this.cpuCoreNumber = cpuCoreNumber;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public ContextSwitchReason getReason() {
        return reason;
    }

    public void setReason(ContextSwitchReason reason) {
        this.reason = reason;
    }

    public double getSwitchDurationMs() {
        return switchDurationMs;
    }

    public void setSwitchDurationMs(double switchDurationMs) {
        this.switchDurationMs = switchDurationMs;
    }

    public SchedulerAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(SchedulerAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public long getSimulationTick() {
        return simulationTick;
    }

    public void setSimulationTick(long simulationTick) {
        this.simulationTick = simulationTick;
    }
}
