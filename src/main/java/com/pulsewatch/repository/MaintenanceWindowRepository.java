package com.pulsewatch.repository;

import com.pulsewatch.entity.MaintenanceWindow;
import com.pulsewatch.entity.Monitor;
import com.pulsewatch.entity.Team;
import com.pulsewatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MaintenanceWindowRepository extends JpaRepository<MaintenanceWindow, Long> {

    @Query("SELECT m FROM MaintenanceWindow m WHERE (m.owner = :user OR m.team IN :teams)")
    List<MaintenanceWindow> findAccessibleWindows(@Param("user") User user, @Param("teams") List<Team> teams);

    @Query("SELECT COUNT(m) > 0 FROM MaintenanceWindow m WHERE (m.monitor = :monitor OR m.monitor IS NULL) AND :now BETWEEN m.startsAt AND m.endsAt")
    boolean isMonitorInMaintenance(@Param("monitor") Monitor monitor, @Param("now") Instant now);
}
