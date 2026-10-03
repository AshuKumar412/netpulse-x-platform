package com.netpulse.whatif.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netpulse.exception.BadRequestException;
import com.netpulse.exception.ResourceNotFoundException;
import com.netpulse.whatif.dto.*;
import com.netpulse.whatif.entity.*;
import com.netpulse.whatif.repository.*;
import com.netpulse.whatif.state.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class WhatIfScenarioService {

    private static final Logger log = LoggerFactory.getLogger(WhatIfScenarioService.class);
    public static final String WS_WHATIF_TOPIC = "/topic/what-if";

    private final SystemSnapshotService snapshotService;
    private final WhatIfSimulationEngine simulationEngine;
    private final WhatIfTemplateService templateService;
    private final WhatIfScenarioRepository scenarioRepository;
    private final SimulationRunRepository runRepository;
    private final SimulationDecisionRepository decisionRepository;
    private final SimulationEventRepository eventRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public WhatIfScenarioService(SystemSnapshotService snapshotService,
                                 WhatIfSimulationEngine simulationEngine,
                                 WhatIfTemplateService templateService,
                                 WhatIfScenarioRepository scenarioRepository,
                                 SimulationRunRepository runRepository,
                                 SimulationDecisionRepository decisionRepository,
                                 SimulationEventRepository eventRepository,
                                 SimpMessagingTemplate messagingTemplate,
                                 ObjectMapper objectMapper) {
        this.snapshotService = snapshotService;
        this.simulationEngine = simulationEngine;
        this.templateService = templateService;
        this.scenarioRepository = scenarioRepository;
        this.runRepository = runRepository;
        this.decisionRepository = decisionRepository;
        this.eventRepository = eventRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    public SystemSnapshotDto captureSystemSnapshot() {
        return snapshotService.captureSnapshot();
    }

    @Transactional
    public WhatIfScenarioResponse createScenario(WhatIfScenarioRequest request, String owner) {
        String scenarioId = "SCEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String changesJson = "[]";
        try {
            if (request.getChanges() != null) {
                changesJson = objectMapper.writeValueAsString(request.getChanges());
            }
        } catch (Exception e) {
            log.warn("Could not serialize scenario changes: {}", e.getMessage());
        }

        WhatIfScenarioEntity entity = new WhatIfScenarioEntity(
                scenarioId,
                request.getName(),
                request.getDescription(),
                owner != null ? owner : "OPERATOR",
                request.getType(),
                changesJson,
                Math.max(10, Math.min(600, request.getDurationSeconds()))
        );

        WhatIfScenarioEntity saved = scenarioRepository.save(entity);
        broadcastUpdate(Map.of("eventType", "SIMULATION_CREATED", "scenarioId", scenarioId, "name", saved.getName()));
        return mapToResponse(saved, null);
    }

    public List<WhatIfScenarioResponse> getAllScenarios() {
        List<WhatIfScenarioEntity> list = scenarioRepository.findRecentScenarios(PageRequest.of(0, 50));
        return list.stream().map(entity -> {
            SimulationRunEntity latestRun = runRepository.findFirstByScenarioIdOrderByStartedAtDesc(entity.getScenarioId()).orElse(null);
            SimulationRunResponse runResponse = latestRun != null ? mapRunEntityToResponse(latestRun) : null;
            return mapToResponse(entity, runResponse);
        }).toList();
    }

    public WhatIfScenarioResponse getScenarioById(String scenarioId) {
        WhatIfScenarioEntity entity = scenarioRepository.findByScenarioId(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("What-If Scenario not found: " + scenarioId));

        SimulationRunEntity latestRun = runRepository.findFirstByScenarioIdOrderByStartedAtDesc(scenarioId).orElse(null);
        SimulationRunResponse runResponse = latestRun != null ? mapRunEntityToResponse(latestRun) : null;
        return mapToResponse(entity, runResponse);
    }

    @Transactional
    public SimulationRunResponse runSimulation(String scenarioId) {
        WhatIfScenarioEntity scenario = scenarioRepository.findByScenarioId(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("What-If Scenario not found: " + scenarioId));

        scenario.setStatus(ScenarioStatus.RUNNING);
        scenarioRepository.save(scenario);

        broadcastUpdate(Map.of("eventType", "SIMULATION_STARTED", "scenarioId", scenarioId, "name", scenario.getName()));

        // Capture live baseline snapshot (read-only)
        SystemSnapshotDto snapshot = snapshotService.captureSnapshot();

        List<WhatIfScenarioChangeDto> changes = Collections.emptyList();
        try {
            if (scenario.getChangesJson() != null && !scenario.getChangesJson().isBlank()) {
                changes = objectMapper.readValue(scenario.getChangesJson(), new TypeReference<List<WhatIfScenarioChangeDto>>() {});
            }
        } catch (Exception e) {
            log.warn("Error parsing scenario changes JSON: {}", e.getMessage());
        }

        // Execute deterministic simulation in isolated in-memory environment
        SimulationRunResponse runResponse = simulationEngine.executeSimulation(
                scenario.getScenarioId(),
                scenario.getName(),
                scenario.getType(),
                changes,
                scenario.getDurationSeconds(),
                snapshot
        );

        // Persist run history
        SimulationRunEntity runEntity = new SimulationRunEntity(runResponse.getRunId(), scenarioId, runResponse.getStartedAt());
        runEntity.setCompletedAt(runResponse.getCompletedAt());
        runEntity.setStatus(ScenarioStatus.COMPLETED);
        runEntity.setTotalSimulationTicks(runResponse.getTotalSimulationTicks());
        runEntity.setExecutiveSummary(runResponse.getExecutiveSummary());

        try {
            runEntity.setBaselineSnapshotJson(objectMapper.writeValueAsString(snapshot));
            runEntity.setMetricsComparisonJson(objectMapper.writeValueAsString(runResponse.getComparisonMetrics()));
            runEntity.setRiskIndicatorsJson(objectMapper.writeValueAsString(runResponse.getRiskIndicators()));
        } catch (Exception e) {
            log.warn("Error serializing run result JSON: {}", e.getMessage());
        }

        runRepository.save(runEntity);

        // Persist decisions
        if (runResponse.getDecisions() != null) {
            for (SimulationDecisionDto dec : runResponse.getDecisions()) {
                String evalsJson = "[]";
                try {
                    evalsJson = objectMapper.writeValueAsString(dec.getCandidateEvaluations());
                } catch (Exception ignored) {}

                SimulationDecisionEntity decEntity = new SimulationDecisionEntity(
                        dec.getDecisionId(),
                        runResponse.getRunId(),
                        dec.getDecisionType(),
                        dec.getSimulationTick(),
                        dec.getSelectedNodeId(),
                        dec.getSourceNodeId(),
                        dec.getDecisionReason(),
                        evalsJson
                );
                decisionRepository.save(decEntity);
            }
        }

        // Persist events
        if (runResponse.getEvents() != null) {
            for (SimulationEventDto evt : runResponse.getEvents()) {
                SimulationEventEntity evtEntity = new SimulationEventEntity(
                        runResponse.getRunId(),
                        evt.getSimulationTick(),
                        evt.getEventType(),
                        evt.getSource(),
                        evt.getTarget(),
                        evt.getReason(),
                        evt.getStateBefore(),
                        evt.getStateAfter()
                );
                eventRepository.save(evtEntity);
            }
        }

        scenario.setStatus(ScenarioStatus.COMPLETED);
        scenario.setCompletedAt(Instant.now());
        scenarioRepository.save(scenario);

        broadcastUpdate(Map.of(
                "eventType", "SIMULATION_COMPLETED",
                "scenarioId", scenarioId,
                "runId", runResponse.getRunId(),
                "summary", runResponse.getExecutiveSummary()
        ));

        return runResponse;
    }

    @Transactional
    public void cancelSimulation(String scenarioId) {
        WhatIfScenarioEntity scenario = scenarioRepository.findByScenarioId(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("What-If Scenario not found: " + scenarioId));
        scenario.setStatus(ScenarioStatus.CANCELLED);
        scenarioRepository.save(scenario);
        broadcastUpdate(Map.of("eventType", "SIMULATION_CANCELLED", "scenarioId", scenarioId));
    }

    public List<SimulationEventDto> getTimeline(String scenarioId) {
        SimulationRunEntity latestRun = runRepository.findFirstByScenarioIdOrderByStartedAtDesc(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No simulation runs found for scenario: " + scenarioId));

        List<SimulationEventEntity> list = eventRepository.findByRunIdOrderBySimulationTickAsc(latestRun.getRunId());
        return list.stream().map(e -> new SimulationEventDto(
                e.getSimulationTick(), e.getEventType(), e.getSource(), e.getTarget(),
                e.getReason(), e.getStateBefore(), e.getStateAfter()
        )).toList();
    }

    public List<SimulationDecisionDto> getDecisions(String scenarioId) {
        SimulationRunEntity latestRun = runRepository.findFirstByScenarioIdOrderByStartedAtDesc(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No simulation runs found for scenario: " + scenarioId));

        List<SimulationDecisionEntity> list = decisionRepository.findByRunIdOrderBySimulationTickAsc(latestRun.getRunId());
        return list.stream().map(d -> {
            List<SimulationCandidateEvaluationDto> evals = Collections.emptyList();
            try {
                if (d.getCandidateEvaluationsJson() != null) {
                    evals = objectMapper.readValue(d.getCandidateEvaluationsJson(), new TypeReference<List<SimulationCandidateEvaluationDto>>() {});
                }
            } catch (Exception ignored) {}

            return new SimulationDecisionDto(
                    d.getDecisionId(), d.getDecisionType(), d.getSimulationTick(),
                    d.getSelectedNodeId(), d.getSourceNodeId(), d.getDecisionReason(), evals
            );
        }).toList();
    }

    public SimulationComparisonResponse getComparison(String scenarioId) {
        WhatIfScenarioEntity scenario = scenarioRepository.findByScenarioId(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("What-If Scenario not found: " + scenarioId));

        SimulationRunEntity latestRun = runRepository.findFirstByScenarioIdOrderByStartedAtDesc(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No simulation runs found for scenario: " + scenarioId));

        SimulationComparisonResponse comp = new SimulationComparisonResponse();
        comp.setScenarioId(scenarioId);
        comp.setScenarioName(scenario.getName());

        try {
            if (latestRun.getBaselineSnapshotJson() != null) {
                comp.setBaselineSnapshot(objectMapper.readValue(latestRun.getBaselineSnapshotJson(), SystemSnapshotDto.class));
            }
            if (latestRun.getMetricsComparisonJson() != null) {
                comp.setMetrics(objectMapper.readValue(latestRun.getMetricsComparisonJson(), BaselineVsSimulatedMetricsDto.class));
            }
            if (latestRun.getRiskIndicatorsJson() != null) {
                comp.setRiskIndicators(objectMapper.readValue(latestRun.getRiskIndicatorsJson(), new TypeReference<List<RiskIndicator>>() {}));
            }
        } catch (Exception e) {
            log.warn("Error parsing comparison JSON: {}", e.getMessage());
        }

        comp.setKeyDecisions(getDecisions(scenarioId));
        comp.setImpactAnalysisSummary(latestRun.getExecutiveSummary());

        return comp;
    }

    public RoutingComparisonResponse compareRouting(RoutingComparisonRequest request) {
        SystemSnapshotDto snapshot = snapshotService.captureSnapshot();
        return simulationEngine.compareRoutingStrategies(snapshot, request);
    }

    public SchedulerComparisonResponse compareScheduler(SchedulerComparisonRequest request) {
        SystemSnapshotDto snapshot = snapshotService.captureSnapshot();
        return simulationEngine.compareSchedulers(snapshot, request);
    }

    public List<WhatIfTemplateDto> getTemplates() {
        return templateService.getPredefinedTemplates();
    }

    public String exportReport(String scenarioId, String format) {
        WhatIfScenarioResponse scenario = getScenarioById(scenarioId);
        if (scenario.getLatestRun() == null) {
            throw new BadRequestException("Cannot export scenario report: simulation has not been executed yet.");
        }

        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Metric,Baseline,Simulated,Absolute Change,Percentage Change,Unit\n");
            if (scenario.getLatestRun().getComparisonMetrics() != null && scenario.getLatestRun().getComparisonMetrics().getImpacts() != null) {
                for (MetricImpactDto imp : scenario.getLatestRun().getComparisonMetrics().getImpacts()) {
                    sb.append(String.format("\"%s\",%.2f,%.2f,%.2f,%s,\"%s\"\n",
                            imp.getMetricName(), imp.getBaselineValue(), imp.getSimulatedValue(),
                            imp.getAbsoluteChange(), imp.getPercentageChange() != null ? imp.getPercentageChange() + "%" : "N/A", imp.getUnit()));
                }
            }
            return sb.toString();
        }

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(scenario.getLatestRun());
        } catch (Exception e) {
            throw new RuntimeException("Error generating JSON export: " + e.getMessage());
        }
    }

    private void broadcastUpdate(Map<String, Object> payload) {
        try {
            messagingTemplate.convertAndSend(WS_WHATIF_TOPIC, payload);
        } catch (Exception e) {
            log.trace("Error broadcasting what-if update: {}", e.getMessage());
        }
    }

    private WhatIfScenarioResponse mapToResponse(WhatIfScenarioEntity entity, SimulationRunResponse runResponse) {
        WhatIfScenarioResponse resp = new WhatIfScenarioResponse();
        resp.setScenarioId(entity.getScenarioId());
        resp.setName(entity.getName());
        resp.setDescription(entity.getDescription());
        resp.setCreatedBy(entity.getCreatedBy());
        resp.setType(entity.getType());
        resp.setStatus(entity.getStatus());
        resp.setDurationSeconds(entity.getDurationSeconds());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setCompletedAt(entity.getCompletedAt());
        resp.setLatestRun(runResponse);

        try {
            if (entity.getChangesJson() != null) {
                resp.setChanges(objectMapper.readValue(entity.getChangesJson(), new TypeReference<List<WhatIfScenarioChangeDto>>() {}));
            }
        } catch (Exception ignored) {}

        return resp;
    }

    private SimulationRunResponse mapRunEntityToResponse(SimulationRunEntity run) {
        SimulationRunResponse res = new SimulationRunResponse();
        res.setRunId(run.getRunId());
        res.setScenarioId(run.getScenarioId());
        res.setStatus(run.getStatus());
        res.setStartedAt(run.getStartedAt());
        res.setCompletedAt(run.getCompletedAt());
        res.setTotalSimulationTicks(run.getTotalSimulationTicks());
        res.setExecutiveSummary(run.getExecutiveSummary());

        try {
            if (run.getBaselineSnapshotJson() != null) {
                res.setBaselineSnapshot(objectMapper.readValue(run.getBaselineSnapshotJson(), SystemSnapshotDto.class));
            }
            if (run.getMetricsComparisonJson() != null) {
                res.setComparisonMetrics(objectMapper.readValue(run.getMetricsComparisonJson(), BaselineVsSimulatedMetricsDto.class));
            }
            if (run.getRiskIndicatorsJson() != null) {
                res.setRiskIndicators(objectMapper.readValue(run.getRiskIndicatorsJson(), new TypeReference<List<RiskIndicator>>() {}));
            }
        } catch (Exception ignored) {}

        return res;
    }
}
