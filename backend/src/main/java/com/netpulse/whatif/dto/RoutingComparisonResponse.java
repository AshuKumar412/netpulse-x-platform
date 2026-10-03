package com.netpulse.whatif.dto;

import com.netpulse.routing.entity.RoutingStrategyType;
import java.util.List;
import java.util.Map;

public class RoutingComparisonResponse {

    private String baselineStrategy;
    private List<RoutingStrategyComparisonItem> strategyResults;
    private String analysisSummary;

    public RoutingComparisonResponse() {
    }

    public static class RoutingStrategyComparisonItem {
        private RoutingStrategyType strategy;
        private String strategyName;
        private double averageLatency;
        private double averageCpuLoad;
        private double packetLossRate;
        private int totalReroutedRequests;
        private Map<String, Integer> trafficDistribution;
        private String tradeOffDescription;

        public RoutingStrategyComparisonItem() {
        }

        public RoutingStrategyType getStrategy() { return strategy; }
        public void setStrategy(RoutingStrategyType strategy) { this.strategy = strategy; }
        public String getStrategyName() { return strategyName; }
        public void setStrategyName(String strategyName) { this.strategyName = strategyName; }
        public double getAverageLatency() { return averageLatency; }
        public void setAverageLatency(double averageLatency) { this.averageLatency = averageLatency; }
        public double getAverageCpuLoad() { return averageCpuLoad; }
        public void setAverageCpuLoad(double averageCpuLoad) { this.averageCpuLoad = averageCpuLoad; }
        public double getPacketLossRate() { return packetLossRate; }
        public void setPacketLossRate(double packetLossRate) { this.packetLossRate = packetLossRate; }
        public int getTotalReroutedRequests() { return totalReroutedRequests; }
        public void setTotalReroutedRequests(int totalReroutedRequests) { this.totalReroutedRequests = totalReroutedRequests; }
        public Map<String, Integer> getTrafficDistribution() { return trafficDistribution; }
        public void setTrafficDistribution(Map<String, Integer> trafficDistribution) { this.trafficDistribution = trafficDistribution; }
        public String getTradeOffDescription() { return tradeOffDescription; }
        public void setTradeOffDescription(String tradeOffDescription) { this.tradeOffDescription = tradeOffDescription; }
    }

    public String getBaselineStrategy() { return baselineStrategy; }
    public void setBaselineStrategy(String baselineStrategy) { this.baselineStrategy = baselineStrategy; }
    public List<RoutingStrategyComparisonItem> getStrategyResults() { return strategyResults; }
    public void setStrategyResults(List<RoutingStrategyComparisonItem> strategyResults) { this.strategyResults = strategyResults; }
    public String getAnalysisSummary() { return analysisSummary; }
    public void setAnalysisSummary(String analysisSummary) { this.analysisSummary = analysisSummary; }
}
