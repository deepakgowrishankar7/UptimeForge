package com.pulsewatch.repository;

import com.pulsewatch.entity.Team;
import com.pulsewatch.entity.TeamInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {
    List<TeamInvitation> findByTeam(Team team);
    Optional<TeamInvitation> findByTokenHash(String tokenHash);
    List<TeamInvitation> findByEmail(String email);
}
