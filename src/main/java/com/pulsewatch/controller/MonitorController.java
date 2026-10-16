package com.pulsewatch.controller;

import com.pulsewatch.analytics.AnalyticsService;
import com.pulsewatch.dto.*;
import com.pulsewatch.entity.MonitorStatus;
import com.pulsewatch.entity.User;
import com.pulsewatch.incident.IncidentService;
import com.pulsewatch.security.SecurityUtils;
import com.pulsewatch.service.MonitorService;
import com.pulsewatch.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/monitors")
@Tag(name = "Monitors", description = "Monitor management, health checks, uptime tracking, and ad-hoc tests")
public class MonitorController {

    private final MonitorService monitorService;
    private final UserService userService;
    private final IncidentService incidentService;
    private final AnalyticsService analyticsService;

    public MonitorController(MonitorService monitorService,
                             UserService userService,
                             IncidentService incidentService,
                             AnalyticsService analyticsService) {
        this.monitorService = monitorService;
        this.userService = userService;
        this.incidentService = incidentService;
        this.analyticsService = analyticsService;
    }

    @PostMapping
    @Operation(summary = "Create a new monitor")
    public ResponseEntity<ApiResponse<MonitorResponse>> createMonitor(@Valid @RequestBody MonitorCreateRequest request, HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        MonitorResponse resp = monitorService.createMonitor(user, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Monitor created successfully", resp));
    }

    @GetMapping
    @Operation(summary = "List monitors accessible to current user")
    public ResponseEntity<ApiResponse<PagedResponse<MonitorResponse>>> getMonitors(
            @RequestParam(required = false) MonitorStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        PagedResponse<MonitorResponse> resp = monitorService.getAccessibleMonitors(user, status, q, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get monitor details by ID")
    public ResponseEntity<ApiResponse<MonitorResponse>> getMonitor(@PathVariable Long id) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        MonitorResponse resp = monitorService.getMonitor(id, user);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing monitor")
    public ResponseEntity<ApiResponse<MonitorResponse>> updateMonitor(@PathVariable Long id, @Valid @RequestBody MonitorUpdateRequest request, HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        MonitorResponse resp = monitorService.updateMonitor(id, user, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Monitor updated successfully", resp));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a monitor")
    public ResponseEntity<ApiResponse<Void>> deleteMonitor(@PathVariable Long id, HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        monitorService.deleteMonitor(id, user, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Monitor deleted successfully", null));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause automated checks for a monitor")
    public ResponseEntity<ApiResponse<MonitorResponse>> pauseMonitor(@PathVariable Long id, HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        MonitorResponse resp = monitorService.pauseMonitor(id, user, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Monitor paused", resp));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume automated checks for a monitor")
    public ResponseEntity<ApiResponse<MonitorResponse>> resumeMonitor(@PathVariable Long id, HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        MonitorResponse resp = monitorService.resumeMonitor(id, user, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Monitor resumed", resp));
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "Get execution history and check results for a monitor")
    public ResponseEntity<ApiResponse<PagedResponse<CheckResultResponse>>> getMonitorResults(
            @PathVariable Long id,
            @PageableDefault(size = 30, sort = "checkedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        PagedResponse<CheckResultResponse> resp = monitorService.getMonitorResults(id, user, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @GetMapping("/{id}/uptime")
    @Operation(summary = "Get historical uptime breakdown for a monitor")
    public ResponseEntity<ApiResponse<UptimeStatsResponse>> getMonitorUptime(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int days) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        UptimeStatsResponse resp = analyticsService.getUptimeStats(id, days, user);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @GetMapping("/{id}/incidents")
    @Operation(summary = "Get incidents for a specific monitor")
    public ResponseEntity<ApiResponse<PagedResponse<IncidentResponse>>> getMonitorIncidents(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        PagedResponse<IncidentResponse> resp = incidentService.getMonitorIncidents(id, user, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PostMapping("/{id}/test-check")
    @Operation(summary = "Trigger immediate test health check for an existing monitor")
    public ResponseEntity<ApiResponse<TestCheckResponse>> testExistingMonitor(@PathVariable Long id) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        TestCheckResponse resp = monitorService.testExistingMonitor(id, user);
        return ResponseEntity.ok(ApiResponse.success("Check executed", resp));
    }

    @PostMapping("/test")
    @Operation(summary = "Ad-hoc test check for an endpoint before saving monitor")
    public ResponseEntity<ApiResponse<TestCheckResponse>> testAdHocCheck(@Valid @RequestBody TestCheckRequest request) {
        TestCheckResponse resp = monitorService.testAdHocCheck(request);
        return ResponseEntity.ok(ApiResponse.success("Test check executed", resp));
    }
}
