package com.pulsewatch.dto;

import com.pulsewatch.entity.Role;
import jakarta.validation.constraints.NotNull;

public class UserRoleUpdateRequest {

    @NotNull(message = "Role is required")
    private Role role;

    public UserRoleUpdateRequest() {}

    public UserRoleUpdateRequest(Role role) {
        this.role = role;
    }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
