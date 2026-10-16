package com.pulsewatch.dto;

import java.time.Instant;

public class TestCheckResponse {
    private boolean success;
    private long responseTimeMs;
    private Integer statusCode;
    private String errorType;
    private String errorMessage;
    private Instant executedAt = Instant.now();

    public TestCheckResponse() {}

    public TestCheckResponse(boolean success, long responseTimeMs, Integer statusCode, String errorType, String errorMessage) {
        this.success = success;
        this.responseTimeMs = responseTimeMs;
        this.statusCode = statusCode;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.executedAt = Instant.now();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }
}
