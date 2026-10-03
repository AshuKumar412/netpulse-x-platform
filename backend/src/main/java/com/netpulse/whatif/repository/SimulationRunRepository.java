package com.netpulse.whatif.repository;

import com.netpulse.whatif.entity.SimulationRunEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SimulationRunRepository extends JpaRepository<SimulationRunEntity, Long> {

    Optional<SimulationRunEntity> findByRunId(String runId);

    List<SimulationRunEntity> findByScenarioIdOrderByStartedAtDesc(String scenarioId);

    Optional<SimulationRunEntity> findFirstByScenarioIdOrderByStartedAtDesc(String scenarioId);

    @Query("SELECT r FROM SimulationRunEntity r ORDER BY r.startedAt DESC")
    List<SimulationRunEntity> findRecentRuns(Pageable pageable);
}
