package com.pulsewatch.service;

import com.pulsewatch.audit.AuditLogService;
import com.pulsewatch.dto.*;
import com.pulsewatch.entity.*;
import com.pulsewatch.exception.BadRequestException;
import com.pulsewatch.exception.ResourceNotFoundException;
import com.pulsewatch.monitoring.CheckExecutionResult;
import com.pulsewatch.monitoring.MonitoringEngine;
import com.pulsewatch.repository.CheckResultRepository;
import com.pulsewatch.repository.MonitorRepository;
import com.pulsewatch.repository.TeamRepository;
import com.pulsewatch.validation.SsrfValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final CheckResultRepository checkResultRepository;
    private final TeamRepository teamRepository;
    private final UserService userService;
    private final MonitoringEngine monitoringEngine;
    private final SsrfValidator ssrfValidator;
    private final AuditLogService auditLogService;

    public MonitorService(MonitorRepository monitorRepository,
                          CheckResultRepository checkResultRepository,
                          TeamRepository teamRepository,
                          UserService userService,
                          MonitoringEngine monitoringEngine,
                          SsrfValidator ssrfValidator,
                          AuditLogService auditLogService) {
        this.monitorRepository = monitorRepository;
        this.checkResultRepository = checkResultRepository;
        this.teamRepository = teamRepository;
        this.userService = userService;
        this.monitoringEngine = monitoringEngine;
        this.ssrfValidator = ssrfValidator;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public MonitorResponse createMonitor(User user, MonitorCreateRequest request, String ipAddress) {
        validateMonitorUrl(request.getUrl(), request.getType());

        Monitor monitor = new Monitor();
        monitor.setOwner(user);
        monitor.setName(request.getName().trim());
        monitor.setDescription(request.getDescription());
        monitor.setUrl(request.getUrl().trim());
        monitor.setType(request.getType() != null ? request.getType() : MonitorType.HTTP);
        monitor.setHttpMethod(request.getHttpMethod() != null ? request.getHttpMethod().toUpperCase() : "GET");
        monitor.setIntervalSeconds(request.getIntervalSeconds() > 0 ? request.getIntervalSeconds() : 60);
        monitor.setTimeoutMs(request.getTimeoutMs() > 0 ? request.getTimeoutMs() : 10000);
        monitor.setExpectedStatusCodes(request.getExpectedStatusCodes() != null ? request.getExpectedStatusCodes() : "200,201,204");
        monitor.setExpectedText(request.getExpectedText());
        monitor.setHeaders(request.getHeaders());
        monitor.setRequestBody(request.getRequestBody());
        monitor.setFailureThreshold(request.getFailureThreshold() > 0 ? request.getFailureThreshold() : 3);
        monitor.setRecoveryThreshold(request.getRecoveryThreshold() > 0 ? request.getRecoveryThreshold() : 2);
        monitor.setEnabled(true);
        monitor.setCurrentStatus(MonitorStatus.UNKNOWN);
        monitor.setNextCheckAt(Instant.now()); // Schedule immediately

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + request.getTeamId()));
            userService.verifyTeamAccess(user, team);
            monitor.setTeam(team);
        }

        monitor = monitorRepository.save(monitor);
        auditLogService.log(user, "CREATE_MONITOR", "MONITOR", monitor.getId().toString(), "Created monitor: " + monitor.getName(), ipAddress);

        return enrichMonitorResponse(monitor);
    }

    @Transactional
    public MonitorResponse updateMonitor(Long id, User user, MonitorUpdateRequest request, String ipAddress) {
        Monitor monitor = getMonitorEntity(id, user);

        validateMonitorUrl(request.getUrl(), request.getType());

        monitor.setName(request.getName().trim());
        monitor.setDescription(request.getDescription());
        monitor.setUrl(request.getUrl().trim());
        monitor.setType(request.getType());
        monitor.setHttpMethod(request.getHttpMethod() != null ? request.getHttpMethod().toUpperCase() : "GET");
        monitor.setIntervalSeconds(request.getIntervalSeconds());
        monitor.setTimeoutMs(request.getTimeoutMs());
        monitor.setExpectedStatusCodes(request.getExpectedStatusCodes());
        monitor.setExpectedText(request.getExpectedText());
        monitor.setHeaders(request.getHeaders());
        monitor.setRequestBody(request.getRequestBody());
        monitor.setFailureThreshold(request.getFailureThreshold());
        monitor.setRecoveryThreshold(request.getRecoveryThreshold());

        if (request.getEnabled() != null) {
            monitor.setEnabled(request.getEnabled());
            if (!monitor.isEnabled()) {
                monitor.setCurrentStatus(MonitorStatus.PAUSED);
            }
        }

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + request.getTeamId()));
            userService.verifyTeamAccess(user, team);
            monitor.setTeam(team);
        } else {
            monitor.setTeam(null);
        }

        monitor = monitorRepository.save(monitor);
        auditLogService.log(user, "UPDATE_MONITOR", "MONITOR", monitor.getId().toString(), "Updated monitor: " + monitor.getName(), ipAddress);

        return enrichMonitorResponse(monitor);
    }

    @Transactional(readOnly = true)
    public MonitorResponse getMonitor(Long id, User user) {
        Monitor monitor = getMonitorEntity(id, user);
        return enrichMonitorResponse(monitor);
    }

    @Transactional(readOnly = true)
    public PagedResponse<MonitorResponse> getAccessibleMonitors(User user, MonitorStatus status, String query, Pageable pageable) {
        List<Team> teams = userService.getUserTeams(user);
        Page<Monitor> page = monitorRepository.findAccessibleMonitors(user, teams, status, query, pageable);
        return PagedResponse.of(page.map(this::enrichMonitorResponse));
    }

    @Transactional
    public void deleteMonitor(Long id, User user, String ipAddress) {
        Monitor monitor = getMonitorEntity(id, user);
        monitorRepository.delete(monitor);
        auditLogService.log(user, "DELETE_MONITOR", "MONITOR", id.toString(), "Deleted monitor: " + monitor.getName(), ipAddress);
    }

    @Transactional
    public MonitorResponse pauseMonitor(Long id, User user, String ipAddress) {
        Monitor monitor = getMonitorEntity(id, user);
        monitor.setEnabled(false);
        monitor.setCurrentStatus(MonitorStatus.PAUSED);
        monitor = monitorRepository.save(monitor);
        auditLogService.log(user, "PAUSE_MONITOR", "MONITOR", id.toString(), "Paused monitor: " + monitor.getName(), ipAddress);
        return enrichMonitorResponse(monitor);
    }

    @Transactional
    public MonitorResponse resumeMonitor(Long id, User user, String ipAddress) {
        Monitor monitor = getMonitorEntity(id, user);
        monitor.setEnabled(true);
        monitor.setCurrentStatus(MonitorStatus.UNKNOWN);
        monitor.setNextCheckAt(Instant.now());
        monitor = monitorRepository.save(monitor);
        auditLogService.log(user, "RESUME_MONITOR", "MONITOR", id.toString(), "Resumed monitor: " + monitor.getName(), ipAddress);
        return enrichMonitorResponse(monitor);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CheckResultResponse> getMonitorResults(Long id, User user, Pageable pageable) {
        Monitor monitor = getMonitorEntity(id, user);
        Page<CheckResult> page = checkResultRepository.findByMonitorOrderByCheckedAtDesc(monitor, pageable);
        return PagedResponse.of(page.map(CheckResultResponse::fromEntity));
    }

    @Transactional
    public TestCheckResponse testAdHocCheck(TestCheckRequest request) {
        validateMonitorUrl(request.getUrl(), request.getType());

        Monitor testMonitor = new Monitor();
        testMonitor.setUrl(request.getUrl().trim());
        testMonitor.setType(request.getType() != null ? request.getType() : MonitorType.HTTP);
        testMonitor.setHttpMethod(request.getHttpMethod() != null ? request.getHttpMethod().toUpperCase() : "GET");
        testMonitor.setTimeoutMs(request.getTimeoutMs() > 0 ? request.getTimeoutMs() : 10000);
        testMonitor.setExpectedStatusCodes(request.getExpectedStatusCodes() != null ? request.getExpectedStatusCodes() : "200,201,204");
        testMonitor.setExpectedText(request.getExpectedText());
        testMonitor.setHeaders(request.getHeaders());
        testMonitor.setRequestBody(request.getRequestBody());

        CheckExecutionResult result = monitoringEngine.executeAdHocCheck(testMonitor);
        return new TestCheckResponse(
                result.isSuccess(),
                result.getResponseTimeMs(),
                result.getStatusCode(),
                result.getErrorType(),
                result.getErrorMessage()
        );
    }

    @Transactional
    public TestCheckResponse testExistingMonitor(Long id, User user) {
        Monitor monitor = getMonitorEntity(id, user);
        CheckResult result = monitoringEngine.executeCheck(monitor);
        return new TestCheckResponse(
                result.isSuccess(),
                result.getResponseTimeMs(),
                result.getStatusCode(),
                result.getErrorType(),
                result.getErrorMessage()
        );
    }

    public Monitor getMonitorEntity(Long id, User user) {
        Monitor monitor = monitorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found with id: " + id));
        userService.verifyMonitorAccess(user, monitor);
        return monitor;
    }

    private void validateMonitorUrl(String url, MonitorType type) {
        if (url == null || url.trim().isEmpty()) {
            throw new BadRequestException("Target URL cannot be empty");
        }
        if (type == MonitorType.TCP) {
            String clean = url.replace("tcp://", "");
            int colonIdx = clean.lastIndexOf(':');
            if (colonIdx > 0) {
                String host = clean.substring(0, colonIdx);
                try {
                    int port = Integer.parseInt(clean.substring(colonIdx + 1));
                    ssrfValidator.validateTcpTarget(host, port);
                } catch (NumberFormatException e) {
                    throw new BadRequestException("Invalid TCP port in target");
                }
            } else {
                ssrfValidator.validateTcpTarget(clean, 80);
            }
        } else {
            ssrfValidator.validateHttpTarget(url);
        }
    }

    public MonitorResponse enrichMonitorResponseFromService(Monitor m) {
        return enrichMonitorResponse(m);
    }

    public MonitorResponse enrichMonitorResponse(Monitor m) {
        MonitorResponse resp = MonitorResponse.fromMonitor(m);

        // Fetch recent check for last latency and calculate 24h uptime
        Instant since24h = Instant.now().minusSeconds(86400);
        long totalChecks = checkResultRepository.countChecksSince(m, since24h);
        if (totalChecks > 0) {
            long successful = checkResultRepository.countSuccessfulChecksSince(m, since24h);
            double uptime = ((double) successful / totalChecks) * 100.0;
            resp.setUptime24h(Math.round(uptime * 100.0) / 100.0);
        } else {
            resp.setUptime24h(m.getCurrentStatus() == MonitorStatus.UP ? 100.0 : 0.0);
        }

        Page<CheckResult> lastResults = checkResultRepository.findByMonitorOrderByCheckedAtDesc(m, PageRequest.of(0, 1));
        if (lastResults.hasContent()) {
            resp.setLastResponseTimeMs(lastResults.getContent().get(0).getResponseTimeMs());
        }

        return resp;
    }
}
