package com.pulsewatch.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "check_results")
public class CheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "response_time_ms", nullable = false)
    private long responseTimeMs;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "error_type", length = 100)
    private String errorType;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public CheckResult() {}

    public CheckResult(Monitor monitor, Instant checkedAt, Instant completedAt, boolean success,
                       long responseTimeMs, Integer statusCode, String errorType, String errorMessage) {
        this.monitor = monitor;
        this.checkedAt = checkedAt;
        this.completedAt = completedAt;
        this.success = success;
        this.responseTimeMs = responseTimeMs;
        this.statusCode = statusCode;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Monitor getMonitor() { return monitor; }
    public void setMonitor(Monitor monitor) { this.monitor = monitor; }

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
