package com.netpulse.topology.dto;

import com.netpulse.topology.entity.LinkStatus;
import com.netpulse.topology.entity.LinkType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CreateLinkRequest {

    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,64}$", message = "Link ID must be 3-64 alphanumeric characters, underscores or hyphens")
    private String linkId;

    @NotBlank(message = "Source Node ID is required")
    private String sourceNodeId;

    @NotBlank(message = "Target Node ID is required")
    private String targetNodeId;

    private LinkType linkType = LinkType.DIRECT;

    @NotNull(message = "Bandwidth is required")
    @Min(value = 1, message = "Bandwidth must be at least 1 Mbps")
    @Max(value = 10000000, message = "Bandwidth cannot exceed 10,000,000 Mbps")
    private Integer bandwidth = 1000;

    @NotNull(message = "Weight is required")
    @Min(value = 0, message = "Weight must be positive or zero")
    @Max(value = 10000, message = "Weight cannot exceed 10,000")
    private Double weight = 1.0;

    private Boolean enabled = true;

    private LinkStatus status = LinkStatus.ACTIVE;

    public CreateLinkRequest() {
    }

    public CreateLinkRequest(String linkId, String sourceNodeId, String targetNodeId,
                             LinkType linkType, Integer bandwidth, Double weight,
                             Boolean enabled, LinkStatus status) {
        this.linkId = linkId;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.linkType = linkType != null ? linkType : LinkType.DIRECT;
        this.bandwidth = bandwidth != null ? bandwidth : 1000;
        this.weight = weight != null ? weight : 1.0;
        this.enabled = enabled != null ? enabled : true;
        this.status = status != null ? status : LinkStatus.ACTIVE;
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
}
