package com.pulsewatch.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "alert_configs")
public class AlertConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertChannel channel = AlertChannel.EMAIL;

    @Column(nullable = false, length = 500)
    private String destination;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "failure_threshold", nullable = false)
    private int failureThreshold = 3;

    @Column(name = "cooldown_seconds", nullable = false)
    private int cooldownSeconds = 900;

    @Column(name = "notify_on_recovery", nullable = false)
    private boolean notifyOnRecovery = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public AlertConfig() {}

    public AlertConfig(User owner, Team team, AlertChannel channel, String destination, boolean enabled, int failureThreshold, int cooldownSeconds, boolean notifyOnRecovery) {
        this.owner = owner;
        this.team = team;
        this.channel = channel != null ? channel : AlertChannel.EMAIL;
        this.destination = destination;
        this.enabled = enabled;
        this.failureThreshold = failureThreshold;
        this.cooldownSeconds = cooldownSeconds;
        this.notifyOnRecovery = notifyOnRecovery;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

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
