package com.netpulse.node.dto;

import com.netpulse.node.entity.NodeStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class UpdateNodeRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 255, message = "Host cannot exceed 255 characters")
    private String host;

    @Min(value = 1, message = "Port must be at least 1")
    @Max(value = 65535, message = "Port must not exceed 65535")
    private Integer port;

    private NodeStatus status;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 1000000, message = "Capacity cannot exceed 1,000,000")
    private Integer capacity;

    public UpdateNodeRequest() {
    }

    public UpdateNodeRequest(String name, String host, Integer port, NodeStatus status, Integer capacity) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.status = status;
        this.capacity = capacity;
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
