package com.pulsewatch.repository;

import com.pulsewatch.entity.Monitor;
import com.pulsewatch.entity.MonitorStatus;
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

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, Long> {

    @Query("SELECT m FROM Monitor m WHERE m.enabled = true AND (m.nextCheckAt IS NULL OR m.nextCheckAt <= :now)")
    List<Monitor> findDueMonitors(@Param("now") Instant now);

    @Query("SELECT m FROM Monitor m WHERE (m.owner = :user OR m.team IN :teams) " +
           "AND (:status IS NULL OR m.currentStatus = :status) " +
           "AND (:q IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(m.url) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Monitor> findAccessibleMonitors(@Param("user") User user,
                                         @Param("teams") List<Team> teams,
                                         @Param("status") MonitorStatus status,
                                         @Param("q") String query,
                                         Pageable pageable);

    @Query("SELECT m FROM Monitor m WHERE (m.owner = :user OR m.team IN :teams)")
    List<Monitor> findAllAccessibleMonitors(@Param("user") User user, @Param("teams") List<Team> teams);

    List<Monitor> findByOwner(User owner);
    List<Monitor> findByTeam(Team team);

    long countByEnabledTrue();
    long countByCurrentStatus(MonitorStatus status);

    @Query("SELECT COUNT(m) FROM Monitor m WHERE (m.owner = :user OR m.team IN :teams) AND m.currentStatus = :status")
    long countAccessibleByStatus(@Param("user") User user, @Param("teams") List<Team> teams, @Param("status") MonitorStatus status);

    @Query("SELECT COUNT(m) FROM Monitor m WHERE (m.owner = :user OR m.team IN :teams)")
    long countAccessibleTotal(@Param("user") User user, @Param("teams") List<Team> teams);
}
