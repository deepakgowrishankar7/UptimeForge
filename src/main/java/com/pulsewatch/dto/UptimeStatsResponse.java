package com.pulsewatch.dto;

import java.util.List;

public class UptimeStatsResponse {
    private double uptimePercent;
    private long totalChecks;
    private long successfulChecks;
    private long failedChecks;
    private long downtimeDurationSeconds;
    private List<TimePoint> trend;

    public UptimeStatsResponse() {}

    public static class TimePoint {
        private String timestamp;
        private double uptime;

        public TimePoint() {}
        public TimePoint(String timestamp, double uptime) {
            this.timestamp = timestamp;
            this.uptime = uptime;
        }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public double getUptime() { return uptime; }
        public void setUptime(double uptime) { this.uptime = uptime; }
    }

    public double getUptimePercent() { return uptimePercent; }
    public void setUptimePercent(double uptimePercent) { this.uptimePercent = uptimePercent; }

    public long getTotalChecks() { return totalChecks; }
    public void setTotalChecks(long totalChecks) { this.totalChecks = totalChecks; }

    public long getSuccessfulChecks() { return successfulChecks; }
    public void setSuccessfulChecks(long successfulChecks) { this.successfulChecks = successfulChecks; }

    public long getFailedChecks() { return failedChecks; }
    public void setFailedChecks(long failedChecks) { this.failedChecks = failedChecks; }

    public long getDowntimeDurationSeconds() { return downtimeDurationSeconds; }
    public void setDowntimeDurationSeconds(long downtimeDurationSeconds) { this.downtimeDurationSeconds = downtimeDurationSeconds; }

    public List<TimePoint> getTrend() { return trend; }
    public void setTrend(List<TimePoint> trend) { this.trend = trend; }
}
