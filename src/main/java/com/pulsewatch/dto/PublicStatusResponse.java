package com.pulsewatch.dto;

import java.time.Instant;
import java.util.List;

public class PublicStatusResponse {
    private String title;
    private String description;
    private String overallStatus; // ALL_OPERATIONAL, DEGRADED, OUTAGE
    private double overallUptimePercent;
    private List<PublicMonitorStatus> services;
    private List<StatusAnnouncementResponse> activeAnnouncements;
    private List<PublicIncidentSummary> recentIncidents;
    private Instant lastUpdated = Instant.now();

    public PublicStatusResponse() {}

    public static class PublicIncidentSummary {
        private String title;
        private String status;
        private String severity;
        private Instant startedAt;
        private Instant resolvedAt;
        private String duration;

        public PublicIncidentSummary() {}
        public PublicIncidentSummary(String title, String status, String severity, Instant startedAt, Instant resolvedAt, String duration) {
            this.title = title;
            this.status = status;
            this.severity = severity;
            this.startedAt = startedAt;
            this.resolvedAt = resolvedAt;
            this.duration = duration;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public Instant getStartedAt() { return startedAt; }
        public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
        public Instant getResolvedAt() { return resolvedAt; }
        public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }

    public double getOverallUptimePercent() { return overallUptimePercent; }
    public void setOverallUptimePercent(double overallUptimePercent) { this.overallUptimePercent = overallUptimePercent; }

    public List<PublicMonitorStatus> getServices() { return services; }
    public void setServices(List<PublicMonitorStatus> services) { this.services = services; }

    public List<StatusAnnouncementResponse> getActiveAnnouncements() { return activeAnnouncements; }
    public void setActiveAnnouncements(List<StatusAnnouncementResponse> activeAnnouncements) { this.activeAnnouncements = activeAnnouncements; }

    public List<PublicIncidentSummary> getRecentIncidents() { return recentIncidents; }
    public void setRecentIncidents(List<PublicIncidentSummary> recentIncidents) { this.recentIncidents = recentIncidents; }

    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}
