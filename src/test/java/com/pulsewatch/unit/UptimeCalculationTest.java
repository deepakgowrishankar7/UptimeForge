package com.pulsewatch.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UptimeCalculationTest {

    @Test
    @DisplayName("Calculate 100% uptime when all checks succeed")
    void testPerfectUptime() {
        List<Boolean> checkResults = Arrays.asList(true, true, true, true, true);
        long success = checkResults.stream().filter(b -> b).count();
        double uptime = ((double) success / checkResults.size()) * 100.0;
        assertEquals(100.0, uptime, 0.001);
    }

    @Test
    @DisplayName("Calculate partial uptime with failed checks")
    void testPartialUptime() {
        List<Boolean> checkResults = Arrays.asList(true, true, false, true, false);
        long success = checkResults.stream().filter(b -> b).count();
        double uptime = ((double) success / checkResults.size()) * 100.0;
        assertEquals(60.0, uptime, 0.001);
    }

    @Test
    @DisplayName("Calculate 0% uptime when all checks fail")
    void testZeroUptime() {
        List<Boolean> checkResults = Arrays.asList(false, false, false);
        long success = checkResults.stream().filter(b -> b).count();
        double uptime = ((double) success / checkResults.size()) * 100.0;
        assertEquals(0.0, uptime, 0.001);
    }

    @Test
    @DisplayName("Calculate downtime duration based on failed checks and interval")
    void testDowntimeDuration() {
        int failedChecks = 5;
        int intervalSeconds = 60;
        long downtimeSeconds = (long) failedChecks * intervalSeconds;
        assertEquals(300L, downtimeSeconds);
    }
}
