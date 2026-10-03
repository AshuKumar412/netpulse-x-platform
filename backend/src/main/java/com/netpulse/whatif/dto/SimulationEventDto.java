package com.netpulse.whatif.dto;

import com.netpulse.whatif.state.SimulationEventType;

import java.time.Instant;

public class SimulationEventDto {

    private Long id;
    private long simulationTick;
    private SimulationEventType eventType;
    private String source;
    private String target;
    private String reason;
    private String stateBefore;
    private String stateAfter;
    private Instant timestamp;

    public SimulationEventDto() {
    }

    public SimulationEventDto(long simulationTick, SimulationEventType eventType, String source, String target, String reason, String stateBefore, String stateAfter) {
        this.simulationTick = simulationTick;
        this.eventType = eventType;
        this.source = source;
        this.target = target;
        this.reason = reason;
        this.stateBefore = stateBefore;
        this.stateAfter = stateAfter;
        this.timestamp = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
