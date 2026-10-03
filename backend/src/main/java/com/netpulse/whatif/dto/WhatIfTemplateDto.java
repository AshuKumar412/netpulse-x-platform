package com.netpulse.whatif.dto;

import com.netpulse.whatif.state.ScenarioType;
import java.util.List;

public class WhatIfTemplateDto {

    private String templateId;
    private String name;
    private String description;
    private ScenarioType type;
    private List<WhatIfScenarioChangeDto> defaultChanges;
    private int defaultDurationSeconds;

    public WhatIfTemplateDto() {
    }

    public WhatIfTemplateDto(String templateId, String name, String description, ScenarioType type, List<WhatIfScenarioChangeDto> defaultChanges, int defaultDurationSeconds) {
        this.templateId = templateId;
        this.name = name;
        this.description = description;
        this.type = type;
        this.defaultChanges = defaultChanges;
        this.defaultDurationSeconds = defaultDurationSeconds;
    }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ScenarioType getType() { return type; }
    public void setType(ScenarioType type) { this.type = type; }
    public List<WhatIfScenarioChangeDto> getDefaultChanges() { return defaultChanges; }
    public void setDefaultChanges(List<WhatIfScenarioChangeDto> defaultChanges) { this.defaultChanges = defaultChanges; }
    public int getDefaultDurationSeconds() { return defaultDurationSeconds; }
    public void setDefaultDurationSeconds(int defaultDurationSeconds) { this.defaultDurationSeconds = defaultDurationSeconds; }
}
