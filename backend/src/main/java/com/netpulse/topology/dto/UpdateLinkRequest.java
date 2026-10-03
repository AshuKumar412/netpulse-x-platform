package com.netpulse.topology.dto;

import com.netpulse.topology.entity.LinkStatus;
import com.netpulse.topology.entity.LinkType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class UpdateLinkRequest {

    private LinkType linkType;

    @Min(value = 1, message = "Bandwidth must be at least 1 Mbps")
    @Max(value = 10000000, message = "Bandwidth cannot exceed 10,000,000 Mbps")
    private Integer bandwidth;

    @Min(value = 0, message = "Weight must be positive or zero")
    @Max(value = 10000, message = "Weight cannot exceed 10,000")
    private Double weight;

    private Boolean enabled;

    private LinkStatus status;

    public UpdateLinkRequest() {
    }

    public UpdateLinkRequest(LinkType linkType, Integer bandwidth, Double weight, Boolean enabled, LinkStatus status) {
        this.linkType = linkType;
        this.bandwidth = bandwidth;
        this.weight = weight;
        this.enabled = enabled;
        this.status = status;
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
