package com.netpulse.topology.dto;

import com.netpulse.topology.entity.LinkStatus;
import com.netpulse.topology.entity.LinkType;
import com.netpulse.topology.entity.NetworkLink;

import java.time.Instant;

public class TopologyLinkResponse {

    private Long id;
    private String linkId;
    private String sourceNodeId;
    private String targetNodeId;
    private LinkType linkType;
    private Integer bandwidth;
    private Double weight;
    private Boolean enabled;
    private LinkStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public TopologyLinkResponse() {
    }

    public TopologyLinkResponse(Long id, String linkId, String sourceNodeId, String targetNodeId,
                                LinkType linkType, Integer bandwidth, Double weight,
                                Boolean enabled, LinkStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.linkId = linkId;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.linkType = linkType;
        this.bandwidth = bandwidth;
        this.weight = weight;
        this.enabled = enabled;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TopologyLinkResponse fromEntity(NetworkLink link) {
        return new TopologyLinkResponse(
                link.getId(),
                link.getLinkId(),
                link.getSourceNodeId(),
                link.getTargetNodeId(),
                link.getLinkType(),
                link.getBandwidth(),
                link.getWeight(),
                link.getEnabled(),
                link.getStatus(),
                link.getCreatedAt(),
                link.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(String sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(String targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public LinkType getLinkType() {
        return linkType;
    }

    public void setLinkType(LinkType linkType) {
        this.linkType = linkType;
    }

    public Integer getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(Integer bandwidth) {
        this.bandwidth = bandwidth;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LinkStatus getStatus() {
        return status;
    }

    public void setStatus(LinkStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
