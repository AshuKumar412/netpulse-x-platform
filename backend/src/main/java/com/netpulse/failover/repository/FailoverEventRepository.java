package com.netpulse.failover.repository;

import com.netpulse.failover.entity.FailoverEvent;
import com.netpulse.failover.state.FailoverState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface FailoverEventRepository extends JpaRepository<FailoverEvent, Long> {

    Optional<FailoverEvent> findByEventId(String eventId);

    List<FailoverEvent> findByRecoveryCycleId(String recoveryCycleId);

    List<FailoverEvent> findByFailureNodeId(String failureNodeId);

    List<FailoverEvent> findAllByOrderByStartedAtDesc(Pageable pageable);

    List<FailoverEvent> findByCurrentStateIn(List<FailoverState> states);

    long countBySuccessTrue();

    long countBySuccessFalse();

    long countByCurrentStateIn(List<FailoverState> states);

    /*
     * Hibernate 6.6 does not support:
     *   EXTRACT(EPOCH FROM (completedAt - startedAt))
     * because timestamp subtraction maps to Duration, not a number.
     *
     * Average duration is therefore computed in Java by fetching completed
     * successful events and calculating the average in the service layer.
     */
    @Query("""
        SELECT e
        FROM FailoverEvent e
        WHERE e.completedAt IS NOT NULL
          AND e.success = true
    """)
    List<FailoverEvent> findCompletedSuccessfulEvents();

    List<FailoverEvent> findAllByStartedAtBefore(Instant cutoff);
}
