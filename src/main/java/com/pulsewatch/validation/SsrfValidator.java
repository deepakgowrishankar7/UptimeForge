package com.pulsewatch.validation;

import com.pulsewatch.exception.SsrfProtectionException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class SsrfValidator {

    private static final Set<String> BLOCKED_HOSTNAMES = new HashSet<>(Arrays.asList(
            "localhost",
            "127.0.0.1",
            "0.0.0.0",
            "::1",
            "metadata.google.internal",
            "instance-data"
    ));

    private static final Set<Integer> DANGEROUS_INTERNAL_PORTS = new HashSet<>(Arrays.asList(
            22, 25, 110, 143, 3306, 5432, 6379, 9200, 9300, 11211, 27017, 28017
    ));

    /**
     * Validates an HTTP or HTTPS target URI to prevent SSRF attacks.
     */
    public void validateHttpTarget(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw new SsrfProtectionException("Target URL cannot be blank");
        }

        URI uri;
        try {
            uri = URI.create(urlString.trim());
        } catch (Exception e) {
            throw new SsrfProtectionException("Malformed URL: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new SsrfProtectionException("Only HTTP and HTTPS protocols are permitted for web monitors");
        }

        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            throw new SsrfProtectionException("URL host cannot be empty");
        }

        int port = uri.getPort();
        if (port != -1 && DANGEROUS_INTERNAL_PORTS.contains(port)) {
            throw new SsrfProtectionException("Target port " + port + " is restricted for security reasons");
        }

        validateHost(host);
    }

    /**
     * Validates a TCP host and port target to prevent SSRF and internal infrastructure probing.
     */
    public void validateTcpTarget(String host, int port) {
        if (host == null || host.trim().isEmpty()) {
            throw new SsrfProtectionException("Target TCP host cannot be blank");
        }
        if (port < 1 || port > 65535) {
            throw new SsrfProtectionException("TCP port must be between 1 and 65535");
        }
        if (DANGEROUS_INTERNAL_PORTS.contains(port)) {
            throw new SsrfProtectionException("TCP port " + port + " is restricted from monitoring");
        }

        validateHost(host.trim());
    }

    /**
     * Resolves the host DNS and verifies that all resolved IP addresses are safe public addresses.
     */
    public void validateHost(String host) {
        String lowerHost = host.toLowerCase().trim();
        if (BLOCKED_HOSTNAMES.contains(lowerHost) || lowerHost.endsWith(".localhost") || lowerHost.endsWith(".internal")) {
            throw new SsrfProtectionException("Access to target hostname is restricted: " + host);
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(lowerHost);
            for (InetAddress address : addresses) {
                if (isPrivateOrRestrictedIp(address)) {
                    throw new SsrfProtectionException("Target resolves to restricted/private IP: " + address.getHostAddress());
                }
            }
        } catch (UnknownHostException e) {
            // DNS resolution failure is handled during monitoring, but we flag if host is invalid
        }
    }

    public boolean isPrivateOrRestrictedIp(InetAddress address) {
        if (address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()) {
            return true;
        }

        if (address.isSiteLocalAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();

        // Check for IPv4 special / private ranges
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;

            // 0.0.0.0/8
            if (b0 == 0) return true;
            // 10.0.0.0/8
            if (b0 == 10) return true;
            // 127.0.0.0/8
            if (b0 == 127) return true;
            // 169.254.0.0/16 (Link Local & Cloud Metadata 169.254.169.254)
            if (b0 == 169 && b1 == 254) return true;
            // 172.16.0.0/12
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return true;
            // 192.168.0.0/16
            if (b0 == 192 && b1 == 168) return true;
            // 100.64.0.0/10 (Carrier-grade NAT / Metadata)
            if (b0 == 100 && (b1 >= 64 && b1 <= 127)) return true;
            // 224.0.0.0/4 (Multicast)
            if (b0 >= 224) return true;
        }

        return false;
    }
}
