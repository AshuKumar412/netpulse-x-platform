package com.netpulse.scheduler.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

public class BatchCreateProcessRequest {

    @Min(value = 1, message = "Count must be at least 1")
    @Max(value = 100, message = "Count cannot exceed 100")
    private int count = 10;

    private String targetNodeId;

    private String processNamePrefix = "PROC";

    public BatchCreateProcessRequest() {
    }

    public BatchCreateProcessRequest(int count, String targetNodeId, String processNamePrefix) {
        this.count = count;
        this.targetNodeId = targetNodeId;
        this.processNamePrefix = processNamePrefix;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public String getProcessNamePrefix() {
        return processNamePrefix;
    }

    public void setProcessNamePrefix(String processNamePrefix) {
        this.processNamePrefix = processNamePrefix;
    }
}
