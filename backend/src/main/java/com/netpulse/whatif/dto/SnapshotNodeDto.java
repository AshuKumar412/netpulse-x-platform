package com.netpulse.whatif.dto;

import com.netpulse.node.entity.NodeStatus;

public class SnapshotNodeDto {

    private String nodeId;
    private String name;
    private String host;
    private Integer port;
    private NodeStatus status;
    private Integer capacity;

    public SnapshotNodeDto() {
    }

    public SnapshotNodeDto(String nodeId, String name, String host, Integer port, NodeStatus status, Integer capacity) {
        this.nodeId = nodeId;
        this.name = name;
        this.host = host;
        this.port = port;
        this.status = status;
        this.capacity = capacity;
    }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
}
