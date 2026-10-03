package com.netpulse.predictive.controller;

import com.netpulse.common.ApiResponse;
import com.netpulse.predictive.dto.*;
import com.netpulse.predictive.entity.ModelMetadataEntity;
import com.netpulse.predictive.entity.PredictionRecordEntity;
import com.netpulse.predictive.service.ModelTrainingService;
import com.netpulse.predictive.service.PredictionService;
import com.netpulse.predictive.service.PredictiveRoutingIntegrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionService predictionService;
    private final ModelTrainingService trainingService;
    private final PredictiveRoutingIntegrationService routingIntegrationService;

    public PredictionController(PredictionService predictionService,
                                ModelTrainingService trainingService,
                                PredictiveRoutingIntegrationService routingIntegrationService) {
        this.predictionService = predictionService;
        this.trainingService = trainingService;
        this.routingIntegrationService = routingIntegrationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<PredictionResponse>>> getAllPredictions() {
        PredictionSummaryResponse summary = predictionService.getSummary();
        return ResponseEntity.ok(ApiResponse.success("Predictions retrieved successfully", summary.getLatestNodePredictions()));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<PredictionSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success("Prediction summary retrieved successfully", predictionService.getSummary()));
    }

    @GetMapping("/nodes/{nodeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<PredictionResponse>> getPredictionForNode(@PathVariable String nodeId) {
        return ResponseEntity.ok(ApiResponse.success("Node prediction retrieved successfully", predictionService.predictForNode(nodeId)));
    }

    @GetMapping("/nodes/{nodeId}/forecast")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<PredictionForecastDto>> getForecastForNode(@PathVariable String nodeId) {
        return ResponseEntity.ok(ApiResponse.success("Node forecast retrieved successfully", predictionService.getForecastForNode(nodeId)));
    }

    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<List<PredictionResponse>>> runPredictions() {
        List<PredictionResponse> results = predictionService.runAllPredictions();
        return ResponseEntity.ok(ApiResponse.success("Predictions executed successfully across all nodes", results));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<ApiResponse<PredictionOutcomeVerificationDto>> verifyOutcomes() {
        PredictionOutcomeVerificationDto result = predictionService.verifyHistoricalPredictions();
        return ResponseEntity.ok(ApiResponse.success("Historical predictions outcome evaluation completed", result));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<PredictionRecordEntity>>> getHistory() {
        return ResponseEntity.ok(ApiResponse.success("Prediction history retrieved", predictionService.getRecentHistory()));
    }

    @GetMapping("/history/{nodeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<PredictionRecordEntity>>> getNodeHistory(@PathVariable String nodeId) {
        return ResponseEntity.ok(ApiResponse.success("Node prediction history retrieved", predictionService.getNodeHistory(nodeId)));
    }

    @PostMapping("/train")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ModelTrainingResponse>> trainModel(@RequestBody ModelTrainingRequest request,
                                                                         Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "ADMIN";
        ModelTrainingResponse res = trainingService.trainModel(request, username);
        return ResponseEntity.ok(ApiResponse.success(res.getMessage(), res));
    }

    @GetMapping("/models")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<ModelMetadataEntity>>> getModels() {
        return ResponseEntity.ok(ApiResponse.success("Models retrieved", trainingService.getAllModels()));
    }

    @GetMapping("/models/{modelVersion}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<ModelMetadataEntity>> getModelByVersion(@PathVariable String modelVersion) {
        return trainingService.getModelByVersion(modelVersion)
                .map(m -> ResponseEntity.ok(ApiResponse.success("Model metadata retrieved", m)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/models/active/features")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<FeatureImportanceDto>>> getActiveFeatureImportances() {
        if (trainingService.getActiveModel() != null) {
            return ResponseEntity.ok(ApiResponse.success("Feature importances retrieved", trainingService.getActiveModel().getFeatureImportances()));
        }
        return ResponseEntity.ok(ApiResponse.success("No active model loaded", List.of()));
    }

    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<PredictiveConfigDto>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success("Predictive configuration retrieved", routingIntegrationService.getConfig()));
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PredictiveConfigDto>> updateConfig(@RequestBody PredictiveConfigDto config) {
        routingIntegrationService.updateConfig(config);
        return ResponseEntity.ok(ApiResponse.success("Predictive configuration updated", routingIntegrationService.getConfig()));
    }
}
