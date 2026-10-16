package com.pulsewatch.controller;

import com.pulsewatch.admin.AdminService;
import com.pulsewatch.dto.*;
import com.pulsewatch.entity.*;
import com.pulsewatch.repository.CheckResultRepository;
import com.pulsewatch.repository.IncidentRepository;
import com.pulsewatch.repository.MonitorRepository;
import com.pulsewatch.security.SecurityUtils;
import com.pulsewatch.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administrator", description = "Platform management, user accounts, audit search, and system settings")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final IncidentRepository incidentRepository;
    private final MonitorRepository monitorRepository;
    private final CheckResultRepository checkResultRepository;

    public AdminController(AdminService adminService,
                           UserService userService,
                           IncidentRepository incidentRepository,
                           MonitorRepository monitorRepository,
                           CheckResultRepository checkResultRepository) {
        this.adminService = adminService;
        this.userService = userService;
        this.incidentRepository = incidentRepository;
        this.monitorRepository = monitorRepository;
        this.checkResultRepository = checkResultRepository;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get platform-wide operational overview")
    public ResponseEntity<ApiResponse<AdminOverviewResponse>> getOverview() {
        AdminOverviewResponse resp = adminService.getOverview();
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @GetMapping("/users")
    @Operation(summary = "List and search all registered users")
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> getUsers(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<UserProfileResponse> resp = adminService.getUsers(q, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Suspend or reactivate a user account")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        User admin = userService.getUserById(SecurityUtils.getCurrentUserId());
        UserProfileResponse resp = adminService.updateUserStatus(id, request, admin, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("User status updated", resp));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Update a user's global platform role")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateRequest request,
            HttpServletRequest httpRequest) {
        User admin = userService.getUserById(SecurityUtils.getCurrentUserId());
        UserProfileResponse resp = adminService.updateUserRole(id, request, admin, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("User role updated", resp));
    }

    @GetMapping("/incidents")
    @Operation(summary = "List all platform-wide incidents")
    public ResponseEntity<ApiResponse<PagedResponse<IncidentResponse>>> getAllIncidents(
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Incident> page = incidentRepository.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(page.map(IncidentResponse::fromIncident))));
    }

    @GetMapping("/monitoring")
    @Operation(summary = "Get system-wide monitoring execution telemetry")
    public ResponseEntity<ApiResponse<List<CheckResultResponse>>> getRecentMonitoring(
            @PageableDefault(size = 20, sort = "checkedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CheckResult> page = checkResultRepository.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(page.getContent().stream().map(CheckResultResponse::fromEntity).collect(Collectors.toList())));
    }

    @GetMapping("/notifications")
    @Operation(summary = "Get recent notification delivery failures")
    public ResponseEntity<ApiResponse<List<NotificationLogResponse>>> getFailedNotifications() {
        List<NotificationLogResponse> logs = adminService.getRecentFailedNotifications();
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Search platform audit logs")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<AuditLogResponse> resp = adminService.searchAuditLogs(action, resourceType, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @GetMapping("/settings")
    @Operation(summary = "Get platform configuration settings")
    public ResponseEntity<ApiResponse<List<PlatformSetting>>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getSettings()));
    }

    @PutMapping("/settings")
    @Operation(summary = "Update a platform configuration setting")
    public ResponseEntity<ApiResponse<PlatformSetting>> updateSetting(
            @Valid @RequestBody PlatformSettingRequest request,
            HttpServletRequest httpRequest) {
        User admin = userService.getUserById(SecurityUtils.getCurrentUserId());
        PlatformSetting updated = adminService.updateSetting(request, admin, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Platform setting updated", updated));
    }
}
