package com.pulsewatch.controller;

import com.pulsewatch.dto.ApiResponse;
import com.pulsewatch.dto.NotificationLogResponse;
import com.pulsewatch.dto.PagedResponse;
import com.pulsewatch.entity.NotificationLog;
import com.pulsewatch.entity.Team;
import com.pulsewatch.entity.User;
import com.pulsewatch.exception.ResourceNotFoundException;
import com.pulsewatch.repository.NotificationLogRepository;
import com.pulsewatch.security.SecurityUtils;
import com.pulsewatch.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Notification delivery logs and attempts")
public class NotificationController {

    private final NotificationLogRepository notificationLogRepository;
    private final UserService userService;

    public NotificationController(NotificationLogRepository notificationLogRepository, UserService userService) {
        this.notificationLogRepository = notificationLogRepository;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get notification delivery logs")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationLogResponse>>> getNotificationLogs(
            @PageableDefault(size = 20, sort = "lastAttemptAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        List<Team> teams = userService.getUserTeams(user);
        Page<NotificationLog> page = notificationLogRepository.findAccessibleLogs(user, teams, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(page.map(NotificationLogResponse::fromEntity))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get details of a specific notification log entry")
    public ResponseEntity<ApiResponse<NotificationLogResponse>> getNotificationLog(@PathVariable Long id) {
        NotificationLog log = notificationLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification log not found with id: " + id));
        return ResponseEntity.ok(ApiResponse.success(NotificationLogResponse.fromEntity(log)));
    }
}
