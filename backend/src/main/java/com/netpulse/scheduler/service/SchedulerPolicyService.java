package com.netpulse.scheduler.service;

import com.netpulse.scheduler.dto.SchedulerPolicyConfig;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class SchedulerPolicyService {

    private final AtomicReference<SchedulerPolicyConfig> policy = new AtomicReference<>(
            new SchedulerPolicyConfig(
                    SchedulerAlgorithm.ROUND_ROBIN,
                    4,      // timeQuantum
                    0.5,    // contextSwitchCostMs
                    4,      // coresPerNode
                    true,   // agingEnabled
                    10,     // agingIntervalTicks
                    25,     // starvationThresholdTicks
                    100     // tickIntervalMs
            )
    );

    public SchedulerPolicyConfig getPolicy() {
        return policy.get();
    }

    public SchedulerPolicyConfig updatePolicy(SchedulerPolicyConfig newPolicy) {
        if (newPolicy != null) {
            policy.set(newPolicy);
        }
        return policy.get();
    }

    public void setAlgorithm(SchedulerAlgorithm algorithm) {
        SchedulerPolicyConfig current = policy.get();
        current.setAlgorithm(algorithm);
    }

    public void setTimeQuantum(int quantum) {
        SchedulerPolicyConfig current = policy.get();
        current.setTimeQuantum(Math.max(1, quantum));
    }
}
