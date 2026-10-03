package com.netpulse.predictive.repository;

import com.netpulse.predictive.entity.PredictionRecordEntity;
import com.netpulse.predictive.state.PredictedCongestionState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionRecordRepository extends JpaRepository<PredictionRecordEntity, Long> {

    Optional<PredictionRecordEntity> findByPredictionId(String predictionId);

    List<PredictionRecordEntity> findByNodeIdOrderByTimestampDesc(String nodeId, Pageable pageable);

    @Query("SELECT p FROM PredictionRecordEntity p WHERE p.nodeId = :nodeId ORDER BY p.timestamp DESC LIMIT 1")
    Optional<PredictionRecordEntity> findLatestByNodeId(@Param("nodeId") String nodeId);

    List<PredictionRecordEntity> findByTimestampBetweenOrderByTimestampDesc(Instant start, Instant end);

    List<PredictionRecordEntity> findTop100ByOrderByTimestampDesc();

    long countByPredictedState(PredictedCongestionState state);

    @Query("SELECT COUNT(p) FROM PredictionRecordEntity p WHERE p.outcomeVerified = true AND p.predictedState = p.actualState")
    long countAccurateVerifiedPredictions();

    @Query("SELECT COUNT(p) FROM PredictionRecordEntity p WHERE p.outcomeVerified = true")
    long countTotalVerifiedPredictions();

    List<PredictionRecordEntity> findByOutcomeVerifiedFalseAndTimestampBefore(Instant cutoff);
}
