package com.pulsewatch.controller;

import com.pulsewatch.analytics.AnalyticsService;
import com.pulsewatch.dto.*;
import com.pulsewatch.entity.Incident;
import com.pulsewatch.entity.IncidentStatus;
import com.pulsewatch.entity.Team;
import com.pulsewatch.entity.User;
import com.pulsewatch.repository.IncidentRepository;
import com.pulsewatch.security.SecurityUtils;
import com.pulsewatch.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Performance metrics, uptime percentages, latency distributions, and exports")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final IncidentRepository incidentRepository;
    private final UserService userService;

    public AnalyticsController(AnalyticsService analyticsService,
                               IncidentRepository incidentRepository,
                               UserService userService) {
        this.analyticsService = analyticsService;
        this.incidentRepository = incidentRepository;
        this.userService = userService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get overall platform analytics overview for dashboard")
    public ResponseEntity<ApiResponse<AnalyticsOverviewResponse>> getOverview() {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        AnalyticsOverviewResponse resp = analyticsService.getOverview(user);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @GetMapping("/uptime")
    @Operation(summary = "Get historical uptime analytics")
    public ResponseEntity<ApiResponse<UptimeStatsResponse>> getUptime(
            @RequestParam(required = false) Long monitorId,
            @RequestParam(defaultValue = "1") int days) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        UptimeStatsResponse resp = analyticsService.getUptimeStats(monitorId, days, user);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @GetMapping("/response-times")
    @Operation(summary = "Get response time percentiles (avg, median, p95, min, max)")
    public ResponseEntity<ApiResponse<ResponseTimeStatsResponse>> getResponseTimes(
            @RequestParam(required = false) Long monitorId,
            @RequestParam(defaultValue = "1") int days) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        ResponseTimeStatsResponse resp = analyticsService.getResponseTimeStats(monitorId, days, user);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @GetMapping("/incidents")
    @Operation(summary = "Get incident statistics and MTTR")
    public ResponseEntity<ApiResponse<IncidentStatsResponse>> getIncidentStats() {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        List<Team> teams = userService.getUserTeams(user);

        long open = incidentRepository.countAccessibleByStatus(user, teams, IncidentStatus.OPEN);
        long ack = incidentRepository.countAccessibleByStatus(user, teams, IncidentStatus.ACKNOWLEDGED);
        long resolved = incidentRepository.countAccessibleByStatus(user, teams, IncidentStatus.RESOLVED);
        long total = open + ack + resolved;

        long avgMttr = 0;
        if (resolved > 0) {
            List<Incident> resolvedList = incidentRepository.findAccessibleResolvedIncidents(user, teams, IncidentStatus.RESOLVED);
            long totalMttrSeconds = 0;
            long resolvedWithDurationCount = 0;
            for (Incident inc : resolvedList) {
                if (inc.getResolvedAt() != null && inc.getStartedAt() != null) {
                    totalMttrSeconds += Duration.between(inc.getStartedAt(), inc.getResolvedAt()).getSeconds();
                    resolvedWithDurationCount++;
                }
            }
            avgMttr = resolvedWithDurationCount > 0 ? (totalMttrSeconds / resolvedWithDurationCount) : 0;
        }

        IncidentStatsResponse resp = new IncidentStatsResponse(total, open, ack, resolved, avgMttr);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @GetMapping("/export")
    @Operation(summary = "Export monitoring check results to CSV")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) Long monitorId,
            @RequestParam(defaultValue = "7") int days) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        String csv = analyticsService.exportCsv(monitorId, days, user);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"pulsewatch_export_" + System.currentTimeMillis() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }
}
