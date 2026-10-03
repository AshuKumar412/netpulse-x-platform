package com.netpulse.chaos.service;

import com.netpulse.chaos.state.ChaosExperimentType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChaosFaultRegistry {

    public record ActiveChaosFault(
            String experimentId,
            ChaosExperimentType experimentType,
            String targetNodeId,
            String targetLinkId,
            Double severity,
            Instant startedAt,
            Instant expiresAt
    ) {}

    // experimentId -> ActiveChaosFault
    private final Map<String, ActiveChaosFault> activeFaultsByExperiment = new ConcurrentHashMap<>();

    // targetNodeId -> experimentId
    private final Map<String, String> activeNodeFaults = new ConcurrentHashMap<>();

    // targetLinkId -> experimentId
    private final Map<String, String> activeLinkFaults = new ConcurrentHashMap<>();

    public void registerFault(ActiveChaosFault fault) {
        activeFaultsByExperiment.put(fault.experimentId(), fault);
        if (fault.targetNodeId() != null) {
            activeNodeFaults.put(fault.targetNodeId(), fault.experimentId());
        }
        if (fault.targetLinkId() != null) {
            activeLinkFaults.put(fault.targetLinkId(), fault.experimentId());
        }
    }

    public ActiveChaosFault removeFault(String experimentId) {
        ActiveChaosFault fault = activeFaultsByExperiment.remove(experimentId);
        if (fault != null) {
            if (fault.targetNodeId() != null) {
                activeNodeFaults.remove(fault.targetNodeId(), experimentId);
            }
            if (fault.targetLinkId() != null) {
                activeLinkFaults.remove(fault.targetLinkId(), experimentId);
            }
        }
        return fault;
    }

    public Optional<ActiveChaosFault> getActiveFaultForNode(String nodeId) {
        if (nodeId == null) return Optional.empty();
        String expId = activeNodeFaults.get(nodeId);
        if (expId == null) return Optional.empty();
        return Optional.ofNullable(activeFaultsByExperiment.get(expId));
    }

    public Optional<ActiveChaosFault> getActiveFaultForLink(String linkId) {
        if (linkId == null) return Optional.empty();
        String expId = activeLinkFaults.get(linkId);
        if (expId == null) return Optional.empty();
        return Optional.ofNullable(activeFaultsByExperiment.get(expId));
    }

    public List<ActiveChaosFault> getAllActiveFaults() {
        return new ArrayList<>(activeFaultsByExperiment.values());
    }

    public boolean hasActiveFaultOnNode(String nodeId) {
        return nodeId != null && activeNodeFaults.containsKey(nodeId);
    }

    public boolean hasActiveFaultOnLink(String linkId) {
        return linkId != null && activeLinkFaults.containsKey(linkId);
    }

    public Double getInjectedCpu(String nodeId) {
        return getActiveFaultForNode(nodeId)
                .filter(f -> f.experimentType() == ChaosExperimentType.CPU_SPIKE)
                .map(ActiveChaosFault::severity)
                .orElse(null);
    }

    public Double getInjectedMemory(String nodeId) {
        return getActiveFaultForNode(nodeId)
                .filter(f -> f.experimentType() == ChaosExperimentType.MEMORY_SPIKE)
                .map(ActiveChaosFault::severity)
                .orElse(null);
    }

    public Double getInjectedLatency(String nodeId) {
        return getActiveFaultForNode(nodeId)
                .filter(f -> f.experimentType() == ChaosExperimentType.HIGH_LATENCY)
                .map(ActiveChaosFault::severity)
                .orElse(null);
    }

    public Double getInjectedPacketLoss(String nodeId) {
        return getActiveFaultForNode(nodeId)
                .filter(f -> f.experimentType() == ChaosExperimentType.PACKET_LOSS)
                .map(ActiveChaosFault::severity)
                .orElse(null);
    }

    public Double getInjectedTraffic(String nodeId) {
        return getActiveFaultForNode(nodeId)
                .filter(f -> f.experimentType() == ChaosExperimentType.TRAFFIC_SPIKE)
                .map(ActiveChaosFault::severity)
                .orElse(null);
    }

    public void clearAll() {
        activeFaultsByExperiment.clear();
        activeNodeFaults.clear();
        activeLinkFaults.clear();
    }
}
