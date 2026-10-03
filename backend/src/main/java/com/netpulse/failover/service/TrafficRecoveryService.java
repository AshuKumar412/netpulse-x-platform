package com.netpulse.failover.service;

import com.netpulse.failover.entity.ActiveSessionRecord;
import com.netpulse.failover.repository.ActiveSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TrafficRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(TrafficRecoveryService.class);

    private final ActiveSessionRepository sessionRepository;

    public TrafficRecoveryService(ActiveSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public ActiveSessionRecord registerSession(String sessionId, String nodeId, String requestType, String strategy) {
        Instant now = Instant.now();
        ActiveSessionRecord record = new ActiveSessionRecord(
                sessionId,
                nodeId,
                nodeId,
                requestType != null ? requestType : "STANDARD",
                strategy != null ? strategy : "ADAPTIVE",
                "ACTIVE",
                now,
                now,
                null
        );
        return sessionRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<ActiveSessionRecord> getAffectedSessions(String failedNodeId) {
        return sessionRepository.findByNodeIdAndStatus(failedNodeId, "ACTIVE");
    }

    public static class RerouteResult {
        private final int affectedCount;
        private final int reroutedCount;
        private final int unrecoveredCount;
        private final List<String> reroutedSessionIds;

        public RerouteResult(int affectedCount, int reroutedCount, int unrecoveredCount, List<String> reroutedSessionIds) {
            this.affectedCount = affectedCount;
            this.reroutedCount = reroutedCount;
            this.unrecoveredCount = unrecoveredCount;
            this.reroutedSessionIds = reroutedSessionIds;
        }

        public int getAffectedCount() {
            return affectedCount;
        }

        public int getReroutedCount() {
            return reroutedCount;
        }

        public int getUnrecoveredCount() {
            return unrecoveredCount;
        }

        public List<String> getReroutedSessionIds() {
            return reroutedSessionIds;
        }
    }

    @Transactional
    public RerouteResult rerouteSessions(String failedNodeId, String replacementNodeId, String cycleId) {
        List<ActiveSessionRecord> activeSessions = sessionRepository.findByNodeIdAndStatus(failedNodeId, "ACTIVE");
        int affected = activeSessions.size();
        if (affected == 0) {
            return new RerouteResult(0, 0, 0, List.of());
        }

        int rerouted = 0;
        int unrecovered = 0;
        List<String> reroutedIds = new ArrayList<>();
        Instant now = Instant.now();

        for (ActiveSessionRecord session : activeSessions) {
            try {
                if (replacementNodeId != null && !replacementNodeId.isBlank()) {
                    session.setOriginalNodeId(session.getNodeId());
                    session.setNodeId(replacementNodeId);
                    session.setStatus("REROUTED");
                    session.setUpdatedAt(now);
                    session.setFailoverCycleId(cycleId);
                    sessionRepository.save(session);
                    rerouted++;
                    reroutedIds.add(session.getSessionId());
                } else {
                    session.setStatus("TERMINATED");
                    session.setUpdatedAt(now);
                    session.setFailoverCycleId(cycleId);
                    sessionRepository.save(session);
                    unrecovered++;
                }
            } catch (Exception e) {
                log.error("Failed to migrate session {}: {}", session.getSessionId(), e.getMessage());
                unrecovered++;
            }
        }

        log.info("Traffic recovery for cycle {}: affected={}, rerouted={}, unrecovered={} to replacement {}",
                cycleId, affected, rerouted, unrecovered, replacementNodeId);

        return new RerouteResult(affected, rerouted, unrecovered, reroutedIds);
    }

    @Transactional
    public void cleanupOldSessions(Duration age) {
        Instant cutoff = Instant.now().minus(age);
        List<ActiveSessionRecord> oldSessions = sessionRepository.findAllByCreatedAtBefore(cutoff);
        if (!oldSessions.isEmpty()) {
            sessionRepository.deleteAll(oldSessions);
            log.debug("Pruned {} expired session records older than {}", oldSessions.size(), cutoff);
        }
    }
}
