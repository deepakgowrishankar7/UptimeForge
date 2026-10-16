package com.pulsewatch.controller;

import com.pulsewatch.dto.*;
import com.pulsewatch.entity.User;
import com.pulsewatch.security.SecurityUtils;
import com.pulsewatch.service.UserService;
import com.pulsewatch.team.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Teams", description = "Team management, invitations, and member access controls")
public class TeamController {

    private final TeamService teamService;
    private final UserService userService;

    public TeamController(TeamService teamService, UserService userService) {
        this.teamService = teamService;
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Create a new team")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(@Valid @RequestBody TeamCreateRequest request, HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        TeamResponse resp = teamService.createTeam(user, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Team created successfully", resp));
    }

    @GetMapping
    @Operation(summary = "List teams accessible to current user")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeams() {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        List<TeamResponse> resp = teamService.getUserTeams(user);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get team details")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeam(@PathVariable Long id) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        TeamResponse resp = teamService.getTeam(id, user);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update team settings")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(
            @PathVariable Long id,
            @Valid @RequestBody TeamCreateRequest request,
            HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        TeamResponse resp = teamService.updateTeam(id, user, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Team updated successfully", resp));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a team")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(@PathVariable Long id, HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        teamService.deleteTeam(id, user, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Team deleted successfully", null));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List members of a team")
    public ResponseEntity<ApiResponse<List<TeamMemberResponse>>> getTeamMembers(@PathVariable Long id) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        List<TeamMemberResponse> resp = teamService.getTeamMembers(id, user);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PostMapping("/{id}/invitations")
    @Operation(summary = "Invite a new member to the team")
    public ResponseEntity<ApiResponse<TeamInvitationResponse>> inviteMember(
            @PathVariable Long id,
            @Valid @RequestBody TeamInviteRequest request,
            HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        TeamInvitationResponse resp = teamService.inviteMember(id, user, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Invitation generated", resp));
    }

    @PostMapping("/invitations/accept")
    @Operation(summary = "Accept an invitation to join a team")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request,
            HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        teamService.acceptInvitation(user, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Team invitation accepted", null));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @Operation(summary = "Remove a member from the team")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long id,
            @PathVariable Long memberId,
            HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        teamService.removeMember(id, memberId, user, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Member removed from team", null));
    }
}
