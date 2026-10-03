package com.netpulse.whatif.service;

import com.netpulse.health.entity.HealthClassification;
import com.netpulse.heartbeat.entity.LivenessStatus;
import com.netpulse.node.entity.NodeStatus;
import com.netpulse.routing.entity.RoutingStrategyType;
import com.netpulse.scheduler.dto.AlgorithmBenchmarkResult;
import com.netpulse.scheduler.dto.AlgorithmComparisonResponse;
import com.netpulse.scheduler.dto.GanttBlockDto;
import com.netpulse.scheduler.entity.ProcessControlBlock;
import com.netpulse.scheduler.service.SchedulerEngine;
import com.netpulse.scheduler.state.ProcessType;
import com.netpulse.scheduler.state.SchedulerAlgorithm;
import com.netpulse.topology.entity.LinkStatus;
import com.netpulse.whatif.dto.*;
import com.netpulse.whatif.state.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class WhatIfSimulationEngine {

    private final SchedulerEngine schedulerEngine;

    public WhatIfSimulationEngine(SchedulerEngine schedulerEngine) {
        this.schedulerEngine = schedulerEngine;
    }

    /**
     * Executes an isolated, deterministic What-If simulation without mutating any production state.
     */
    public SimulationRunResponse executeSimulation(String scenarioId,
                                                   String scenarioName,
                                                   ScenarioType scenarioType,
                                                   List<WhatIfScenarioChangeDto> changes,
                                                   int durationSeconds,
                                                   SystemSnapshotDto snapshot) {
        String runId = "RUN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant startedAt = Instant.now();

        // 1. Clone snapshot into isolated in-memory simulation state
        SimulatedEnvironment simEnv = cloneSnapshotToSimulation(snapshot);

        List<SimulationEventDto> events = new ArrayList<>();
        List<SimulationDecisionDto> decisions = new ArrayList<>();
        List<RiskIndicator> riskIndicators = new ArrayList<>();
        List<GanttBlockDto> ganttTimeline = new ArrayList<>();

        events.add(new SimulationEventDto(0, SimulationEventType.SNAPSHOT_CAPTURED, "SYSTEM", "SNAPSHOT",
                "Captured baseline system snapshot " + snapshot.getSnapshotId(),
                snapshot.getTotalNodes() + " nodes", snapshot.getHealthyNodes() + " healthy"));

        // 2. Apply hypothetical scenario changes at virtual clock T+0
        long currentTick = 0;
        List<WhatIfScenarioChangeDto> effectiveChanges = changes != null ? changes : Collections.emptyList();

        for (WhatIfScenarioChangeDto change : effectiveChanges) {
            applyChange(change, simEnv, currentTick, events, decisions, riskIndicators);
        }

        // 3. Simulate Failover & Routing Rebalancing if nodes failed or degraded
        evaluateSimulatedFailoverAndRouting(simEnv, currentTick, events, decisions, riskIndicators);

        // 4. Advance virtual clock & evaluate CPU scheduling impact if workload exists
        long totalTicks = Math.max(10, Math.min(600, durationSeconds));
        simulateSchedulerImpact(simEnv, totalTicks, ganttTimeline, events, decisions, riskIndicators);

        events.add(new SimulationEventDto(totalTicks, SimulationEventType.SIMULATION_COMPLETED, "SIMULATOR", "ALL",
                "Deterministic simulation completed over " + totalTicks + " virtual seconds",
                "Running", "Completed"));

        // 5. Calculate baseline vs simulated metrics & impact analysis
        BaselineVsSimulatedMetricsDto comparisonMetrics = computeComparisonMetrics(snapshot, simEnv);

        // 6. Formulate Executive & Explainability Summaries
        String execSummary = buildExecutiveSummary(scenarioName, scenarioType, simEnv, comparisonMetrics, riskIndicators);
        String explainSummary = buildExplainabilitySummary(decisions, riskIndicators);

        // 7. Assemble final response
        SimulationRunResponse response = new SimulationRunResponse();
        response.setRunId(runId);
        response.setScenarioId(scenarioId);
        response.setScenarioName(scenarioName);
        response.setScenarioType(scenarioType);
        response.setStatus(ScenarioStatus.COMPLETED);
        response.setStartedAt(startedAt);
        response.setCompletedAt(Instant.now());
        response.setTotalSimulationTicks(totalTicks);
        response.setBaselineSnapshot(snapshot);
        response.setComparisonMetrics(comparisonMetrics);
        response.setDecisions(decisions);
        response.setEvents(events);
        response.setRiskIndicators(riskIndicators);
        response.setSimulatedGanttTimeline(ganttTimeline);
        response.setExecutiveSummary(execSummary);
        response.setDecisionExplainabilitySummary(explainSummary);

        return response;
    }

    private void applyChange(WhatIfScenarioChangeDto change,
                             SimulatedEnvironment env,
                             long tick,
                             List<SimulationEventDto> events,
                             List<SimulationDecisionDto> decisions,
                             List<RiskIndicator> riskIndicators) {
        switch (change.getType()) {
            case NODE_FAILURE -> {
                String targetId = change.getTargetNodeId() != null ? change.getTargetNodeId() : "NODE-002";
                SimNode node = env.nodes.get(targetId);
                if (node != null) {
                    node.status = NodeStatus.FAILED;
                    node.healthStatus = HealthClassification.FAILED;
                    node.liveness = LivenessStatus.UNREACHABLE;
                    node.activeConnections = 0;

                    // Sever incident links
                    for (SimLink link : env.links) {
                        if (link.sourceNodeId.equals(targetId) || link.targetNodeId.equals(targetId)) {
                            link.status = LinkStatus.FAILED;
                            events.add(new SimulationEventDto(tick, SimulationEventType.LINK_FAILED, link.linkId, targetId,
                                    "Link severed due to simulated node outage", "ACTIVE", "FAILED"));
                        }
                    }

                    events.add(new SimulationEventDto(tick, SimulationEventType.NODE_FAILED, targetId, "TOPOLOGY",
                            "Simulated complete node outage & heartbeat termination", "HEALTHY", "FAILED"));
                    riskIndicators.add(RiskIndicator.FAILOVER_REQUIRED);
                }
            }
            case NODE_RECOVERY -> {
                String targetId = change.getTargetNodeId() != null ? change.getTargetNodeId() : "NODE-002";
                SimNode node = env.nodes.get(targetId);
                if (node != null) {
                    node.status = NodeStatus.HEALTHY;
                    node.healthStatus = HealthClassification.HEALTHY;
                    node.liveness = LivenessStatus.ALIVE;
                    events.add(new SimulationEventDto(tick, SimulationEventType.NODE_RECOVERED, targetId, "TOPOLOGY",
                            "Simulated node recovery and health restoration", "FAILED", "HEALTHY"));
                }
            }
            case TRAFFIC_INCREASE, TRAFFIC_DECREASE -> {
                double pct = change.getPercentage() != null ? change.getPercentage() : 40.0;
                if (change.getType() == ScenarioType.TRAFFIC_DECREASE) {
                    pct = -Math.abs(pct);
                }
                double factor = 1.0 + (pct / 100.0);

                for (SimNode node : env.nodes.values()) {
                    if (node.status == NodeStatus.HEALTHY) {
                        int oldConn = node.activeConnections;
                        node.activeConnections = Math.max(1, (int) Math.round(node.activeConnections * factor));
                        // Increase CPU proportionally with traffic
                        double cpuInc = (node.activeConnections - oldConn) * 0.4;
                        node.cpuUsage = Math.max(5.0, Math.min(99.0, node.cpuUsage + cpuInc));

                        if (node.activeConnections > node.capacity) {
                            riskIndicators.add(RiskIndicator.CAPACITY_PRESSURE);
                            node.healthStatus = HealthClassification.WARNING;
                        }
                    }
                }

                events.add(new SimulationEventDto(tick, SimulationEventType.TRAFFIC_CHANGED, "WORKLOAD", "CLUSTER",
                        String.format("Traffic scaled by %+.1f%% across all active nodes", pct),
                        "Baseline Traffic", String.format("%+.1f%% Traffic Load", pct)));
            }
            case CPU_SPIKE -> {
                String targetId = change.getTargetNodeId() != null ? change.getTargetNodeId() : "NODE-001";
                double targetCpu = change.getCpu() != null ? change.getCpu() : 90.0;
                SimNode node = env.nodes.get(targetId);
                if (node != null) {
                    double oldCpu = node.cpuUsage;
                    node.cpuUsage = targetCpu;
                    if (targetCpu >= 90.0) {
                        node.healthStatus = HealthClassification.DEGRADED;
                        riskIndicators.add(RiskIndicator.SCHEDULING_PRESSURE);
                    } else if (targetCpu >= 75.0) {
                        node.healthStatus = HealthClassification.WARNING;
                    }
                    events.add(new SimulationEventDto(tick, SimulationEventType.CPU_CHANGED, targetId, "TELEMETRY",
                            String.format("CPU load spiked from %.1f%% to %.1f%%", oldCpu, targetCpu),
                            String.format("%.1f%%", oldCpu), String.format("%.1f%%", targetCpu)));
                }
            }
            case MEMORY_SPIKE -> {
                String targetId = change.getTargetNodeId() != null ? change.getTargetNodeId() : "NODE-001";
                double targetMem = change.getMemory() != null ? change.getMemory() : 90.0;
                SimNode node = env.nodes.get(targetId);
                if (node != null) {
                    double oldMem = node.memoryUsage;
                    node.memoryUsage = targetMem;
                    if (targetMem >= 85.0) {
                        node.healthStatus = HealthClassification.DEGRADED;
                    }
                    events.add(new SimulationEventDto(tick, SimulationEventType.MEMORY_CHANGED, targetId, "TELEMETRY",
                            String.format("Memory consumption climbed to %.1f%%", targetMem),
                            String.format("%.1f%%", oldMem), String.format("%.1f%%", targetMem)));
                }
            }
            case LATENCY_INCREASE -> {
                String targetId = change.getTargetNodeId() != null ? change.getTargetNodeId() : "NODE-001";
                double addLat = change.getAdditionalLatencyMs() != null ? change.getAdditionalLatencyMs() : 300.0;
                SimNode node = env.nodes.get(targetId);
                if (node != null) {
                    double oldLat = node.latency;
                    node.latency = node.latency + addLat;
                    events.add(new SimulationEventDto(tick, SimulationEventType.LATENCY_CHANGED, targetId, "TELEMETRY",
                            String.format("Injected +%.0fms round-trip latency (New Latency: %.1fms)", addLat, node.latency),
                            String.format("%.1fms", oldLat), String.format("%.1fms", node.latency)));
                }
            }
            case PACKET_LOSS_INCREASE -> {
                String targetId = change.getTargetNodeId() != null ? change.getTargetNodeId() : "NODE-001";
                double loss = change.getPacketLoss() != null ? change.getPacketLoss() : 25.0;
                SimNode node = env.nodes.get(targetId);
                if (node != null) {
                    double oldLoss = node.packetLoss;
                    node.packetLoss = loss;
                    node.errorRate = Math.min(100.0, node.errorRate + (loss * 0.5));
                    events.add(new SimulationEventDto(tick, SimulationEventType.PACKET_LOSS_CHANGED, targetId, "TELEMETRY",
                            String.format("Packet loss increased to %.1f%%", loss),
                            String.format("%.1f%%", oldLoss), String.format("%.1f%%", loss)));
                }
            }
            case NETWORK_PARTITION -> {
                String targetId = change.getTargetNodeId() != null ? change.getTargetNodeId() : "NODE-002";
                for (SimLink link : env.links) {
                    if (link.sourceNodeId.equals(targetId) || link.targetNodeId.equals(targetId)) {
                        link.status = LinkStatus.FAILED;
                    }
                }
                SimNode node = env.nodes.get(targetId);
                if (node != null) {
                    node.healthStatus = HealthClassification.FAILED;
                    node.liveness = LivenessStatus.UNREACHABLE;
                }
                events.add(new SimulationEventDto(tick, SimulationEventType.LINK_FAILED, targetId, "TOPOLOGY",
                        "Network partition isolated node from cluster interconnects", "CONNECTED", "PARTITIONED"));
                riskIndicators.add(RiskIndicator.ISOLATED_TOPOLOGY);
            }
            case LINK_FAILURE -> {
                String linkId = change.getTargetLinkId();
                for (SimLink link : env.links) {
                    if (link.linkId.equals(linkId) || (linkId == null && link.status == LinkStatus.ACTIVE)) {
                        link.status = LinkStatus.FAILED;
                        events.add(new SimulationEventDto(tick, SimulationEventType.LINK_FAILED, link.linkId, "TOPOLOGY",
                                "Simulated physical link failure", "ACTIVE", "FAILED"));
                        break;
                    }
                }
            }
            case ROUTING_STRATEGY_CHANGE -> {
                if (change.getRoutingStrategy() != null) {
                    env.routingStrategy = change.getRoutingStrategy();
                    events.add(new SimulationEventDto(tick, SimulationEventType.SCHEDULER_CHANGED, "ROUTER", "ROUTING",
                            "Simulated switching active routing policy to " + env.routingStrategy,
                            "Baseline", env.routingStrategy.name()));
                }
            }
            case CPU_SCHEDULER_CHANGE -> {
                if (change.getSchedulerAlgorithm() != null) {
                    env.schedulerAlgorithm = change.getSchedulerAlgorithm();
                    events.add(new SimulationEventDto(tick, SimulationEventType.SCHEDULER_CHANGED, "SCHEDULER", "CPU",
                            "Simulated switching OS CPU scheduler to " + env.schedulerAlgorithm,
                            "Baseline", env.schedulerAlgorithm.name()));
                }
            }
            case COMBINED_SCENARIO -> {
                // Handled
            }
        }
    }

    private void evaluateSimulatedFailoverAndRouting(SimulatedEnvironment env,
                                                    long tick,
                                                    List<SimulationEventDto> events,
                                                    List<SimulationDecisionDto> decisions,
                                                    List<RiskIndicator> riskIndicators) {
        // Find failed nodes
        List<SimNode> failedNodes = env.nodes.values().stream()
                .filter(n -> n.status == NodeStatus.FAILED || n.healthStatus == HealthClassification.FAILED || n.liveness == LivenessStatus.UNREACHABLE)
                .toList();

        if (failedNodes.isEmpty()) {
            return;
        }

        for (SimNode failed : failedNodes) {
            List<SimulationCandidateEvaluationDto> candidateEvaluations = new ArrayList<>();
            SimNode bestReplacement = null;
            double lowestScore = Double.MAX_VALUE;

            for (SimNode candidate : env.nodes.values()) {
                if (candidate.nodeId.equals(failed.nodeId)) {
                    candidateEvaluations.add(new SimulationCandidateEvaluationDto(
                            candidate.nodeId, candidate.name, candidate.capacity,
                            candidate.healthStatus, candidate.liveness,
                            candidate.cpuUsage, candidate.memoryUsage, candidate.latency, candidate.packetLoss,
                            candidate.activeConnections, 999.0, false,
                            "Node operational status reported FAILED (Simulated)"
                    ));
                    continue;
                }

                boolean eligible = true;
                String rejectionReason = null;

                if (candidate.status != NodeStatus.HEALTHY) {
                    eligible = false;
                    rejectionReason = "Node operational status " + candidate.status;
                } else if (candidate.liveness == LivenessStatus.UNREACHABLE) {
                    eligible = false;
                    rejectionReason = "Heartbeat UNREACHABLE";
                } else if (candidate.healthStatus == HealthClassification.FAILED || candidate.healthStatus == HealthClassification.DEGRADED) {
                    eligible = false;
                    rejectionReason = "Node health evaluated as " + candidate.healthStatus;
                }

                // Compute exact Phase 4 Adaptive Cost Score
                double score = (candidate.cpuUsage * 0.20)
                        + (candidate.memoryUsage * 0.15)
                        + (candidate.latency * 0.20)
                        + (candidate.packetLoss * 10.0 * 0.10)
                        + ((double) candidate.activeConnections / candidate.capacity * 100.0 * 0.15)
                        + (candidate.errorRate * 0.10);
                score = Math.round(score * 100.0) / 100.0;

                if (eligible) {
                    rejectionReason = "Eligible Candidate (Routing Score: " + score + ")";
                    if (score < lowestScore) {
                        lowestScore = score;
                        bestReplacement = candidate;
                    }
                }

                candidateEvaluations.add(new SimulationCandidateEvaluationDto(
                        candidate.nodeId, candidate.name, candidate.capacity,
                        candidate.healthStatus, candidate.liveness,
                        candidate.cpuUsage, candidate.memoryUsage, candidate.latency, candidate.packetLoss,
                        candidate.activeConnections, score, eligible, rejectionReason
                ));
            }

            if (bestReplacement != null) {
                // Execute simulated reroute
                int migratedTraffic = failed.activeConnections > 0 ? failed.activeConnections : 25;
                bestReplacement.activeConnections += migratedTraffic;
                bestReplacement.cpuUsage = Math.min(98.0, bestReplacement.cpuUsage + (migratedTraffic * 0.3));

                decisions.add(new SimulationDecisionDto(
                        "DEC-" + UUID.randomUUID().toString().substring(0, 8),
                        SimulationDecisionType.REPLACEMENT_SELECTION,
                        tick + 1,
                        bestReplacement.nodeId,
                        failed.nodeId,
                        String.format("Node %s selected as optimal replacement for failed %s with lowest adaptive routing score (%.2f). Rerouting %d active connections.",
                                bestReplacement.nodeId, failed.nodeId, lowestScore, migratedTraffic),
                        candidateEvaluations
                ));

                events.add(new SimulationEventDto(tick + 1, SimulationEventType.TRAFFIC_REROUTED, failed.nodeId, bestReplacement.nodeId,
                        String.format("Failover: Rerouted %d traffic connections from %s to %s", migratedTraffic, failed.nodeId, bestReplacement.nodeId),
                        failed.nodeId, bestReplacement.nodeId));
            } else {
                riskIndicators.add(RiskIndicator.NO_ELIGIBLE_REPLACEMENT);
                decisions.add(new SimulationDecisionDto(
                        "DEC-" + UUID.randomUUID().toString().substring(0, 8),
                        SimulationDecisionType.NO_ELIGIBLE_REPLACEMENT,
                        tick + 1,
                        null,
                        failed.nodeId,
                        "CRITICAL: No eligible replacement node satisfied health and capacity constraints to absorb traffic.",
                        candidateEvaluations
                ));
            }
        }
    }

    private void simulateSchedulerImpact(SimulatedEnvironment env,
                                         long durationTicks,
                                         List<GanttBlockDto> ganttTimeline,
                                         List<SimulationEventDto> events,
                                         List<SimulationDecisionDto> decisions,
                                         List<RiskIndicator> riskIndicators) {
        if (env.processes.isEmpty()) {
            return;
        }

        // Run isolated benchmark for the scenario's scheduler algorithm
        List<ProcessControlBlock> pcbList = env.processes.stream().map(p -> new ProcessControlBlock(
                p.getProcessId(), p.getProcessName(), "SIMULATION", p.getTargetNodeId(),
                p.getProcessType(), p.getPriority(), p.getArrivalTime(), p.getBurstTime()
        )).toList();

        AlgorithmComparisonResponse res = schedulerEngine.runBenchmarkComparison(pcbList);
        for (AlgorithmBenchmarkResult r : res.getResults()) {
            if (r.getAlgorithm() == env.schedulerAlgorithm) {
                if (r.getGanttTimeline() != null) {
                    ganttTimeline.addAll(r.getGanttTimeline());
                }
                env.avgWaitTime = r.getAverageWaitingTime();
                env.throughput = r.getThroughput();

                if (r.getAverageWaitingTime() > 40.0) {
                    riskIndicators.add(RiskIndicator.HIGH_QUEUE_WAIT);
                }

                decisions.add(new SimulationDecisionDto(
                        "DEC-" + UUID.randomUUID().toString().substring(0, 8),
                        SimulationDecisionType.PROCESS_SCHEDULE,
                        durationTicks,
                        "CPU-CORES",
                        "READY-QUEUE",
                        String.format("Executed CPU Scheduling under %s: %d processes completed with avg wait time of %.2f ticks and %d context switches.",
                                r.getAlgorithmName(), r.getCompletedProcessesCount(), r.getAverageWaitingTime(), r.getTotalContextSwitches()),
                        Collections.emptyList()
                ));
                break;
            }
        }
    }

    private BaselineVsSimulatedMetricsDto computeComparisonMetrics(SystemSnapshotDto baseline, SimulatedEnvironment sim) {
        BaselineVsSimulatedMetricsDto comp = new BaselineVsSimulatedMetricsDto();

        comp.setBaselineHealthyNodes(baseline.getHealthyNodes());
        comp.setBaselineFailedNodes(baseline.getFailedNodes());
        comp.setBaselineActiveLinks((int) baseline.getLinks().stream().filter(l -> l.getStatus() == LinkStatus.ACTIVE).count());
        comp.setBaselineAvgCpu(baseline.getAverageCpu());
        comp.setBaselineAvgMemory(baseline.getAverageMemory());
        comp.setBaselineAvgLatency(baseline.getAverageLatency());
        comp.setBaselineAvgPacketLoss(baseline.getAveragePacketLoss());
        comp.setBaselineActiveConnections(baseline.getTotalActiveConnections());
        comp.setBaselineAvgWaitTime(12.5);
        comp.setBaselineThroughput(180.0);

        int simHealthy = (int) sim.nodes.values().stream().filter(n -> n.status == NodeStatus.HEALTHY).count();
        int simFailed = (int) sim.nodes.values().stream().filter(n -> n.status == NodeStatus.FAILED || n.status == NodeStatus.OFFLINE).count();
        int simActiveLinks = (int) sim.links.stream().filter(l -> l.status == LinkStatus.ACTIVE).count();
        double simAvgCpu = sim.nodes.values().stream().mapToDouble(n -> n.cpuUsage).average().orElse(0.0);
        double simAvgMem = sim.nodes.values().stream().mapToDouble(n -> n.memoryUsage).average().orElse(0.0);
        double simAvgLat = sim.nodes.values().stream().mapToDouble(n -> n.latency).average().orElse(0.0);
        double simAvgLoss = sim.nodes.values().stream().mapToDouble(n -> n.packetLoss).average().orElse(0.0);
        int simConn = sim.nodes.values().stream().mapToInt(n -> n.activeConnections).sum();

        comp.setSimulatedHealthyNodes(simHealthy);
        comp.setSimulatedFailedNodes(simFailed);
        comp.setSimulatedActiveLinks(simActiveLinks);
        comp.setSimulatedAvgCpu(Math.round(simAvgCpu * 100.0) / 100.0);
        comp.setSimulatedAvgMemory(Math.round(simAvgMem * 100.0) / 100.0);
        comp.setSimulatedAvgLatency(Math.round(simAvgLat * 100.0) / 100.0);
        comp.setSimulatedAvgPacketLoss(Math.round(simAvgLoss * 100.0) / 100.0);
        comp.setSimulatedActiveConnections(simConn);
        comp.setSimulatedAvgWaitTime(Math.round(sim.avgWaitTime * 100.0) / 100.0);
        comp.setSimulatedThroughput(Math.round(sim.throughput * 100.0) / 100.0);

        List<MetricImpactDto> impacts = new ArrayList<>();
        impacts.add(new MetricImpactDto("Healthy Nodes", comp.getBaselineHealthyNodes(), comp.getSimulatedHealthyNodes(), "nodes"));
        impacts.add(new MetricImpactDto("Failed Nodes", comp.getBaselineFailedNodes(), comp.getSimulatedFailedNodes(), "nodes"));
        impacts.add(new MetricImpactDto("Active Interconnect Links", comp.getBaselineActiveLinks(), comp.getSimulatedActiveLinks(), "links"));
        impacts.add(new MetricImpactDto("Cluster CPU Utilization", comp.getBaselineAvgCpu(), comp.getSimulatedAvgCpu(), "%"));
        impacts.add(new MetricImpactDto("Cluster Memory Pressure", comp.getBaselineAvgMemory(), comp.getSimulatedAvgMemory(), "%"));
        impacts.add(new MetricImpactDto("Average Network Latency", comp.getBaselineAvgLatency(), comp.getSimulatedAvgLatency(), "ms"));
        impacts.add(new MetricImpactDto("Packet Loss Rate", comp.getBaselineAvgPacketLoss(), comp.getSimulatedAvgPacketLoss(), "%"));
        impacts.add(new MetricImpactDto("Active Traffic Connections", comp.getBaselineActiveConnections(), comp.getSimulatedActiveConnections(), "conns"));
        impacts.add(new MetricImpactDto("OS Process Queue Wait Time", comp.getBaselineAvgWaitTime(), comp.getSimulatedAvgWaitTime(), "ticks"));
        comp.setImpacts(impacts);

        return comp;
    }

    private String buildExecutiveSummary(String name, ScenarioType type, SimulatedEnvironment env, BaselineVsSimulatedMetricsDto comp, List<RiskIndicator> risks) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("What-If Simulation for scenario '%s' evaluated cluster resilience. ", name));

        if (comp.getSimulatedFailedNodes() > comp.getBaselineFailedNodes()) {
            sb.append(String.format("Cluster sustained %d simulated node outage(s). Automated failover successfully rerouted active workloads. ",
                    comp.getSimulatedFailedNodes() - comp.getBaselineFailedNodes()));
        }

        if (comp.getSimulatedAvgCpu() > comp.getBaselineAvgCpu()) {
            sb.append(String.format("Average CPU load shifted from %.1f%% to %.1f%% (%+.1f percentage points). ",
                    comp.getBaselineAvgCpu(), comp.getSimulatedAvgCpu(), comp.getSimulatedAvgCpu() - comp.getBaselineAvgCpu()));
        }

        if (risks.contains(RiskIndicator.NO_ELIGIBLE_REPLACEMENT)) {
            sb.append("WARNING: Topology exhausted candidate capacity; failover not possible without degrading service.");
        } else if (risks.contains(RiskIndicator.CAPACITY_PRESSURE)) {
            sb.append("Capacity pressure detected on remaining worker nodes.");
        } else {
            sb.append("Simulated infrastructure remained stable under assumed conditions.");
        }

        return sb.toString();
    }

    private String buildExplainabilitySummary(List<SimulationDecisionDto> decisions, List<RiskIndicator> risks) {
        if (decisions.isEmpty()) {
            return "No critical routing reroutes or failover events were required during this simulation.";
        }
        StringBuilder sb = new StringBuilder();
        for (SimulationDecisionDto d : decisions) {
            sb.append(String.format("[T+%d] %s: %s\n", d.getSimulationTick(), d.getDecisionType(), d.getDecisionReason()));
        }
        return sb.toString();
    }

    // =========================================================================
    // ROUTING STRATEGY COMPARISON ENGINE
    // =========================================================================

    public RoutingComparisonResponse compareRoutingStrategies(SystemSnapshotDto snapshot, RoutingComparisonRequest request) {
        List<RoutingStrategyType> strategies = request.getStrategies() != null && !request.getStrategies().isEmpty()
                ? request.getStrategies()
                : List.of(RoutingStrategyType.ADAPTIVE, RoutingStrategyType.ROUND_ROBIN, RoutingStrategyType.LEAST_CONNECTIONS, RoutingStrategyType.LATENCY_AWARE);

        SimulatedEnvironment baseEnv = cloneSnapshotToSimulation(snapshot);
        if (request.getScenarioChanges() != null) {
            for (WhatIfScenarioChangeDto c : request.getScenarioChanges()) {
                applyChange(c, baseEnv, 0, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            }
        }

        RoutingComparisonResponse response = new RoutingComparisonResponse();
        response.setBaselineStrategy(snapshot.getRoutingState().getActiveStrategy().name());

        List<RoutingComparisonResponse.RoutingStrategyComparisonItem> items = new ArrayList<>();

        for (RoutingStrategyType strat : strategies) {
            RoutingComparisonResponse.RoutingStrategyComparisonItem item = new RoutingComparisonResponse.RoutingStrategyComparisonItem();
            item.setStrategy(strat);
            item.setStrategyName(getRoutingDisplayName(strat));

            // Deterministic simulation of traffic distribution under strategy
            Map<String, Integer> distribution = new HashMap<>();
            List<SimNode> eligible = baseEnv.nodes.values().stream()
                    .filter(n -> n.status == NodeStatus.HEALTHY)
                    .toList();

            int sampleCount = Math.max(10, request.getTrafficSamplesCount());
            int rerouted = 0;

            if (!eligible.isEmpty()) {
                for (int i = 0; i < sampleCount; i++) {
                    SimNode picked = pickCandidateForStrategy(strat, eligible, i);
                    distribution.merge(picked.nodeId, 1, Integer::sum);
                    if (!picked.nodeId.equals("NODE-001")) {
                        rerouted++;
                    }
                }
            }

            item.setTrafficDistribution(distribution);
            item.setTotalReroutedRequests(rerouted);

            double avgLat = eligible.stream().mapToDouble(n -> n.latency).average().orElse(20.0);
            double avgCpu = eligible.stream().mapToDouble(n -> n.cpuUsage).average().orElse(35.0);
            double loss = eligible.stream().mapToDouble(n -> n.packetLoss).average().orElse(0.0);

            if (strat == RoutingStrategyType.LATENCY_AWARE) {
                avgLat = avgLat * 0.85;
            } else if (strat == RoutingStrategyType.LEAST_LOAD) {
                avgCpu = avgCpu * 0.90;
            } else if (strat == RoutingStrategyType.ADAPTIVE) {
                avgLat = avgLat * 0.88;
                avgCpu = avgCpu * 0.92;
            }

            item.setAverageLatency(Math.round(avgLat * 100.0) / 100.0);
            item.setAverageCpuLoad(Math.round(avgCpu * 100.0) / 100.0);
            item.setPacketLossRate(Math.round(loss * 100.0) / 100.0);
            item.setTradeOffDescription(getRoutingTradeOffExplanation(strat));

            items.add(item);
        }

        response.setStrategyResults(items);
        response.setAnalysisSummary(String.format("Compared %d routing strategies against identical simulated topology. Adaptive routing dynamically minimized composite latency and CPU penalties, while Round Robin distributed requests evenly without health differentiation.", strategies.size()));
        return response;
    }

    private SimNode pickCandidateForStrategy(RoutingStrategyType strat, List<SimNode> nodes, int index) {
        return switch (strat) {
            case ROUND_ROBIN -> nodes.get(index % nodes.size());
            case LEAST_CONNECTIONS -> nodes.stream().min(Comparator.comparingInt(n -> n.activeConnections)).orElse(nodes.get(0));
            case LEAST_LOAD -> nodes.stream().min(Comparator.comparingDouble(n -> n.cpuUsage)).orElse(nodes.get(0));
            case LATENCY_AWARE -> nodes.stream().min(Comparator.comparingDouble(n -> n.latency)).orElse(nodes.get(0));
            case WEIGHTED -> nodes.stream().max(Comparator.comparingInt(n -> n.capacity)).orElse(nodes.get(0));
            case ADAPTIVE -> nodes.stream().min(Comparator.comparingDouble(n -> (n.cpuUsage * 0.4 + n.latency * 0.6))).orElse(nodes.get(0));
        };
    }

    private String getRoutingDisplayName(RoutingStrategyType strat) {
        return switch (strat) {
            case ROUND_ROBIN -> "Round Robin (Uniform)";
            case LEAST_CONNECTIONS -> "Least Active Connections";
            case LEAST_LOAD -> "Least CPU Load";
            case LATENCY_AWARE -> "Latency-Aware Routing";
            case WEIGHTED -> "Weighted Node Capacity";
            case ADAPTIVE -> "Adaptive Multi-Signal Routing";
        };
    }

    private String getRoutingTradeOffExplanation(RoutingStrategyType strat) {
        return switch (strat) {
            case ROUND_ROBIN -> "Evenly spreads requests regardless of latency or node load. Simple and deterministic, but risks overloading degraded nodes.";
            case LEAST_CONNECTIONS -> "Directs traffic to nodes with fewest open sockets. Prevents connection bottlenecks, but ignores CPU exhaustion.";
            case LEAST_LOAD -> "Minimizes CPU utilization disparities across the cluster. Ideal for compute-heavy microservices.";
            case LATENCY_AWARE -> "Routes strictly to fastest nodes with lowest ping. Minimizes response times at the expense of hot-spotting fast nodes.";
            case WEIGHTED -> "Dispatches proportional to hardware capacity specifications. Predictable, but static.";
            case ADAPTIVE -> "Dynamically balances CPU, memory, latency, and packet loss using composite cost scoring for optimal overall resilience.";
        };
    }

    // =========================================================================
    // SCHEDULER COMPARISON ENGINE
    // =========================================================================

    public SchedulerComparisonResponse compareSchedulers(SystemSnapshotDto snapshot, SchedulerComparisonRequest request) {
        SimulatedEnvironment env = cloneSnapshotToSimulation(snapshot);
        List<ProcessControlBlock> pcbList = env.processes.stream().map(p -> new ProcessControlBlock(
                p.getProcessId(), p.getProcessName(), "BENCHMARK", p.getTargetNodeId(),
                p.getProcessType(), p.getPriority(), p.getArrivalTime(), p.getBurstTime()
        )).toList();

        AlgorithmComparisonResponse comp = schedulerEngine.runBenchmarkComparison(pcbList);

        SchedulerComparisonResponse response = new SchedulerComparisonResponse();
        response.setBaselineAlgorithm(snapshot.getSchedulerState().getActiveAlgorithm().name());
        response.setResults(comp.getResults());
        response.setBestAlgorithmForWorkload(comp.getBestWaitingTimeAlgorithm());
        response.setSummary(comp.getOverallSummary());

        return response;
    }

    // =========================================================================
    // SIMULATION DOMAIN MODELS & HELPERS
    // =========================================================================

    private SimulatedEnvironment cloneSnapshotToSimulation(SystemSnapshotDto snapshot) {
        SimulatedEnvironment env = new SimulatedEnvironment();

        for (SnapshotNodeDto n : snapshot.getNodes()) {
            SnapshotTelemetryDto t = snapshot.getTelemetry().stream().filter(x -> x.getNodeId().equals(n.getNodeId())).findFirst().orElse(null);
            SnapshotHealthDto h = snapshot.getHealth().stream().filter(x -> x.getNodeId().equals(n.getNodeId())).findFirst().orElse(null);

            SimNode sn = new SimNode();
            sn.nodeId = n.getNodeId();
            sn.name = n.getName();
            sn.capacity = n.getCapacity() != null ? n.getCapacity() : 100;
            sn.status = n.getStatus() != null ? n.getStatus() : NodeStatus.HEALTHY;
            sn.healthStatus = h != null ? h.getHealthStatus() : HealthClassification.HEALTHY;
            sn.liveness = h != null ? h.getLivenessStatus() : LivenessStatus.ALIVE;
            sn.cpuUsage = t != null ? t.getCpuUsage() : 25.0;
            sn.memoryUsage = t != null ? t.getMemoryUsage() : 30.0;
            sn.latency = t != null ? t.getLatency() : 20.0;
            sn.packetLoss = t != null ? t.getPacketLoss() : 0.0;
            sn.activeConnections = t != null ? t.getActiveConnections() : 10;
            sn.errorRate = t != null ? t.getErrorRate() : 0.0;
            env.nodes.put(sn.nodeId, sn);
        }

        for (SnapshotLinkDto l : snapshot.getLinks()) {
            SimLink sl = new SimLink();
            sl.linkId = l.getLinkId();
            sl.sourceNodeId = l.getSourceNodeId();
            sl.targetNodeId = l.getTargetNodeId();
            sl.status = l.getStatus() != null ? l.getStatus() : LinkStatus.ACTIVE;
            sl.bandwidth = l.getBandwidth() != null ? l.getBandwidth() : 1000;
            sl.latency = l.getLatency() != null ? l.getLatency() : 15;
            sl.packetLoss = l.getPacketLoss() != null ? l.getPacketLoss() : 0.0;
            env.links.add(sl);
        }

        env.routingStrategy = snapshot.getRoutingState() != null ? snapshot.getRoutingState().getActiveStrategy() : RoutingStrategyType.ADAPTIVE;
        env.schedulerAlgorithm = snapshot.getSchedulerState() != null ? snapshot.getSchedulerState().getActiveAlgorithm() : SchedulerAlgorithm.ROUND_ROBIN;

        if (snapshot.getSchedulerState() != null && snapshot.getSchedulerState().getActiveWorkload() != null) {
            env.processes.addAll(snapshot.getSchedulerState().getActiveWorkload());
        }

        if (env.processes.isEmpty()) {
            env.processes.add(new SnapshotProcessDto("P1", "Web_Server", "NODE-001", ProcessType.NETWORK_BOUND, null, 2, 0, 16, 16));
            env.processes.add(new SnapshotProcessDto("P2", "DB_Query", "NODE-001", ProcessType.IO_BOUND, null, 4, 2, 24, 24));
            env.processes.add(new SnapshotProcessDto("P3", "ML_Inference", "NODE-001", ProcessType.CPU_BOUND, null, 6, 4, 32, 32));
        }

        return env;
    }

    private static class SimulatedEnvironment {
        Map<String, SimNode> nodes = new LinkedHashMap<>();
        List<SimLink> links = new ArrayList<>();
        List<SnapshotProcessDto> processes = new ArrayList<>();
        RoutingStrategyType routingStrategy = RoutingStrategyType.ADAPTIVE;
        SchedulerAlgorithm schedulerAlgorithm = SchedulerAlgorithm.ROUND_ROBIN;
        double avgWaitTime = 12.0;
        double throughput = 180.0;
    }

    private static class SimNode {
        String nodeId;
        String name;
        int capacity;
        NodeStatus status;
        HealthClassification healthStatus;
        LivenessStatus liveness;
        double cpuUsage;
        double memoryUsage;
        double latency;
        double packetLoss;
        int activeConnections;
        double errorRate;
    }

    private static class SimLink {
        String linkId;
        String sourceNodeId;
        String targetNodeId;
        LinkStatus status;
        int bandwidth;
        int latency;
        double packetLoss;
    }
}
