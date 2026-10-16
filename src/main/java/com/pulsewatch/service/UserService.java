package com.pulsewatch.service;

import com.pulsewatch.entity.*;
import com.pulsewatch.exception.ForbiddenException;
import com.pulsewatch.exception.ResourceNotFoundException;
import com.pulsewatch.repository.TeamMemberRepository;
import com.pulsewatch.repository.TeamRepository;
import com.pulsewatch.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    public UserService(UserRepository userRepository,
                       TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public List<Team> getUserTeams(User user) {
        if (user == null) return Collections.emptyList();
        return teamRepository.findAllUserTeams(user);
    }

    @Transactional(readOnly = true)
    public void verifyMonitorAccess(User user, Monitor monitor) {
        if (user.getRole() == Role.ADMIN) return; // Admin has platform-wide access

        if (monitor.getOwner().getId().equals(user.getId())) return;

        if (monitor.getTeam() != null) {
            boolean isMember = teamMemberRepository.existsByTeamAndUser(monitor.getTeam(), user)
                    || monitor.getTeam().getOwner().getId().equals(user.getId());
            if (isMember) return;
        }

        throw new ForbiddenException("You do not have permission to access this monitor");
    }

    @Transactional(readOnly = true)
    public void verifyTeamAccess(User user, Team team) {
        if (user.getRole() == Role.ADMIN) return;

        if (team.getOwner().getId().equals(user.getId())) return;

        boolean isMember = teamMemberRepository.existsByTeamAndUser(team, user);
        if (!isMember) {
            throw new ForbiddenException("You do not have access to this team");
        }
    }

    @Transactional(readOnly = true)
    public void verifyTeamAdmin(User user, Team team) {
        if (user.getRole() == Role.ADMIN) return;

        if (team.getOwner().getId().equals(user.getId())) return;

        TeamMember member = teamMemberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new ForbiddenException("You do not belong to this team"));

        if (member.getRole() != TeamRole.ADMIN && member.getRole() != TeamRole.OWNER) {
            throw new ForbiddenException("Team administrator permissions required");
        }
    }
}
