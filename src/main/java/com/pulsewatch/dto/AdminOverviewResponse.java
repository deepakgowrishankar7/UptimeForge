package com.pulsewatch.dto;

import java.util.List;

public class AdminOverviewResponse {
    private long totalUsers;
    private long activeUsers;
    private long totalMonitors;
    private long enabledMonitors;
    private long openIncidents;
    private long totalCheckResults;
    private long failedNotifications;
    private List<UserProfileResponse> recentUsers;
    private List<AuditLogResponse> recentAuditLogs;

    public AdminOverviewResponse() {}

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

    public long getTotalMonitors() { return totalMonitors; }
    public void setTotalMonitors(long totalMonitors) { this.totalMonitors = totalMonitors; }

    public long getEnabledMonitors() { return enabledMonitors; }
    public void setEnabledMonitors(long enabledMonitors) { this.enabledMonitors = enabledMonitors; }

    public long getOpenIncidents() { return openIncidents; }
    public void setOpenIncidents(long openIncidents) { this.openIncidents = openIncidents; }

    public long getTotalCheckResults() { return totalCheckResults; }
    public void setTotalCheckResults(long totalCheckResults) { this.totalCheckResults = totalCheckResults; }

    public long getFailedNotifications() { return failedNotifications; }
    public void setFailedNotifications(long failedNotifications) { this.failedNotifications = failedNotifications; }

    public List<UserProfileResponse> getRecentUsers() { return recentUsers; }
    public void setRecentUsers(List<UserProfileResponse> recentUsers) { this.recentUsers = recentUsers; }

    public List<AuditLogResponse> getRecentAuditLogs() { return recentAuditLogs; }
    public void setRecentAuditLogs(List<AuditLogResponse> recentAuditLogs) { this.recentAuditLogs = recentAuditLogs; }
}
