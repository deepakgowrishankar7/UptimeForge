package com.pulsewatch.dto;

import com.pulsewatch.entity.Monitor;
import com.pulsewatch.entity.MonitorStatus;
import com.pulsewatch.entity.MonitorType;

import java.time.Instant;

public class MonitorResponse {
    private Long id;
    private Long ownerId;
    private String ownerName;
    private Long teamId;
    private String teamName;
    private String name;
    private String description;
    private String url;
    private MonitorType type;
    private String httpMethod;
    private int intervalSeconds;
    private int timeoutMs;
    private String expectedStatusCodes;
    private String expectedText;
    private String headers;
    private String requestBody;
    private int failureThreshold;
    private int recoveryThreshold;
    private boolean enabled;
    private MonitorStatus currentStatus;
    private int consecutiveFailures;
    private int consecutiveSuccesses;
    private Instant lastCheckAt;
    private Instant nextCheckAt;
    private Long lastResponseTimeMs;
    private Double uptime24h;
    private Instant createdAt;
    private Instant updatedAt;

    public MonitorResponse() {}

    public static MonitorResponse fromMonitor(Monitor m) {
        if (m == null) return null;
        MonitorResponse resp = new MonitorResponse();
        resp.setId(m.getId());
        if (m.getOwner() != null) {
            resp.setOwnerId(m.getOwner().getId());
            resp.setOwnerName(m.getOwner().getName());
        }
        if (m.getTeam() != null) {
            resp.setTeamId(m.getTeam().getId());
            resp.setTeamName(m.getTeam().getName());
        }
        resp.setName(m.getName());
        resp.setDescription(m.getDescription());
        resp.setUrl(m.getUrl());
        resp.setType(m.getType());
        resp.setHttpMethod(m.getHttpMethod());
        resp.setIntervalSeconds(m.getIntervalSeconds());
        resp.setTimeoutMs(m.getTimeoutMs());
        resp.setExpectedStatusCodes(m.getExpectedStatusCodes());
        resp.setExpectedText(m.getExpectedText());
        resp.setHeaders(m.getHeaders());
        resp.setRequestBody(m.getRequestBody());
        resp.setFailureThreshold(m.getFailureThreshold());
        resp.setRecoveryThreshold(m.getRecoveryThreshold());
        resp.setEnabled(m.isEnabled());
        resp.setCurrentStatus(m.getCurrentStatus());
        resp.setConsecutiveFailures(m.getConsecutiveFailures());
        resp.setConsecutiveSuccesses(m.getConsecutiveSuccesses());
        resp.setLastCheckAt(m.getLastCheckAt());
        resp.setNextCheckAt(m.getNextCheckAt());
        resp.setCreatedAt(m.getCreatedAt());
        resp.setUpdatedAt(m.getUpdatedAt());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public MonitorType getType() { return type; }
    public void setType(MonitorType type) { this.type = type; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public int getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }

    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }

    public String getExpectedStatusCodes() { return expectedStatusCodes; }
    public void setExpectedStatusCodes(String expectedStatusCodes) { this.expectedStatusCodes = expectedStatusCodes; }

    public String getExpectedText() { return expectedText; }
    public void setExpectedText(String expectedText) { this.expectedText = expectedText; }

    public String getHeaders() { return headers; }
    public void setHeaders(String headers) { this.headers = headers; }

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }

    public int getFailureThreshold() { return failureThreshold; }
    public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }

    public int getRecoveryThreshold() { return recoveryThreshold; }
    public void setRecoveryThreshold(int recoveryThreshold) { this.recoveryThreshold = recoveryThreshold; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public MonitorStatus getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(MonitorStatus currentStatus) { this.currentStatus = currentStatus; }

    public int getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(int consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }

    public int getConsecutiveSuccesses() { return consecutiveSuccesses; }
    public void setConsecutiveSuccesses(int consecutiveSuccesses) { this.consecutiveSuccesses = consecutiveSuccesses; }

    public Instant getLastCheckAt() { return lastCheckAt; }
    public void setLastCheckAt(Instant lastCheckAt) { this.lastCheckAt = lastCheckAt; }

    public Instant getNextCheckAt() { return nextCheckAt; }
    public void setNextCheckAt(Instant nextCheckAt) { this.nextCheckAt = nextCheckAt; }

    public Long getLastResponseTimeMs() { return lastResponseTimeMs; }
    public void setLastResponseTimeMs(Long lastResponseTimeMs) { this.lastResponseTimeMs = lastResponseTimeMs; }

    public Double getUptime24h() { return uptime24h; }
    public void setUptime24h(Double uptime24h) { this.uptime24h = uptime24h; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
