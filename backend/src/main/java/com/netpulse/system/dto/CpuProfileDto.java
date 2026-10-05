package com.netpulse.system.dto;

import java.util.List;

/**
 * CPU hardware detection profile discovered dynamically at runtime.
 */
public record CpuProfileDto(
        String vendor,
        String modelName,
        int physicalCores,
        int logicalCores,
        double currentCpuLoadPercent,
        List<Double> perCoreLoads,
        long maxFrequencyHz,
        double systemLoadAverage
) {}
