package com.netpulse.scheduler.strategy;

import com.netpulse.scheduler.state.SchedulerAlgorithm;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class CpuSchedulerFactory {

    private final Map<SchedulerAlgorithm, CpuScheduler> schedulers = new EnumMap<>(SchedulerAlgorithm.class);

    public CpuSchedulerFactory(List<CpuScheduler> schedulerList) {
        for (CpuScheduler scheduler : schedulerList) {
            schedulers.put(scheduler.getAlgorithm(), scheduler);
        }
    }

    public CpuScheduler getScheduler(SchedulerAlgorithm algorithm) {
        CpuScheduler scheduler = schedulers.get(algorithm);
        if (scheduler == null) {
            // Default to ROUND_ROBIN if not found
            return schedulers.get(SchedulerAlgorithm.ROUND_ROBIN);
        }
        return scheduler;
    }
}
