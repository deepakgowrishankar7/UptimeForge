package com.pulsewatch.incident;

import com.pulsewatch.audit.AuditLogService;
import com.pulsewatch.dto.IncidentEventResponse;
import com.pulsewatch.dto.IncidentNoteRequest;
import com.pulsewatch.dto.IncidentNoteResponse;
import com.pulsewatch.dto.IncidentResolveRequest;
import com.pulsewatch.dto.IncidentResponse;
import com.pulsewatch.dto.PagedResponse;
import com.pulsewatch.entity.*;
import com.pulsewatch.exception.BadRequestException;
import com.pulsewatch.exception.ResourceNotFoundException;
import com.pulsewatch.monitoring.CheckExecutionResult;
import com.pulsewatch.notification.NotificationService;
import com.pulsewatch.repository.IncidentEventRepository;
import com.pulsewatch.repository.IncidentNoteRepository;
import com.pulsewatch.repository.IncidentRepository;
import com.pulsewatch.repository.MaintenanceWindowRepository;
import com.pulsewatch.repository.MonitorRepository;
import com.pulsewatch.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final IncidentNoteRepository incidentNoteRepository;
    private final MaintenanceWindowRepository maintenanceWindowRepository;
    private final MonitorRepository monitorRepository;
    private final NotificationService notificationService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public IncidentService(IncidentRepository incidentRepository,
                           IncidentEventRepository incidentEventRepository,
                           IncidentNoteRepository incidentNoteRepository,
                           MaintenanceWindowRepository maintenanceWindowRepository,
                           MonitorRepository monitorRepository,
                           NotificationService notificationService,
                           UserService userService,
                           AuditLogService auditLogService) {
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.incidentNoteRepository = incidentNoteRepository;
        this.maintenanceWindowRepository = maintenanceWindowRepository;
        this.monitorRepository = monitorRepository;
        this.notificationService = notificationService;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void handleMonitorFailure(Monitor monitor, CheckExecutionResult result) {
        // If monitor is in scheduled maintenance window, do not create alerts or incidents
        if (maintenanceWindowRepository.isMonitorInMaintenance(monitor, Instant.now())) {
            log.info("Monitor {} failure ignored because it is currently in a scheduled maintenance window", monitor.getId());
            return;
        }

        // Check if an open incident already exists for this monitor
        Optional<Incident> openIncidentOpt = incidentRepository.findFirstByMonitorAndStatusInOrderByIdDesc(
                monitor,
                Arrays.asList(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED)
        );

        if (openIncidentOpt.isPresent()) {
            // Update last failure time
            Incident incident = openIncidentOpt.get();
            incident.setLastFailureAt(result.getCheckedAt());
            incidentRepository.save(incident);
        } else {
            // Create New Incident
            IncidentSeverity severity = IncidentSeverity.HIGH;
            if (result.getStatusCode() != null && result.getStatusCode() >= 500) {
                severity = IncidentSeverity.CRITICAL;
            }

            Incident incident = new Incident(
                    monitor,
                    monitor.getTeam(),
                    severity,
                    result.getCheckedAt(),
                    Instant.now(),
                    result.getCheckedAt()
            );
            incident = incidentRepository.save(incident);

            // Create Incident Event
            String desc = "Monitor failed health check. Error: " + (result.getErrorMessage() != null ? result.getErrorMessage() : "Target unreachable");
            IncidentEvent event = new IncidentEvent(incident, null, IncidentEventType.DETECTED, desc);
            incidentEventRepository.save(event);

            // Dispatch Alerts
            notificationService.dispatchAlerts(monitor, incident, "DOWN", desc);

            log.warn("New Incident opened for monitor {} (ID: {})", monitor.getName(), incident.getId());
        }
    }

    @Transactional
    public void handleMonitorRecovery(Monitor monitor, CheckExecutionResult result) {
        Optional<Incident> openIncidentOpt = incidentRepository.findFirstByMonitorAndStatusInOrderByIdDesc(
                monitor,
                Arrays.asList(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED)
        );

        if (openIncidentOpt.isPresent()) {
            Incident incident = openIncidentOpt.get();
            incident.setStatus(IncidentStatus.RESOLVED);
            incident.setResolvedAt(Instant.now());
            incident.setResolutionNotes("Auto-resolved: service recovered after meeting recovery threshold");
            incidentRepository.save(incident);

            // Event
            IncidentEvent event = new IncidentEvent(
                    incident,
                    null,
                    IncidentEventType.AUTO_RESOLVED,
                    "Service returned to normal operation. Response time: " + result.getResponseTimeMs() + "ms"
            );
            incidentEventRepository.save(event);

            // Notification
            notificationService.dispatchAlerts(
                    monitor,
                    incident,
                    "RECOVERY",
                    "Service recovered and is now UP. Response time: " + result.getResponseTimeMs() + "ms"
            );

            log.info("Incident {} auto-resolved for monitor {}", incident.getId(), monitor.getName());
        }
    }

    @Transactional
    public IncidentResponse acknowledgeIncident(Long incidentId, User user, String ipAddress) {
        Incident incident = getIncidentEntity(incidentId, user);

        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new BadRequestException("Resolved incidents cannot be acknowledged");
        }

        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        incident.setAcknowledgedBy(user);
        incident.setAcknowledgedAt(Instant.now());
        incident = incidentRepository.save(incident);

        IncidentEvent event = new IncidentEvent(
                incident,
                user,
                IncidentEventType.ACKNOWLEDGED,
                "Incident acknowledged by " + user.getName()
        );
        incidentEventRepository.save(event);

        auditLogService.log(user, "INCIDENT_ACKNOWLEDGED", "INCIDENT", incidentId.toString(), "Incident acknowledged", ipAddress);

        return IncidentResponse.fromIncident(incident);
    }

    @Transactional
    public IncidentResponse resolveIncident(Long incidentId, User user, IncidentResolveRequest request, String ipAddress) {
        Incident incident = getIncidentEntity(incidentId, user);

        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new BadRequestException("Incident is already resolved");
        }

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(Instant.now());
        String notes = request != null && request.getResolutionNotes() != null
                ? request.getResolutionNotes().trim()
                : "Manually resolved by " + user.getName();
        incident.setResolutionNotes(notes);
        incident = incidentRepository.save(incident);

        IncidentEvent event = new IncidentEvent(
                incident,
                user,
                IncidentEventType.RESOLVED,
                "Incident resolved by " + user.getName() + ". Notes: " + notes
        );
        incidentEventRepository.save(event);

        auditLogService.log(user, "INCIDENT_RESOLVED", "INCIDENT", incidentId.toString(), "Incident resolved: " + notes, ipAddress);

        return IncidentResponse.fromIncident(incident);
    }

    @Transactional
    public IncidentNoteResponse addNote(Long incidentId, User user, IncidentNoteRequest request, String ipAddress) {
        Incident incident = getIncidentEntity(incidentId, user);

        IncidentNote note = new IncidentNote(incident, user, request.getNote().trim());
        note = incidentNoteRepository.save(note);

        IncidentEvent event = new IncidentEvent(
                incident,
                user,
                IncidentEventType.NOTE_ADDED,
                user.getName() + " added a note: " + note.getNote()
        );
        incidentEventRepository.save(event);

        auditLogService.log(user, "INCIDENT_NOTE_ADDED", "INCIDENT", incidentId.toString(), "Note added to incident", ipAddress);

        return IncidentNoteResponse.fromEntity(note);
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncident(Long incidentId, User user) {
        Incident incident = getIncidentEntity(incidentId, user);
        return IncidentResponse.fromIncident(incident);
    }

    @Transactional(readOnly = true)
    public List<IncidentEventResponse> getIncidentEvents(Long incidentId, User user) {
        Incident incident = getIncidentEntity(incidentId, user);
        return incidentEventRepository.findByIncidentOrderByCreatedAtAsc(incident)
                .stream()
                .map(IncidentEventResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IncidentNoteResponse> getIncidentNotes(Long incidentId, User user) {
        Incident incident = getIncidentEntity(incidentId, user);
        return incidentNoteRepository.findByIncidentOrderByCreatedAtAsc(incident)
                .stream()
                .map(IncidentNoteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagedResponse<IncidentResponse> getAccessibleIncidents(User user, IncidentStatus status, Pageable pageable) {
        List<Team> teams = userService.getUserTeams(user);
        Page<Incident> page = incidentRepository.findAccessibleIncidents(user, teams, status, pageable);
        return PagedResponse.of(page.map(IncidentResponse::fromIncident));
    }

    @Transactional(readOnly = true)
    public PagedResponse<IncidentResponse> getMonitorIncidents(Long monitorId, User user, Pageable pageable) {
        Monitor monitor = userService.getUserById(user.getId()) != null ? getMonitorByIdAndVerify(monitorId, user) : null;
        Page<Incident> page = incidentRepository.findByMonitorOrderByStartedAtDesc(monitor, pageable);
        return PagedResponse.of(page.map(IncidentResponse::fromIncident));
    }

    private Incident getIncidentEntity(Long incidentId, User user) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with id: " + incidentId));

        userService.verifyMonitorAccess(user, incident.getMonitor());
        return incident;
    }

    private Monitor getMonitorByIdAndVerify(Long monitorId, User user) {
        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found with id: " + monitorId));
        userService.verifyMonitorAccess(user, monitor);
        return monitor;
    }
}
