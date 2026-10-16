package com.pulsewatch.controller;

import com.pulsewatch.dto.ApiResponse;
import com.pulsewatch.dto.ChangePasswordRequest;
import com.pulsewatch.dto.UpdateProfileRequest;
import com.pulsewatch.dto.UserProfileResponse;
import com.pulsewatch.entity.User;
import com.pulsewatch.security.SecurityUtils;
import com.pulsewatch.service.AuthService;
import com.pulsewatch.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Profile", description = "Endpoints for user profile and password management")
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    public UserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PatchMapping("/me")
    @Operation(summary = "Update current user profile information")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request, HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        UserProfileResponse updated = authService.updateProfile(user, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Change current user password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        authService.changePassword(user, request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}
