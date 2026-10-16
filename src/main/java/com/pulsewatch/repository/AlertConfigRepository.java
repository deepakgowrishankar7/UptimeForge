package com.pulsewatch.repository;

import com.pulsewatch.entity.AlertConfig;
import com.pulsewatch.entity.Team;
import com.pulsewatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertConfigRepository extends JpaRepository<AlertConfig, Long> {
    List<AlertConfig> findByOwner(User owner);
    List<AlertConfig> findByTeam(Team team);

    @Query("SELECT a FROM AlertConfig a WHERE a.owner = :user OR a.team IN :teams")
    List<AlertConfig> findAccessibleConfigs(@Param("user") User user, @Param("teams") List<Team> teams);

    @Query("SELECT a FROM AlertConfig a WHERE (a.owner = :user OR a.team = :team) AND a.enabled = true")
    List<AlertConfig> findActiveConfigsForMonitor(@Param("user") User user, @Param("team") Team team);
}
