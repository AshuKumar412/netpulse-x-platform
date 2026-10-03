package com.netpulse.whatif.dto;

public class WhatIfLimitsConfig {

    private int maxSimulationDurationSeconds = 600;
    private int maxScenarioChanges = 10;
    private int maxConcurrentSimulations = 2;
    private int maxHistoryScenarios = 100;

    public WhatIfLimitsConfig() {
    }

    public WhatIfLimitsConfig(int maxSimulationDurationSeconds, int maxScenarioChanges, int maxConcurrentSimulations, int maxHistoryScenarios) {
        this.maxSimulationDurationSeconds = maxSimulationDurationSeconds;
        this.maxScenarioChanges = maxScenarioChanges;
        this.maxConcurrentSimulations = maxConcurrentSimulations;
        this.maxHistoryScenarios = maxHistoryScenarios;
    }

    public int getMaxSimulationDurationSeconds() { return maxSimulationDurationSeconds; }
    public void setMaxSimulationDurationSeconds(int maxSimulationDurationSeconds) { this.maxSimulationDurationSeconds = maxSimulationDurationSeconds; }
    public int getMaxScenarioChanges() { return maxScenarioChanges; }
    public void setMaxScenarioChanges(int maxScenarioChanges) { this.maxScenarioChanges = maxScenarioChanges; }
    public int getMaxConcurrentSimulations() { return maxConcurrentSimulations; }
    public void setMaxConcurrentSimulations(int maxConcurrentSimulations) { this.maxConcurrentSimulations = maxConcurrentSimulations; }
    public int getMaxHistoryScenarios() { return maxHistoryScenarios; }
    public void setMaxHistoryScenarios(int maxHistoryScenarios) { this.maxHistoryScenarios = maxHistoryScenarios; }
}
