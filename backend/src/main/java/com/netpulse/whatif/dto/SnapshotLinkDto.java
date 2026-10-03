package com.netpulse.whatif.dto;

import com.netpulse.topology.entity.LinkStatus;

public class SnapshotLinkDto {

    private String linkId;
    private String sourceNodeId;
    private String targetNodeId;
    private LinkStatus status;
    private Integer bandwidth;
    private Integer latency;
    private Double packetLoss;

    public SnapshotLinkDto() {
    }

    public SnapshotLinkDto(String linkId, String sourceNodeId, String targetNodeId, LinkStatus status, Integer bandwidth, Integer latency, Double packetLoss) {
        this.linkId = linkId;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.status = status;
        this.bandwidth = bandwidth;
        this.latency = latency;
        this.packetLoss = packetLoss;
    }

    public String getLinkId() { return linkId; }
    public void setLinkId(String linkId) { this.linkId = linkId; }
    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }
    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }
    public LinkStatus getStatus() { return status; }
    public void setStatus(LinkStatus status) { this.status = status; }
    public Integer getBandwidth() { return bandwidth; }
    public void setBandwidth(Integer bandwidth) { this.bandwidth = bandwidth; }
    public Integer getLatency() { return latency; }
    public void setLatency(Integer latency) { this.latency = latency; }
    public Double getPacketLoss() { return packetLoss; }
    public void setPacketLoss(Double packetLoss) { this.packetLoss = packetLoss; }
}
