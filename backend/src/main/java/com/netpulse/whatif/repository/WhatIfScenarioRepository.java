package com.netpulse.whatif.repository;

import com.netpulse.whatif.entity.WhatIfScenarioEntity;
import com.netpulse.whatif.state.ScenarioStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WhatIfScenarioRepository extends JpaRepository<WhatIfScenarioEntity, Long> {

    Optional<WhatIfScenarioEntity> findByScenarioId(String scenarioId);

    List<WhatIfScenarioEntity> findByStatus(ScenarioStatus status);

    @Query("SELECT s FROM WhatIfScenarioEntity s ORDER BY s.createdAt DESC")
    List<WhatIfScenarioEntity> findRecentScenarios(Pageable pageable);

    @Query("SELECT COUNT(s) FROM WhatIfScenarioEntity s WHERE s.status = :status")
    long countByStatus(@Param("status") ScenarioStatus status);

    void deleteByScenarioId(String scenarioId);
}
