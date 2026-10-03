package com.netpulse.whatif.repository;

import com.netpulse.whatif.entity.SimulationDecisionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimulationDecisionRepository extends JpaRepository<SimulationDecisionEntity, Long> {

    List<SimulationDecisionEntity> findByRunIdOrderBySimulationTickAsc(String runId);

    List<SimulationDecisionEntity> findByRunId(String runId, Pageable pageable);
}
