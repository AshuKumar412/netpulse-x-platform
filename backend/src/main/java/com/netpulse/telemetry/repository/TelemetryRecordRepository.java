package com.netpulse.telemetry.repository;

import com.netpulse.telemetry.entity.TelemetryRecord;
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
public interface TelemetryRecordRepository extends JpaRepository<TelemetryRecord, Long> {

    List<TelemetryRecord> findByNodeIdOrderByTimestampDesc(String nodeId, Pageable pageable);

    List<TelemetryRecord> findByNodeIdOrderByTimestampAsc(String nodeId);

    List<TelemetryRecord> findByNodeIdAndTimestampBetweenOrderByTimestampAsc(String nodeId, Instant start, Instant end);

    List<TelemetryRecord> findByTimestampBetweenOrderByTimestampAsc(Instant start, Instant end);

    List<TelemetryRecord> findAllByOrderByTimestampAsc();

    long countByTimestampBetween(Instant start, Instant end);

    @Query("SELECT t FROM TelemetryRecord t WHERE t.nodeId = :nodeId ORDER BY t.timestamp DESC LIMIT 1")
    Optional<TelemetryRecord> findLatestByNodeId(@Param("nodeId") String nodeId);

    @Modifying
    @Query("DELETE FROM TelemetryRecord t WHERE t.timestamp < :cutoff")
    int deleteByTimestampBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("DELETE FROM TelemetryRecord t WHERE t.nodeId = :nodeId")
    int deleteByNodeId(@Param("nodeId") String nodeId);
}
