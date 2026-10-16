package com.pulsewatch.statuspage;

import com.pulsewatch.audit.AuditLogService;
import com.pulsewatch.dto.*;
import com.pulsewatch.entity.*;
import com.pulsewatch.exception.BadRequestException;
import com.pulsewatch.exception.ResourceNotFoundException;
import com.pulsewatch.repository.*;
import com.pulsewatch.service.MonitorService;
import com.pulsewatch.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatusPageService {

    private final StatusPageRepository statusPageRepository;
    private final StatusPageMonitorRepository statusPageMonitorRepository;
    private final StatusAnnouncementRepository statusAnnouncementRepository;
    private final MonitorRepository monitorRepository;
    private final CheckResultRepository checkResultRepository;
    private final IncidentRepository incidentRepository;
    private final TeamRepository teamRepository;
    private final UserService userService;
    private final MonitorService monitorService;
    private final AuditLogService auditLogService;

    public StatusPageService(StatusPageRepository statusPageRepository,
                             StatusPageMonitorRepository statusPageMonitorRepository,
                             StatusAnnouncementRepository statusAnnouncementRepository,
                             MonitorRepository monitorRepository,
                             CheckResultRepository checkResultRepository,
                             IncidentRepository incidentRepository,
                             TeamRepository teamRepository,
                             UserService userService,
                             MonitorService monitorService,
                             AuditLogService auditLogService) {
        this.statusPageRepository = statusPageRepository;
        this.statusPageMonitorRepository = statusPageMonitorRepository;
        this.statusAnnouncementRepository = statusAnnouncementRepository;
        this.monitorRepository = monitorRepository;
        this.checkResultRepository = checkResultRepository;
        this.incidentRepository = incidentRepository;
        this.teamRepository = teamRepository;
        this.userService = userService;
        this.monitorService = monitorService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public StatusPageResponse createStatusPage(User user, StatusPageRequest request, String ipAddress) {
        String slug = request.getSlug().toLowerCase().trim();
        if (statusPageRepository.existsBySlug(slug)) {
            throw new BadRequestException("Status page with slug '" + slug + "' already exists");
        }

        StatusPage sp = new StatusPage();
        sp.setOwner(user);
        sp.setSlug(slug);
        sp.setTitle(request.getTitle().trim());
        sp.setDescription(request.getDescription());
        sp.setPublished(request.isPublished());

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
            userService.verifyTeamAccess(user, team);
            sp.setTeam(team);
        }

        sp = statusPageRepository.save(sp);

        if (request.getMonitorIds() != null) {
            for (Long mId : request.getMonitorIds()) {
                Monitor m = monitorRepository.findById(mId).orElse(null);
                if (m != null) {
                    userService.verifyMonitorAccess(user, m);
                    statusPageMonitorRepository.save(new StatusPageMonitor(sp, m));
                }
            }
        }

        auditLogService.log(user, "CREATE_STATUS_PAGE", "STATUS_PAGE", sp.getId().toString(), "Created status page: " + sp.getTitle(), ipAddress);

        return enrichStatusPage(sp);
    }

    @Transactional
    public StatusPageResponse updateStatusPage(Long id, User user, StatusPageRequest request, String ipAddress) {
        StatusPage sp = getStatusPageEntity(id, user);

        String newSlug = request.getSlug().toLowerCase().trim();
        if (!sp.getSlug().equalsIgnoreCase(newSlug) && statusPageRepository.existsBySlug(newSlug)) {
            throw new BadRequestException("Slug is already in use");
        }

        sp.setSlug(newSlug);
        sp.setTitle(request.getTitle().trim());
        sp.setDescription(request.getDescription());
        sp.setPublished(request.isPublished());

        // Update assigned monitors
        statusPageMonitorRepository.deleteByStatusPage(sp);
        if (request.getMonitorIds() != null) {
            for (Long mId : request.getMonitorIds()) {
                Monitor m = monitorRepository.findById(mId).orElse(null);
                if (m != null) {
                    userService.verifyMonitorAccess(user, m);
                    statusPageMonitorRepository.save(new StatusPageMonitor(sp, m));
                }
            }
        }

        sp = statusPageRepository.save(sp);
        auditLogService.log(user, "UPDATE_STATUS_PAGE", "STATUS_PAGE", id.toString(), "Updated status page: " + sp.getTitle(), ipAddress);

        return enrichStatusPage(sp);
    }

    @Transactional(readOnly = true)
    public List<StatusPageResponse> getUserStatusPages(User user) {
        List<Team> teams = userService.getUserTeams(user);
        List<StatusPage> pages = statusPageRepository.findAccessibleStatusPages(user, teams);
        return pages.stream().map(this::enrichStatusPage).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StatusPageResponse getStatusPage(Long id, User user) {
        StatusPage sp = getStatusPageEntity(id, user);
        return enrichStatusPage(sp);
    }

    @Transactional
    public void deleteStatusPage(Long id, User user, String ipAddress) {
        StatusPage sp = getStatusPageEntity(id, user);
        statusPageRepository.delete(sp);
        auditLogService.log(user, "DELETE_STATUS_PAGE", "STATUS_PAGE", id.toString(), "Deleted status page", ipAddress);
    }

    @Transactional
    public StatusPageResponse publish(Long id, boolean published, User user, String ipAddress) {
        StatusPage sp = getStatusPageEntity(id, user);
        sp.setPublished(published);
        sp = statusPageRepository.save(sp);
        auditLogService.log(user, published ? "PUBLISH_STATUS_PAGE" : "UNPUBLISH_STATUS_PAGE", "STATUS_PAGE", id.toString(), "Published status toggled to " + published, ipAddress);
        return enrichStatusPage(sp);
    }

    @Transactional
    public StatusAnnouncementResponse addAnnouncement(Long statusPageId, User user, StatusAnnouncementRequest request, String ipAddress) {
        StatusPage sp = getStatusPageEntity(statusPageId, user);

        StatusAnnouncement announcement = new StatusAnnouncement(
                sp,
                request.getTitle().trim(),
                request.getMessage().trim(),
                request.getStartsAt() != null ? request.getStartsAt() : Instant.now(),
                request.getEndsAt()
        );
        announcement = statusAnnouncementRepository.save(announcement);

        auditLogService.log(user, "ADD_ANNOUNCEMENT", "STATUS_PAGE", statusPageId.toString(), "Added announcement: " + announcement.getTitle(), ipAddress);

        return StatusAnnouncementResponse.fromEntity(announcement);
    }

    @Transactional(readOnly = true)
    public List<StatusAnnouncementResponse> getAnnouncements(Long statusPageId, User user) {
        StatusPage sp = getStatusPageEntity(statusPageId, user);
        return statusAnnouncementRepository.findByStatusPageOrderByStartsAtDesc(sp)
                .stream()
                .map(StatusAnnouncementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Public Read-Only View of Status Page (Zero leakage of private URLs, headers, or tokens)
     */
    @Transactional(readOnly = true)
    public PublicStatusResponse getPublicStatus(String slug) {
        StatusPage sp = statusPageRepository.findBySlugAndPublishedTrue(slug.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found or is currently unpublished"));

        List<StatusPageMonitor> spms = statusPageMonitorRepository.findByStatusPage(sp);
        List<PublicMonitorStatus> publicServices = new ArrayList<>();

        boolean anyDown = false;
        boolean anyDegraded = false;
        double uptimeSum = 0;
        int uptimeCount = 0;

        Instant since24h = Instant.now().minusSeconds(86400);

        for (StatusPageMonitor spm : spms) {
            Monitor m = spm.getMonitor();
            if (!m.isEnabled()) continue;

            if (m.getCurrentStatus() == MonitorStatus.DOWN) anyDown = true;
            if (m.getCurrentStatus() == MonitorStatus.DEGRADED) anyDegraded = true;

            long totalChecks = checkResultRepository.countChecksSince(m, since24h);
            double uptime = 100.0;
            if (totalChecks > 0) {
                long successful = checkResultRepository.countSuccessfulChecksSince(m, since24h);
                uptime = ((double) successful / totalChecks) * 100.0;
            }
            uptimeSum += uptime;
            uptimeCount++;

            Long lastLatency = null;
            var lastResultPage = checkResultRepository.findByMonitorOrderByCheckedAtDesc(m, org.springframework.data.domain.PageRequest.of(0, 1));
            if (lastResultPage.hasContent()) {
                lastLatency = lastResultPage.getContent().get(0).getResponseTimeMs();
            }

            publicServices.add(new PublicMonitorStatus(
                    m.getName(),
                    m.getCurrentStatus(),
                    Math.round(uptime * 10.0) / 10.0,
                    lastLatency,
                    m.getType() == MonitorType.TCP ? "Network TCP" : "Web Service"
            ));
        }

        String overallStatus = "ALL_OPERATIONAL";
        if (anyDown) {
            overallStatus = "MAJOR_OUTAGE";
        } else if (anyDegraded) {
            overallStatus = "PARTIAL_OUTAGE";
        }

        double overallUptime = uptimeCount > 0 ? (uptimeSum / uptimeCount) : 100.0;

        // Announcements
        List<StatusAnnouncementResponse> announcements = statusAnnouncementRepository.findByStatusPageOrderByStartsAtDesc(sp)
                .stream()
                .filter(StatusAnnouncement::isActiveNow)
                .map(StatusAnnouncementResponse::fromEntity)
                .collect(Collectors.toList());

        // Recent Incidents (Public summary only)
        List<PublicStatusResponse.PublicIncidentSummary> recentIncidents = new ArrayList<>();
        for (StatusPageMonitor spm : spms) {
            List<Incident> incs = incidentRepository.findByMonitorOrderByStartedAtDesc(spm.getMonitor());
            for (Incident inc : incs.stream().limit(3).collect(Collectors.toList())) {
                String dur = "Ongoing";
                if (inc.getResolvedAt() != null) {
                    dur = Duration.between(inc.getStartedAt(), inc.getResolvedAt()).toMinutes() + " mins";
                }
                recentIncidents.add(new PublicStatusResponse.PublicIncidentSummary(
                        spm.getMonitor().getName() + " Outage",
                        inc.getStatus().name(),
                        inc.getSeverity().name(),
                        inc.getStartedAt(),
                        inc.getResolvedAt(),
                        dur
                ));
            }
        }

        PublicStatusResponse resp = new PublicStatusResponse();
        resp.setTitle(sp.getTitle());
        resp.setDescription(sp.getDescription());
        resp.setOverallStatus(overallStatus);
        resp.setOverallUptimePercent(Math.round(overallUptime * 100.0) / 100.0);
        resp.setServices(publicServices);
        resp.setActiveAnnouncements(announcements);
        resp.setRecentIncidents(recentIncidents);

        return resp;
    }

    private StatusPage getStatusPageEntity(Long id, User user) {
        StatusPage sp = statusPageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found with id: " + id));

        if (user.getRole() == Role.ADMIN) return sp;

        if (sp.getOwner().getId().equals(user.getId())) return sp;

        if (sp.getTeam() != null) {
            userService.verifyTeamAccess(user, sp.getTeam());
            return sp;
        }

        throw new BadRequestException("You do not have access to this status page");
    }

    private StatusPageResponse enrichStatusPage(StatusPage sp) {
        List<StatusPageMonitor> spms = statusPageMonitorRepository.findByStatusPage(sp);
        List<MonitorResponse> monitors = spms.stream()
                .map(spm -> monitorService.enrichMonitorResponseFromService(spm.getMonitor()))
                .collect(Collectors.toList());

        return StatusPageResponse.fromEntity(sp, monitors);
    }
}
