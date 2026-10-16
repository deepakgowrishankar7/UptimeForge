package com.pulsewatch.admin;

import com.pulsewatch.audit.AuditLogService;
import com.pulsewatch.dto.*;
import com.pulsewatch.entity.*;
import com.pulsewatch.exception.BadRequestException;
import com.pulsewatch.exception.ResourceNotFoundException;
import com.pulsewatch.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final MonitorRepository monitorRepository;
    private final IncidentRepository incidentRepository;
    private final CheckResultRepository checkResultRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final PlatformSettingRepository platformSettingRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogService auditLogService;

    public AdminService(UserRepository userRepository,
                        MonitorRepository monitorRepository,
                        IncidentRepository incidentRepository,
                        CheckResultRepository checkResultRepository,
                        NotificationLogRepository notificationLogRepository,
                        AuditLogRepository auditLogRepository,
                        PlatformSettingRepository platformSettingRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.monitorRepository = monitorRepository;
        this.incidentRepository = incidentRepository;
        this.checkResultRepository = checkResultRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.auditLogRepository = auditLogRepository;
        this.platformSettingRepository = platformSettingRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public AdminOverviewResponse getOverview() {
        AdminOverviewResponse resp = new AdminOverviewResponse();
        resp.setTotalUsers(userRepository.count());
        resp.setActiveUsers(userRepository.countByEnabledTrue());
        resp.setTotalMonitors(monitorRepository.count());
        resp.setEnabledMonitors(monitorRepository.countByEnabledTrue());
        resp.setOpenIncidents(incidentRepository.countByStatus(IncidentStatus.OPEN));
        resp.setTotalCheckResults(checkResultRepository.count());
        resp.setFailedNotifications(notificationLogRepository.countByDeliveryStatus(DeliveryStatus.FAILED));

        Page<User> recentUsers = userRepository.findAll(PageRequest.of(0, 5));
        resp.setRecentUsers(recentUsers.getContent().stream().map(UserProfileResponse::fromUser).collect(Collectors.toList()));

        Page<AuditLog> recentLogs = auditLogRepository.findAll(PageRequest.of(0, 5));
        resp.setRecentAuditLogs(recentLogs.getContent().stream().map(AuditLogResponse::fromEntity).collect(Collectors.toList()));

        return resp;
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserProfileResponse> getUsers(String query, Pageable pageable) {
        Page<User> page;
        if (query != null && !query.trim().isEmpty()) {
            page = userRepository.searchUsers(query.trim(), pageable);
        } else {
            page = userRepository.findAll(pageable);
        }
        return PagedResponse.of(page.map(UserProfileResponse::fromUser));
    }

    @Transactional
    public UserProfileResponse updateUserStatus(Long userId, UserStatusUpdateRequest request, User admin, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getId().equals(admin.getId())) {
            throw new BadRequestException("You cannot disable your own administrator account");
        }

        user.setEnabled(request.getEnabled());
        user = userRepository.save(user);

        if (!user.isEnabled()) {
            refreshTokenRepository.revokeAllUserTokens(user, Instant.now());
        }

        auditLogService.log(admin, "ADMIN_USER_STATUS_CHANGE", "USER", userId.toString(), "Set enabled to " + request.getEnabled(), ipAddress);

        return UserProfileResponse.fromUser(user);
    }

    @Transactional
    public UserProfileResponse updateUserRole(Long userId, UserRoleUpdateRequest request, User admin, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getId().equals(admin.getId()) && request.getRole() != Role.ADMIN) {
            throw new BadRequestException("You cannot remove your own admin privileges");
        }

        user.setRole(request.getRole());
        user = userRepository.save(user);

        auditLogService.log(admin, "ADMIN_USER_ROLE_CHANGE", "USER", userId.toString(), "Changed role to " + request.getRole(), ipAddress);

        return UserProfileResponse.fromUser(user);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> searchAuditLogs(String action, String resourceType, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.searchLogs(action, resourceType, pageable);
        return PagedResponse.of(page.map(AuditLogResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public List<PlatformSetting> getSettings() {
        return platformSettingRepository.findAll();
    }

    @Transactional
    public PlatformSetting updateSetting(PlatformSettingRequest request, User admin, String ipAddress) {
        PlatformSetting setting = platformSettingRepository.findBySettingKey(request.getSettingKey())
                .orElse(new PlatformSetting(request.getSettingKey(), request.getSettingValue()));

        setting.setSettingValue(request.getSettingValue());
        setting = platformSettingRepository.save(setting);

        auditLogService.log(admin, "ADMIN_SETTING_UPDATE", "SETTING", setting.getSettingKey(), "Updated setting value", ipAddress);

        return setting;
    }

    @Transactional(readOnly = true)
    public List<NotificationLogResponse> getRecentFailedNotifications() {
        return notificationLogRepository.findRecentByStatus(DeliveryStatus.FAILED, PageRequest.of(0, 20))
                .stream()
                .map(NotificationLogResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
