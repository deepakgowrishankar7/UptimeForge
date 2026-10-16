package com.pulsewatch.repository;

import com.pulsewatch.entity.CheckResult;
import com.pulsewatch.entity.Monitor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {

    Page<CheckResult> findByMonitorOrderByCheckedAtDesc(Monitor monitor, Pageable pageable);

    List<CheckResult> findByMonitorAndCheckedAtBetweenOrderByCheckedAtAsc(Monitor monitor, Instant start, Instant end);

    @Query("SELECT cr FROM CheckResult cr WHERE cr.monitor.id = :monitorId AND cr.checkedAt >= :since ORDER BY cr.checkedAt ASC")
    List<CheckResult> findRecentResults(@Param("monitorId") Long monitorId, @Param("since") Instant since);

    @Query("SELECT COUNT(cr) FROM CheckResult cr WHERE cr.monitor = :monitor AND cr.checkedAt >= :since")
    long countChecksSince(@Param("monitor") Monitor monitor, @Param("since") Instant since);

    @Query("SELECT COUNT(cr) FROM CheckResult cr WHERE cr.monitor = :monitor AND cr.success = true AND cr.checkedAt >= :since")
    long countSuccessfulChecksSince(@Param("monitor") Monitor monitor, @Param("since") Instant since);

    @Query("SELECT AVG(cr.responseTimeMs) FROM CheckResult cr WHERE cr.monitor = :monitor AND cr.success = true AND cr.checkedAt >= :since")
    Double getAverageResponseTimeSince(@Param("monitor") Monitor monitor, @Param("since") Instant since);

    @Query("SELECT cr FROM CheckResult cr WHERE cr.success = false ORDER BY cr.checkedAt DESC")
    List<CheckResult> findRecentFailedChecks(Pageable pageable);

    @Query("SELECT cr FROM CheckResult cr WHERE cr.monitor IN :monitors AND cr.success = false ORDER BY cr.checkedAt DESC")
    List<CheckResult> findRecentFailedChecksByMonitors(@Param("monitors") List<Monitor> monitors, Pageable pageable);

    @Modifying
    @Query("DELETE FROM CheckResult cr WHERE cr.checkedAt < :retentionDate")
    int deleteOldResults(@Param("retentionDate") Instant retentionDate);
}
