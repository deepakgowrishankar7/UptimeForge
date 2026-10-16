package com.pulsewatch.dto;

import com.pulsewatch.entity.CheckResult;
import java.time.Instant;

public class CheckResultResponse {
    private Long id;
    private Long monitorId;
    private String monitorName;
    private Instant checkedAt;
    private Instant completedAt;
    private boolean success;
    private long responseTimeMs;
    private Integer statusCode;
    private String errorType;
    private String errorMessage;

    public CheckResultResponse() {}

    public static CheckResultResponse fromEntity(CheckResult r) {
        if (r == null) return null;
        CheckResultResponse resp = new CheckResultResponse();
        resp.setId(r.getId());
        if (r.getMonitor() != null) {
            resp.setMonitorId(r.getMonitor().getId());
            resp.setMonitorName(r.getMonitor().getName());
        }
        resp.setCheckedAt(r.getCheckedAt());
        resp.setCompletedAt(r.getCompletedAt());
        resp.setSuccess(r.isSuccess());
        resp.setResponseTimeMs(r.getResponseTimeMs());
        resp.setStatusCode(r.getStatusCode());
        resp.setErrorType(r.getErrorType());
        resp.setErrorMessage(r.getErrorMessage());
        return resp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMonitorId() { return monitorId; }
    public void setMonitorId(Long monitorId) { this.monitorId = monitorId; }

    public String getMonitorName() { return monitorName; }
    public void setMonitorName(String monitorName) { this.monitorName = monitorName; }

    public Instant getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Instant checkedAt) { this.checkedAt = checkedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

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
}
