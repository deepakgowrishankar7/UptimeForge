package com.pulsewatch.monitoring;

import com.pulsewatch.entity.Monitor;
import com.pulsewatch.validation.SsrfValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class HttpHealthChecker implements HealthChecker {

    private static final Logger log = LoggerFactory.getLogger(HttpHealthChecker.class);

    private final SsrfValidator ssrfValidator;
    private final HttpClient httpClient;

    public HttpHealthChecker(SsrfValidator ssrfValidator,
                             @Value("${pulsewatch.monitoring.connect-timeout-ms:10000}") int connectTimeoutMs) {
        this.ssrfValidator = ssrfValidator;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public CheckExecutionResult check(Monitor monitor) {
        Instant checkedAt = Instant.now();
        long startTime = System.nanoTime();

        try {
            // 1. SSRF and Destination Safety Check
            ssrfValidator.validateHttpTarget(monitor.getUrl());

            // 2. Prepare HTTP Request
            URI uri = URI.create(monitor.getUrl().trim());
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMillis(monitor.getTimeoutMs() > 0 ? monitor.getTimeoutMs() : 10000))
                    .header("User-Agent", "PulseWatch-Monitor/1.0 (+https://pulsewatch.io)");

            // Apply custom headers if present
            if (monitor.getHeaders() != null && !monitor.getHeaders().trim().isEmpty()) {
                String[] lines = monitor.getHeaders().split("\n");
                for (String line : lines) {
                    int colonIndex = line.indexOf(':');
                    if (colonIndex > 0) {
                        String headerName = line.substring(0, colonIndex).trim();
                        String headerValue = line.substring(colonIndex + 1).trim();
                        if (!headerName.equalsIgnoreCase("host")) { // host header is managed by client
                            reqBuilder.header(headerName, headerValue);
                        }
                    }
                }
            }

            // Apply Method and Body
            String method = monitor.getHttpMethod() != null ? monitor.getHttpMethod().toUpperCase() : "GET";
            HttpRequest.BodyPublisher bodyPublisher = (monitor.getRequestBody() != null && !monitor.getRequestBody().isEmpty())
                    ? HttpRequest.BodyPublishers.ofString(monitor.getRequestBody())
                    : HttpRequest.BodyPublishers.noBody();

            if ("POST".equals(method)) {
                reqBuilder.POST(bodyPublisher);
            } else if ("PUT".equals(method)) {
                reqBuilder.PUT(bodyPublisher);
            } else if ("DELETE".equals(method)) {
                reqBuilder.DELETE();
            } else if ("HEAD".equals(method)) {
                reqBuilder.method("HEAD", HttpRequest.BodyPublishers.noBody());
            } else {
                reqBuilder.GET();
            }

            // 3. Execute Request
            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
            Instant completedAt = Instant.now();

            int statusCode = response.statusCode();

            // 4. Validate Status Code
            if (!isStatusCodeExpected(statusCode, monitor.getExpectedStatusCodes())) {
                return CheckExecutionResult.failure(
                        elapsedMs,
                        statusCode,
                        "STATUS_MISMATCH",
                        "Expected status [" + monitor.getExpectedStatusCodes() + "] but received " + statusCode,
                        checkedAt,
                        completedAt
                );
            }

            // 5. Validate Expected Text if configured
            if (monitor.getExpectedText() != null && !monitor.getExpectedText().trim().isEmpty()) {
                String body = response.body();
                if (body == null || !body.contains(monitor.getExpectedText().trim())) {
                    return CheckExecutionResult.failure(
                            elapsedMs,
                            statusCode,
                            "CONTENT_MISMATCH",
                            "Response did not contain expected keyword: " + monitor.getExpectedText().trim(),
                            checkedAt,
                            completedAt
                    );
                }
            }

            return CheckExecutionResult.success(elapsedMs, statusCode, checkedAt, completedAt);

        } catch (HttpTimeoutException e) {
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
            return CheckExecutionResult.failure(elapsedMs, null, "TIMEOUT", "Request timed out after " + elapsedMs + "ms", checkedAt, Instant.now());
        } catch (UnknownHostException e) {
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
            return CheckExecutionResult.failure(elapsedMs, null, "DNS_ERROR", "Failed to resolve host: " + e.getMessage(), checkedAt, Instant.now());
        } catch (ConnectException e) {
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
            return CheckExecutionResult.failure(elapsedMs, null, "CONNECTION_REFUSED", "Connection refused by host", checkedAt, Instant.now());
        } catch (SSLHandshakeException e) {
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
            return CheckExecutionResult.failure(elapsedMs, null, "SSL_ERROR", "TLS handshake failure: " + e.getMessage(), checkedAt, Instant.now());
        } catch (Exception e) {
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
            return CheckExecutionResult.failure(elapsedMs, null, "TRANSPORT_ERROR", e.getMessage() != null ? e.getMessage() : "Unknown transport error", checkedAt, Instant.now());
        }
    }

    private boolean isStatusCodeExpected(int statusCode, String expectedCodes) {
        if (expectedCodes == null || expectedCodes.trim().isEmpty()) {
            return statusCode >= 200 && statusCode < 300;
        }

        String[] parts = expectedCodes.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.equalsIgnoreCase("2xx") && statusCode >= 200 && statusCode < 300) return true;
            if (part.equalsIgnoreCase("3xx") && statusCode >= 300 && statusCode < 400) return true;
            if (part.equalsIgnoreCase("4xx") && statusCode >= 400 && statusCode < 500) return true;
            try {
                if (Integer.parseInt(part) == statusCode) {
                    return true;
                }
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }
}
