package com.pulsewatch.analytics;

import com.pulsewatch.dto.*;
import com.pulsewatch.entity.*;
import com.pulsewatch.exception.BadRequestException;
import com.pulsewatch.repository.CheckResultRepository;
import com.pulsewatch.repository.IncidentRepository;
import com.pulsewatch.repository.MonitorRepository;
import com.pulsewatch.service.MonitorService;
import com.pulsewatch.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final MonitorRepository monitorRepository;
    private final CheckResultRepository checkResultRepository;
    private final IncidentRepository incidentRepository;
    private final UserService userService;
    private final MonitorService monitorService;

    public AnalyticsService(MonitorRepository monitorRepository,
                            CheckResultRepository checkResultRepository,
                            IncidentRepository incidentRepository,
                            UserService userService,
                            MonitorService monitorService) {
        this.monitorRepository = monitorRepository;
        this.checkResultRepository = checkResultRepository;
        this.incidentRepository = incidentRepository;
        this.userService = userService;
        this.monitorService = monitorService;
    }

    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse getOverview(User user) {
        List<Team> teams = userService.getUserTeams(user);
        List<Monitor> monitors = monitorRepository.findAllAccessibleMonitors(user, teams);

        long totalMonitors = monitors.size();
        long monitorsUp = monitors.stream().filter(m -> m.getCurrentStatus() == MonitorStatus.UP).count();
        long monitorsDown = monitors.stream().filter(m -> m.getCurrentStatus() == MonitorStatus.DOWN).count();
        long monitorsDegraded = monitors.stream().filter(m -> m.getCurrentStatus() == MonitorStatus.DEGRADED).count();
        long monitorsPaused = monitors.stream().filter(m -> m.getCurrentStatus() == MonitorStatus.PAUSED).count();

        Instant since24h = Instant.now().minusSeconds(86400);

        long totalChecks = 0;
        long totalSuccess = 0;
        long totalLatencySum = 0;
        long latencyCount = 0;

        for (Monitor m : monitors) {
            long checks = checkResultRepository.countChecksSince(m, since24h);
            totalChecks += checks;
            long success = checkResultRepository.countSuccessfulChecksSince(m, since24h);
            totalSuccess += success;

            Double avgLatency = checkResultRepository.getAverageResponseTimeSince(m, since24h);
            if (avgLatency != null && success > 0) {
                totalLatencySum += (long) (avgLatency * success);
                latencyCount += success;
            }
        }

        double overallUptime = totalChecks > 0 ? ((double) totalSuccess / totalChecks) * 100.0 : 0.0;
        long avgLatency = latencyCount > 0 ? (totalLatencySum / latencyCount) : 0;

        AnalyticsOverviewResponse resp = new AnalyticsOverviewResponse();
        resp.setTotalMonitors(totalMonitors);
        resp.setMonitorsUp(monitorsUp);
        resp.setMonitorsDown(monitorsDown);
        resp.setMonitorsDegraded(monitorsDegraded);
        resp.setMonitorsPaused(monitorsPaused);
        resp.setOverallUptimePercent(Math.round(overallUptime * 100.0) / 100.0);
        resp.setAvgResponseTimeMs(avgLatency);
        resp.setTotalChecksCount(totalChecks);

        // Incidents
        long openCount = incidentRepository.countAccessibleByStatus(user, teams, IncidentStatus.OPEN);
        long ackCount = incidentRepository.countAccessibleByStatus(user, teams, IncidentStatus.ACKNOWLEDGED);
        resp.setActiveIncidentsCount(openCount + ackCount);

        List<Incident> topIncidents = incidentRepository.findTopAccessibleIncidents(user, teams, PageRequest.of(0, 5));
        resp.setRecentIncidents(topIncidents.stream().map(IncidentResponse::fromIncident).collect(Collectors.toList()));

        List<CheckResult> failures;
        if (monitors.isEmpty()) {
            failures = Collections.emptyList();
        } else {
            failures = checkResultRepository.findRecentFailedChecksByMonitors(monitors, PageRequest.of(0, 5));
        }
        resp.setRecentFailures(failures.stream().map(CheckResultResponse::fromEntity).collect(Collectors.toList()));

        resp.setTopMonitors(monitors.stream().limit(5).map(monitorService::enrichMonitorResponseFromService).collect(Collectors.toList()));

        return resp;
    }

    @Transactional(readOnly = true)
    public UptimeStatsResponse getUptimeStats(Long monitorId, int days, User user) {
        if (days <= 0) days = 1;
        Instant since = Instant.now().minusSeconds((long) days * 86400L);

        List<CheckResult> results;
        if (monitorId != null) {
            Monitor monitor = monitorService.getMonitorEntity(monitorId, user);
            results = checkResultRepository.findRecentResults(monitor.getId(), since);
        } else {
            List<Team> teams = userService.getUserTeams(user);
            List<Monitor> monitors = monitorRepository.findAllAccessibleMonitors(user, teams);
            results = new ArrayList<>();
            for (Monitor m : monitors) {
                results.addAll(checkResultRepository.findRecentResults(m.getId(), since));
            }
        }

        long totalChecks = results.size();
        long successful = results.stream().filter(CheckResult::isSuccess).count();
        long failed = totalChecks - successful;
        double uptimePercent = totalChecks > 0 ? ((double) successful / totalChecks) * 100.0 : 0.0;

        if (totalChecks == 0) {
            UptimeStatsResponse resp = new UptimeStatsResponse();
            resp.setUptimePercent(0.0);
            resp.setTotalChecks(0);
            resp.setSuccessfulChecks(0);
            resp.setFailedChecks(0);
            resp.setDowntimeDurationSeconds(0);
            resp.setTrend(Collections.emptyList());
            return resp;
        }

        // Bucket into trend points (e.g. 24 buckets)
        int bucketCount = 24;
        long windowMillis = (long) days * 86400_000L;
        long bucketMillis = Math.max(1, windowMillis / bucketCount);
        long startTime = since.toEpochMilli();

        List<UptimeStatsResponse.TimePoint> trend = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneOffset.UTC);

        for (int i = 0; i < bucketCount; i++) {
            long bStart = startTime + (i * bucketMillis);
            long bEnd = bStart + bucketMillis;
            List<CheckResult> bucketResults = results.stream()
                    .filter(r -> r.getCheckedAt().toEpochMilli() >= bStart && r.getCheckedAt().toEpochMilli() < bEnd)
                    .collect(Collectors.toList());

            if (!bucketResults.isEmpty()) {
                long succ = bucketResults.stream().filter(CheckResult::isSuccess).count();
                double bucketUptime = ((double) succ / bucketResults.size()) * 100.0;
                trend.add(new UptimeStatsResponse.TimePoint(dtf.format(Instant.ofEpochMilli(bStart)), Math.round(bucketUptime * 10.0) / 10.0));
            }
        }

        UptimeStatsResponse resp = new UptimeStatsResponse();
        resp.setUptimePercent(Math.round(uptimePercent * 100.0) / 100.0);
        resp.setTotalChecks(totalChecks);
        resp.setSuccessfulChecks(successful);
        resp.setFailedChecks(failed);
        resp.setDowntimeDurationSeconds(failed * 60L); // Estimate based on check interval
        resp.setTrend(trend);
        return resp;
    }

    @Transactional(readOnly = true)
    public ResponseTimeStatsResponse getResponseTimeStats(Long monitorId, int days, User user) {
        if (days <= 0) days = 1;
        Instant since = Instant.now().minusSeconds((long) days * 86400L);

        List<CheckResult> results;
        if (monitorId != null) {
            Monitor monitor = monitorService.getMonitorEntity(monitorId, user);
            results = checkResultRepository.findRecentResults(monitor.getId(), since);
        } else {
            List<Team> teams = userService.getUserTeams(user);
            List<Monitor> monitors = monitorRepository.findAllAccessibleMonitors(user, teams);
            results = new ArrayList<>();
            for (Monitor m : monitors) {
                results.addAll(checkResultRepository.findRecentResults(m.getId(), since));
            }
        }

        List<Long> latencies = results.stream()
                .filter(CheckResult::isSuccess)
                .map(CheckResult::getResponseTimeMs)
                .sorted()
                .collect(Collectors.toList());

        ResponseTimeStatsResponse resp = new ResponseTimeStatsResponse();
        if (latencies.isEmpty()) {
            resp.setAvgMs(0);
            resp.setMedianMs(0);
            resp.setP95Ms(0);
            resp.setMinMs(0);
            resp.setMaxMs(0);
            resp.setTrend(Collections.emptyList());
            return resp;
        }

        long sum = latencies.stream().mapToLong(Long::longValue).sum();
        long avg = sum / latencies.size();
        long min = latencies.get(0);
        long max = latencies.get(latencies.size() - 1);
        long median = latencies.get(latencies.size() / 2);
        int p95Index = (int) Math.min(latencies.size() - 1, Math.round(latencies.size() * 0.95));
        long p95 = latencies.get(p95Index);

        resp.setAvgMs(avg);
        resp.setMinMs(min);
        resp.setMaxMs(max);
        resp.setMedianMs(median);
        resp.setP95Ms(p95);

        // Trend points
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC);
        List<ResponseTimeStatsResponse.LatencyPoint> trend = results.stream()
                .filter(CheckResult::isSuccess)
                .limit(50)
                .map(r -> new ResponseTimeStatsResponse.LatencyPoint(dtf.format(r.getCheckedAt()), r.getResponseTimeMs()))
                .collect(Collectors.toList());
        resp.setTrend(trend);

        return resp;
    }

    @Transactional(readOnly = true)
    public String exportCsv(Long monitorId, int days, User user) {
        if (days <= 0) days = 7;
        Instant since = Instant.now().minusSeconds((long) days * 86400L);

        List<CheckResult> results;
        if (monitorId != null) {
            Monitor monitor = monitorService.getMonitorEntity(monitorId, user);
            results = checkResultRepository.findRecentResults(monitor.getId(), since);
        } else {
            List<Team> teams = userService.getUserTeams(user);
            List<Monitor> monitors = monitorRepository.findAllAccessibleMonitors(user, teams);
            results = new ArrayList<>();
            for (Monitor m : monitors) {
                results.addAll(checkResultRepository.findRecentResults(m.getId(), since));
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Timestamp,MonitorName,Success,ResponseTimeMs,StatusCode,ErrorType,ErrorMessage\n");

        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
        for (CheckResult r : results) {
            sb.append(dtf.format(r.getCheckedAt())).append(",");
            sb.append("\"").append(r.getMonitor().getName().replace("\"", "\"\"")).append("\",");
            sb.append(r.isSuccess()).append(",");
            sb.append(r.getResponseTimeMs()).append(",");
            sb.append(r.getStatusCode() != null ? r.getStatusCode() : "").append(",");
            sb.append("\"").append(r.getErrorType() != null ? r.getErrorType() : "").append("\",");
            sb.append("\"").append(r.getErrorMessage() != null ? r.getErrorMessage().replace("\"", "\"\"") : "").append("\"\n");
        }

        return sb.toString();
    }
}
