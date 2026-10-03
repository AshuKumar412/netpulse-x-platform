package com.netpulse.scheduler.repository;

import com.netpulse.scheduler.entity.ContextSwitchEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContextSwitchEventRepository extends JpaRepository<ContextSwitchEvent, Long> {

    List<ContextSwitchEvent> findByNodeIdOrderByTimestampDesc(String nodeId, Pageable pageable);

    @Query("SELECT c FROM ContextSwitchEvent c ORDER BY c.timestamp DESC")
    List<ContextSwitchEvent> findRecentEvents(Pageable pageable);

    @Query("SELECT COUNT(c) FROM ContextSwitchEvent c WHERE c.nodeId = :nodeId")
    long countByNodeId(@Param("nodeId") String nodeId);
}
