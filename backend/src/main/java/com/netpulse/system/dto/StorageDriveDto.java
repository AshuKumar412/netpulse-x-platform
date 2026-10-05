package com.netpulse.system.dto;

/**
 * Storage filesystem drive detected dynamically from the host machine.
 */
public record StorageDriveDto(
        String name,
        String mountPoint,
        String fileSystemType,
        long totalBytes,
        long freeBytes,
        long usedBytes,
        double utilizationPercent
) {}
