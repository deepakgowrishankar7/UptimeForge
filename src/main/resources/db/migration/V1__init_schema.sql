-- PulseWatch Initial Database Schema
-- Compatible with MySQL 8.0 and H2 (MySQL Mode)

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Refresh Tokens Table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_refresh_token_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_token_hash ON refresh_tokens(token_hash);

-- 3. Email Verification Tokens Table
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    CONSTRAINT fk_email_verif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. Password Reset Tokens Table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    CONSTRAINT fk_pwd_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 5. Teams Table
CREATE TABLE IF NOT EXISTS teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_team_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_teams_owner ON teams(owner_id);

-- 6. Team Members Table
CREATE TABLE IF NOT EXISTS team_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_team_member_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_team_member_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_team_user UNIQUE (team_id, user_id)
);
CREATE INDEX idx_team_members_user ON team_members(user_id);

-- 7. Team Invitations Table
CREATE TABLE IF NOT EXISTS team_invitations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP NULL,
    CONSTRAINT fk_team_invitation_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
);
CREATE INDEX idx_team_inv_email ON team_invitations(email);

-- 8. Monitors Table
CREATE TABLE IF NOT EXISTS monitors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    team_id BIGINT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    url VARCHAR(2048) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'HTTP',
    http_method VARCHAR(10) NOT NULL DEFAULT 'GET',
    interval_seconds INT NOT NULL DEFAULT 60,
    timeout_ms INT NOT NULL DEFAULT 10000,
    expected_status_codes VARCHAR(100) NOT NULL DEFAULT '200,201,204',
    expected_text VARCHAR(500) NULL,
    headers TEXT NULL,
    request_body TEXT NULL,
    failure_threshold INT NOT NULL DEFAULT 3,
    recovery_threshold INT NOT NULL DEFAULT 2,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    current_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    consecutive_failures INT NOT NULL DEFAULT 0,
    consecutive_successes INT NOT NULL DEFAULT 0,
    last_check_at TIMESTAMP NULL,
    next_check_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_monitor_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_monitor_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL
);
CREATE INDEX idx_monitors_owner ON monitors(owner_id);
CREATE INDEX idx_monitors_team ON monitors(team_id);
CREATE INDEX idx_monitors_schedule ON monitors(enabled, next_check_at);
CREATE INDEX idx_monitors_status ON monitors(current_status);

-- 9. Check Results Table
CREATE TABLE IF NOT EXISTS check_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    monitor_id BIGINT NOT NULL,
    checked_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NOT NULL,
    success BOOLEAN NOT NULL,
    response_time_ms BIGINT NOT NULL,
    status_code INT NULL,
    error_type VARCHAR(100) NULL,
    error_message VARCHAR(1000) NULL,
    CONSTRAINT fk_check_result_monitor FOREIGN KEY (monitor_id) REFERENCES monitors(id) ON DELETE CASCADE
);
CREATE INDEX idx_check_results_monitor_time ON check_results(monitor_id, checked_at);
CREATE INDEX idx_check_results_success ON check_results(monitor_id, success);

-- 10. Incidents Table
CREATE TABLE IF NOT EXISTS incidents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    monitor_id BIGINT NOT NULL,
    team_id BIGINT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    severity VARCHAR(30) NOT NULL DEFAULT 'HIGH',
    started_at TIMESTAMP NOT NULL,
    detected_at TIMESTAMP NOT NULL,
    last_failure_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP NULL,
    acknowledged_by BIGINT NULL,
    acknowledged_at TIMESTAMP NULL,
    resolution_notes TEXT NULL,
    CONSTRAINT fk_incident_monitor FOREIGN KEY (monitor_id) REFERENCES monitors(id) ON DELETE CASCADE,
    CONSTRAINT fk_incident_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL,
    CONSTRAINT fk_incident_ack_user FOREIGN KEY (acknowledged_by) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_incidents_monitor ON incidents(monitor_id);
CREATE INDEX idx_incidents_status ON incidents(status);
CREATE INDEX idx_incidents_started ON incidents(started_at);

-- 11. Incident Events Table
CREATE TABLE IF NOT EXISTS incident_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    actor_id BIGINT NULL,
    event_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_incident FOREIGN KEY (incident_id) REFERENCES incidents(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_incident_events_incident ON incident_events(incident_id);

-- 12. Incident Notes Table
CREATE TABLE IF NOT EXISTS incident_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    note TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_note_incident FOREIGN KEY (incident_id) REFERENCES incidents(id) ON DELETE CASCADE,
    CONSTRAINT fk_note_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_incident_notes_incident ON incident_notes(incident_id);

-- 13. Alert Configurations Table
CREATE TABLE IF NOT EXISTS alert_configs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    team_id BIGINT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'EMAIL',
    destination VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    failure_threshold INT NOT NULL DEFAULT 3,
    cooldown_seconds INT NOT NULL DEFAULT 900,
    notify_on_recovery BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL
);
CREATE INDEX idx_alert_configs_owner ON alert_configs(owner_id);
CREATE INDEX idx_alert_configs_team ON alert_configs(team_id);

-- 14. Notification Logs Table
CREATE TABLE IF NOT EXISTS notification_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_config_id BIGINT NULL,
    incident_id BIGINT NULL,
    event_type VARCHAR(50) NOT NULL,
    delivery_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 1,
    last_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP NULL,
    error_message VARCHAR(1000) NULL,
    CONSTRAINT fk_notif_alert FOREIGN KEY (alert_config_id) REFERENCES alert_configs(id) ON DELETE SET NULL,
    CONSTRAINT fk_notif_incident FOREIGN KEY (incident_id) REFERENCES incidents(id) ON DELETE SET NULL
);
CREATE INDEX idx_notif_logs_incident ON notification_logs(incident_id);
CREATE INDEX idx_notif_logs_status ON notification_logs(delivery_status);

-- 15. Maintenance Windows Table
CREATE TABLE IF NOT EXISTS maintenance_windows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    team_id BIGINT NULL,
    monitor_id BIGINT NULL,
    title VARCHAR(150) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_maint_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_maint_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL,
    CONSTRAINT fk_maint_monitor FOREIGN KEY (monitor_id) REFERENCES monitors(id) ON DELETE CASCADE
);
CREATE INDEX idx_maint_timerange ON maintenance_windows(starts_at, ends_at);

-- 16. Status Pages Table
CREATE TABLE IF NOT EXISTS status_pages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    team_id BIGINT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(1000) NULL,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_status_page_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_status_page_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL
);
CREATE INDEX idx_status_pages_slug ON status_pages(slug);

-- 17. Status Page Monitors (Mapping) Table
CREATE TABLE IF NOT EXISTS status_page_monitors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status_page_id BIGINT NOT NULL,
    monitor_id BIGINT NOT NULL,
    CONSTRAINT fk_spm_page FOREIGN KEY (status_page_id) REFERENCES status_pages(id) ON DELETE CASCADE,
    CONSTRAINT fk_spm_monitor FOREIGN KEY (monitor_id) REFERENCES monitors(id) ON DELETE CASCADE,
    CONSTRAINT uq_spm_pair UNIQUE (status_page_id, monitor_id)
);

-- 18. Status Announcements Table
CREATE TABLE IF NOT EXISTS status_announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status_page_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    starts_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ends_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_announcement_page FOREIGN KEY (status_page_id) REFERENCES status_pages(id) ON DELETE CASCADE
);
CREATE INDEX idx_announcements_page ON status_announcements(status_page_id);

-- 19. Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100) NULL,
    details TEXT NULL,
    ip_address VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_time ON audit_logs(created_at);

-- 20. Platform Settings Table
CREATE TABLE IF NOT EXISTS platform_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Initial Seed Data
-- Default Admin User: admin@pulsewatch.io / Admin@123456 (BCrypt $2a$10$wT0X8U0Q1q.d5yv5f3uAbe7e/R2/y3wS16lA4bA9rB6I7F9V8I1e2)
INSERT INTO users (id, name, email, password_hash, role, enabled, email_verified, created_at, updated_at)
VALUES (1, 'System Administrator', 'admin@pulsewatch.io', '$2a$10$63b7.1Lp/Wd9v9B5K249Q.L5n4E52pM5r0F4aL5sC8eN2tG6fF7G6', 'ADMIN', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Default Demo Regular User: user@pulsewatch.io / User@123456
INSERT INTO users (id, name, email, password_hash, role, enabled, email_verified, created_at, updated_at)
VALUES (2, 'Alex Turner', 'user@pulsewatch.io', '$2a$10$63b7.1Lp/Wd9v9B5K249Q.L5n4E52pM5r0F4aL5sC8eN2tG6fF7G6', 'USER', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Initial Platform Settings
INSERT INTO platform_settings (setting_key, setting_value, updated_at) VALUES
('default_check_interval_seconds', '60', CURRENT_TIMESTAMP),
('min_check_interval_seconds', '10', CURRENT_TIMESTAMP),
('max_monitors_per_user', '50', CURRENT_TIMESTAMP),
('max_teams_per_user', '5', CURRENT_TIMESTAMP),
('data_retention_days', '90', CURRENT_TIMESTAMP),
('maintenance_mode', 'false', CURRENT_TIMESTAMP),
('email_notifications_enabled', 'true', CURRENT_TIMESTAMP),
('webhook_notifications_enabled', 'true', CURRENT_TIMESTAMP);
