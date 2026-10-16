package com.pulsewatch.dto;

import com.pulsewatch.entity.StatusAnnouncement;
import java.time.Instant;

public class StatusAnnouncementResponse {
    private Long id;
    private Long statusPageId;
    private String title;
    private String message;
    private Instant startsAt;
    private Instant endsAt;
    private boolean active;
    private Instant createdAt;

    public StatusAnnouncementResponse() {}

    public static StatusAnnouncementResponse fromEntity(StatusAnnouncement a) {
        if (a == null) return null;
        StatusAnnouncementResponse resp = new StatusAnnouncementResponse();
        resp.setId(a.getId());
        if (a.getStatusPage() != null) {
            resp.setStatusPageId(a.getStatusPage().getId());
        }
        resp.setTitle(a.getTitle());
        resp.setMessage(a.getMessage());
        resp.setStartsAt(a.getStartsAt());
        resp.setEndsAt(a.getEndsAt());
        resp.setActive(a.isActiveNow());
        resp.setCreatedAt(a.getCreatedAt());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStatusPageId() { return statusPageId; }
    public void setStatusPageId(Long statusPageId) { this.statusPageId = statusPageId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }

    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
