package com.pulsewatch.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationCooldownTest {

    @Test
    @DisplayName("Suppress alert when last sent within cooldown period")
    void testAlertSuppressionInCooldown() {
        int cooldownSeconds = 900; // 15 mins
        Instant now = Instant.now();
        Instant lastSent = now.minusSeconds(300); // 5 mins ago

        boolean isSuppressed = lastSent.isAfter(now.minusSeconds(cooldownSeconds));
        assertTrue(isSuppressed, "Notification should be suppressed within cooldown window");
    }

    @Test
    @DisplayName("Allow alert when cooldown period has elapsed")
    void testAlertAllowedAfterCooldown() {
        int cooldownSeconds = 900;
        Instant now = Instant.now();
        Instant lastSent = now.minusSeconds(1200); // 20 mins ago

        boolean isSuppressed = lastSent.isAfter(now.minusSeconds(cooldownSeconds));
        assertFalse(isSuppressed, "Notification should be allowed after cooldown expires");
    }
}
