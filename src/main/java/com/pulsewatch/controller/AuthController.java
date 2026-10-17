package com.pulsewatch.controller;

import com.pulsewatch.dto.*;
import com.pulsewatch.entity.User;
import com.pulsewatch.security.SecurityUtils;
import com.pulsewatch.service.AuthService;
import com.pulsewatch.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, login, token refresh, and password management")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register/send-otp")
    @Operation(summary = "Send 6-digit OTP verification code to prospective registrant's email via Brevo")
    public ResponseEntity<ApiResponse<SendOtpResponse>> sendRegistrationOtp(@Valid @RequestBody SendOtpRequest request, HttpServletRequest httpRequest) {
        authService.sendRegistrationOtp(request, httpRequest.getRemoteAddr());
        SendOtpResponse response = new SendOtpResponse(request.getEmail(), null, "Verification code sent to " + request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Verification code sent to " + request.getEmail(), response));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account with OTP verification")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh expired access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.refreshToken(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out user and revoke refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) RefreshTokenRequest request, HttpServletRequest httpRequest) {
        User user = SecurityUtils.getCurrentUserPrincipal().map(p -> userService.getUserById(p.getId())).orElse(null);
        authService.logout(request, user, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify user email address using token")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Email successfully verified", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        authService.forgotPassword(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("If your email is registered, password reset instructions have been sent", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using reset token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
        authService.resetPassword(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success("Password has been successfully reset", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser() {
        User user = userService.getUserById(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(UserProfileResponse.fromUser(user)));
    }
}
