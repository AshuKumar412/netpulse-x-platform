package com.netpulse.node.dto;

public class NodeSummaryResponse {

    private long totalNodes;
    private long healthyNodes;
    private long warningNodes;
    private long congestedNodes;
    private long failedNodes;
    private long offlineNodes;
    private long totalCapacity;

    public NodeSummaryResponse() {
    }

    public NodeSummaryResponse(long totalNodes, long healthyNodes, long warningNodes,
                               long congestedNodes, long failedNodes, long offlineNodes,
                               long totalCapacity) {
        this.totalNodes = totalNodes;
        this.healthyNodes = healthyNodes;
        this.warningNodes = warningNodes;
        this.congestedNodes = congestedNodes;
        this.failedNodes = failedNodes;
        this.offlineNodes = offlineNodes;
        this.totalCapacity = totalCapacity;
    }

    public long getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(long totalNodes) {
        this.totalNodes = totalNodes;
    }

    public long getHealthyNodes() {
        return healthyNodes;
    }

    public void setHealthyNodes(long healthyNodes) {
        this.healthyNodes = healthyNodes;
    }

    public long getWarningNodes() {
        return warningNodes;
    }

    public void setWarningNodes(long warningNodes) {
        this.warningNodes = warningNodes;
    }

    public long getCongestedNodes() {
        return congestedNodes;
    }

    public void setCongestedNodes(long congestedNodes) {
        this.congestedNodes = congestedNodes;
    }

    public long getFailedNodes() {
        return failedNodes;
    }

    public void setFailedNodes(long failedNodes) {
        this.failedNodes = failedNodes;
    }

    public long getOfflineNodes() {
        return offlineNodes;
    }

    public void setOfflineNodes(long offlineNodes) {
        this.offlineNodes = offlineNodes;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
}
