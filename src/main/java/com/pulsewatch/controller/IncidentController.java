package com.pulsewatch.controller;

import com.pulsewatch.dto.*;
import com.pulsewatch.entity.IncidentStatus;
import com.pulsewatch.entity.User;
import com.pulsewatch.incident.IncidentService;
import com.pulsewatch.security.SecurityUtils;
import com.pulsewatch.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incidents", description = "Incident lifecycle management, acknowledgment, resolution, and event timelines")
public class IncidentController {

    private final IncidentService incidentService;
    private final UserService userService;

    public IncidentController(IncidentService incidentService, UserService userService) {
        this.incidentService = incidentService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List incidents accessible to current user")
    public ResponseEntity<ApiResponse<PagedResponse<IncidentResponse>>> getIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        PagedResponse<IncidentResponse> resp = incidentService.getAccessibleIncidents(user, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information for an incident")
    public ResponseEntity<ApiResponse<IncidentResponse>> getIncident(@PathVariable Long id) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        IncidentResponse resp = incidentService.getIncident(id, user);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge an ongoing incident")
    public ResponseEntity<ApiResponse<IncidentResponse>> acknowledgeIncident(@PathVariable Long id, HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        IncidentResponse resp = incidentService.acknowledgeIncident(id, user, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Incident acknowledged", resp));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Manually resolve an incident with resolution notes")
    public ResponseEntity<ApiResponse<IncidentResponse>> resolveIncident(
            @PathVariable Long id,
            @RequestBody(required = false) IncidentResolveRequest request,
            HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        IncidentResponse resp = incidentService.resolveIncident(id, user, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Incident resolved", resp));
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Get timeline events for an incident")
    public ResponseEntity<ApiResponse<List<IncidentEventResponse>>> getIncidentEvents(@PathVariable Long id) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        List<IncidentEventResponse> events = incidentService.getIncidentEvents(id, user);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/{id}/notes")
    @Operation(summary = "Get internal team notes for an incident")
    public ResponseEntity<ApiResponse<List<IncidentNoteResponse>>> getIncidentNotes(@PathVariable Long id) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        List<IncidentNoteResponse> notes = incidentService.getIncidentNotes(id, user);
        return ResponseEntity.ok(ApiResponse.success(notes));
    }

    @PostMapping("/{id}/notes")
    @Operation(summary = "Add an internal note to an incident")
    public ResponseEntity<ApiResponse<IncidentNoteResponse>> addIncidentNote(
            @PathVariable Long id,
            @Valid @RequestBody IncidentNoteRequest request,
            HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        IncidentNoteResponse note = incidentService.addNote(id, user, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Note added", note));
    }
}
