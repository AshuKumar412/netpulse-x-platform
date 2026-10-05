package com.netpulse.system.dto;

import java.util.List;

/**
 * Network adapter interface detected dynamically from the host machine.
 */
public record NetworkInterfaceDto(
        String name,
        String displayName,
        String macAddress,
        List<String> ipv4Addresses,
        List<String> ipv6Addresses,
        boolean isUp,
        boolean isLoopback,
        long speedBps,
        long bytesSent,
        long bytesReceived
) {}
