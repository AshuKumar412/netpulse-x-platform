package com.netpulse.chaos.service;

import com.netpulse.chaos.dto.ChaosPolicyConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChaosPolicyService {

    @Value("${app.chaos.enabled:true}")
    private boolean enabled = true;

    @Value("${app.chaos.max-duration-seconds:300}")
    private int maxDurationSeconds = 300;

    @Value("${app.chaos.max-concurrent-experiments:3}")
    private int maxConcurrentExperiments = 3;

    @Value("${app.chaos.cooldown-seconds:30}")
    private int cooldownSeconds = 30;

    @Value("${app.chaos.protected-nodes:}")
    private String protectedNodesConfig = "";

    private final Set<String> dynamicProtectedNodes = Collections.synchronizedSet(new HashSet<>());

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxDurationSeconds() {
        return maxDurationSeconds;
    }

    public void setMaxDurationSeconds(int maxDurationSeconds) {
        this.maxDurationSeconds = maxDurationSeconds;
    }

    public int getMaxConcurrentExperiments() {
        return maxConcurrentExperiments;
    }

    public void setMaxConcurrentExperiments(int maxConcurrentExperiments) {
        this.maxConcurrentExperiments = maxConcurrentExperiments;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public List<String> getProtectedNodes() {
        Set<String> all = new HashSet<>(dynamicProtectedNodes);
        if (protectedNodesConfig != null && !protectedNodesConfig.isBlank()) {
            all.addAll(Arrays.stream(protectedNodesConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet()));
        }
        return new ArrayList<>(all);
    }

    public boolean isNodeProtected(String nodeId) {
        if (nodeId == null) {
            return false;
        }
        return getProtectedNodes().stream().anyMatch(p -> p.equalsIgnoreCase(nodeId.trim()));
    }

    public void addProtectedNode(String nodeId) {
        if (nodeId != null && !nodeId.isBlank()) {
            dynamicProtectedNodes.add(nodeId.trim());
        }
    }

    public void removeProtectedNode(String nodeId) {
        if (nodeId != null) {
            dynamicProtectedNodes.remove(nodeId.trim());
        }
    }

    public ChaosPolicyConfig getPolicyConfig() {
        return new ChaosPolicyConfig(
                enabled,
                maxDurationSeconds,
                maxConcurrentExperiments,
                cooldownSeconds,
                getProtectedNodes()
        );
    }
}
