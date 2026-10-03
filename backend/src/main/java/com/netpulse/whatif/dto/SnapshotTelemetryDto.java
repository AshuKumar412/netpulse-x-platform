package com.netpulse.whatif.dto;

public class SnapshotTelemetryDto {

    private String nodeId;
    private Double cpuUsage;
    private Double memoryUsage;
    private Double latency;
    private Double packetLoss;
    private Integer activeConnections;
    private Double errorRate;
    private Long throughput;

    public SnapshotTelemetryDto() {
    }

    public SnapshotTelemetryDto(String nodeId, Double cpuUsage, Double memoryUsage, Double latency, Double packetLoss, Integer activeConnections, Double errorRate, Long throughput) {
        this.nodeId = nodeId;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.latency = latency;
        this.packetLoss = packetLoss;
        this.activeConnections = activeConnections;
        this.errorRate = errorRate;
        this.throughput = throughput;
    }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public Double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }
    public Double getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(Double memoryUsage) { this.memoryUsage = memoryUsage; }
    public Double getLatency() { return latency; }
    public void setLatency(Double latency) { this.latency = latency; }
    public Double getPacketLoss() { return packetLoss; }
    public void setPacketLoss(Double packetLoss) { this.packetLoss = packetLoss; }
    public Integer getActiveConnections() { return activeConnections; }
    public void setActiveConnections(Integer activeConnections) { this.activeConnections = activeConnections; }
    public Double getErrorRate() { return errorRate; }
    public void setErrorRate(Double errorRate) { this.errorRate = errorRate; }
    public Long getThroughput() { return throughput; }
    public void setThroughput(Long throughput) { this.throughput = throughput; }
}
