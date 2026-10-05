package com.netpulse.system.dto;

/**
 * Host machine physical and virtual memory profile discovered dynamically at runtime.
 */
public record MemoryProfileDto(
        long totalBytes,
        long availableBytes,
        long usedBytes,
        double utilizationPercent,
        long swapTotalBytes,
        long swapUsedBytes
) {}
