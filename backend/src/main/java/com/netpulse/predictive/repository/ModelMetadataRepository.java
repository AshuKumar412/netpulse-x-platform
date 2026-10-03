package com.netpulse.predictive.repository;

import com.netpulse.predictive.entity.ModelMetadataEntity;
import com.netpulse.predictive.state.ModelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelMetadataRepository extends JpaRepository<ModelMetadataEntity, Long> {

    Optional<ModelMetadataEntity> findByModelVersion(String modelVersion);

    List<ModelMetadataEntity> findByStatusOrderByTrainingCompletedAtDesc(ModelStatus status);

    Optional<ModelMetadataEntity> findTopByStatusOrderByTrainingCompletedAtDesc(ModelStatus status);

    List<ModelMetadataEntity> findAllByOrderByTrainingStartedAtDesc();

    @Query("SELECT COUNT(m) FROM ModelMetadataEntity m WHERE m.status = 'READY'")
    long countReadyModels();
}
