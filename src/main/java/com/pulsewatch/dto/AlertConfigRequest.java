package com.pulsewatch.dto;

import com.pulsewatch.entity.AlertChannel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AlertConfigRequest {

    @NotNull(message = "Channel is required")
    private AlertChannel channel = AlertChannel.EMAIL;

    @NotBlank(message = "Destination (email/webhook URL) is required")
    @Size(max = 500, message = "Destination cannot exceed 500 characters")
    private String destination;

    private boolean enabled = true;

    @Min(value = 1, message = "Failure threshold must be at least 1")
    @Max(value = 20, message = "Failure threshold cannot exceed 20")
    private int failureThreshold = 3;

    @Min(value = 0, message = "Cooldown seconds cannot be negative")
    @Max(value = 86400, message = "Cooldown cannot exceed 86400 seconds")
    private int cooldownSeconds = 900;

    private boolean notifyOnRecovery = true;

    private Long teamId;

    public AlertConfigRequest() {}

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

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
}
