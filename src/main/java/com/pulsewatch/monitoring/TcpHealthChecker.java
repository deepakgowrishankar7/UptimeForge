package com.pulsewatch.monitoring;

import com.pulsewatch.entity.Monitor;
import com.pulsewatch.validation.SsrfValidator;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Instant;

@Component
public class TcpHealthChecker implements HealthChecker {

    private final SsrfValidator ssrfValidator;

    public TcpHealthChecker(SsrfValidator ssrfValidator) {
        this.ssrfValidator = ssrfValidator;
    }

    @Override
    public CheckExecutionResult check(Monitor monitor) {
        Instant checkedAt = Instant.now();
        long startTime = System.nanoTime();

        String raw = monitor.getUrl().trim();
        // Remove tcp:// prefix if provided
        if (raw.toLowerCase().startsWith("tcp://")) {
            raw = raw.substring(6);
        }

        String host = raw;
        int port = 80;

        int colonIndex = raw.lastIndexOf(':');
        if (colonIndex > 0) {
            host = raw.substring(0, colonIndex).trim();
            try {
                port = Integer.parseInt(raw.substring(colonIndex + 1).trim());
            } catch (NumberFormatException e) {
                return CheckExecutionResult.failure(0, null, "INVALID_PORT", "Invalid TCP port in target", checkedAt, Instant.now());
            }
        }

        try {
            // SSRF and destination check
            ssrfValidator.validateTcpTarget(host, port);

            int timeoutMs = monitor.getTimeoutMs() > 0 ? monitor.getTimeoutMs() : 10000;

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), timeoutMs);
                long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
                Instant completedAt = Instant.now();
                return CheckExecutionResult.success(elapsedMs, null, checkedAt, completedAt);
            }
        } catch (SocketTimeoutException e) {
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
            return CheckExecutionResult.failure(elapsedMs, null, "TIMEOUT", "TCP connection timed out after " + elapsedMs + "ms", checkedAt, Instant.now());
        } catch (Exception e) {
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
            return CheckExecutionResult.failure(elapsedMs, null, "CONNECTION_FAILED", "Failed to connect to " + host + ":" + port + " (" + e.getMessage() + ")", checkedAt, Instant.now());
        }
    }
}
