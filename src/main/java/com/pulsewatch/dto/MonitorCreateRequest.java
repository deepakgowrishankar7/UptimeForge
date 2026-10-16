package com.pulsewatch.dto;

import com.pulsewatch.entity.MonitorType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MonitorCreateRequest {

    @NotBlank(message = "Monitor name is required")
    @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotBlank(message = "Target URL or Host:Port is required")
    @Size(max = 2048, message = "URL cannot exceed 2048 characters")
    private String url;

    @NotNull(message = "Monitor type is required")
    private MonitorType type = MonitorType.HTTP;

    private String httpMethod = "GET";

    @Min(value = 10, message = "Interval must be at least 10 seconds")
    @Max(value = 86400, message = "Interval cannot exceed 86400 seconds (24h)")
    private int intervalSeconds = 60;

    @Min(value = 1000, message = "Timeout must be at least 1000ms")
    @Max(value = 60000, message = "Timeout cannot exceed 60000ms")
    private int timeoutMs = 10000;

    private String expectedStatusCodes = "200,201,204";
    private String expectedText;
    private String headers;
    private String requestBody;

    @Min(value = 1, message = "Failure threshold must be at least 1")
    @Max(value = 20, message = "Failure threshold cannot exceed 20")
    private int failureThreshold = 1;

    @Min(value = 1, message = "Recovery threshold must be at least 1")
    @Max(value = 20, message = "Recovery threshold cannot exceed 20")
    private int recoveryThreshold = 1;

    private Long teamId;

    public MonitorCreateRequest() {}

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

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
}
