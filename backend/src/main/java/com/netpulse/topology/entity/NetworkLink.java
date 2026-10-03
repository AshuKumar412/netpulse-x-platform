package com.netpulse.topology.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "network_links",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"link_id"}),
        @UniqueConstraint(columnNames = {"source_node_id", "target_node_id"})
    },
    indexes = {
        @Index(name = "idx_link_source", columnList = "source_node_id"),
        @Index(name = "idx_link_target", columnList = "target_node_id"),
        @Index(name = "idx_link_status", columnList = "status")
    }
)
public class NetworkLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "link_id", nullable = false, length = 64)
    private String linkId;

    @Column(name = "source_node_id", nullable = false, length = 64)
    private String sourceNodeId;

    @Column(name = "target_node_id", nullable = false, length = 64)
    private String targetNodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 32)
    private LinkType linkType = LinkType.DIRECT;

    @Column(nullable = false)
    private Integer bandwidth = 1000; // in Mbps

    @Column(nullable = false)
    private Double weight = 1.0; // Routing cost metric

    @Column(nullable = false)
    private Boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LinkStatus status = LinkStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NetworkLink() {
    }

    public NetworkLink(String linkId, String sourceNodeId, String targetNodeId,
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

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
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
