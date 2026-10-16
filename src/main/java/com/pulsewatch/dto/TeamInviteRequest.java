package com.pulsewatch.dto;

import com.pulsewatch.entity.TeamRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TeamInviteRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    @NotNull(message = "Role is required")
    private TeamRole role = TeamRole.MEMBER;

    public TeamInviteRequest() {}

    public TeamInviteRequest(String email, TeamRole role) {
        this.email = email;
        this.role = role != null ? role : TeamRole.MEMBER;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public TeamRole getRole() { return role; }
    public void setRole(TeamRole role) { this.role = role; }
}
