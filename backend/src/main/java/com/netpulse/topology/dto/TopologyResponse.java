package com.netpulse.topology.dto;

import java.time.Instant;
import java.util.List;

public class TopologyResponse {

    private List<TopologyNodeResponse> nodes;
    private List<TopologyLinkResponse> links;
    private TopologySummaryResponse summary;
    private String simulationStatus;
    private Instant timestamp;

    public TopologyResponse() {
    }

    public TopologyResponse(List<TopologyNodeResponse> nodes, List<TopologyLinkResponse> links,
                            TopologySummaryResponse summary, String simulationStatus, Instant timestamp) {
        this.nodes = nodes;
        this.links = links;
        this.summary = summary;
        this.simulationStatus = simulationStatus;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public List<TopologyNodeResponse> getNodes() {
        return nodes;
    }

    public void setNodes(List<TopologyNodeResponse> nodes) {
        this.nodes = nodes;
    }

    public List<TopologyLinkResponse> getLinks() {
        return links;
    }

    public void setLinks(List<TopologyLinkResponse> links) {
        this.links = links;
    }

    public TopologySummaryResponse getSummary() {
        return summary;
    }

    public void setSummary(TopologySummaryResponse summary) {
        this.summary = summary;
    }

    public String getSimulationStatus() {
        return simulationStatus;
    }

    public void setSimulationStatus(String simulationStatus) {
        this.simulationStatus = simulationStatus;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
