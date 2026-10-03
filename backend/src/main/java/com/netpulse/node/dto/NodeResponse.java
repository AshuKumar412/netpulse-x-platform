package com.netpulse.node.dto;

import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;

import java.time.Instant;

public class NodeResponse {
    private Long id;
    private String nodeId;
    private String name;
    private String host;
    private Integer port;
    private NodeStatus status;
    private Integer capacity;
    private Instant createdAt;
    private Instant updatedAt;

    public NodeResponse() {
    }

    public NodeResponse(Long id, String nodeId, String name, String host, Integer port,
                        NodeStatus status, Integer capacity, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.nodeId = nodeId;
        this.name = name;
        this.host = host;
        this.port = port;
        this.status = status;
        this.capacity = capacity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static NodeResponse fromEntity(NetworkNode node) {
        return new NodeResponse(
                node.getId(),
                node.getNodeId(),
                node.getName(),
                node.getHost(),
                node.getPort(),
                node.getStatus(),
                node.getCapacity(),
                node.getCreatedAt(),
                node.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
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
