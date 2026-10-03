package com.netpulse.predictive.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netpulse.predictive.dto.FeatureImportanceDto;
import com.netpulse.predictive.state.PredictedCongestionState;

import java.util.*;

/**
 * Deterministic Multinomial Logistic Regression model with Softmax and L2 Regularized Gradient Descent.
 * Computes exact calibrated class probabilities and analytical feature importances.
 */
public class MultinomialLogisticRegressionModel implements PredictiveModel {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int NUM_CLASSES = 4;

    private int numFeatures = 16;
    private String[] featureNames = new String[0];

    // Model weights: weights[k][j] for class k and feature j
    private double[][] weights;
    // Biases: bias[k] for class k
    private double[] bias;

    // Feature normalization (StandardScaler)
    private double[] featureMeans;
    private double[] featureStdDevs;

    public MultinomialLogisticRegressionModel() {
    }

    @Override
    public void train(double[][] X, int[] y, String[] featureNames, Map<String, Object> hyperparams) {
        if (X == null || X.length == 0 || y == null || y.length == 0) {
            throw new IllegalArgumentException("Training dataset cannot be empty");
        }

        int N = X.length;
        this.numFeatures = X[0].length;
        this.featureNames = (featureNames != null && featureNames.length == numFeatures) ?
                featureNames : generateDefaultFeatureNames(numFeatures);

        double learningRate = hyperparams.containsKey("learningRate") ? ((Number) hyperparams.get("learningRate")).doubleValue() : 0.05;
        int maxEpochs = hyperparams.containsKey("maxEpochs") ? ((Number) hyperparams.get("maxEpochs")).intValue() : 250;
        double l2Reg = hyperparams.containsKey("l2Regularization") ? ((Number) hyperparams.get("l2Regularization")).doubleValue() : 0.001;

        // 1. Compute Mean and StdDev for feature scaling
        featureMeans = new double[numFeatures];
        featureStdDevs = new double[numFeatures];

        for (int j = 0; j < numFeatures; j++) {
            double sum = 0.0;
            for (int i = 0; i < N; i++) {
                sum += X[i][j];
            }
            featureMeans[j] = sum / N;

            double sumSq = 0.0;
            for (int i = 0; i < N; i++) {
                double diff = X[i][j] - featureMeans[j];
                sumSq += diff * diff;
            }
            double variance = sumSq / Math.max(1, N - 1);
            featureStdDevs[j] = Math.sqrt(variance);
            if (featureStdDevs[j] < 1e-6) {
                featureStdDevs[j] = 1.0; // Prevent division by zero
            }
        }

        // 2. Standardize training matrix
        double[][] X_scaled = new double[N][numFeatures];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < numFeatures; j++) {
                X_scaled[i][j] = (X[i][j] - featureMeans[j]) / featureStdDevs[j];
            }
        }

        // 3. Initialize weights deterministically to 0.0
        weights = new double[NUM_CLASSES][numFeatures];
        bias = new double[NUM_CLASSES];

        // 4. Batch Gradient Descent with L2 Regularization
        for (int epoch = 0; epoch < maxEpochs; epoch++) {
            double[][] gradW = new double[NUM_CLASSES][numFeatures];
            double[] gradB = new double[NUM_CLASSES];

            for (int i = 0; i < N; i++) {
                double[] probs = computeSoftmax(X_scaled[i]);
                int trueClass = Math.max(0, Math.min(NUM_CLASSES - 1, y[i]));

                for (int k = 0; k < NUM_CLASSES; k++) {
                    double error = probs[k] - (k == trueClass ? 1.0 : 0.0);
                    for (int j = 0; j < numFeatures; j++) {
                        gradW[k][j] += error * X_scaled[i][j];
                    }
                    gradB[k] += error;
                }
            }

            // Apply gradients with L2 penalty
            for (int k = 0; k < NUM_CLASSES; k++) {
                for (int j = 0; j < numFeatures; j++) {
                    double grad = (gradW[k][j] / N) + (l2Reg * weights[k][j]);
                    weights[k][j] -= learningRate * grad;
                }
                bias[k] -= learningRate * (gradB[k] / N);
            }
        }
    }

    private double[] computeSoftmax(double[] xScaled) {
        double[] logits = new double[NUM_CLASSES];
        double maxLogit = Double.NEGATIVE_INFINITY;

        for (int k = 0; k < NUM_CLASSES; k++) {
            double z = bias[k];
            for (int j = 0; j < numFeatures; j++) {
                z += weights[k][j] * xScaled[j];
            }
            logits[k] = z;
            if (z > maxLogit) {
                maxLogit = z;
            }
        }

        double sumExp = 0.0;
        double[] exp = new double[NUM_CLASSES];
        for (int k = 0; k < NUM_CLASSES; k++) {
            exp[k] = Math.exp(logits[k] - maxLogit);
            sumExp += exp[k];
        }

        double[] probs = new double[NUM_CLASSES];
        for (int k = 0; k < NUM_CLASSES; k++) {
            probs[k] = exp[k] / Math.max(1e-12, sumExp);
        }
        return probs;
    }

    private double[] scaleFeatures(double[] raw) {
        double[] scaled = new double[numFeatures];
        for (int j = 0; j < numFeatures; j++) {
            if (featureStdDevs != null && featureStdDevs[j] > 1e-6) {
                scaled[j] = (raw[j] - featureMeans[j]) / featureStdDevs[j];
            } else {
                scaled[j] = raw[j];
            }
        }
        return scaled;
    }

    @Override
    public PredictedCongestionState predict(double[] features) {
        double[] probs = predictProbabilities(features);
        int bestClass = 0;
        double maxProb = -1.0;
        for (int k = 0; k < NUM_CLASSES; k++) {
            if (probs[k] > maxProb) {
                maxProb = probs[k];
                bestClass = k;
            }
        }

        return mapIndexToState(bestClass);
    }

    @Override
    public double[] predictProbabilities(double[] features) {
        if (weights == null || bias == null) {
            return new double[] { 0.25, 0.25, 0.25, 0.25 };
        }
        double[] scaled = scaleFeatures(features);
        return computeSoftmax(scaled);
    }

    @Override
    public List<FeatureImportanceDto> getFeatureImportances() {
        List<FeatureImportanceDto> list = new ArrayList<>();
        if (weights == null || featureNames == null) return list;

        double[] totalMagnitude = new double[numFeatures];
        double sumTotal = 0.0;

        for (int j = 0; j < numFeatures; j++) {
            double mag = 0.0;
            for (int k = 0; k < NUM_CLASSES; k++) {
                mag += Math.abs(weights[k][j]);
            }
            totalMagnitude[j] = mag;
            sumTotal += mag;
        }

        for (int j = 0; j < numFeatures; j++) {
            double score = totalMagnitude[j];
            double pct = sumTotal > 1e-9 ? (score / sumTotal) * 100.0 : 0.0;
            String name = (j < featureNames.length) ? featureNames[j] : "FEATURE_" + j;
            list.add(new FeatureImportanceDto(name, Math.round(score * 1000.0) / 1000.0, Math.round(pct * 10.0) / 10.0));
        }

        list.sort((a, b) -> Double.compare(b.getImportanceScore(), a.getImportanceScore()));
        return list;
    }

    @Override
    public Map<String, Double> explainPrediction(double[] features) {
        Map<String, Double> contributions = new LinkedHashMap<>();
        if (weights == null) return contributions;

        double[] scaled = scaleFeatures(features);
        double[] probs = computeSoftmax(scaled);
        int predictedClass = 0;
        double maxP = -1.0;
        for (int k = 0; k < NUM_CLASSES; k++) {
            if (probs[k] > maxP) {
                maxP = probs[k];
                predictedClass = k;
            }
        }

        double totalImpact = 0.0;
        double[] impacts = new double[numFeatures];
        for (int j = 0; j < numFeatures; j++) {
            impacts[j] = weights[predictedClass][j] * scaled[j];
            totalImpact += Math.abs(impacts[j]);
        }

        for (int j = 0; j < numFeatures; j++) {
            String name = (j < featureNames.length) ? featureNames[j] : "FEATURE_" + j;
            double relWeight = totalImpact > 1e-9 ? (impacts[j] / totalImpact) * 100.0 : 0.0;
            contributions.put(name, Math.round(relWeight * 10.0) / 10.0);
        }
        return contributions;
    }

    @Override
    public String serializeToJson() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("numFeatures", numFeatures);
            data.put("featureNames", featureNames);
            data.put("weights", weights);
            data.put("bias", bias);
            data.put("featureMeans", featureMeans);
            data.put("featureStdDevs", featureStdDevs);
            return MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Logistic Regression model", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void deserializeFromJson(String json) {
        try {
            Map<String, Object> data = MAPPER.readValue(json, Map.class);
            this.numFeatures = ((Number) data.get("numFeatures")).intValue();

            List<String> names = (List<String>) data.get("featureNames");
            this.featureNames = names.toArray(new String[0]);

            List<List<Number>> wList = (List<List<Number>>) data.get("weights");
            this.weights = new double[NUM_CLASSES][numFeatures];
            for (int k = 0; k < NUM_CLASSES; k++) {
                for (int j = 0; j < numFeatures; j++) {
                    this.weights[k][j] = wList.get(k).get(j).doubleValue();
                }
            }

            List<Number> bList = (List<Number>) data.get("bias");
            this.bias = new double[NUM_CLASSES];
            for (int k = 0; k < NUM_CLASSES; k++) {
                this.bias[k] = bList.get(k).doubleValue();
            }

            List<Number> means = (List<Number>) data.get("featureMeans");
            this.featureMeans = new double[numFeatures];
            for (int j = 0; j < numFeatures; j++) {
                this.featureMeans[j] = means.get(j).doubleValue();
            }

            List<Number> stds = (List<Number>) data.get("featureStdDevs");
            this.featureStdDevs = new double[numFeatures];
            for (int j = 0; j < numFeatures; j++) {
                this.featureStdDevs[j] = stds.get(j).doubleValue();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize Logistic Regression model", e);
        }
    }

    private PredictedCongestionState mapIndexToState(int index) {
        return switch (index) {
            case 0 -> PredictedCongestionState.NORMAL;
            case 1 -> PredictedCongestionState.WARNING;
            case 2 -> PredictedCongestionState.CONGESTION_LIKELY;
            case 3 -> PredictedCongestionState.CONGESTED;
            default -> PredictedCongestionState.NORMAL;
        };
    }

    private String[] generateDefaultFeatureNames(int count) {
        String[] arr = new String[count];
        for (int i = 0; i < count; i++) {
            arr[i] = "FEATURE_" + i;
        }
        return arr;
    }
}
