package com.pulsewatch.scheduler;

import com.pulsewatch.entity.Monitor;
import com.pulsewatch.monitoring.MonitoringEngine;
import com.pulsewatch.repository.MonitorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Component
public class MonitoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonitoringScheduler.class);

    private final MonitorRepository monitorRepository;
    private final MonitoringEngine monitoringEngine;
    private final Executor monitoringExecutor;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // In-memory set to prevent duplicate concurrent checks on single-instance
    private final Set<Long> inFlightMonitors = ConcurrentHashMap.newKeySet();

    public MonitoringScheduler(MonitorRepository monitorRepository,
                               MonitoringEngine monitoringEngine,
                               @Qualifier("monitoringExecutor") Executor monitoringExecutor) {
        this.monitorRepository = monitorRepository;
        this.monitoringEngine = monitoringEngine;
        this.monitoringExecutor = monitoringExecutor;
    }

    @Scheduled(fixedDelayString = "${pulsewatch.monitoring.scheduler-fixed-delay-ms:5000}", initialDelay = 3000)
    public void runScheduledChecks() {
        try {
            Instant now = Instant.now();
            List<Monitor> dueMonitors = monitorRepository.findDueMonitors(now);

            for (Monitor monitor : dueMonitors) {
                Long monitorId = monitor.getId();

                if (!tryAcquireLock(monitorId)) {
                    continue; // Already being checked
                }

                monitoringExecutor.execute(() -> {
                    try {
                        monitoringEngine.executeCheck(monitor);
                    } catch (Exception e) {
                        log.error("Unhandled error while running check for monitor {}", monitorId, e);
                    } finally {
                        releaseLock(monitorId);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Error in scheduled monitoring tick", e);
        }
    }

    private boolean tryAcquireLock(Long monitorId) {
        if (redisTemplate != null) {
            try {
                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent("lock:monitor:" + monitorId, "running", Duration.ofSeconds(30));
                return Boolean.TRUE.equals(acquired);
            } catch (Exception e) {
                log.warn("Redis lock unavailable, using memory lock: {}", e.getMessage());
            }
        }
        return inFlightMonitors.add(monitorId);
    }

    private void releaseLock(Long monitorId) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete("lock:monitor:" + monitorId);
            } catch (Exception ignored) {}
        }
        inFlightMonitors.remove(monitorId);
    }
}
