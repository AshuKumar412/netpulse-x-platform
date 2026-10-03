package com.netpulse.whatif.repository;

import com.netpulse.whatif.entity.SimulationEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimulationEventRepository extends JpaRepository<SimulationEventEntity, Long> {

    List<SimulationEventEntity> findByRunIdOrderBySimulationTickAsc(String runId);

    List<SimulationEventEntity> findByRunId(String runId, Pageable pageable);
}
