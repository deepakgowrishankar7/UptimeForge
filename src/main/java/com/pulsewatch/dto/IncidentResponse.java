package com.pulsewatch.dto;

import com.pulsewatch.entity.Incident;
import com.pulsewatch.entity.IncidentSeverity;
import com.pulsewatch.entity.IncidentStatus;

import java.time.Duration;
import java.time.Instant;

public class IncidentResponse {
    private Long id;
    private Long monitorId;
    private String monitorName;
    private String monitorUrl;
    private Long teamId;
    private String teamName;
    private IncidentStatus status;
    private IncidentSeverity severity;
    private Instant startedAt;
    private Instant detectedAt;
    private Instant lastFailureAt;
    private Instant resolvedAt;
    private Long durationSeconds;
    private Long acknowledgedById;
    private String acknowledgedByName;
    private Instant acknowledgedAt;
    private String resolutionNotes;

    public IncidentResponse() {}

    public static IncidentResponse fromIncident(Incident inc) {
        if (inc == null) return null;
        IncidentResponse resp = new IncidentResponse();
        resp.setId(inc.getId());
        if (inc.getMonitor() != null) {
            resp.setMonitorId(inc.getMonitor().getId());
            resp.setMonitorName(inc.getMonitor().getName());
            resp.setMonitorUrl(inc.getMonitor().getUrl());
        }
        if (inc.getTeam() != null) {
            resp.setTeamId(inc.getTeam().getId());
            resp.setTeamName(inc.getTeam().getName());
        }
        resp.setStatus(inc.getStatus());
        resp.setSeverity(inc.getSeverity());
        resp.setStartedAt(inc.getStartedAt());
        resp.setDetectedAt(inc.getDetectedAt());
        resp.setLastFailureAt(inc.getLastFailureAt());
        resp.setResolvedAt(inc.getResolvedAt());

        Instant end = inc.getResolvedAt() != null ? inc.getResolvedAt() : Instant.now();
        if (inc.getStartedAt() != null) {
            resp.setDurationSeconds(Duration.between(inc.getStartedAt(), end).getSeconds());
        }

        if (inc.getAcknowledgedBy() != null) {
            resp.setAcknowledgedById(inc.getAcknowledgedBy().getId());
            resp.setAcknowledgedByName(inc.getAcknowledgedBy().getName());
        }
        resp.setAcknowledgedAt(inc.getAcknowledgedAt());
        resp.setResolutionNotes(inc.getResolutionNotes());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMonitorId() { return monitorId; }
    public void setMonitorId(Long monitorId) { this.monitorId = monitorId; }

    public String getMonitorName() { return monitorName; }
    public void setMonitorName(String monitorName) { this.monitorName = monitorName; }

    public String getMonitorUrl() { return monitorUrl; }
    public void setMonitorUrl(String monitorUrl) { this.monitorUrl = monitorUrl; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public IncidentSeverity getSeverity() { return severity; }
    public void setSeverity(IncidentSeverity severity) { this.severity = severity; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }

    public Instant getLastFailureAt() { return lastFailureAt; }
    public void setLastFailureAt(Instant lastFailureAt) { this.lastFailureAt = lastFailureAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }

    public Long getAcknowledgedById() { return acknowledgedById; }
    public void setAcknowledgedById(Long acknowledgedById) { this.acknowledgedById = acknowledgedById; }

    public String getAcknowledgedByName() { return acknowledgedByName; }
    public void setAcknowledgedByName(String acknowledgedByName) { this.acknowledgedByName = acknowledgedByName; }

    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(Instant acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
}
