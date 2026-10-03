package com.netpulse.whatif.service;

import com.netpulse.routing.entity.RoutingStrategyType;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import com.netpulse.whatif.dto.WhatIfScenarioChangeDto;
import com.netpulse.whatif.dto.WhatIfTemplateDto;
import com.netpulse.whatif.state.ScenarioType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WhatIfTemplateService {

    public List<WhatIfTemplateDto> getPredefinedTemplates() {
        List<WhatIfTemplateDto> templates = new ArrayList<>();

        // 1. Single Node Failure
        templates.add(new WhatIfTemplateDto(
                "TPL-01",
                "Single Node Failure Outage",
                "Simulates the sudden crash of a primary node, testing automated failover, candidate exclusion, and rerouting.",
                ScenarioType.NODE_FAILURE,
                List.of(new WhatIfScenarioChangeDto(ScenarioType.NODE_FAILURE, "NODE-002", null, null, null, null, null, null, null, null)),
                60
        ));

        // 2. Traffic Surge (+40%)
        templates.add(new WhatIfTemplateDto(
                "TPL-02",
                "40% Traffic Spike Surge",
                "Injects an immediate +40% increase in active traffic connections to assess load shedding, queue pressure, and capacity limits.",
                ScenarioType.TRAFFIC_INCREASE,
                List.of(new WhatIfScenarioChangeDto(ScenarioType.TRAFFIC_INCREASE, null, null, 40.0, null, null, null, null, null, null)),
                60
        ));

        // 3. Node Failure + Traffic Surge
        templates.add(new WhatIfTemplateDto(
                "TPL-03",
                "Node Failure during Traffic Spike",
                "Stress-tests the platform under combined strain: NODE-002 failure accompanied by a +40% overall network traffic surge.",
                ScenarioType.COMBINED_SCENARIO,
                List.of(
                        new WhatIfScenarioChangeDto(ScenarioType.NODE_FAILURE, "NODE-002", null, null, null, null, null, null, null, null),
                        new WhatIfScenarioChangeDto(ScenarioType.TRAFFIC_INCREASE, null, null, 40.0, null, null, null, null, null, null)
                ),
                60
        ));

        // 4. High Latency Event
        templates.add(new WhatIfTemplateDto(
                "TPL-04",
                "Severe Network Latency Degradation",
                "Injects +300ms artificial round-trip latency to a key node to observe adaptive latency-aware penalty routing.",
                ScenarioType.LATENCY_INCREASE,
                List.of(new WhatIfScenarioChangeDto(ScenarioType.LATENCY_INCREASE, "NODE-001", null, null, null, null, 300.0, null, null, null)),
                60
        ));

        // 5. Network Partition
        templates.add(new WhatIfTemplateDto(
                "TPL-05",
                "Network Partition Isolation",
                "Simulates complete link severance to a cluster zone to evaluate isolated topology subgraphs and split-brain resilience.",
                ScenarioType.NETWORK_PARTITION,
                List.of(new WhatIfScenarioChangeDto(ScenarioType.NETWORK_PARTITION, "NODE-003", null, null, null, null, null, null, null, null)),
                60
        ));

        // 6. CPU Saturation Spike
        templates.add(new WhatIfTemplateDto(
                "TPL-06",
                "Critical CPU Saturation (95%)",
                "Forces node CPU load to 95%, evaluating health degradation, scheduler preemption, and routing score penalties.",
                ScenarioType.CPU_SPIKE,
                List.of(new WhatIfScenarioChangeDto(ScenarioType.CPU_SPIKE, "NODE-001", null, null, 95.0, null, null, null, null, null)),
                60
        ));

        // 7. Memory Pressure Spike
        templates.add(new WhatIfTemplateDto(
                "TPL-07",
                "Exhaustive Memory Pressure (90%)",
                "Simulates rapid memory depletion to 90%, testing memory weight penalty in candidate ranking.",
                ScenarioType.MEMORY_SPIKE,
                List.of(new WhatIfScenarioChangeDto(ScenarioType.MEMORY_SPIKE, "NODE-002", null, null, null, 90.0, null, null, null, null)),
                60
        ));

        // 8. Routing Strategy Comparison
        templates.add(new WhatIfTemplateDto(
                "TPL-08",
                "Routing Strategy Comparative Benchmark",
                "Simulates shifting active routing from Round Robin to Adaptive Routing under identical synthetic traffic patterns.",
                ScenarioType.ROUTING_STRATEGY_CHANGE,
                List.of(new WhatIfScenarioChangeDto(ScenarioType.ROUTING_STRATEGY_CHANGE, null, null, null, null, null, null, null, RoutingStrategyType.ADAPTIVE, null)),
                60
        ));

        // 9. CPU Scheduler Comparison
        templates.add(new WhatIfTemplateDto(
                "TPL-09",
                "CPU Scheduler Algorithm Switch",
                "Simulates replacing Round Robin CPU scheduling with CFS-Inspired (Virtual Runtime) to measure queue turnaround and fair shares.",
                ScenarioType.CPU_SCHEDULER_CHANGE,
                List.of(new WhatIfScenarioChangeDto(ScenarioType.CPU_SCHEDULER_CHANGE, null, null, null, null, null, null, null, null, SchedulerAlgorithm.CFS_INSPIRED)),
                60
        ));

        // 10. Multi-Failure Cascading Scenario
        templates.add(new WhatIfTemplateDto(
                "TPL-10",
                "Cascading Multi-Failure Disaster Recovery",
                "Simulates concurrent node failure, link degradation, packet loss (25%), and CPU spike across multiple nodes.",
                ScenarioType.COMBINED_SCENARIO,
                List.of(
                        new WhatIfScenarioChangeDto(ScenarioType.NODE_FAILURE, "NODE-002", null, null, null, null, null, null, null, null),
                        new WhatIfScenarioChangeDto(ScenarioType.PACKET_LOSS_INCREASE, "NODE-003", null, null, null, null, null, 25.0, null, null),
                        new WhatIfScenarioChangeDto(ScenarioType.CPU_SPIKE, "NODE-001", null, null, 85.0, null, null, null, null, null)
                ),
                90
        ));

        return templates;
    }
}
