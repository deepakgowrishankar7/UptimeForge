package com.pulsewatch.repository;

import com.pulsewatch.entity.Team;
import com.pulsewatch.entity.TeamMember;
import com.pulsewatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByTeam(Team team);
    Optional<TeamMember> findByTeamAndUser(Team team, User user);
    boolean existsByTeamAndUser(Team team, User user);
    void deleteByTeamAndUser(Team team, User user);
}
