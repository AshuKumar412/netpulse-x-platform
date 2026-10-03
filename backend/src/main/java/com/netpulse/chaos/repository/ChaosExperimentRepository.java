package com.netpulse.chaos.repository;

import com.netpulse.chaos.entity.ChaosExperiment;
import com.netpulse.chaos.state.ChaosExperimentResult;
import com.netpulse.chaos.state.ChaosExperimentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChaosExperimentRepository extends JpaRepository<ChaosExperiment, Long> {

    Optional<ChaosExperiment> findByExperimentId(String experimentId);

    List<ChaosExperiment> findByStatus(ChaosExperimentStatus status);

    List<ChaosExperiment> findByStatusIn(List<ChaosExperimentStatus> statuses);

    List<ChaosExperiment> findAllByOrderByStartedAtDesc(Pageable pageable);

    long countByStatus(ChaosExperimentStatus status);

    long countByStatusIn(List<ChaosExperimentStatus> statuses);

    long countByResult(ChaosExperimentResult result);

    boolean existsByTargetNodeIdAndStatusIn(String targetNodeId, List<ChaosExperimentStatus> statuses);

    boolean existsByTargetLinkIdAndStatusIn(String targetLinkId, List<ChaosExperimentStatus> statuses);

    List<ChaosExperiment> findAllByStartedAtBefore(Instant cutoff);
}
