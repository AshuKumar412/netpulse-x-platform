package com.netpulse.predictive.dto;

public class ConfusionMatrixDto {

    private String[] labels;
    private int[][] matrix;

    public ConfusionMatrixDto() {
    }

    public ConfusionMatrixDto(String[] labels, int[][] matrix) {
        this.labels = labels;
        this.matrix = matrix;
    }

    public String[] getLabels() {
        return labels;
    }

    public void setLabels(String[] labels) {
        this.labels = labels;
    }

    public int[][] getMatrix() {
        return matrix;
    }

    public void setMatrix(int[][] matrix) {
        this.matrix = matrix;
    }
}
