package com.pulsewatch.dto;

import com.pulsewatch.entity.TeamMember;
import com.pulsewatch.entity.TeamRole;
import java.time.Instant;

public class TeamMemberResponse {
    private Long id;
    private Long teamId;
    private Long userId;
    private String userName;
    private String userEmail;
    private TeamRole role;
    private Instant joinedAt;

    public TeamMemberResponse() {}

    public static TeamMemberResponse fromEntity(TeamMember tm) {
        if (tm == null) return null;
        TeamMemberResponse resp = new TeamMemberResponse();
        resp.setId(tm.getId());
        if (tm.getTeam() != null) {
            resp.setTeamId(tm.getTeam().getId());
        }
        if (tm.getUser() != null) {
            resp.setUserId(tm.getUser().getId());
            resp.setUserName(tm.getUser().getName());
            resp.setUserEmail(tm.getUser().getEmail());
        }
        resp.setRole(tm.getRole());
        resp.setJoinedAt(tm.getJoinedAt());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public TeamRole getRole() { return role; }
    public void setRole(TeamRole role) { this.role = role; }

    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }
}
