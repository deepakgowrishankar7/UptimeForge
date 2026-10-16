package com.pulsewatch.controller;

import com.pulsewatch.dto.*;
import com.pulsewatch.entity.User;
import com.pulsewatch.security.SecurityUtils;
import com.pulsewatch.service.UserService;
import com.pulsewatch.statuspage.StatusPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/status-pages")
@Tag(name = "Status Pages", description = "Management of public status pages, monitor attachments, and announcements")
public class StatusPageController {

    private final StatusPageService statusPageService;
    private final UserService userService;

    public StatusPageController(StatusPageService statusPageService, UserService userService) {
        this.statusPageService = statusPageService;
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Create a status page")
    public ResponseEntity<ApiResponse<StatusPageResponse>> createStatusPage(
            @Valid @RequestBody StatusPageRequest request,
            HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        StatusPageResponse resp = statusPageService.createStatusPage(user, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Status page created successfully", resp));
    }

    @GetMapping
    @Operation(summary = "List status pages accessible to user")
    public ResponseEntity<ApiResponse<List<StatusPageResponse>>> getStatusPages() {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        List<StatusPageResponse> resp = statusPageService.getUserStatusPages(user);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get status page details")
    public ResponseEntity<ApiResponse<StatusPageResponse>> getStatusPage(@PathVariable Long id) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        StatusPageResponse resp = statusPageService.getStatusPage(id, user);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update status page configuration")
    public ResponseEntity<ApiResponse<StatusPageResponse>> updateStatusPage(
            @PathVariable Long id,
            @Valid @RequestBody StatusPageRequest request,
            HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        StatusPageResponse resp = statusPageService.updateStatusPage(id, user, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Status page updated successfully", resp));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a status page")
    public ResponseEntity<ApiResponse<Void>> deleteStatusPage(@PathVariable Long id, HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        statusPageService.deleteStatusPage(id, user, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Status page deleted", null));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish status page publicly")
    public ResponseEntity<ApiResponse<StatusPageResponse>> publishStatusPage(@PathVariable Long id, HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        StatusPageResponse resp = statusPageService.publish(id, true, user, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Status page published", resp));
    }

    @PostMapping("/{id}/unpublish")
    @Operation(summary = "Unpublish status page")
    public ResponseEntity<ApiResponse<StatusPageResponse>> unpublishStatusPage(@PathVariable Long id, HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        StatusPageResponse resp = statusPageService.publish(id, false, user, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Status page unpublished", resp));
    }

    @PostMapping("/{id}/announcements")
    @Operation(summary = "Add an announcement or maintenance alert to status page")
    public ResponseEntity<ApiResponse<StatusAnnouncementResponse>> addAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody StatusAnnouncementRequest request,
            HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        StatusAnnouncementResponse resp = statusPageService.addAnnouncement(id, user, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Announcement posted", resp));
    }

    @GetMapping("/{id}/announcements")
    @Operation(summary = "Get announcements for a status page")
    public ResponseEntity<ApiResponse<List<StatusAnnouncementResponse>>> getAnnouncements(@PathVariable Long id) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        List<StatusAnnouncementResponse> resp = statusPageService.getAnnouncements(id, user);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }
}
