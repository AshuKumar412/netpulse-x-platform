package com.netpulse.system.dto;

import java.time.Instant;
import java.util.List;

/**
 * Complete dynamic host machine profile snapshot discovered at runtime.
 */
public record HostSystemProfileDto(
        LocalSystemIdentityDto identity,
        OsProfileDto os,
        CpuProfileDto cpu,
        MemoryProfileDto memory,
        List<StorageDriveDto> storageDrives,
        List<NetworkInterfaceDto> networkInterfaces,
        String javaVersion,
        String javaVendor,
        Instant discoveryTimestamp
) {}
