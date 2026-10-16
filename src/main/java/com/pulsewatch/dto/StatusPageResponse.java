package com.pulsewatch.dto;

import com.pulsewatch.entity.StatusPage;
import java.time.Instant;
import java.util.List;

public class StatusPageResponse {
    private Long id;
    private Long ownerId;
    private Long teamId;
    private String teamName;
    private String slug;
    private String title;
    private String description;
    private boolean published;
    private List<MonitorResponse> monitors;
    private Instant createdAt;
    private Instant updatedAt;

    public StatusPageResponse() {}

    public static StatusPageResponse fromEntity(StatusPage sp, List<MonitorResponse> monitors) {
        if (sp == null) return null;
        StatusPageResponse resp = new StatusPageResponse();
        resp.setId(sp.getId());
        if (sp.getOwner() != null) {
            resp.setOwnerId(sp.getOwner().getId());
        }
        if (sp.getTeam() != null) {
            resp.setTeamId(sp.getTeam().getId());
            resp.setTeamName(sp.getTeam().getName());
        }
        resp.setSlug(sp.getSlug());
        resp.setTitle(sp.getTitle());
        resp.setDescription(sp.getDescription());
        resp.setPublished(sp.isPublished());
        resp.setMonitors(monitors);
        resp.setCreatedAt(sp.getCreatedAt());
        resp.setUpdatedAt(sp.getUpdatedAt());
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

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public List<MonitorResponse> getMonitors() { return monitors; }
    public void setMonitors(List<MonitorResponse> monitors) { this.monitors = monitors; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
