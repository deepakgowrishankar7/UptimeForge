package com.pulsewatch.dto;

import com.pulsewatch.entity.AlertChannel;
import com.pulsewatch.entity.AlertConfig;
import java.time.Instant;

public class AlertConfigResponse {
    private Long id;
    private Long ownerId;
    private Long teamId;
    private String teamName;
    private AlertChannel channel;
    private String destination;
    private boolean enabled;
    private int failureThreshold;
    private int cooldownSeconds;
    private boolean notifyOnRecovery;
    private Instant createdAt;
    private Instant updatedAt;

    public AlertConfigResponse() {}

    public static AlertConfigResponse fromEntity(AlertConfig c) {
        if (c == null) return null;
        AlertConfigResponse resp = new AlertConfigResponse();
        resp.setId(c.getId());
        if (c.getOwner() != null) {
            resp.setOwnerId(c.getOwner().getId());
        }
        if (c.getTeam() != null) {
            resp.setTeamId(c.getTeam().getId());
            resp.setTeamName(c.getTeam().getName());
        }
        resp.setChannel(c.getChannel());
        resp.setDestination(c.getDestination());
        resp.setEnabled(c.isEnabled());
        resp.setFailureThreshold(c.getFailureThreshold());
        resp.setCooldownSeconds(c.getCooldownSeconds());
        resp.setNotifyOnRecovery(c.isNotifyOnRecovery());
        resp.setCreatedAt(c.getCreatedAt());
        resp.setUpdatedAt(c.getUpdatedAt());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public AlertChannel getChannel() { return channel; }
    public void setChannel(AlertChannel channel) { this.channel = channel; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getFailureThreshold() { return failureThreshold; }
    public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }

    public int getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(int cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }

    public boolean isNotifyOnRecovery() { return notifyOnRecovery; }
    public void setNotifyOnRecovery(boolean notifyOnRecovery) { this.notifyOnRecovery = notifyOnRecovery; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
