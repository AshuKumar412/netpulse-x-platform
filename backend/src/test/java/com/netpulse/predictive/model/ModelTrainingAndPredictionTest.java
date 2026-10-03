package com.netpulse.predictive.model;

import com.netpulse.predictive.dto.FeatureImportanceDto;
import com.netpulse.predictive.dto.ModelEvaluationDto;
import com.netpulse.predictive.service.ModelEvaluationService;
import com.netpulse.predictive.state.PredictedCongestionState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelTrainingAndPredictionTest {

    private final ModelEvaluationService evaluationService = new ModelEvaluationService();

    @Test
    @DisplayName("Logistic Regression should train deterministically, predict correctly, and export feature importances")
    void testLogisticRegressionTrainingAndPrediction() {
        MultinomialLogisticRegressionModel model = new MultinomialLogisticRegressionModel();

        // 16-dimensional feature vectors: Normal vs Congested
        double[][] X = new double[][] {
                { 20.0, 30.0, 15.0, 0.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 30.0, 15.0, 0.0, 10.0 },
                { 25.0, 35.0, 18.0, 0.0, 12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 25.0, 35.0, 18.0, 0.0, 12.0 },
                { 92.0, 88.0, 250.0, 8.0, 300.0, 2.0, 5.0, 3.0, 20.0, 1.0, 10.0, 90.0, 85.0, 240.0, 7.0, 290.0 },
                { 95.0, 92.0, 300.0, 10.0, 350.0, 3.0, 4.0, 2.0, 25.0, 2.0, 12.0, 94.0, 90.0, 290.0, 9.0, 340.0 },
        };
        int[] y = new int[] { 0, 0, 3, 3 }; // 0: NORMAL, 3: CONGESTED

        Map<String, Object> hyperparams = new HashMap<>();
        hyperparams.put("learningRate", 0.1);
        hyperparams.put("maxEpochs", 150);
        hyperparams.put("l2Regularization", 0.001);

        model.train(X, y, null, hyperparams);

        // Predict on normal sample
        double[] normalSample = new double[] { 22.0, 32.0, 16.0, 0.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 22.0, 32.0, 16.0, 0.0, 11.0 };
        PredictedCongestionState normalPred = model.predict(normalSample);
        assertEquals(PredictedCongestionState.NORMAL, normalPred);

        // Predict on congested sample
        double[] congestedSample = new double[] { 94.0, 90.0, 280.0, 9.0, 320.0, 2.5, 4.5, 2.5, 22.0, 1.5, 11.0, 92.0, 88.0, 260.0, 8.0, 310.0 };
        PredictedCongestionState congestedPred = model.predict(congestedSample);
        assertEquals(PredictedCongestionState.CONGESTED, congestedPred);

        // Feature importance validation
        List<FeatureImportanceDto> importances = model.getFeatureImportances();
        assertNotNull(importances);
        assertEquals(16, importances.size());
        assertTrue(importances.get(0).getImportanceScore() > 0.0);

        // Serialization test
        String json = model.serializeToJson();
        assertNotNull(json);
        assertTrue(json.contains("weights"));

        MultinomialLogisticRegressionModel reloaded = new MultinomialLogisticRegressionModel();
        reloaded.deserializeFromJson(json);
        assertEquals(PredictedCongestionState.CONGESTED, reloaded.predict(congestedSample));
    }

    @Test
    @DisplayName("Decision Tree classifier should build tree, predict, and serialize")
    void testDecisionTreeModel() {
        DecisionTreeModel dt = new DecisionTreeModel();

        double[][] X = new double[][] {
                { 20.0, 30.0, 15.0, 0.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20.0, 30.0, 15.0, 0.0, 10.0 },
                { 95.0, 90.0, 250.0, 8.0, 300.0, 2.0, 5.0, 3.0, 20.0, 1.0, 10.0, 90.0, 85.0, 240.0, 7.0, 290.0 },
        };
        int[] y = new int[] { 0, 3 };

        Map<String, Object> hyperparams = new HashMap<>();
        hyperparams.put("maxDepth", 4);

        dt.train(X, y, null, hyperparams);

        PredictedCongestionState pred = dt.predict(X[0]);
        assertEquals(PredictedCongestionState.NORMAL, pred);

        String json = dt.serializeToJson();
        DecisionTreeModel reloaded = new DecisionTreeModel();
        reloaded.deserializeFromJson(json);
        assertEquals(PredictedCongestionState.NORMAL, reloaded.predict(X[0]));
    }

    @Test
    @DisplayName("Model Evaluation Service should calculate genuine metrics and confusion matrix")
    void testEvaluationMetricsCalculation() {
        int[] trueLabels = new int[] { 0, 0, 1, 2, 3 };
        int[] predLabels = new int[] { 0, 0, 1, 2, 2 }; // 1 error (index 4 predicted as 2 instead of 3)

        ModelEvaluationDto dto = evaluationService.evaluate(trueLabels, predLabels);

        assertNotNull(dto);
        assertEquals(0.80, dto.getAccuracy(), 0.01); // 4 / 5 = 0.80
        assertNotNull(dto.getConfusionMatrix());
        assertEquals(4, dto.getConfusionMatrix().getLabels().length);
    }
}
