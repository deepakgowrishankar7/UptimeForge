package com.pulsewatch.repository;

import com.pulsewatch.entity.Incident;
import com.pulsewatch.entity.IncidentStatus;
import com.pulsewatch.entity.Monitor;
import com.pulsewatch.entity.Team;
import com.pulsewatch.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findFirstByMonitorAndStatusNotOrderByIdDesc(Monitor monitor, IncidentStatus status);

    Optional<Incident> findFirstByMonitorAndStatusInOrderByIdDesc(Monitor monitor, List<IncidentStatus> statuses);

    @Query("SELECT i FROM Incident i WHERE (i.monitor.owner = :user OR i.team IN :teams) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "ORDER BY i.startedAt DESC")
    Page<Incident> findAccessibleIncidents(@Param("user") User user,
                                           @Param("teams") List<Team> teams,
                                           @Param("status") IncidentStatus status,
                                           Pageable pageable);

    @Query("SELECT i FROM Incident i WHERE (i.monitor.owner = :user OR i.team IN :teams) ORDER BY i.startedAt DESC")
    List<Incident> findTopAccessibleIncidents(@Param("user") User user, @Param("teams") List<Team> teams, Pageable pageable);

    List<Incident> findByMonitorOrderByStartedAtDesc(Monitor monitor);

    Page<Incident> findByMonitorOrderByStartedAtDesc(Monitor monitor, Pageable pageable);

    long countByStatus(IncidentStatus status);

    @Query("SELECT COUNT(i) FROM Incident i WHERE (i.monitor.owner = :user OR i.team IN :teams) AND i.status = :status")
    long countAccessibleByStatus(@Param("user") User user, @Param("teams") List<Team> teams, @Param("status") IncidentStatus status);

    @Query("SELECT i FROM Incident i WHERE (i.monitor.owner = :user OR i.team IN :teams) AND i.status = :status AND i.resolvedAt IS NOT NULL AND i.startedAt IS NOT NULL")
    List<Incident> findAccessibleResolvedIncidents(@Param("user") User user, @Param("teams") List<Team> teams, @Param("status") IncidentStatus status);
}

