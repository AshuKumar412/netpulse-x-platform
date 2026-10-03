package com.netpulse.predictive.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netpulse.predictive.dto.FeatureImportanceDto;
import com.netpulse.predictive.state.PredictedCongestionState;

import java.util.*;

/**
 * Deterministic CART Decision Tree Classifier with Gini Impurity splits and MDI feature importance.
 */
public class DecisionTreeModel implements PredictiveModel {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int NUM_CLASSES = 4;

    private int maxDepth = 6;
    private int minSamplesSplit = 4;
    private TreeNode root;
    private String[] featureNames = new String[0];
    private double[] featureImportancesMdi;

    public static class TreeNode {
        public boolean isLeaf;
        public int predictedClass;
        public double[] classProbabilities = new double[NUM_CLASSES];
        public int splitFeatureIndex = -1;
        public double splitThreshold = 0.0;
        public TreeNode left;
        public TreeNode right;

        public TreeNode() {}
    }

    public DecisionTreeModel() {
    }

    @Override
    public void train(double[][] X, int[] y, String[] featureNames, Map<String, Object> hyperparams) {
        if (X == null || X.length == 0 || y == null || y.length == 0) {
            throw new IllegalArgumentException("Training dataset cannot be empty");
        }

        int numFeatures = X[0].length;
        this.featureNames = (featureNames != null && featureNames.length == numFeatures) ?
                featureNames : generateDefaultFeatureNames(numFeatures);
        this.maxDepth = hyperparams.containsKey("maxDepth") ? ((Number) hyperparams.get("maxDepth")).intValue() : 6;
        this.minSamplesSplit = hyperparams.containsKey("minSamplesSplit") ? ((Number) hyperparams.get("minSamplesSplit")).intValue() : 4;
        this.featureImportancesMdi = new double[numFeatures];

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < X.length; i++) indices.add(i);

        this.root = buildTree(X, y, indices, 0);

        // Normalize feature importances
        double sumImp = 0.0;
        for (double v : featureImportancesMdi) sumImp += v;
        if (sumImp > 1e-9) {
            for (int j = 0; j < featureImportancesMdi.length; j++) {
                featureImportancesMdi[j] /= sumImp;
            }
        }
    }

    private TreeNode buildTree(double[][] X, int[] y, List<Integer> sampleIndices, int depth) {
        TreeNode node = new TreeNode();
        int[] classCounts = new int[NUM_CLASSES];
        for (int idx : sampleIndices) {
            int c = Math.max(0, Math.min(NUM_CLASSES - 1, y[idx]));
            classCounts[c]++;
        }

        int dominantClass = 0;
        int maxCount = -1;
        for (int k = 0; k < NUM_CLASSES; k++) {
            node.classProbabilities[k] = (double) classCounts[k] / Math.max(1, sampleIndices.size());
            if (classCounts[k] > maxCount) {
                maxCount = classCounts[k];
                dominantClass = k;
            }
        }
        node.predictedClass = dominantClass;

        // Stopping criteria
        if (depth >= maxDepth || sampleIndices.size() < minSamplesSplit || isPure(classCounts)) {
            node.isLeaf = true;
            return node;
        }

        // Find best split across all features
        double currentGini = calculateGini(classCounts, sampleIndices.size());
        double bestGain = 0.0;
        int bestFeature = -1;
        double bestThreshold = 0.0;
        List<Integer> bestLeft = null;
        List<Integer> bestRight = null;

        int numFeatures = X[0].length;
        for (int f = 0; f < numFeatures; f++) {
            // Find candidate split thresholds
            Set<Double> distinctVals = new TreeSet<>();
            for (int idx : sampleIndices) {
                distinctVals.add(X[idx][f]);
            }
            List<Double> sortedVals = new ArrayList<>(distinctVals);
            for (int s = 0; s < sortedVals.size() - 1; s++) {
                double threshold = (sortedVals.get(s) + sortedVals.get(s + 1)) / 2.0;

                List<Integer> left = new ArrayList<>();
                List<Integer> right = new ArrayList<>();
                int[] leftCounts = new int[NUM_CLASSES];
                int[] rightCounts = new int[NUM_CLASSES];

                for (int idx : sampleIndices) {
                    int c = Math.max(0, Math.min(NUM_CLASSES - 1, y[idx]));
                    if (X[idx][f] <= threshold) {
                        left.add(idx);
                        leftCounts[c]++;
                    } else {
                        right.add(idx);
                        rightCounts[c]++;
                    }
                }

                if (left.isEmpty() || right.isEmpty()) continue;

                double leftGini = calculateGini(leftCounts, left.size());
                double rightGini = calculateGini(rightCounts, right.size());
                double splitGini = (left.size() * leftGini + right.size() * rightGini) / sampleIndices.size();
                double gain = currentGini - splitGini;

                if (gain > bestGain) {
                    bestGain = gain;
                    bestFeature = f;
                    bestThreshold = threshold;
                    bestLeft = left;
                    bestRight = right;
                }
            }
        }

        if (bestGain > 1e-6 && bestLeft != null && bestRight != null) {
            node.isLeaf = false;
            node.splitFeatureIndex = bestFeature;
            node.splitThreshold = bestThreshold;
            featureImportancesMdi[bestFeature] += bestGain * sampleIndices.size();
            node.left = buildTree(X, y, bestLeft, depth + 1);
            node.right = buildTree(X, y, bestRight, depth + 1);
        } else {
            node.isLeaf = true;
        }

        return node;
    }

    private double calculateGini(int[] counts, int total) {
        if (total == 0) return 0.0;
        double sumSq = 0.0;
        for (int count : counts) {
            double p = (double) count / total;
            sumSq += p * p;
        }
        return 1.0 - sumSq;
    }

    private boolean isPure(int[] counts) {
        int nonZero = 0;
        for (int c : counts) {
            if (c > 0) nonZero++;
        }
        return nonZero <= 1;
    }

    @Override
    public PredictedCongestionState predict(double[] features) {
        if (root == null) return PredictedCongestionState.NORMAL;
        TreeNode node = findLeaf(root, features);
        return mapIndexToState(node.predictedClass);
    }

    @Override
    public double[] predictProbabilities(double[] features) {
        if (root == null) return new double[] { 0.25, 0.25, 0.25, 0.25 };
        TreeNode node = findLeaf(root, features);
        return node.classProbabilities;
    }

    private TreeNode findLeaf(TreeNode current, double[] features) {
        if (current.isLeaf) return current;
        if (current.splitFeatureIndex < 0 || current.splitFeatureIndex >= features.length) return current;

        if (features[current.splitFeatureIndex] <= current.splitThreshold) {
            return current.left != null ? findLeaf(current.left, features) : current;
        } else {
            return current.right != null ? findLeaf(current.right, features) : current;
        }
    }

    @Override
    public List<FeatureImportanceDto> getFeatureImportances() {
        List<FeatureImportanceDto> list = new ArrayList<>();
        if (featureImportancesMdi == null || featureNames == null) return list;

        for (int j = 0; j < featureImportancesMdi.length; j++) {
            double score = featureImportancesMdi[j];
            double pct = score * 100.0;
            String name = (j < featureNames.length) ? featureNames[j] : "FEATURE_" + j;
            list.add(new FeatureImportanceDto(name, Math.round(score * 1000.0) / 1000.0, Math.round(pct * 10.0) / 10.0));
        }

        list.sort((a, b) -> Double.compare(b.getImportanceScore(), a.getImportanceScore()));
        return list;
    }

    @Override
    public Map<String, Double> explainPrediction(double[] features) {
        Map<String, Double> path = new LinkedHashMap<>();
        if (root == null) return path;

        TreeNode cur = root;
        while (!cur.isLeaf) {
            int f = cur.splitFeatureIndex;
            String fName = (f >= 0 && f < featureNames.length) ? featureNames[f] : "FEATURE_" + f;
            double val = (f >= 0 && f < features.length) ? features[f] : 0.0;
            boolean goLeft = val <= cur.splitThreshold;
            path.put(fName, Math.round(val * 100.0) / 100.0);
            cur = goLeft ? cur.left : cur.right;
            if (cur == null) break;
        }
        return path;
    }

    @Override
    public String serializeToJson() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("maxDepth", maxDepth);
            data.put("featureNames", featureNames);
            data.put("featureImportancesMdi", featureImportancesMdi);
            data.put("root", root);
            return MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Decision Tree model", e);
        }
    }

    @Override
    public void deserializeFromJson(String json) {
        try {
            Map<String, Object> data = MAPPER.readValue(json, Map.class);
            this.maxDepth = ((Number) data.get("maxDepth")).intValue();

            List<String> names = (List<String>) data.get("featureNames");
            this.featureNames = names.toArray(new String[0]);

            List<Number> imps = (List<Number>) data.get("featureImportancesMdi");
            this.featureImportancesMdi = new double[imps.size()];
            for (int i = 0; i < imps.size(); i++) {
                this.featureImportancesMdi[i] = imps.get(i).doubleValue();
            }

            String rootJson = MAPPER.writeValueAsString(data.get("root"));
            this.root = MAPPER.readValue(rootJson, TreeNode.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize Decision Tree model", e);
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
        for (int i = 0; i < count; i++) arr[i] = "FEATURE_" + i;
        return arr;
    }
}
