package com.netpulse.topology.dto;

import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;

import java.time.Instant;

public class TopologyNodeResponse {

    private Long id;
    private String nodeId;
    private String name;
    private String host;
    private Integer port;
    private NodeStatus status;
    private Integer capacity;
    private Integer degree; // Number of connected links
    private Instant createdAt;
    private Instant updatedAt;

    public TopologyNodeResponse() {
    }

    public TopologyNodeResponse(Long id, String nodeId, String name, String host, Integer port,
                                NodeStatus status, Integer capacity, Integer degree,
                                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.nodeId = nodeId;
        this.name = name;
        this.host = host;
        this.port = port;
        this.status = status;
        this.capacity = capacity;
        this.degree = degree;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TopologyNodeResponse fromEntity(NetworkNode node, int degree) {
        return new TopologyNodeResponse(
                node.getId(),
                node.getNodeId(),
                node.getName(),
                node.getHost(),
                node.getPort(),
                node.getStatus(),
                node.getCapacity(),
                degree,
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

    public Integer getDegree() {
        return degree;
    }

    public void setDegree(Integer degree) {
        this.degree = degree;
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
