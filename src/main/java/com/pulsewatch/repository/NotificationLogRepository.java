package com.pulsewatch.repository;

import com.pulsewatch.entity.AlertConfig;
import com.pulsewatch.entity.DeliveryStatus;
import com.pulsewatch.entity.Incident;
import com.pulsewatch.entity.NotificationLog;
import com.pulsewatch.entity.Team;
import com.pulsewatch.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    @Query("SELECT nl FROM NotificationLog nl WHERE (nl.alertConfig.owner = :user OR nl.alertConfig.team IN :teams) ORDER BY nl.lastAttemptAt DESC")
    Page<NotificationLog> findAccessibleLogs(@Param("user") User user, @Param("teams") List<Team> teams, Pageable pageable);

    List<NotificationLog> findByIncidentOrderByLastAttemptAtDesc(Incident incident);

    Optional<NotificationLog> findFirstByAlertConfigAndIncidentAndEventTypeOrderByLastAttemptAtDesc(AlertConfig alertConfig, Incident incident, String eventType);

    long countByDeliveryStatus(DeliveryStatus status);

    @Query("SELECT nl FROM NotificationLog nl WHERE nl.deliveryStatus = :status ORDER BY nl.lastAttemptAt DESC")
    List<NotificationLog> findRecentByStatus(@Param("status") DeliveryStatus status, Pageable pageable);

    @Query("SELECT nl FROM NotificationLog nl WHERE nl.alertConfig = :alertConfig AND nl.lastAttemptAt >= :since ORDER BY nl.lastAttemptAt DESC")
    List<NotificationLog> findRecentForConfig(@Param("alertConfig") AlertConfig alertConfig, @Param("since") Instant since);
}
