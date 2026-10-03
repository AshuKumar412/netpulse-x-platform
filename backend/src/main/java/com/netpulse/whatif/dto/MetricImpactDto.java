package com.netpulse.whatif.dto;

public class MetricImpactDto {

    private String metricName;
    private double baselineValue;
    private double simulatedValue;
    private double absoluteChange;
    private Double percentageChange;
    private String unit;
    private String direction; // INCREASED, DECREASED, UNCHANGED

    public MetricImpactDto() {
    }

    public MetricImpactDto(String metricName, double baselineValue, double simulatedValue, String unit) {
        this.metricName = metricName;
        this.baselineValue = Math.round(baselineValue * 100.0) / 100.0;
        this.simulatedValue = Math.round(simulatedValue * 100.0) / 100.0;
        this.absoluteChange = Math.round((simulatedValue - baselineValue) * 100.0) / 100.0;
        this.unit = unit;

        if (baselineValue != 0.0) {
            this.percentageChange = Math.round(((simulatedValue - baselineValue) / baselineValue) * 10000.0) / 100.0;
        } else {
            this.percentageChange = null;
        }

        if (this.absoluteChange > 0.0001) {
            this.direction = "INCREASED";
        } else if (this.absoluteChange < -0.0001) {
            this.direction = "DECREASED";
        } else {
            this.direction = "UNCHANGED";
        }
    }

    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }
    public double getBaselineValue() { return baselineValue; }
    public void setBaselineValue(double baselineValue) { this.baselineValue = baselineValue; }
    public double getSimulatedValue() { return simulatedValue; }
    public void setSimulatedValue(double simulatedValue) { this.simulatedValue = simulatedValue; }
    public double getAbsoluteChange() { return absoluteChange; }
    public void setAbsoluteChange(double absoluteChange) { this.absoluteChange = absoluteChange; }
    public Double getPercentageChange() { return percentageChange; }
    public void setPercentageChange(Double percentageChange) { this.percentageChange = percentageChange; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
}
