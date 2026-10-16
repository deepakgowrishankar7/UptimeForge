package com.pulsewatch.dto;

import com.pulsewatch.entity.Role;
import com.pulsewatch.entity.User;
import java.time.Instant;

public class UserProfileResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean enabled;
    private boolean emailVerified;
    private Instant createdAt;

    public UserProfileResponse() {}

    public UserProfileResponse(Long id, String name, String email, Role role, boolean enabled, boolean emailVerified, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
        this.emailVerified = emailVerified;
        this.createdAt = createdAt;
    }

    public static UserProfileResponse fromUser(User user) {
        if (user == null) return null;
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
