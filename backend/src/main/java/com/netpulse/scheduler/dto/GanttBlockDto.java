package com.netpulse.scheduler.dto;

import com.netpulse.scheduler.state.SchedulerAlgorithm;

public class GanttBlockDto {

    private String processId;
    private String processName;
    private int coreId;
    private String nodeId;
    private long startTick;
    private long endTick;
    private int priority;
    private SchedulerAlgorithm algorithm;
    private boolean contextSwitchBlock;

    public GanttBlockDto() {
    }

    public GanttBlockDto(String processId, String processName, int coreId, String nodeId, long startTick, long endTick, int priority, SchedulerAlgorithm algorithm, boolean contextSwitchBlock) {
        this.processId = processId;
        this.processName = processName;
        this.coreId = coreId;
        this.nodeId = nodeId;
        this.startTick = startTick;
        this.endTick = endTick;
        this.priority = priority;
        this.algorithm = algorithm;
        this.contextSwitchBlock = contextSwitchBlock;
    }

    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }
    public int getCoreId() { return coreId; }
    public void setCoreId(int coreId) { this.coreId = coreId; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public long getStartTick() { return startTick; }
    public void setStartTick(long startTick) { this.startTick = startTick; }
    public long getEndTick() { return endTick; }
    public void setEndTick(long endTick) { this.endTick = endTick; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public SchedulerAlgorithm getAlgorithm() { return algorithm; }
    public void setAlgorithm(SchedulerAlgorithm algorithm) { this.algorithm = algorithm; }
    public boolean isContextSwitchBlock() { return contextSwitchBlock; }
    public void setContextSwitchBlock(boolean contextSwitchBlock) { this.contextSwitchBlock = contextSwitchBlock; }
}
