package com.pulsewatch.repository;

import com.pulsewatch.entity.Team;
import com.pulsewatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByOwner(User owner);

    @Query("SELECT t FROM Team t WHERE t.owner = :user OR t.id IN (SELECT tm.team.id FROM TeamMember tm WHERE tm.user = :user)")
    List<Team> findAllUserTeams(@Param("user") User user);
}
