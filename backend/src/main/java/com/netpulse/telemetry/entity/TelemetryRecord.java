package com.netpulse.telemetry.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "telemetry_records", indexes = {
        @Index(name = "idx_telemetry_node_id", columnList = "nodeId"),
        @Index(name = "idx_telemetry_timestamp", columnList = "timestamp"),
        @Index(name = "idx_telemetry_node_time", columnList = "nodeId, timestamp DESC")
})
public class TelemetryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String nodeId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private Double cpuUsage; // 0.0 - 100.0%

    @Column(nullable = false)
    private Double memoryUsage; // 0.0 - 100.0%

    @Column(nullable = false)
    private Double latency; // Milliseconds >= 0.0

    @Column(nullable = false)
    private Double packetLoss; // 0.0 - 100.0%

    @Column(nullable = false)
    private Integer activeConnections; // >= 0

    @Column(nullable = false)
    private Double errorRate; // 0.0 - 100.0%

    public TelemetryRecord() {
    }

    public TelemetryRecord(String nodeId, Instant timestamp, Double cpuUsage, Double memoryUsage,
                           Double latency, Double packetLoss, Integer activeConnections, Double errorRate) {
        this.nodeId = nodeId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.cpuUsage = clamp(cpuUsage, 0.0, 100.0);
        this.memoryUsage = clamp(memoryUsage, 0.0, 100.0);
        this.latency = Math.max(0.0, latency != null ? latency : 0.0);
        this.packetLoss = clamp(packetLoss, 0.0, 100.0);
        this.activeConnections = Math.max(0, activeConnections != null ? activeConnections : 0);
        this.errorRate = clamp(errorRate, 0.0, 100.0);
    }

    private Double clamp(Double val, double min, double max) {
        if (val == null) return min;
        return Math.max(min, Math.min(max, val));
    }

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = clamp(cpuUsage, 0.0, 100.0);
    }

    public Double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(Double memoryUsage) {
        this.memoryUsage = clamp(memoryUsage, 0.0, 100.0);
    }

    public Double getLatency() {
        return latency;
    }

    public void setLatency(Double latency) {
        this.latency = Math.max(0.0, latency != null ? latency : 0.0);
    }

    public Double getPacketLoss() {
        return packetLoss;
    }

    public void setPacketLoss(Double packetLoss) {
        this.packetLoss = clamp(packetLoss, 0.0, 100.0);
    }

    public Integer getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(Integer activeConnections) {
        this.activeConnections = Math.max(0, activeConnections != null ? activeConnections : 0);
    }

    public Double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(Double errorRate) {
        this.errorRate = clamp(errorRate, 0.0, 100.0);
    }
}
