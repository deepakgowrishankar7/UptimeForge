package com.pulsewatch.service;

import com.pulsewatch.audit.AuditLogService;
import com.pulsewatch.dto.*;
import com.pulsewatch.entity.*;
import com.pulsewatch.exception.BadRequestException;
import com.pulsewatch.exception.RateLimitExceededException;
import com.pulsewatch.exception.UnauthorizedException;
import com.pulsewatch.repository.EmailVerificationTokenRepository;
import com.pulsewatch.repository.PasswordResetTokenRepository;
import com.pulsewatch.repository.RefreshTokenRepository;
import com.pulsewatch.repository.RegistrationOtpRepository;
import com.pulsewatch.repository.UserRepository;
import com.pulsewatch.security.JwtTokenProvider;
import com.pulsewatch.security.RateLimiterService;
import com.pulsewatch.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RegistrationOtpRepository registrationOtpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RateLimiterService rateLimiterService;
    private final AuditLogService auditLogService;

    @Value("${pulsewatch.rate-limiting.login-max-attempts:5}")
    private int loginMaxAttempts;

    @Value("${pulsewatch.rate-limiting.login-window-seconds:300}")
    private int loginWindowSeconds;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       RegistrationOtpRepository registrationOtpRepository,
                       EmailService emailService,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       RateLimiterService rateLimiterService,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.registrationOtpRepository = registrationOtpRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.rateLimiterService = rateLimiterService;
        this.auditLogService = auditLogService;
    }

    /**
     * Issues and dispatches a 6-digit OTP verification code to the prospective registrant's email via Brevo
     */
    @Transactional
    public String sendRegistrationOtp(SendOtpRequest request, String ipAddress) {
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email address already exists. Please sign in instead.");
        }

        // Rate limit OTP requests: 3 requests per 3 minutes per email
        String rateLimitKey = "otp_req:" + email;
        if (!rateLimiterService.isAllowed(rateLimitKey, 3, 180)) {
            throw new RateLimitExceededException("Too many verification requests. Please wait a few moments before requesting another code.");
        }

        // Supersede previous unused OTPs for this email
        registrationOtpRepository.findFirstByEmailAndUsedAtIsNullOrderByCreatedAtDesc(email).ifPresent(prev -> {
            prev.setUsedAt(Instant.now());
            registrationOtpRepository.save(prev);
        });

        // Generate 6-digit cryptographic OTP code
        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
        Instant expiresAt = Instant.now().plusSeconds(600); // 10 minutes

        RegistrationOtp regOtp = new RegistrationOtp(email, otp, expiresAt);
        registrationOtpRepository.save(regOtp);

        // Dispatch email via Brevo
        emailService.sendRegistrationOtp(email, request.getName(), otp);
        log.info("Registration OTP dispatched to {} from IP {}", email, ipAddress);
        return otp;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress) {
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email address already exists. Please sign in instead.");
        }

        String inputOtp = request.getOtp();
        if (inputOtp == null || inputOtp.trim().isEmpty()) {
            throw new BadRequestException("Email verification code is required. Please enter the 6-digit code sent to your email.");
        }

        RegistrationOtp regOtp = registrationOtpRepository.findFirstByEmailAndUsedAtIsNullOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestException("No active verification code found for this email. Please click 'Send Verification Code'."));

        if (!regOtp.isValid(inputOtp.trim())) {
            regOtp.setAttempts(regOtp.getAttempts() + 1);
            registrationOtpRepository.save(regOtp);
            if (regOtp.getAttempts() >= 5) {
                throw new BadRequestException("Too many invalid attempts. Please request a new verification code.");
            }
            throw new BadRequestException("Invalid or expired verification code. Please check your inbox or click 'Resend Code'.");
        }

        // Mark OTP as used
        regOtp.setUsedAt(Instant.now());
        registrationOtpRepository.save(regOtp);

        User user = new User(
                null,
                request.getName().trim(),
                email,
                passwordEncoder.encode(request.getPassword()),
                Role.USER,
                true,
                true // Verified via Brevo OTP
        );
        user = userRepository.save(user);

        // Also generate email verification token entity for traceability
        String rawVerifToken = tokenProvider.generateOpaqueToken();
        EmailVerificationToken verificationToken = new EmailVerificationToken(
                user,
                tokenProvider.hashToken(rawVerifToken),
                Instant.now().plusSeconds(86400)
        );
        verificationToken.setUsedAt(Instant.now()); // Already verified via OTP
        emailVerificationTokenRepository.save(verificationToken);

        auditLogService.log(user, "REGISTER_OTP_VERIFIED", "USER", user.getId().toString(), "User registration completed with email verification", ipAddress);

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        String email = request.getEmail().toLowerCase().trim();
        String rateLimitKey = "login:" + ipAddress + ":" + email;

        if (!rateLimiterService.isAllowed(rateLimitKey, loginMaxAttempts, loginWindowSeconds)) {
            throw new RateLimitExceededException("Too many failed login attempts. Please try again in 5 minutes.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("Your account has been suspended. Please contact administrator.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // Login succeeded, reset rate limiter
        rateLimiterService.reset(rateLimitKey);

        auditLogService.log(user, "LOGIN", "USER", user.getId().toString(), "User login succeeded", ipAddress);

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress) {
        String rawToken = request.getRefreshToken();
        if (rawToken == null || rawToken.trim().isEmpty()) {
            throw new BadRequestException("Refresh token is required");
        }

        String hash = tokenProvider.hashToken(rawToken.trim());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!storedToken.isValid()) {
            throw new UnauthorizedException("Refresh token is expired or has been revoked");
        }

        User user = storedToken.getUser();
        if (!user.isEnabled()) {
            throw new UnauthorizedException("User account is disabled");
        }

        // Rotate refresh token
        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        auditLogService.log(user, "REFRESH_TOKEN", "USER", user.getId().toString(), "Access token refreshed", ipAddress);

        return createAuthResponse(user);
    }

    @Transactional
    public void logout(RefreshTokenRequest request, User currentUser, String ipAddress) {
        if (request != null && request.getRefreshToken() != null) {
            String hash = tokenProvider.hashToken(request.getRefreshToken().trim());
            refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            });
        }
        if (currentUser != null) {
            refreshTokenRepository.revokeAllUserTokens(currentUser, Instant.now());
            auditLogService.log(currentUser, "LOGOUT", "USER", currentUser.getId().toString(), "User logged out", ipAddress);
        }
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String hash = tokenProvider.hashToken(request.getToken().trim());
        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        if (!token.isValid()) {
            throw new BadRequestException("Verification token has already been used or has expired");
        }

        token.setUsedAt(Instant.now());
        emailVerificationTokenRepository.save(token);

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, String ipAddress) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail().toLowerCase().trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String rawOtp = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
            PasswordResetToken resetToken = new PasswordResetToken(
                    user,
                    tokenProvider.hashToken(rawOtp),
                    Instant.now().plusSeconds(900) // 15 mins
            );
            passwordResetTokenRepository.save(resetToken);
            auditLogService.log(user, "FORGOT_PASSWORD_REQUESTED", "USER", user.getId().toString(), "Password reset requested", ipAddress);
            emailService.sendPasswordResetOtp(user.getEmail(), user.getName(), rawOtp);
            log.info("Password reset code sent to {}", user.getEmail());
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, String ipAddress) {
        String hash = tokenProvider.hashToken(request.getToken().trim());
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));

        if (!token.isValid()) {
            throw new BadRequestException("Password reset token has already been used or has expired");
        }

        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all existing refresh tokens
        refreshTokenRepository.revokeAllUserTokens(user, Instant.now());

        auditLogService.log(user, "PASSWORD_RESET_SUCCESS", "USER", user.getId().toString(), "Password reset successfully completed", ipAddress);
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequest request, String ipAddress) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all user refresh tokens on password change
        refreshTokenRepository.revokeAllUserTokens(user, Instant.now());

        auditLogService.log(user, "PASSWORD_CHANGED", "USER", user.getId().toString(), "User changed password", ipAddress);

        // Send instant security alert email via Brevo
        emailService.sendPasswordChangedAlert(user.getEmail(), user.getName(), ipAddress);
    }

    @Transactional
    public UserProfileResponse updateProfile(User user, UpdateProfileRequest request, String ipAddress) {
        user.setName(request.getName().trim());
        user = userRepository.save(user);
        auditLogService.log(user, "PROFILE_UPDATED", "USER", user.getId().toString(), "User updated profile name to " + user.getName(), ipAddress);
        return UserProfileResponse.fromUser(user);
    }

    private AuthResponse createAuthResponse(User user) {
        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = tokenProvider.generateAccessToken(principal);

        String rawRefreshToken = tokenProvider.generateOpaqueToken();
        String hash = tokenProvider.hashToken(rawRefreshToken);
        Instant expiresAt = Instant.now().plusMillis(tokenProvider.getRefreshTokenExpirationMs());

        RefreshToken refreshToken = new RefreshToken(user, hash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                tokenProvider.getAccessTokenExpirationMs(),
                UserProfileResponse.fromUser(user)
        );
    }
}
