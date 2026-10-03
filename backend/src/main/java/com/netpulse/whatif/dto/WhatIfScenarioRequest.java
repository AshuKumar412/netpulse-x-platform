package com.netpulse.whatif.dto;

import com.netpulse.whatif.state.ScenarioType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class WhatIfScenarioRequest {

    @NotBlank(message = "Scenario name is required")
    private String name;

    private String description;

    @NotNull(message = "Primary scenario type is required")
    private ScenarioType type = ScenarioType.NODE_FAILURE;

    private List<WhatIfScenarioChangeDto> changes;

    private int durationSeconds = 60;

    public WhatIfScenarioRequest() {
    }

    public WhatIfScenarioRequest(String name, String description, ScenarioType type, List<WhatIfScenarioChangeDto> changes, int durationSeconds) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.changes = changes;
        this.durationSeconds = durationSeconds;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ScenarioType getType() { return type; }
    public void setType(ScenarioType type) { this.type = type; }
    public List<WhatIfScenarioChangeDto> getChanges() { return changes; }
    public void setChanges(List<WhatIfScenarioChangeDto> changes) { this.changes = changes; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
}
