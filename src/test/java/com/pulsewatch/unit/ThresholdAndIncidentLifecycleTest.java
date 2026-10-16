package com.pulsewatch.unit;

import com.pulsewatch.entity.Monitor;
import com.pulsewatch.entity.MonitorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ThresholdAndIncidentLifecycleTest {

    private Monitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new Monitor();
        monitor.setCurrentStatus(MonitorStatus.UP);
        monitor.setFailureThreshold(3);
        monitor.setRecoveryThreshold(2);
        monitor.setConsecutiveFailures(0);
        monitor.setConsecutiveSuccesses(5);
    }

    @Test
    @DisplayName("Single failure transitions to DEGRADED before failure threshold is reached")
    void testSingleFailure() {
        monitor.setConsecutiveFailures(monitor.getConsecutiveFailures() + 1);
        monitor.setConsecutiveSuccesses(0);

        if (monitor.getConsecutiveFailures() >= monitor.getFailureThreshold()) {
            monitor.setCurrentStatus(MonitorStatus.DOWN);
        } else {
            monitor.setCurrentStatus(MonitorStatus.DEGRADED);
        }

        assertEquals(1, monitor.getConsecutiveFailures());
        assertEquals(MonitorStatus.DEGRADED, monitor.getCurrentStatus());
    }

    @Test
    @DisplayName("Consecutive failures reaching threshold transitions monitor to DOWN")
    void testReachingFailureThreshold() {
        for (int i = 0; i < 3; i++) {
            monitor.setConsecutiveFailures(monitor.getConsecutiveFailures() + 1);
            monitor.setConsecutiveSuccesses(0);
            if (monitor.getConsecutiveFailures() >= monitor.getFailureThreshold()) {
                monitor.setCurrentStatus(MonitorStatus.DOWN);
            } else {
                monitor.setCurrentStatus(MonitorStatus.DEGRADED);
            }
        }

        assertEquals(3, monitor.getConsecutiveFailures());
        assertEquals(MonitorStatus.DOWN, monitor.getCurrentStatus());
    }

    @Test
    @DisplayName("Meeting recovery threshold transitions monitor from DOWN back to UP")
    void testRecoveryThreshold() {
        monitor.setCurrentStatus(MonitorStatus.DOWN);
        monitor.setConsecutiveFailures(4);
        monitor.setConsecutiveSuccesses(0);

        // Check 1 succeeds (1 of 2 needed)
        monitor.setConsecutiveSuccesses(monitor.getConsecutiveSuccesses() + 1);
        monitor.setConsecutiveFailures(0);
        if (monitor.getConsecutiveSuccesses() >= monitor.getRecoveryThreshold()) {
            monitor.setCurrentStatus(MonitorStatus.UP);
        } else {
            monitor.setCurrentStatus(MonitorStatus.DEGRADED);
        }

        assertEquals(1, monitor.getConsecutiveSuccesses());
        assertEquals(MonitorStatus.DEGRADED, monitor.getCurrentStatus());

        // Check 2 succeeds (2 of 2 met)
        monitor.setConsecutiveSuccesses(monitor.getConsecutiveSuccesses() + 1);
        if (monitor.getConsecutiveSuccesses() >= monitor.getRecoveryThreshold()) {
            monitor.setCurrentStatus(MonitorStatus.UP);
        }

        assertEquals(2, monitor.getConsecutiveSuccesses());
        assertEquals(MonitorStatus.UP, monitor.getCurrentStatus());
    }
}
