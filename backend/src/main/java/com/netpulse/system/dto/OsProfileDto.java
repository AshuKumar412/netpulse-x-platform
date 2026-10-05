package com.netpulse.system.dto;

import java.time.Instant;

/**
 * Operating System detection information discovered dynamically from the host machine.
 */
public record OsProfileDto(
        String osName,
        String osVersion,
        String osArch,
        String kernelVersion,
        Instant bootTime,
        long uptimeSeconds
) {}
