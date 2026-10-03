package com.netpulse.predictive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netpulse.predictive.dto.*;
import com.netpulse.predictive.entity.ModelMetadataEntity;
import com.netpulse.predictive.model.DecisionTreeModel;
import com.netpulse.predictive.model.MultinomialLogisticRegressionModel;
import com.netpulse.predictive.model.PredictiveModel;
import com.netpulse.predictive.repository.ModelMetadataRepository;
import com.netpulse.predictive.state.DataQualityStatus;
import com.netpulse.predictive.state.ModelStatus;
import com.netpulse.predictive.state.ModelType;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ModelTrainingService {

    private static final Logger log = LoggerFactory.getLogger(ModelTrainingService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DatasetService datasetService;
    private final ModelEvaluationService evaluationService;
    private final ModelMetadataRepository metadataRepository;

    private PredictiveModel activeModel;
    private ModelMetadataEntity activeMetadata;

    public ModelTrainingService(DatasetService datasetService,
                                ModelEvaluationService evaluationService,
                                ModelMetadataRepository metadataRepository) {
        this.datasetService = datasetService;
        this.evaluationService = evaluationService;
        this.metadataRepository = metadataRepository;
    }

    @PostConstruct
    public void init() {
        // Load latest READY model on startup
        metadataRepository.findTopByStatusOrderByTrainingCompletedAtDesc(ModelStatus.READY)
                .ifPresent(this::loadModelFromMetadata);
    }

    /**
     * Trains a new predictive model using historical telemetry data.
     */
    @Transactional
    public ModelTrainingResponse trainModel(ModelTrainingRequest request, String initiatedBy) {
        Instant startTime = Instant.now();
        String modelVersion = "MOD-" + request.getModelType().name().substring(0, 3) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ModelMetadataEntity entity = new ModelMetadataEntity(
                modelVersion,
                request.getModelType(),
                ModelStatus.TRAINING,
                startTime,
                initiatedBy != null ? initiatedBy : "SYSTEM"
        );
        entity.setNotes(request.getNotes());
        entity.setFeatureSchemaVersion("v1");
        entity.setLabelPolicyVersion(request.getLabelPolicyVersion() != null ? request.getLabelPolicyVersion() : "v1");
        entity = metadataRepository.save(entity);

        // 1. Extract historical dataset
        DatasetExtractionResult dataset = datasetService.extractDataset(request.getStartTime(), request.getEndTime());
        if (dataset.getQualityStatus() == DataQualityStatus.INSUFFICIENT) {
            entity.setStatus(ModelStatus.FAILED);
            entity.setNotes("Training aborted: " + dataset.getQualityReason());
            metadataRepository.save(entity);

            ModelTrainingResponse res = new ModelTrainingResponse();
            res.setModelVersion(modelVersion);
            res.setModelType(request.getModelType());
            res.setStatus(ModelStatus.FAILED);
            res.setTrainingStartedAt(startTime);
            res.setMessage("INSUFFICIENT_DATA: " + dataset.getQualityReason());
            return res;
        }

        List<PredictiveFeatureVector> vectors = dataset.getFeatures();
        List<Integer> labels = dataset.getLabels();
        int totalSamples = vectors.size();
        int numFeatures = 16;
        String[] featureNames = PredictiveFeatureVector.getFeatureNames();

        // 2. Deterministic Train/Test Split
        double splitRatio = Math.max(0.10, Math.min(0.50, request.getTestSplitRatio()));
        int testCount = Math.max(1, (int) Math.round(totalSamples * splitRatio));
        int trainCount = totalSamples - testCount;

        // Shuffle with configured fixed random seed for deterministic reproducibility
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < totalSamples; i++) indices.add(i);
        Collections.shuffle(indices, new Random(request.getRandomSeed()));

        double[][] X_train = new double[trainCount][numFeatures];
        int[] y_train = new int[trainCount];
        double[][] X_test = new double[testCount][numFeatures];
        int[] y_test = new int[testCount];

        for (int i = 0; i < trainCount; i++) {
            int idx = indices.get(i);
            X_train[i] = vectors.get(idx).toFeatureArray();
            y_train[i] = labels.get(idx);
        }
        for (int i = 0; i < testCount; i++) {
            int idx = indices.get(trainCount + i);
            X_test[i] = vectors.get(idx).toFeatureArray();
            y_test[i] = labels.get(idx);
        }

        // 3. Train Model
        PredictiveModel model;
        Map<String, Object> hyperparams = new HashMap<>();
        hyperparams.put("learningRate", request.getLearningRate());
        hyperparams.put("maxEpochs", request.getMaxEpochs());
        hyperparams.put("l2Regularization", request.getL2Regularization());
        hyperparams.put("maxDepth", request.getMaxDepth());

        if (request.getModelType() == ModelType.DECISION_TREE_CLASSIFIER) {
            model = new DecisionTreeModel();
        } else {
            model = new MultinomialLogisticRegressionModel();
        }

        model.train(X_train, y_train, featureNames, hyperparams);

        // 4. Evaluate against held-out test split
        int[] predicted_test = new int[testCount];
        for (int i = 0; i < testCount; i++) {
            predicted_test[i] = mapStateToIndex(model.predict(X_test[i]));
        }

        ModelEvaluationDto evaluation = evaluationService.evaluate(y_test, predicted_test);
        List<FeatureImportanceDto> importances = model.getFeatureImportances();

        // 5. Serialize and persist metadata
        Instant completedTime = Instant.now();
        entity.setStatus(ModelStatus.READY);
        entity.setTrainingCompletedAt(completedTime);
        entity.setTrainingDataStart(dataset.getRangeStart());
        entity.setTrainingDataEnd(dataset.getRangeEnd());
        entity.setNumberOfSamples(totalSamples);
        entity.setFeatureCount(numFeatures);
        entity.setAccuracy(evaluation.getAccuracy());
        entity.setPrecisionScore(evaluation.getPrecisionScore());
        entity.setRecallScore(evaluation.getRecallScore());
        entity.setF1Score(evaluation.getF1Score());

        try {
            entity.setConfusionMatrixJson(MAPPER.writeValueAsString(evaluation.getConfusionMatrix()));
            entity.setWeightsJson(model.serializeToJson());
            entity.setHyperparamsJson(MAPPER.writeValueAsString(hyperparams));
        } catch (Exception e) {
            log.warn("Failed to serialize model components to JSON", e);
        }

        // Retire previously ready models of the same type or replace active
        List<ModelMetadataEntity> readyModels = metadataRepository.findByStatusOrderByTrainingCompletedAtDesc(ModelStatus.READY);
        for (ModelMetadataEntity prev : readyModels) {
            if (!prev.getModelVersion().equals(modelVersion)) {
                prev.setStatus(ModelStatus.RETIRED);
                metadataRepository.save(prev);
            }
        }

        entity = metadataRepository.save(entity);

        // Set as active model in memory
        this.activeModel = model;
        this.activeMetadata = entity;

        log.info("Successfully trained and activated model {} ({}) with accuracy {}",
                modelVersion, request.getModelType(), evaluation.getAccuracy());

        ModelTrainingResponse response = new ModelTrainingResponse();
        response.setModelVersion(modelVersion);
        response.setModelType(request.getModelType());
        response.setStatus(ModelStatus.READY);
        response.setTrainingStartedAt(startTime);
        response.setTrainingCompletedAt(completedTime);
        response.setNumberOfSamples(totalSamples);
        response.setFeatureCount(numFeatures);
        response.setEvaluation(evaluation);
        response.setFeatureImportances(importances);
        response.setMessage("Model training and validation completed successfully.");
        return response;
    }

    /**
     * Loads a serialized model into memory.
     */
    public boolean loadModelFromMetadata(ModelMetadataEntity metadata) {
        try {
            if (metadata == null || metadata.getWeightsJson() == null) return false;

            PredictiveModel model;
            if (metadata.getModelType() == ModelType.DECISION_TREE_CLASSIFIER) {
                model = new DecisionTreeModel();
            } else {
                model = new MultinomialLogisticRegressionModel();
            }

            model.deserializeFromJson(metadata.getWeightsJson());
            this.activeModel = model;
            this.activeMetadata = metadata;
            log.info("Loaded active predictive model {}", metadata.getModelVersion());
            return true;
        } catch (Exception e) {
            log.error("Failed to load model from metadata {}", metadata.getModelVersion(), e);
            return false;
        }
    }

    public PredictiveModel getActiveModel() {
        return activeModel;
    }

    public ModelMetadataEntity getActiveMetadata() {
        return activeMetadata;
    }

    public List<ModelMetadataEntity> getAllModels() {
        return metadataRepository.findAllByOrderByTrainingStartedAtDesc();
    }

    public Optional<ModelMetadataEntity> getModelByVersion(String version) {
        return metadataRepository.findByModelVersion(version);
    }

    private int mapStateToIndex(com.netpulse.predictive.state.PredictedCongestionState s) {
        return switch (s) {
            case NORMAL -> 0;
            case WARNING -> 1;
            case CONGESTION_LIKELY -> 2;
            case CONGESTED -> 3;
            default -> 0;
        };
    }
}
