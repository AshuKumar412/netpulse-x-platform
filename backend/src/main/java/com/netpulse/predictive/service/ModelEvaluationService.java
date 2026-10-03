package com.netpulse.predictive.service;

import com.netpulse.predictive.dto.ConfusionMatrixDto;
import com.netpulse.predictive.dto.ModelEvaluationDto;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ModelEvaluationService {

    private static final String[] CLASS_NAMES = new String[] { "NORMAL", "WARNING", "CONGESTION_LIKELY", "CONGESTED" };
    private static final int NUM_CLASSES = 4;

    /**
     * Evaluates predictions against actual held-out test ground truth labels.
     * Computes genuine, mathematically derived Accuracy, Precision, Recall, F1, and Confusion Matrix.
     */
    public ModelEvaluationDto evaluate(int[] trueLabels, int[] predictedLabels) {
        ModelEvaluationDto dto = new ModelEvaluationDto();
        if (trueLabels == null || predictedLabels == null || trueLabels.length == 0 || trueLabels.length != predictedLabels.length) {
            dto.setAccuracy(0.0);
            dto.setPrecisionScore(0.0);
            dto.setRecallScore(0.0);
            dto.setF1Score(0.0);
            dto.setEvaluationSamples(0);
            return dto;
        }

        int N = trueLabels.length;
        dto.setEvaluationSamples(N);

        int[][] matrix = new int[NUM_CLASSES][NUM_CLASSES]; // matrix[true][pred]
        int correctCount = 0;

        for (int i = 0; i < N; i++) {
            int t = Math.max(0, Math.min(NUM_CLASSES - 1, trueLabels[i]));
            int p = Math.max(0, Math.min(NUM_CLASSES - 1, predictedLabels[i]));
            matrix[t][p]++;
            if (t == p) {
                correctCount++;
            }
        }

        double accuracy = (double) correctCount / N;
        dto.setAccuracy(Math.round(accuracy * 1000.0) / 1000.0);
        dto.setConfusionMatrix(new ConfusionMatrixDto(CLASS_NAMES, matrix));

        Map<String, Double> perClassPrecision = new LinkedHashMap<>();
        Map<String, Double> perClassRecall = new LinkedHashMap<>();
        Map<String, Double> perClassF1 = new LinkedHashMap<>();

        double sumPrecision = 0.0;
        double sumRecall = 0.0;
        int activeClassCount = 0;

        for (int k = 0; k < NUM_CLASSES; k++) {
            int tp = matrix[k][k];
            int totalPredClass = 0;
            int totalTrueClass = 0;

            for (int j = 0; j < NUM_CLASSES; j++) {
                totalPredClass += matrix[j][k]; // Column sum
                totalTrueClass += matrix[k][j]; // Row sum
            }

            double precision = totalPredClass > 0 ? (double) tp / totalPredClass : (totalTrueClass == 0 ? 1.0 : 0.0);
            double recall = totalTrueClass > 0 ? (double) tp / totalTrueClass : (totalPredClass == 0 ? 1.0 : 0.0);
            double f1 = (precision + recall) > 0 ? (2 * precision * recall) / (precision + recall) : 0.0;

            perClassPrecision.put(CLASS_NAMES[k], Math.round(precision * 1000.0) / 1000.0);
            perClassRecall.put(CLASS_NAMES[k], Math.round(recall * 1000.0) / 1000.0);
            perClassF1.put(CLASS_NAMES[k], Math.round(f1 * 1000.0) / 1000.0);

            if (totalTrueClass > 0) {
                sumPrecision += precision;
                sumRecall += recall;
                activeClassCount++;
            }
        }

        int divisor = Math.max(1, activeClassCount);
        double macroPrecision = sumPrecision / divisor;
        double macroRecall = sumRecall / divisor;
        double macroF1 = (macroPrecision + macroRecall) > 0 ? (2 * macroPrecision * macroRecall) / (macroPrecision + macroRecall) : 0.0;

        dto.setPrecisionScore(Math.round(macroPrecision * 1000.0) / 1000.0);
        dto.setRecallScore(Math.round(macroRecall * 1000.0) / 1000.0);
        dto.setF1Score(Math.round(macroF1 * 1000.0) / 1000.0);
        dto.setPerClassPrecision(perClassPrecision);
        dto.setPerClassRecall(perClassRecall);
        dto.setPerClassF1(perClassF1);

        return dto;
    }
}
