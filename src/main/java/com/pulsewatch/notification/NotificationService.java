package com.pulsewatch.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsewatch.entity.*;
import com.pulsewatch.repository.AlertConfigRepository;
import com.pulsewatch.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final AlertConfigRepository alertConfigRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.mail.username:deepakgowrishankar7@gmail.com}")
    private String fromEmail;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public NotificationService(AlertConfigRepository alertConfigRepository,
                               NotificationLogRepository notificationLogRepository) {
        this.alertConfigRepository = alertConfigRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Async("notificationExecutor")
    @Transactional
    public void dispatchAlerts(Monitor monitor, Incident incident, String eventType, String details) {
        List<AlertConfig> configs = alertConfigRepository.findActiveConfigsForMonitor(monitor.getOwner(), monitor.getTeam());

        for (AlertConfig config : configs) {
            // Check if recovery event is requested
            if ("RECOVERY".equalsIgnoreCase(eventType) && !config.isNotifyOnRecovery()) {
                continue;
            }

            // Check Cooldown
            if (isInCooldown(config, incident, config.getCooldownSeconds())) {
                log.info("Alert for config {} suppressed due to cooldown ({} seconds)", config.getId(), config.getCooldownSeconds());
                NotificationLog logEntry = new NotificationLog(config, incident, eventType, DeliveryStatus.SUPPRESSED_COOLDOWN, "Suppressed by cooldown period");
                notificationLogRepository.save(logEntry);
                continue;
            }

            sendNotification(config, incident, monitor, eventType, details);
        }
    }

    private boolean isInCooldown(AlertConfig config, Incident incident, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return false;
        Instant since = Instant.now().minusSeconds(cooldownSeconds);
        List<NotificationLog> recent = notificationLogRepository.findRecentForConfig(config, since);
        return recent.stream().anyMatch(l -> l.getDeliveryStatus() == DeliveryStatus.DELIVERED);
    }

    public NotificationLog sendNotification(AlertConfig config, Incident incident, Monitor monitor, String eventType, String details) {
        NotificationLog logEntry = new NotificationLog(config, incident, eventType, DeliveryStatus.PENDING, null);
        logEntry = notificationLogRepository.save(logEntry);

        try {
            if (config.getChannel() == AlertChannel.EMAIL) {
                sendEmailAlert(config.getDestination(), monitor, incident, eventType, details);
            } else if (config.getChannel() == AlertChannel.SLACK) {
                sendSlackAlert(config.getDestination(), monitor, incident, eventType, details);
            } else if (config.getChannel() == AlertChannel.WEBHOOK) {
                sendWebhookAlert(config.getDestination(), monitor, incident, eventType, details);
            }

            logEntry.setDeliveryStatus(DeliveryStatus.DELIVERED);
            logEntry.setDeliveredAt(Instant.now());
            logEntry.setErrorMessage(null);
            log.info("Alert {} successfully sent via {} to {}", eventType, config.getChannel(), config.getDestination());
        } catch (Exception e) {
            logEntry.setDeliveryStatus(DeliveryStatus.FAILED);
            logEntry.setErrorMessage("Delivery failure: " + e.getMessage());
            log.warn("Failed to send alert via {}: {}", config.getChannel(), e.getMessage());
        }

        return notificationLogRepository.save(logEntry);
    }

    private void sendEmailAlert(String destination, Monitor monitor, Incident incident, String eventType, String details) {
        if (mailSender == null) {
            log.info("[MOCK EMAIL] To: {} | Subject: [PulseWatch Alert] {} is {} | Body: {}",
                    destination, monitor != null ? monitor.getName() : "Service", eventType, details);
            return;
        }

        boolean isRecovery = "RECOVERY".equalsIgnoreCase(eventType);
        boolean isTestAlert = "TEST_ALERT".equalsIgnoreCase(eventType);
        String monitorName = monitor != null ? monitor.getName() : (isTestAlert ? "Alert Verification Channel" : "Monitored Service");
        String monitorUrl = monitor != null ? monitor.getUrl() : (isTestAlert ? "https://uptimeforge.local/notifications" : "N/A");
        
        String subject;
        if (isTestAlert) {
            subject = "[TEST ALERT] UptimeForge Notification Channel Verified";
        } else if (isRecovery) {
            subject = "[RECOVERED] " + monitorName + " is back UP";
        } else {
            subject = "[ALERT] " + monitorName + " is DOWN";
        }

        String formattedTime = DateTimeFormatter.RFC_1123_DATE_TIME
                .withZone(ZoneId.of("UTC"))
                .format(Instant.now());

        String headerBg;
        String borderAccent;
        String badgeBg;
        String badgeColor;
        String badgeText;
        String heroTitle;

        if (isTestAlert) {
            headerBg = "linear-gradient(135deg, #1E3A8A 0%, #1D4ED8 100%)";
            borderAccent = "#2563EB";
            badgeBg = "rgba(37, 99, 235, 0.25)";
            badgeColor = "#60A5FA";
            badgeText = "● TEST VERIFICATION";
            heroTitle = "Alert Notification Channel Verified";
        } else if (isRecovery) {
            headerBg = "linear-gradient(135deg, #064E3B 0%, #065F46 100%)";
            borderAccent = "#10B981";
            badgeBg = "rgba(16, 185, 129, 0.2)";
            badgeColor = "#34D399";
            badgeText = "● RESOLVED & OPERATIONAL";
            heroTitle = "Service Recovered & Normal Operation Resumed";
        } else {
            headerBg = "linear-gradient(135deg, #7F1D1D 0%, #991B1B 100%)";
            borderAccent = "#EF4444";
            badgeBg = "rgba(239, 68, 68, 0.2)";
            badgeColor = "#FCA5A5";
            badgeText = "● CRITICAL OUTAGE";
            heroTitle = "Outage Detected: Immediate Attention Required";
        }

        String htmlContent = "<!DOCTYPE html>"
                + "<html lang='en'>"
                + "<head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>"
                + "<body style='margin:0; padding:0; background-color:#0B0F19; font-family:-apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing:antialiased;'>"
                + "  <table width='100%' cellpadding='0' cellspacing='0' style='background-color:#0B0F19; padding:48px 16px;'>"
                + "    <tr>"
                + "      <td align='center'>"
                + "        <table width='100%' cellpadding='0' cellspacing='0' style='max-width:580px; background-color:#FFFFFF; border-radius:18px; overflow:hidden; box-shadow:0 25px 50px -12px rgba(0, 0, 0, 0.5);'>"
                + "          <!-- Header -->"
                + "          <tr>"
                + "            <td style='background:" + headerBg + "; padding:32px 40px; border-bottom:3px solid " + borderAccent + ";'>"
                + "              <table width='100%' cellpadding='0' cellspacing='0'>"
                + "                <tr>"
                + "                  <td>"
                + "                    <div style='font-size:24px; font-weight:800; color:#FFFFFF;'>Uptime<span style='color:#38BDF8;'>Forge</span></div>"
                + "                    <div style='font-size:12px; color:#E2E8F0; margin-top:4px; font-weight:600; text-transform:uppercase; letter-spacing:1px;'>Incident Management & Alerting</div>"
                + "                  </td>"
                + "                  <td align='right'>"
                + "                    <span style='background:" + badgeBg + "; border:1px solid " + badgeColor + "; color:" + badgeColor + "; font-size:11px; font-weight:700; padding:5px 12px; border-radius:20px; text-transform:uppercase; letter-spacing:0.8px;'>"
                + "                      " + badgeText
                + "                    </span>"
                + "                  </td>"
                + "                </tr>"
                + "              </table>"
                + "            </td>"
                + "          </tr>"
                + "          <!-- Body Content -->"
                + "          <tr>"
                + "            <td style='padding:36px 40px; background-color:#FFFFFF; text-align:left;'>"
                + "              <h1 style='margin:0 0 14px 0; font-size:21px; font-weight:800; color:#0F172A;'>" + heroTitle + "</h1>"
                + "              <p style='margin:0 0 24px 0; font-size:14.5px; line-height:1.6; color:#475569;'>"
                + "                " + (isRecovery
                                      ? "Our synthetic health probe confirmed that <strong>" + monitorName + "</strong> has successfully recovered and is passing all validation criteria."
                                      : (isTestAlert
                                          ? "This is an automated test notification confirming that your UptimeForge email alert channel is active, verified, and ready to dispatch real-time outage alerts."
                                          : "Our automated monitoring probe detected an outage on <strong>" + monitorName + "</strong>. Traffic or availability may be degraded."))
                + "              </p>"
                + "              <!-- Incident Telemetry Grid -->"
                + "              <table width='100%' cellpadding='0' cellspacing='0' style='background-color:#F8FAFC; border:1px solid #E2E8F0; border-radius:12px; margin:24px 0;'>"
                + "                <tr>"
                + "                  <td style='padding:12px 18px; border-bottom:1px solid #E2E8F0; font-size:13px; color:#64748B; font-weight:600; width:130px;'>Monitor</td>"
                + "                  <td style='padding:12px 18px; border-bottom:1px solid #E2E8F0; font-size:13px; color:#0F172A; font-weight:700;'>" + monitorName + "</td>"
                + "                </tr>"
                + "                <tr>"
                + "                  <td style='padding:12px 18px; border-bottom:1px solid #E2E8F0; font-size:13px; color:#64748B; font-weight:600;'>Endpoint URL</td>"
                + "                  <td style='padding:12px 18px; border-bottom:1px solid #E2E8F0; font-size:13px; color:#2563EB; font-weight:600; font-family:monospace; word-break:break-all;'>" + monitorUrl + "</td>"
                + "                </tr>"
                + "                <tr>"
                + "                  <td style='padding:12px 18px; border-bottom:1px solid #E2E8F0; font-size:13px; color:#64748B; font-weight:600;'>Diagnostic Trace</td>"
                + "                  <td style='padding:12px 18px; border-bottom:1px solid #E2E8F0; font-size:13px; color:" + (isRecovery ? "#059669" : "#DC2626") + "; font-weight:600;'>" + details + "</td>"
                + "                </tr>"
                + "                <tr>"
                + "                  <td style='padding:12px 18px; font-size:13px; color:#64748B; font-weight:600;'>Timestamp</td>"
                + "                  <td style='padding:12px 18px; font-size:13px; color:#0F172A; font-weight:600; font-family:monospace;'>" + formattedTime + "</td>"
                + "                </tr>"
                + "              </table>"
                + "              <!-- CTA Button -->"
                + "              <div style='text-align:center; margin:32px 0 16px 0;'>"
                + "                <a href='http://localhost:8080/incidents.html' style='display:inline-block; background-color:#2563EB; color:#FFFFFF; text-decoration:none; font-size:14px; font-weight:700; padding:12px 28px; border-radius:10px; box-shadow:0 4px 12px rgba(37,99,235,0.3);'>"
                + "                  View Live Incident in Dashboard &rarr;"
                + "                </a>"
                + "              </div>"
                + "            </td>"
                + "          </tr>"
                + "          <!-- Footer -->"
                + "          <tr>"
                + "            <td style='background-color:#F8FAFC; border-top:1px solid #E2E8F0; padding:22px 40px; text-align:center; font-size:12px; color:#64748B; line-height:1.6;'>"
                + "              <div style='font-weight:600; color:#334155;'>UptimeForge Real-Time Incident Alerting Engine</div>"
                + "              <div>Automated alert delivered to <strong>" + destination + "</strong> via Brevo SMTP</div>"
                + "              <div style='margin-top:6px; font-size:11px; color:#94A3B8;'>&copy; 2026 UptimeForge Inc. All rights reserved.</div>"
                + "            </td>"
                + "          </tr>"
                + "        </table>"
                + "      </td>"
                + "    </tr>"
                + "  </table>"
                + "</body>"
                + "</html>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "UptimeForge Alerts");
            helper.setTo(destination);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Incident alert successfully sent via Brevo to {} [Subject: {}]", destination, subject);
        } catch (Exception e) {
            log.warn("HTML email dispatch failed, falling back to plain text for {}: {}", destination, e.getMessage());
            try {
                SimpleMailMessage plain = new SimpleMailMessage();
                plain.setFrom(fromEmail);
                plain.setTo(destination);
                plain.setSubject(subject);
                plain.setText("UptimeForge Alert: " + monitorName + " is " + eventType + "\n\nDetails: " + details + "\nTime: " + formattedTime);
                mailSender.send(plain);
            } catch (Exception ex) {
                log.error("Failed to send fallback alert email to {}: {}", destination, ex.getMessage());
            }
        }
    }

    private void sendSlackAlert(String webhookUrl, Monitor monitor, Incident incident, String eventType, String details) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        String emoji = "RECOVERY".equalsIgnoreCase(eventType) ? ":white_check_mark:" : ":rotating_light:";
        String monitorName = monitor != null ? monitor.getName() : "Service";

        payload.put("text", String.format("%s *[PulseWatch Alert]* `%s` is *%s*\n>%s", emoji, monitorName, eventType, details));

        String json = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("Slack webhook responded with HTTP " + response.statusCode());
        }
    }

    private void sendWebhookAlert(String webhookUrl, Monitor monitor, Incident incident, String eventType, String details) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("source", "PulseWatch");
        payload.put("eventType", eventType);
        payload.put("timestamp", Instant.now().toString());
        payload.put("details", details);
        if (monitor != null) {
            payload.put("monitorId", monitor.getId());
            payload.put("monitorName", monitor.getName());
            payload.put("monitorUrl", monitor.getUrl());
        }
        if (incident != null) {
            payload.put("incidentId", incident.getId());
            payload.put("status", incident.getStatus().name());
            payload.put("severity", incident.getSeverity().name());
        }

        String json = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("Webhook endpoint responded with HTTP " + response.statusCode());
        }
    }
}
