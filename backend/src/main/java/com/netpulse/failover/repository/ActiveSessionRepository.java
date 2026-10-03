package com.netpulse.failover.repository;

import com.netpulse.failover.entity.ActiveSessionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActiveSessionRepository extends JpaRepository<ActiveSessionRecord, Long> {

    Optional<ActiveSessionRecord> findBySessionId(String sessionId);

    List<ActiveSessionRecord> findByNodeIdAndStatus(String nodeId, String status);

    long countByNodeIdAndStatus(String nodeId, String status);

    List<ActiveSessionRecord> findAllByStatus(String status);

    List<ActiveSessionRecord> findAllByCreatedAtBefore(Instant cutoff);
}
