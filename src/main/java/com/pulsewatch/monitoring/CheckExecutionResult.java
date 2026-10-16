package com.pulsewatch.monitoring;

import java.time.Instant;

public class CheckExecutionResult {
    private final boolean success;
    private final long responseTimeMs;
    private final Integer statusCode;
    private final String errorType;
    private final String errorMessage;
    private final Instant checkedAt;
    private final Instant completedAt;

    public CheckExecutionResult(boolean success, long responseTimeMs, Integer statusCode,
                                String errorType, String errorMessage, Instant checkedAt, Instant completedAt) {
        this.success = success;
        this.responseTimeMs = responseTimeMs;
        this.statusCode = statusCode;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.checkedAt = checkedAt;
        this.completedAt = completedAt;
    }

    public static CheckExecutionResult success(long responseTimeMs, Integer statusCode, Instant checkedAt, Instant completedAt) {
        return new CheckExecutionResult(true, responseTimeMs, statusCode, null, null, checkedAt, completedAt);
    }

    public static CheckExecutionResult failure(long responseTimeMs, Integer statusCode, String errorType, String errorMessage, Instant checkedAt, Instant completedAt) {
        return new CheckExecutionResult(false, responseTimeMs, statusCode, errorType, errorMessage, checkedAt, completedAt);
    }

    public boolean isSuccess() { return success; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public Integer getStatusCode() { return statusCode; }
    public String getErrorType() { return errorType; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCheckedAt() { return checkedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
