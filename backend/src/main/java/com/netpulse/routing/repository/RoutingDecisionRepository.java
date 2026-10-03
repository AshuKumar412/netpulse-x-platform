package com.netpulse.routing.repository;

import com.netpulse.routing.entity.RoutingDecisionRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoutingDecisionRepository extends JpaRepository<RoutingDecisionRecord, Long> {

    Optional<RoutingDecisionRecord> findByRequestId(String requestId);

    List<RoutingDecisionRecord> findAllByOrderByTimestampDesc(Pageable pageable);

    @Modifying
    @Query("DELETE FROM RoutingDecisionRecord r WHERE r.timestamp < :cutoff")
    int deleteByTimestampBefore(@Param("cutoff") Instant cutoff);
}
