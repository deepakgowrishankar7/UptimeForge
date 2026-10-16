package com.pulsewatch.dto;

import com.pulsewatch.entity.MonitorStatus;

public class PublicMonitorStatus {
    private String name;
    private MonitorStatus status;
    private double uptimePercent;
    private Long lastResponseTimeMs;
    private String serviceType;

    public PublicMonitorStatus() {}

    public PublicMonitorStatus(String name, MonitorStatus status, double uptimePercent, Long lastResponseTimeMs, String serviceType) {
        this.name = name;
        this.status = status;
        this.uptimePercent = uptimePercent;
        this.lastResponseTimeMs = lastResponseTimeMs;
        this.serviceType = serviceType;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public MonitorStatus getStatus() { return status; }
    public void setStatus(MonitorStatus status) { this.status = status; }

    public double getUptimePercent() { return uptimePercent; }
    public void setUptimePercent(double uptimePercent) { this.uptimePercent = uptimePercent; }

    public Long getLastResponseTimeMs() { return lastResponseTimeMs; }
    public void setLastResponseTimeMs(Long lastResponseTimeMs) { this.lastResponseTimeMs = lastResponseTimeMs; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
}
