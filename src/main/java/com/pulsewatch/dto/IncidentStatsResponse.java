package com.pulsewatch.dto;

public class IncidentStatsResponse {
    private long totalIncidents;
    private long openIncidents;
    private long acknowledgedIncidents;
    private long resolvedIncidents;
    private long avgMttrSeconds;

    public IncidentStatsResponse() {}

    public IncidentStatsResponse(long totalIncidents, long openIncidents, long acknowledgedIncidents, long resolvedIncidents, long avgMttrSeconds) {
        this.totalIncidents = totalIncidents;
        this.openIncidents = openIncidents;
        this.acknowledgedIncidents = acknowledgedIncidents;
        this.resolvedIncidents = resolvedIncidents;
        this.avgMttrSeconds = avgMttrSeconds;
    }

    public long getTotalIncidents() { return totalIncidents; }
    public void setTotalIncidents(long totalIncidents) { this.totalIncidents = totalIncidents; }

    public long getOpenIncidents() { return openIncidents; }
    public void setOpenIncidents(long openIncidents) { this.openIncidents = openIncidents; }

    public long getAcknowledgedIncidents() { return acknowledgedIncidents; }
    public void setAcknowledgedIncidents(long acknowledgedIncidents) { this.acknowledgedIncidents = acknowledgedIncidents; }

    public long getResolvedIncidents() { return resolvedIncidents; }
    public void setResolvedIncidents(long resolvedIncidents) { this.resolvedIncidents = resolvedIncidents; }

    public long getAvgMttrSeconds() { return avgMttrSeconds; }
    public void setAvgMttrSeconds(long avgMttrSeconds) { this.avgMttrSeconds = avgMttrSeconds; }
}
