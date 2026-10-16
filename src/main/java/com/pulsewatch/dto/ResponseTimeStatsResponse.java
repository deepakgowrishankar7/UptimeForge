package com.pulsewatch.dto;

import java.util.List;

public class ResponseTimeStatsResponse {
    private long avgMs;
    private long medianMs;
    private long p95Ms;
    private long minMs;
    private long maxMs;
    private List<LatencyPoint> trend;

    public ResponseTimeStatsResponse() {}

    public static class LatencyPoint {
        private String timestamp;
        private long latencyMs;

        public LatencyPoint() {}
        public LatencyPoint(String timestamp, long latencyMs) {
            this.timestamp = timestamp;
            this.latencyMs = latencyMs;
        }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public long getLatencyMs() { return latencyMs; }
        public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    }

    public long getAvgMs() { return avgMs; }
    public void setAvgMs(long avgMs) { this.avgMs = avgMs; }

    public long getMedianMs() { return medianMs; }
    public void setMedianMs(long medianMs) { this.medianMs = medianMs; }

    public long getP95Ms() { return p95Ms; }
    public void setP95Ms(long p95Ms) { this.p95Ms = p95Ms; }

    public long getMinMs() { return minMs; }
    public void setMinMs(long minMs) { this.minMs = minMs; }

    public long getMaxMs() { return maxMs; }
    public void setMaxMs(long maxMs) { this.maxMs = maxMs; }

    public List<LatencyPoint> getTrend() { return trend; }
    public void setTrend(List<LatencyPoint> trend) { this.trend = trend; }
}
