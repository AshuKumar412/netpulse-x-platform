package com.netpulse.node.dto;

import com.netpulse.node.entity.NodeStatus;
import jakarta.validation.constraints.*;

public class CreateNodeRequest {

    @NotBlank(message = "Node ID is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,64}$", message = "Node ID must be 3-64 alphanumeric characters, underscores or hyphens")
    private String nodeId;

    @NotBlank(message = "Node name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Host is required")
    @Size(max = 255, message = "Host cannot exceed 255 characters")
    private String host;

    @NotNull(message = "Port is required")
    @Min(value = 1, message = "Port must be at least 1")
    @Max(value = 65535, message = "Port must not exceed 65535")
    private Integer port;

    private NodeStatus status = NodeStatus.HEALTHY;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 1000000, message = "Capacity cannot exceed 1,000,000")
    private Integer capacity;

    public CreateNodeRequest() {
    }

    public CreateNodeRequest(String nodeId, String name, String host, Integer port, NodeStatus status, Integer capacity) {
        this.nodeId = nodeId;
        this.name = name;
        this.host = host;
        this.port = port;
        this.status = status;
        this.capacity = capacity;
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
}
