package com.netpulse.failover.service;

import com.netpulse.failover.entity.FailoverEvent;
import com.netpulse.failover.dto.FailoverEventDto;
import com.netpulse.failover.repository.FailoverEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Phase 5 — Audit service for failover events.
 *
 * Responsibilities:
 * - Persist every failover state transition to the database
 * - Broadcast real-time WebSocket events to /topic/failover
 * - Generate deterministic event and cycle IDs
 *
 * NEVER generates fake data. All metrics originate from persisted
 * FailoverEvent records in PostgreSQL.
 */
@Service
public class FailoverAuditService {

    private static final Logger log = LoggerFactory.getLogger(FailoverAuditService.class);
    private static final String WS_TOPIC = "/topic/failover";

    private final FailoverEventRepository eventRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public FailoverAuditService(FailoverEventRepository eventRepository,
                                SimpMessagingTemplate messagingTemplate) {
        this.eventRepository = eventRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Persist and broadcast a failover event state transition.
     * Returns the saved entity so callers can continue mutating it.
     */
    @Transactional
    public FailoverEvent recordEvent(FailoverEvent event) {
        FailoverEvent saved = eventRepository.save(event);
        try {
            messagingTemplate.convertAndSend(WS_TOPIC, FailoverEventDto.fromEntity(saved));
        } catch (Exception e) {
            log.warn("WebSocket broadcast failed for failover event {}: {}", saved.getEventId(), e.getMessage());
        }
        log.debug("Failover event recorded: eventId={} state={} node={}",
                saved.getEventId(), saved.getCurrentState(), saved.getFailureNodeId());
        return saved;
    }

    /**
     * Generate a unique event ID for a new failover lifecycle.
     */
    public String generateEventId() {
        return "FE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * Generate a unique recovery cycle ID that groups all events in one failover round.
     */
    public String generateRecoveryCycleId() {
        return "RC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
