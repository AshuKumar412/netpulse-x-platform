package com.netpulse.whatif.dto;

import com.netpulse.scheduler.state.SchedulerAlgorithm;
import java.util.List;

public class SchedulerComparisonRequest {

    private List<SchedulerAlgorithm> algorithms;
    private List<WhatIfScenarioChangeDto> scenarioChanges;
    private int workloadSize = 10;

    public SchedulerComparisonRequest() {
    }

    public List<SchedulerAlgorithm> getAlgorithms() { return algorithms; }
    public void setAlgorithms(List<SchedulerAlgorithm> algorithms) { this.algorithms = algorithms; }
    public List<WhatIfScenarioChangeDto> getScenarioChanges() { return scenarioChanges; }
    public void setScenarioChanges(List<WhatIfScenarioChangeDto> scenarioChanges) { this.scenarioChanges = scenarioChanges; }
    public int getWorkloadSize() { return workloadSize; }
    public void setWorkloadSize(int workloadSize) { this.workloadSize = workloadSize; }
}
