package com.netpulse.topology.dto;

public class TopologySummaryResponse {

    private long totalNodes;
    private long activeNodes;
    private long degradedNodes;
    private long failedNodes;
    private long offlineNodes;
    private long totalLinks;
    private long activeLinks;
    private long degradedLinks;
    private long failedLinks;
    private long offlineLinks;
    private long disconnectedNodes; // Nodes with no active links
    private int connectedComponents; // Number of connected subgraphs
    private double graphDensity; // Actual links / Possible links
    private long totalCapacity; // Sum of node capacities
    private long totalBandwidth; // Sum of link bandwidths

    public TopologySummaryResponse() {
    }

    public TopologySummaryResponse(long totalNodes, long activeNodes, long degradedNodes,
                                   long failedNodes, long offlineNodes, long totalLinks,
                                   long activeLinks, long degradedLinks, long failedLinks,
                                   long offlineLinks, long disconnectedNodes,
                                   int connectedComponents, double graphDensity,
                                   long totalCapacity, long totalBandwidth) {
        this.totalNodes = totalNodes;
        this.activeNodes = activeNodes;
        this.degradedNodes = degradedNodes;
        this.failedNodes = failedNodes;
        this.offlineNodes = offlineNodes;
        this.totalLinks = totalLinks;
        this.activeLinks = activeLinks;
        this.degradedLinks = degradedLinks;
        this.failedLinks = failedLinks;
        this.offlineLinks = offlineLinks;
        this.disconnectedNodes = disconnectedNodes;
        this.connectedComponents = connectedComponents;
        this.graphDensity = graphDensity;
        this.totalCapacity = totalCapacity;
        this.totalBandwidth = totalBandwidth;
    }

    public long getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(long totalNodes) {
        this.totalNodes = totalNodes;
    }

    public long getActiveNodes() {
        return activeNodes;
    }

    public void setActiveNodes(long activeNodes) {
        this.activeNodes = activeNodes;
    }

    public long getDegradedNodes() {
        return degradedNodes;
    }

    public void setDegradedNodes(long degradedNodes) {
        this.degradedNodes = degradedNodes;
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

    public long getTotalLinks() {
        return totalLinks;
    }

    public void setTotalLinks(long totalLinks) {
        this.totalLinks = totalLinks;
    }

    public long getActiveLinks() {
        return activeLinks;
    }

    public void setActiveLinks(long activeLinks) {
        this.activeLinks = activeLinks;
    }

    public long getDegradedLinks() {
        return degradedLinks;
    }

    public void setDegradedLinks(long degradedLinks) {
        this.degradedLinks = degradedLinks;
    }

    public long getFailedLinks() {
        return failedLinks;
    }

    public void setFailedLinks(long failedLinks) {
        this.failedLinks = failedLinks;
    }

    public long getOfflineLinks() {
        return offlineLinks;
    }

    public void setOfflineLinks(long offlineLinks) {
        this.offlineLinks = offlineLinks;
    }

    public long getDisconnectedNodes() {
        return disconnectedNodes;
    }

    public void setDisconnectedNodes(long disconnectedNodes) {
        this.disconnectedNodes = disconnectedNodes;
    }

    public int getConnectedComponents() {
        return connectedComponents;
    }

    public void setConnectedComponents(int connectedComponents) {
        this.connectedComponents = connectedComponents;
    }

    public double getGraphDensity() {
        return graphDensity;
    }

    public void setGraphDensity(double graphDensity) {
        this.graphDensity = graphDensity;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getTotalBandwidth() {
        return totalBandwidth;
    }

    public void setTotalBandwidth(long totalBandwidth) {
        this.totalBandwidth = totalBandwidth;
    }
}
