package com.netpulse.telemetry.service;

import com.netpulse.chaos.service.ChaosFaultRegistry;
import com.netpulse.node.entity.NetworkNode;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.telemetry.entity.TelemetryRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TelemetryEngine {

    private final ChaosFaultRegistry chaosFaultRegistry;

    public TelemetryEngine() {
        this.chaosFaultRegistry = null;
    }

    @Autowired
    public TelemetryEngine(@Autowired(required = false) ChaosFaultRegistry chaosFaultRegistry) {
        this.chaosFaultRegistry = chaosFaultRegistry;
    }

    public TelemetryRecord computeTelemetry(NetworkNode node, int linkDegree, long tick, TelemetryRecord prev) {
        String nodeId = node.getNodeId();
        Instant now = Instant.now();

        if (node.getStatus() == NodeStatus.OFFLINE) {
            return new TelemetryRecord(nodeId, now, 0.0, 0.0, 0.0, 0.0, 0, 0.0);
        }

        if (node.getStatus() == NodeStatus.FAILED) {
            return new TelemetryRecord(nodeId, now, 98.5, 95.0, 450.0, 100.0, 0, 100.0);
        }

        double capFactor = Math.max(0.5, Math.min(2.0, 1000.0 / Math.max(100, node.getCapacity())));
        int nodeSeed = Math.abs(nodeId.hashCode());
        double wave1 = Math.sin(Math.toRadians((tick * 12 + nodeSeed) % 360));
        double wave2 = Math.cos(Math.toRadians((tick * 7 + (nodeSeed / 2)) % 360));
        double workloadFactor = (wave1 * 0.7 + wave2 * 0.3); // Range [-1.0, 1.0]

        int baseConns = (int) (node.getCapacity() * 0.4);
        int targetConnections = Math.max(5, (int) (baseConns + (workloadFactor * baseConns * 0.3) + (linkDegree * 15)));

        // Chaos Fault Injection: Traffic Spike
        if (chaosFaultRegistry != null) {
            Double injectedTraffic = chaosFaultRegistry.getInjectedTraffic(nodeId);
            if (injectedTraffic != null) {
                // If severity > 10, treat as explicit connection boost or percentage
                if (injectedTraffic > 10.0) {
                    targetConnections = Math.max(targetConnections, (int) (node.getCapacity() * (injectedTraffic / 100.0)));
                } else {
                    targetConnections = (int) (targetConnections * Math.max(1.5, injectedTraffic));
                }
            }
        }

        double targetCpu;
        double targetMemory;
        double targetLatency;
        double targetPacketLoss;
        double targetErrorRate;

        if (node.getStatus() == NodeStatus.CONGESTED) {
            targetCpu = 91.0 + (workloadFactor * 4.0);
            targetMemory = 88.0 + (workloadFactor * 3.0);
            targetLatency = 195.0 + (workloadFactor * 30.0);
            targetPacketLoss = 4.8 + (workloadFactor * 0.8);
            targetErrorRate = 3.5 + (workloadFactor * 0.5);
        } else if (node.getStatus() == NodeStatus.WARNING) {
            targetCpu = 74.0 + (workloadFactor * 6.0);
            targetMemory = 76.0 + (workloadFactor * 4.0);
            targetLatency = 110.0 + (workloadFactor * 20.0);
            targetPacketLoss = 2.4 + (workloadFactor * 0.5);
            targetErrorRate = 1.2 + (workloadFactor * 0.3);
        } else {
            // NodeStatus.HEALTHY
            targetCpu = 25.0 + (workloadFactor * 8.0 * capFactor) + (linkDegree * 1.5);
            targetMemory = 35.0 + ((double) targetConnections / Math.max(1, node.getCapacity()) * 25.0);
            targetLatency = 12.0 + (linkDegree * 2.5) + (workloadFactor * 3.0);
            targetPacketLoss = Math.max(0.0, workloadFactor * 0.1);
            targetErrorRate = 0.0;
        }

        // Apply smooth exponential moving average if previous record exists
        double finalCpu = prev != null ? (0.7 * targetCpu + 0.3 * prev.getCpuUsage()) : targetCpu;
        double finalMem = prev != null ? (0.7 * targetMemory + 0.3 * prev.getMemoryUsage()) : targetMemory;
        double finalLat = prev != null ? (0.7 * targetLatency + 0.3 * prev.getLatency()) : targetLatency;
        double finalLoss = prev != null ? (0.7 * targetPacketLoss + 0.3 * prev.getPacketLoss()) : targetPacketLoss;
        double finalErr = prev != null ? (0.7 * targetErrorRate + 0.3 * prev.getErrorRate()) : targetErrorRate;
        int finalConns = prev != null ? (int) (0.7 * targetConnections + 0.3 * prev.getActiveConnections()) : targetConnections;

        // Apply Real-Time Chaos Injected Faults
        if (chaosFaultRegistry != null) {
            Double injectedCpu = chaosFaultRegistry.getInjectedCpu(nodeId);
            if (injectedCpu != null) {
                finalCpu = Math.max(finalCpu, injectedCpu);
            }

            Double injectedMem = chaosFaultRegistry.getInjectedMemory(nodeId);
            if (injectedMem != null) {
                finalMem = Math.max(finalMem, injectedMem);
            }

            Double injectedLat = chaosFaultRegistry.getInjectedLatency(nodeId);
            if (injectedLat != null) {
                finalLat = finalLat + injectedLat;
            }

            Double injectedLoss = chaosFaultRegistry.getInjectedPacketLoss(nodeId);
            if (injectedLoss != null) {
                finalLoss = Math.max(finalLoss, injectedLoss);
                if (finalLoss > 5.0) {
                    finalErr = Math.max(finalErr, finalLoss * 0.4);
                }
            }
        }

        return new TelemetryRecord(
                nodeId,
                now,
                Math.round(finalCpu * 10.0) / 10.0,
                Math.round(finalMem * 10.0) / 10.0,
                Math.round(finalLat * 10.0) / 10.0,
                Math.round(finalLoss * 10.0) / 10.0,
                finalConns,
                Math.round(finalErr * 10.0) / 10.0
        );
    }
}
