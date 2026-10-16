package com.pulsewatch.unit;

import com.pulsewatch.exception.SsrfProtectionException;
import com.pulsewatch.validation.SsrfValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SsrfProtectionTest {

    private SsrfValidator ssrfValidator;

    @BeforeEach
    void setUp() {
        ssrfValidator = new SsrfValidator();
    }

    @Test
    @DisplayName("Block localhost target")
    void testBlockLocalhost() {
        assertThrows(SsrfProtectionException.class, () -> ssrfValidator.validateHttpTarget("http://localhost:8080/metrics"));
    }

    @Test
    @DisplayName("Block 127.0.0.1 loopback target")
    void testBlockLoopbackIp() {
        assertThrows(SsrfProtectionException.class, () -> ssrfValidator.validateHttpTarget("http://127.0.0.1/admin"));
    }

    @Test
    @DisplayName("Block AWS and Cloud metadata target 169.254.169.254")
    void testBlockCloudMetadata() {
        assertThrows(SsrfProtectionException.class, () -> ssrfValidator.validateHttpTarget("http://169.254.169.254/latest/meta-data"));
    }

    @Test
    @DisplayName("Block non-HTTP/HTTPS protocol (e.g. file:// or ftp://)")
    void testBlockInvalidProtocols() {
        assertThrows(SsrfProtectionException.class, () -> ssrfValidator.validateHttpTarget("file:///etc/passwd"));
        assertThrows(SsrfProtectionException.class, () -> ssrfValidator.validateHttpTarget("ftp://ftp.example.com"));
    }

    @Test
    @DisplayName("Block restricted internal infrastructure ports")
    void testBlockRestrictedPorts() {
        assertThrows(SsrfProtectionException.class, () -> ssrfValidator.validateHttpTarget("http://example.com:22")); // SSH
        assertThrows(SsrfProtectionException.class, () -> ssrfValidator.validateHttpTarget("http://example.com:3306")); // MySQL
        assertThrows(SsrfProtectionException.class, () -> ssrfValidator.validateHttpTarget("http://example.com:6379")); // Redis
    }

    @Test
    @DisplayName("Allow safe public targets")
    void testAllowSafeTargets() {
        assertDoesNotThrow(() -> ssrfValidator.validateHttpTarget("https://www.google.com"));
        assertDoesNotThrow(() -> ssrfValidator.validateHttpTarget("https://api.github.com/status"));
    }
}
