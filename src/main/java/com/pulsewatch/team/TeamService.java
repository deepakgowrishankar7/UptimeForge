package com.pulsewatch.team;

import com.pulsewatch.audit.AuditLogService;
import com.pulsewatch.dto.*;
import com.pulsewatch.entity.*;
import com.pulsewatch.exception.BadRequestException;
import com.pulsewatch.exception.ForbiddenException;
import com.pulsewatch.exception.ResourceNotFoundException;
import com.pulsewatch.repository.TeamInvitationRepository;
import com.pulsewatch.repository.TeamMemberRepository;
import com.pulsewatch.repository.TeamRepository;
import com.pulsewatch.repository.UserRepository;
import com.pulsewatch.security.JwtTokenProvider;
import com.pulsewatch.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final AuditLogService auditLogService;

    public TeamService(TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository,
                       TeamInvitationRepository teamInvitationRepository,
                       UserRepository userRepository,
                       UserService userService,
                       JwtTokenProvider tokenProvider,
                       AuditLogService auditLogService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamInvitationRepository = teamInvitationRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.tokenProvider = tokenProvider;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public TeamResponse createTeam(User user, TeamCreateRequest request, String ipAddress) {
        Team team = new Team(request.getName().trim(), user);
        team = teamRepository.save(team);

        // Add creator as OWNER member
        TeamMember ownerMember = new TeamMember(team, user, TeamRole.OWNER);
        teamMemberRepository.save(ownerMember);

        auditLogService.log(user, "CREATE_TEAM", "TEAM", team.getId().toString(), "Created team: " + team.getName(), ipAddress);

        return TeamResponse.fromEntity(team, 1);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getUserTeams(User user) {
        List<Team> teams = teamRepository.findAllUserTeams(user);
        return teams.stream()
                .map(t -> TeamResponse.fromEntity(t, teamMemberRepository.findByTeam(t).size()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeam(Long teamId, User user) {
        Team team = getTeamEntity(teamId, user);
        return TeamResponse.fromEntity(team, teamMemberRepository.findByTeam(team).size());
    }

    @Transactional
    public TeamResponse updateTeam(Long teamId, User user, TeamCreateRequest request, String ipAddress) {
        Team team = getTeamEntity(teamId, user);
        userService.verifyTeamAdmin(user, team);

        team.setName(request.getName().trim());
        team = teamRepository.save(team);

        auditLogService.log(user, "UPDATE_TEAM", "TEAM", teamId.toString(), "Updated team name to " + team.getName(), ipAddress);

        return TeamResponse.fromEntity(team, teamMemberRepository.findByTeam(team).size());
    }

    @Transactional
    public void deleteTeam(Long teamId, User user, String ipAddress) {
        Team team = getTeamEntity(teamId, user);
        if (!team.getOwner().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only the team owner can delete the team");
        }

        teamRepository.delete(team);
        auditLogService.log(user, "DELETE_TEAM", "TEAM", teamId.toString(), "Deleted team: " + team.getName(), ipAddress);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeamMembers(Long teamId, User user) {
        Team team = getTeamEntity(teamId, user);
        return teamMemberRepository.findByTeam(team)
                .stream()
                .map(TeamMemberResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public TeamInvitationResponse inviteMember(Long teamId, User user, TeamInviteRequest request, String ipAddress) {
        Team team = getTeamEntity(teamId, user);
        userService.verifyTeamAdmin(user, team);

        String email = request.getEmail().toLowerCase().trim();

        // Check if user is already a member
        userRepository.findByEmail(email).ifPresent(invitedUser -> {
            if (teamMemberRepository.existsByTeamAndUser(team, invitedUser)) {
                throw new BadRequestException("User " + email + " is already a member of this team");
            }
        });

        String rawToken = tokenProvider.generateOpaqueToken();
        String hash = tokenProvider.hashToken(rawToken);

        TeamInvitation invitation = new TeamInvitation(
                team,
                email,
                request.getRole() != null ? request.getRole() : TeamRole.MEMBER,
                hash,
                Instant.now().plusSeconds(604800) // 7 days expiration
        );
        invitation = teamInvitationRepository.save(invitation);

        auditLogService.log(user, "INVITE_TEAM_MEMBER", "TEAM", teamId.toString(), "Invited " + email + " with role " + invitation.getRole(), ipAddress);

        return TeamInvitationResponse.fromEntity(invitation);
    }

    @Transactional
    public void acceptInvitation(User user, AcceptInvitationRequest request, String ipAddress) {
        String hash = tokenProvider.hashToken(request.getToken().trim());
        TeamInvitation invitation = teamInvitationRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired team invitation token"));

        if (!invitation.isValid()) {
            throw new BadRequestException("This invitation has already been accepted or has expired");
        }

        if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new ForbiddenException("This invitation was sent to " + invitation.getEmail() + ", not " + user.getEmail());
        }

        Team team = invitation.getTeam();
        if (teamMemberRepository.existsByTeamAndUser(team, user)) {
            invitation.setAcceptedAt(Instant.now());
            teamInvitationRepository.save(invitation);
            return;
        }

        TeamMember member = new TeamMember(team, user, invitation.getRole());
        teamMemberRepository.save(member);

        invitation.setAcceptedAt(Instant.now());
        teamInvitationRepository.save(invitation);

        auditLogService.log(user, "JOINED_TEAM", "TEAM", team.getId().toString(), "Joined team via invitation", ipAddress);
    }

    @Transactional
    public void removeMember(Long teamId, Long memberUserId, User currentUser, String ipAddress) {
        Team team = getTeamEntity(teamId, currentUser);
        userService.verifyTeamAdmin(currentUser, team);

        if (team.getOwner().getId().equals(memberUserId)) {
            throw new BadRequestException("Cannot remove the team owner. Transfer ownership first.");
        }

        User targetUser = userRepository.findById(memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member user not found"));

        teamMemberRepository.deleteByTeamAndUser(team, targetUser);

        auditLogService.log(currentUser, "REMOVE_TEAM_MEMBER", "TEAM", teamId.toString(), "Removed user " + targetUser.getEmail() + " from team", ipAddress);
    }

    public Team getTeamEntity(Long teamId, User user) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));
        userService.verifyTeamAccess(user, team);
        return team;
    }
}
