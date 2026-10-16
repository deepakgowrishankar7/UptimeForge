package com.pulsewatch.monitoring;

import com.pulsewatch.entity.CheckResult;
import com.pulsewatch.entity.Monitor;
import com.pulsewatch.entity.MonitorStatus;
import com.pulsewatch.entity.MonitorType;
import com.pulsewatch.incident.IncidentService;
import com.pulsewatch.repository.CheckResultRepository;
import com.pulsewatch.repository.MonitorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class MonitoringEngine {

    private static final Logger log = LoggerFactory.getLogger(MonitoringEngine.class);

    private final HttpHealthChecker httpHealthChecker;
    private final TcpHealthChecker tcpHealthChecker;
    private final CheckResultRepository checkResultRepository;
    private final MonitorRepository monitorRepository;
    private final IncidentService incidentService;

    public MonitoringEngine(HttpHealthChecker httpHealthChecker,
                            TcpHealthChecker tcpHealthChecker,
                            CheckResultRepository checkResultRepository,
                            MonitorRepository monitorRepository,
                            IncidentService incidentService) {
        this.httpHealthChecker = httpHealthChecker;
        this.tcpHealthChecker = tcpHealthChecker;
        this.checkResultRepository = checkResultRepository;
        this.monitorRepository = monitorRepository;
        this.incidentService = incidentService;
    }

    @Transactional
    public CheckResult executeCheck(Monitor monitor) {
        CheckExecutionResult result;

        try {
            if (monitor.getType() == MonitorType.TCP) {
                result = tcpHealthChecker.check(monitor);
            } else {
                result = httpHealthChecker.check(monitor);
            }
        } catch (Exception e) {
            log.error("Monitoring engine failed to execute check for monitor ID {}", monitor.getId(), e);
            result = CheckExecutionResult.failure(0, null, "ENGINE_ERROR", "Internal execution error: " + e.getMessage(), Instant.now(), Instant.now());
        }

        // 1. Persist CheckResult
        CheckResult checkResult = new CheckResult(
                monitor,
                result.getCheckedAt(),
                result.getCompletedAt(),
                result.isSuccess(),
                result.getResponseTimeMs(),
                result.getStatusCode(),
                result.getErrorType(),
                result.getErrorMessage()
        );
        checkResult = checkResultRepository.save(checkResult);

        // 2. State Transition and Threshold Evaluation
        monitor.setLastCheckAt(result.getCheckedAt());
        int interval = monitor.getIntervalSeconds() > 0 ? monitor.getIntervalSeconds() : 60;
        monitor.setNextCheckAt(Instant.now().plusSeconds(interval));

        if (result.isSuccess()) {
            monitor.setConsecutiveSuccesses(monitor.getConsecutiveSuccesses() + 1);
            monitor.setConsecutiveFailures(0);

            if (monitor.getCurrentStatus() == MonitorStatus.DOWN || monitor.getCurrentStatus() == MonitorStatus.DEGRADED) {
                if (monitor.getConsecutiveSuccesses() >= monitor.getRecoveryThreshold()) {
                    monitor.setCurrentStatus(MonitorStatus.UP);
                    incidentService.handleMonitorRecovery(monitor, result);
                } else {
                    monitor.setCurrentStatus(MonitorStatus.DEGRADED);
                }
            } else if (monitor.getCurrentStatus() == MonitorStatus.UNKNOWN) {
                monitor.setCurrentStatus(MonitorStatus.UP);
            }
        } else {
            monitor.setConsecutiveFailures(monitor.getConsecutiveFailures() + 1);
            monitor.setConsecutiveSuccesses(0);

            if (monitor.getConsecutiveFailures() >= monitor.getFailureThreshold()) {
                monitor.setCurrentStatus(MonitorStatus.DOWN);
                incidentService.handleMonitorFailure(monitor, result);
            } else {
                monitor.setCurrentStatus(MonitorStatus.DEGRADED);
            }
        }

        monitorRepository.save(monitor);
        return checkResult;
    }

    public CheckExecutionResult executeAdHocCheck(Monitor monitor) {
        if (monitor.getType() == MonitorType.TCP) {
            return tcpHealthChecker.check(monitor);
        } else {
            return httpHealthChecker.check(monitor);
        }
    }
}
