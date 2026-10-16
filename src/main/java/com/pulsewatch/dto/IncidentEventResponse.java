package com.pulsewatch.dto;

import com.pulsewatch.entity.IncidentEvent;
import com.pulsewatch.entity.IncidentEventType;
import java.time.Instant;

public class IncidentEventResponse {
    private Long id;
    private Long incidentId;
    private Long actorId;
    private String actorName;
    private IncidentEventType eventType;
    private String description;
    private Instant createdAt;

    public IncidentEventResponse() {}

    public static IncidentEventResponse fromEntity(IncidentEvent e) {
        if (e == null) return null;
        IncidentEventResponse resp = new IncidentEventResponse();
        resp.setId(e.getId());
        if (e.getIncident() != null) {
            resp.setIncidentId(e.getIncident().getId());
        }
        if (e.getActor() != null) {
            resp.setActorId(e.getActor().getId());
            resp.setActorName(e.getActor().getName());
        }
        resp.setEventType(e.getEventType());
        resp.setDescription(e.getDescription());
        resp.setCreatedAt(e.getCreatedAt());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }

    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public IncidentEventType getEventType() { return eventType; }
    public void setEventType(IncidentEventType eventType) { this.eventType = eventType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
