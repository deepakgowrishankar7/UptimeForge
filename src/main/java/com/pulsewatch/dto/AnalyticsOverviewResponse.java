package com.pulsewatch.dto;

import java.util.List;

public class AnalyticsOverviewResponse {
    private long totalMonitors;
    private long monitorsUp;
    private long monitorsDown;
    private long monitorsDegraded;
    private long monitorsPaused;
    private double overallUptimePercent;
    private long avgResponseTimeMs;
    private long totalChecksCount;
    private long totalIncidentsCount;
    private long activeIncidentsCount;
    private List<MonitorResponse> topMonitors;
    private List<IncidentResponse> recentIncidents;
    private List<CheckResultResponse> recentFailures;

    public AnalyticsOverviewResponse() {}

    public long getTotalMonitors() { return totalMonitors; }
    public void setTotalMonitors(long totalMonitors) { this.totalMonitors = totalMonitors; }

    public long getMonitorsUp() { return monitorsUp; }
    public void setMonitorsUp(long monitorsUp) { this.monitorsUp = monitorsUp; }

    public long getMonitorsDown() { return monitorsDown; }
    public void setMonitorsDown(long monitorsDown) { this.monitorsDown = monitorsDown; }

    public long getMonitorsDegraded() { return monitorsDegraded; }
    public void setMonitorsDegraded(long monitorsDegraded) { this.monitorsDegraded = monitorsDegraded; }

    public long getMonitorsPaused() { return monitorsPaused; }
    public void setMonitorsPaused(long monitorsPaused) { this.monitorsPaused = monitorsPaused; }

    public double getOverallUptimePercent() { return overallUptimePercent; }
    public void setOverallUptimePercent(double overallUptimePercent) { this.overallUptimePercent = overallUptimePercent; }

    public long getAvgResponseTimeMs() { return avgResponseTimeMs; }
    public void setAvgResponseTimeMs(long avgResponseTimeMs) { this.avgResponseTimeMs = avgResponseTimeMs; }

    public long getTotalChecksCount() { return totalChecksCount; }
    public void setTotalChecksCount(long totalChecksCount) { this.totalChecksCount = totalChecksCount; }

    public long getTotalIncidentsCount() { return totalIncidentsCount; }
    public void setTotalIncidentsCount(long totalIncidentsCount) { this.totalIncidentsCount = totalIncidentsCount; }

    public long getActiveIncidentsCount() { return activeIncidentsCount; }
    public void setActiveIncidentsCount(long activeIncidentsCount) { this.activeIncidentsCount = activeIncidentsCount; }

    public List<MonitorResponse> getTopMonitors() { return topMonitors; }
    public void setTopMonitors(List<MonitorResponse> topMonitors) { this.topMonitors = topMonitors; }

    public List<IncidentResponse> getRecentIncidents() { return recentIncidents; }
    public void setRecentIncidents(List<IncidentResponse> recentIncidents) { this.recentIncidents = recentIncidents; }

    public List<CheckResultResponse> getRecentFailures() { return recentFailures; }
    public void setRecentFailures(List<CheckResultResponse> recentFailures) { this.recentFailures = recentFailures; }
}
