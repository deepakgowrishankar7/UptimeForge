package com.pulsewatch.controller;

import com.pulsewatch.audit.AuditLogService;
import com.pulsewatch.dto.AlertConfigRequest;
import com.pulsewatch.dto.AlertConfigResponse;
import com.pulsewatch.dto.ApiResponse;
import com.pulsewatch.entity.AlertConfig;
import com.pulsewatch.entity.NotificationLog;
import com.pulsewatch.entity.Team;
import com.pulsewatch.entity.User;
import com.pulsewatch.exception.BadRequestException;
import com.pulsewatch.exception.ResourceNotFoundException;
import com.pulsewatch.notification.NotificationService;
import com.pulsewatch.repository.AlertConfigRepository;
import com.pulsewatch.repository.TeamRepository;
import com.pulsewatch.security.SecurityUtils;
import com.pulsewatch.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Alert configurations and destinations (Email, Webhook, Slack)")
public class AlertController {

    private final AlertConfigRepository alertConfigRepository;
    private final TeamRepository teamRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public AlertController(AlertConfigRepository alertConfigRepository,
                           TeamRepository teamRepository,
                           UserService userService,
                           NotificationService notificationService,
                           AuditLogService auditLogService) {
        this.alertConfigRepository = alertConfigRepository;
        this.teamRepository = teamRepository;
        this.userService = userService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Operation(summary = "List all alert channels and configurations for current user")
    public ResponseEntity<ApiResponse<List<AlertConfigResponse>>> getAlerts() {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        List<Team> teams = userService.getUserTeams(user);
        List<AlertConfig> configs = alertConfigRepository.findAccessibleConfigs(user, teams);
        return ResponseEntity.ok(ApiResponse.success(configs.stream().map(AlertConfigResponse::fromEntity).collect(Collectors.toList())));
    }

    @PostMapping
    @Operation(summary = "Create an alert configuration channel")
    public ResponseEntity<ApiResponse<AlertConfigResponse>> createAlert(@Valid @RequestBody AlertConfigRequest request, HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());

        AlertConfig config = new AlertConfig();
        config.setOwner(user);
        config.setChannel(request.getChannel());
        config.setDestination(request.getDestination().trim());
        config.setEnabled(request.isEnabled());
        config.setFailureThreshold(request.getFailureThreshold() > 0 ? request.getFailureThreshold() : 3);
        config.setCooldownSeconds(request.getCooldownSeconds() >= 0 ? request.getCooldownSeconds() : 900);
        config.setNotifyOnRecovery(request.isNotifyOnRecovery());

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
            userService.verifyTeamAccess(user, team);
            config.setTeam(team);
        }

        config = alertConfigRepository.save(config);
        auditLogService.log(user, "CREATE_ALERT_CONFIG", "ALERT_CONFIG", config.getId().toString(), "Created " + config.getChannel() + " alert to " + config.getDestination(), httpRequest.getRemoteAddr());

        return ResponseEntity.ok(ApiResponse.success("Alert channel created", AlertConfigResponse.fromEntity(config)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing alert configuration")
    public ResponseEntity<ApiResponse<AlertConfigResponse>> updateAlert(
            @PathVariable Long id,
            @Valid @RequestBody AlertConfigRequest request,
            HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        AlertConfig config = getAlertConfigEntity(id, user);

        config.setChannel(request.getChannel());
        config.setDestination(request.getDestination().trim());
        config.setEnabled(request.isEnabled());
        config.setFailureThreshold(request.getFailureThreshold());
        config.setCooldownSeconds(request.getCooldownSeconds());
        config.setNotifyOnRecovery(request.isNotifyOnRecovery());

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
            userService.verifyTeamAccess(user, team);
            config.setTeam(team);
        } else {
            config.setTeam(null);
        }

        config = alertConfigRepository.save(config);
        auditLogService.log(user, "UPDATE_ALERT_CONFIG", "ALERT_CONFIG", id.toString(), "Updated " + config.getChannel() + " alert to " + config.getDestination(), httpRequest.getRemoteAddr());

        return ResponseEntity.ok(ApiResponse.success("Alert channel updated", AlertConfigResponse.fromEntity(config)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an alert configuration")
    public ResponseEntity<ApiResponse<Void>> deleteAlert(@PathVariable Long id, HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        AlertConfig config = getAlertConfigEntity(id, user);
        alertConfigRepository.delete(config);
        auditLogService.log(user, "DELETE_ALERT_CONFIG", "ALERT_CONFIG", id.toString(), "Deleted alert configuration", httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Alert channel deleted", null));
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "Send a test alert through this channel")
    public ResponseEntity<ApiResponse<Void>> testAlert(@PathVariable Long id) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        AlertConfig config = getAlertConfigEntity(id, user);

        NotificationLog logEntry = notificationService.sendNotification(
                config,
                null,
                null,
                "TEST_ALERT",
                "This is a test notification from PulseWatch verifying your alert channel configuration."
        );

        if (logEntry.getDeliveryStatus() == com.pulsewatch.entity.DeliveryStatus.FAILED) {
            throw new BadRequestException("Test notification failed: " + logEntry.getErrorMessage());
        }

        return ResponseEntity.ok(ApiResponse.success("Test notification dispatched successfully", null));
    }

    private AlertConfig getAlertConfigEntity(Long id, User user) {
        AlertConfig config = alertConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert config not found with id: " + id));

        if (user.getRole() == com.pulsewatch.entity.Role.ADMIN) return config;

        if (config.getOwner().getId().equals(user.getId())) return config;

        if (config.getTeam() != null) {
            userService.verifyTeamAccess(user, config.getTeam());
            return config;
        }

        throw new BadRequestException("Access denied to this alert configuration");
    }
}
