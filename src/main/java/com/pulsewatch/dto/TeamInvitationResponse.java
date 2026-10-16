package com.pulsewatch.dto;

import com.pulsewatch.entity.TeamInvitation;
import com.pulsewatch.entity.TeamRole;
import java.time.Instant;

public class TeamInvitationResponse {
    private Long id;
    private Long teamId;
    private String teamName;
    private String email;
    private TeamRole role;
    private Instant expiresAt;
    private Instant acceptedAt;

    public TeamInvitationResponse() {}

    public static TeamInvitationResponse fromEntity(TeamInvitation inv) {
        if (inv == null) return null;
        TeamInvitationResponse resp = new TeamInvitationResponse();
        resp.setId(inv.getId());
        if (inv.getTeam() != null) {
            resp.setTeamId(inv.getTeam().getId());
            resp.setTeamName(inv.getTeam().getName());
        }
        resp.setEmail(inv.getEmail());
        resp.setRole(inv.getRole());
        resp.setExpiresAt(inv.getExpiresAt());
        resp.setAcceptedAt(inv.getAcceptedAt());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public TeamRole getRole() { return role; }
    public void setRole(TeamRole role) { this.role = role; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
}
