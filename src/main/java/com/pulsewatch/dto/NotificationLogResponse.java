package com.pulsewatch.dto;

import com.pulsewatch.entity.DeliveryStatus;
import com.pulsewatch.entity.NotificationLog;
import java.time.Instant;

public class NotificationLogResponse {
    private Long id;
    private Long alertConfigId;
    private String destination;
    private String channel;
    private Long incidentId;
    private String eventType;
    private DeliveryStatus deliveryStatus;
    private int attempts;
    private Instant lastAttemptAt;
    private Instant deliveredAt;
    private String errorMessage;

    public NotificationLogResponse() {}

    public static NotificationLogResponse fromEntity(NotificationLog l) {
        if (l == null) return null;
        NotificationLogResponse resp = new NotificationLogResponse();
        resp.setId(l.getId());
        if (l.getAlertConfig() != null) {
            resp.setAlertConfigId(l.getAlertConfig().getId());
            resp.setDestination(l.getAlertConfig().getDestination());
            if (l.getAlertConfig().getChannel() != null) {
                resp.setChannel(l.getAlertConfig().getChannel().name());
            }
        }
        if (l.getIncident() != null) {
            resp.setIncidentId(l.getIncident().getId());
        }
        resp.setEventType(l.getEventType());
        resp.setDeliveryStatus(l.getDeliveryStatus());
        resp.setAttempts(l.getAttempts());
        resp.setLastAttemptAt(l.getLastAttemptAt());
        resp.setDeliveredAt(l.getDeliveredAt());
        resp.setErrorMessage(l.getErrorMessage());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAlertConfigId() { return alertConfigId; }
    public void setAlertConfigId(Long alertConfigId) { this.alertConfigId = alertConfigId; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }

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
