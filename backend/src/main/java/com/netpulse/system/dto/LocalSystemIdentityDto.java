package com.netpulse.system.dto;

/**
 * Safe local machine identification and adaptive hardware capability tier.
 */
public record LocalSystemIdentityDto(
        String safeMachineId,
        String hostname,
        String capabilityTier,
        long recommendedPollingIntervalMs,
        int recommendedMaxThreads,
        int availableProcessors,
        long maxJvmMemoryBytes
) {}
