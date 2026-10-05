package com.netpulse.system.dto;

import java.time.Instant;
import java.util.List;

/**
 * Lightweight real-time live host metrics for streaming dashboard updates.
 */
public record HostLiveMetricsDto(
        double cpuLoadPercent,
        List<Double> perCoreCpuLoads,
        long usedMemoryBytes,
        long totalMemoryBytes,
        double memoryUtilizationPercent,
        long uptimeSeconds,
        List<StorageDriveDto> storageDrives,
        List<NetworkInterfaceDto> activeNetworkInterfaces,
        long jvmUsedMemoryBytes,
        long jvmTotalMemoryBytes,
        Instant timestamp
) {}
