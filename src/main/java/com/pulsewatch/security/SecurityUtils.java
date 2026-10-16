package com.pulsewatch.security;

import com.pulsewatch.entity.Role;
import com.pulsewatch.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityUtils {

    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return Optional.of((UserPrincipal) authentication.getPrincipal());
        }
        return Optional.empty();
    }

    public static UserPrincipal getRequiredCurrentUser() {
        return getCurrentUserPrincipal()
                .orElseThrow(() -> new UnauthorizedException("User is not authenticated"));
    }

    public static Long getCurrentUserId() {
        return getRequiredCurrentUser().getId();
    }

    public static String getCurrentUserEmail() {
        return getRequiredCurrentUser().getEmail();
    }

    public static boolean isAdmin() {
        return getCurrentUserPrincipal()
                .map(u -> u.getRole() == Role.ADMIN)
                .orElse(false);
    }
}
