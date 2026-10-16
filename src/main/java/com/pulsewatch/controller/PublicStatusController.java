package com.pulsewatch.controller;

import com.pulsewatch.dto.ApiResponse;
import com.pulsewatch.dto.PublicStatusResponse;
import com.pulsewatch.statuspage.StatusPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Public Status", description = "Public read-only endpoints for published status pages without sensitive data exposure")
public class PublicStatusController {

    private final StatusPageService statusPageService;

    public PublicStatusController(StatusPageService statusPageService) {
        this.statusPageService = statusPageService;
    }

    @GetMapping("/status/{slug}")
    @Operation(summary = "Get published public status page by slug")
    public ResponseEntity<ApiResponse<PublicStatusResponse>> getPublicStatus(@PathVariable String slug) {
        PublicStatusResponse resp = statusPageService.getPublicStatus(slug);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @GetMapping("/status/{slug}/incidents")
    @Operation(summary = "Get public incident history for a published status page")
    public ResponseEntity<ApiResponse<List<PublicStatusResponse.PublicIncidentSummary>>> getPublicIncidents(@PathVariable String slug) {
        PublicStatusResponse resp = statusPageService.getPublicStatus(slug);
        return ResponseEntity.ok(ApiResponse.success(resp.getRecentIncidents()));
    }
}
