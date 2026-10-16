package com.pulsewatch.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notification_logs")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_config_id")
    private AlertConfig alertConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 30)
    private DeliveryStatus deliveryStatus = DeliveryStatus.PENDING;

    @Column(nullable = false)
    private int attempts = 1;

    @Column(name = "last_attempt_at", nullable = false)
    private Instant lastAttemptAt = Instant.now();

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public NotificationLog() {}

    public NotificationLog(AlertConfig alertConfig, Incident incident, String eventType, DeliveryStatus deliveryStatus, String errorMessage) {
        this.alertConfig = alertConfig;
        this.incident = incident;
        this.eventType = eventType;
        this.deliveryStatus = deliveryStatus != null ? deliveryStatus : DeliveryStatus.PENDING;
        this.attempts = 1;
        this.lastAttemptAt = Instant.now();
        this.errorMessage = errorMessage;
        if (deliveryStatus == DeliveryStatus.DELIVERED) {
            this.deliveredAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AlertConfig getAlertConfig() { return alertConfig; }
    public void setAlertConfig(AlertConfig alertConfig) { this.alertConfig = alertConfig; }

    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }

    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
