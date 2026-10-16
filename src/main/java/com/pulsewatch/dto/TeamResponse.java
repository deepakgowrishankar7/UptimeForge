package com.pulsewatch.dto;

import com.pulsewatch.entity.Team;
import java.time.Instant;

public class TeamResponse {
    private Long id;
    private String name;
    private Long ownerId;
    private String ownerName;
    private int memberCount;
    private Instant createdAt;
    private Instant updatedAt;

    public TeamResponse() {}

    public static TeamResponse fromEntity(Team t, int memberCount) {
        if (t == null) return null;
        TeamResponse resp = new TeamResponse();
        resp.setId(t.getId());
        resp.setName(t.getName());
        if (t.getOwner() != null) {
            resp.setOwnerId(t.getOwner().getId());
            resp.setOwnerName(t.getOwner().getName());
        }
        resp.setMemberCount(memberCount);
        resp.setCreatedAt(t.getCreatedAt());
        resp.setUpdatedAt(t.getUpdatedAt());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
