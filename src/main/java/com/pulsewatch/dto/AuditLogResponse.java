package com.pulsewatch.dto;

import com.pulsewatch.entity.AuditLog;
import java.time.Instant;

public class AuditLogResponse {
    private Long id;
    private Long actorId;
    private String actorName;
    private String action;
    private String resourceType;
    private String resourceId;
    private String details;
    private String ipAddress;
    private Instant createdAt;

    public AuditLogResponse() {}

    public static AuditLogResponse fromEntity(AuditLog a) {
        if (a == null) return null;
        AuditLogResponse resp = new AuditLogResponse();
        resp.setId(a.getId());
        if (a.getActor() != null) {
            resp.setActorId(a.getActor().getId());
            resp.setActorName(a.getActor().getName());
        }
        resp.setAction(a.getAction());
        resp.setResourceType(a.getResourceType());
        resp.setResourceId(a.getResourceId());
        resp.setDetails(a.getDetails());
        resp.setIpAddress(a.getIpAddress());
        resp.setCreatedAt(a.getCreatedAt());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
