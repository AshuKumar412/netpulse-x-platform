package com.netpulse.whatif.entity;

import com.netpulse.whatif.state.SimulationEventType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "whatif_simulation_events", indexes = {
        @Index(name = "idx_simevt_run_id", columnList = "run_id"),
        @Index(name = "idx_simevt_tick", columnList = "simulation_tick")
})
public class SimulationEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false, length = 64)
    private String runId;

    @Column(name = "simulation_tick", nullable = false)
    private long simulationTick;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private SimulationEventType eventType;

    @Column(length = 100)
    private String source;

    @Column(length = 100)
    private String target;

    @Column(length = 500)
    private String reason;

    @Column(name = "state_before", length = 200)
    private String stateBefore;

    @Column(name = "state_after", length = 200)
    private String stateAfter;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public SimulationEventEntity() {
    }

    public SimulationEventEntity(String runId, long simulationTick, SimulationEventType eventType, String source, String target, String reason, String stateBefore, String stateAfter) {
        this.runId = runId;
        this.simulationTick = simulationTick;
        this.eventType = eventType;
        this.source = source;
        this.target = target;
        this.reason = reason;
        this.stateBefore = stateBefore;
        this.stateAfter = stateAfter;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public long getSimulationTick() { return simulationTick; }
    public void setSimulationTick(long simulationTick) { this.simulationTick = simulationTick; }
    public SimulationEventType getEventType() { return eventType; }
    public void setEventType(SimulationEventType eventType) { this.eventType = eventType; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStateBefore() { return stateBefore; }
    public void setStateBefore(String stateBefore) { this.stateBefore = stateBefore; }
    public String getStateAfter() { return stateAfter; }
    public void setStateAfter(String stateAfter) { this.stateAfter = stateAfter; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
