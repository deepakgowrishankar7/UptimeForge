package com.pulsewatch.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.from:deepakgowrishankar7@gmail.com}")
    private String fromEmail;

    @Value("${spring.mail.username:${SMTP_USER:be6987001@smtp-brevo.com}}")
    private String smtpUser;

    @Value("${spring.mail.password:${SMTP_PASS:}}")
    private String smtpPass;

    @Value("${spring.mail.host:${SMTP_HOST:smtp-relay.brevo.com}}")
    private String smtpHost;

    @Value("${spring.mail.port:${SMTP_PORT:587}}")
    private int smtpPort;

    @Value("${brevo.api.key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    /**
     * Dispatches a 6-digit Registration Verification Code via Brevo SMTP
     */
    @Async
    public void sendRegistrationOtp(String toEmail, String name, String otpCode) {
        String recipientName = (name != null && !name.trim().isEmpty()) ? name.trim() : "Developer";
        String subject = "[UptimeForge] Your Verification Code: " + otpCode;

        String htmlContent = buildOtpEmailTemplate(
                recipientName,
                "Welcome to UptimeForge",
                "Verify your work email to activate your account and start monitoring APIs.",
                otpCode,
                "10 minutes"
        );

        sendHtmlEmail(toEmail, subject, htmlContent, otpCode);
    }

    /**
     * Dispatches a 6-digit Password Reset Code via Brevo SMTP
     */
    @Async
    public void sendPasswordResetOtp(String toEmail, String name, String otpCode) {
        String recipientName = (name != null && !name.trim().isEmpty()) ? name.trim() : "User";
        String subject = "[UptimeForge Security] Password Reset Code: " + otpCode;

        String htmlContent = buildOtpEmailTemplate(
                recipientName,
                "Password Reset Verification",
                "You recently requested to reset your UptimeForge account password. Use the verification code below to proceed.",
                otpCode,
                "15 minutes"
        );

        sendHtmlEmail(toEmail, subject, htmlContent, otpCode);
    }

    /**
     * Dispatches a Security Alert Email when password has been modified
     */
    @Async
    public void sendPasswordChangedAlert(String toEmail, String name, String ipAddress) {
        String recipientName = (name != null && !name.trim().isEmpty()) ? name.trim() : "User";
        String subject = "[UptimeForge Security Alert] Your account password was changed";
        String formattedTime = DateTimeFormatter.RFC_1123_DATE_TIME
                .withZone(ZoneId.of("UTC"))
                .format(Instant.now());

        String htmlContent = buildSecurityNoticeTemplate(recipientName, formattedTime, ipAddress);

        sendHtmlEmail(toEmail, subject, htmlContent, null);
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent, String otpForLog) {
        log.info("Initiating email dispatch to {} [Subject: {}]", toEmail, subject);
        if (otpForLog != null) {
            log.info("==================================================================");
            log.info(">>> [LIVE OTP CODE FOR {}]: {} <<<", toEmail, otpForLog);
            log.info("==================================================================");
        }

        boolean sent = false;

        // Channel 1: Brevo HTTPS REST API (Port 443 - NEVER blocked by cloud firewalls or free tiers)
        if (brevoApiKey != null && !brevoApiKey.trim().isEmpty()) {
            sent = sendViaBrevoApi(toEmail, subject, htmlContent);
        }

        // Channel 2: Standard configured mailSender (default port 587 STARTTLS)
        if (!sent && mailSender != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(fromEmail, "UptimeForge Security");
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("Email successfully dispatched via Brevo SMTP (Port {}) to {} [Subject: {}]", smtpPort, toEmail, subject);
                sent = true;
            } catch (Exception e) {
                log.warn("Primary dispatch (Port {}) failed to {}: {}. Attempting SSL fallback (Port 465)...", smtpPort, toEmail, e.getMessage());
            }
        }

        // Channel 3: Port 2525 STARTTLS (specifically designed for cloud hosts where 587/465 are filtered)
        if (!sent && smtpPass != null && !smtpPass.trim().isEmpty() && smtpPort != 2525) {
            sent = sendViaStarttlsPort(toEmail, subject, htmlContent, 2525);
        }

        // Channel 4: Direct SMTPS SSL (Port 465) fallback if primary port was blocked or failed
        if (!sent && smtpPass != null && !smtpPass.trim().isEmpty() && smtpPort != 465) {
            sent = sendViaSmtpsFallback(toEmail, subject, htmlContent);
        }

        // Channel 5: Port 587 STARTTLS (if not already attempted)
        if (!sent && smtpPass != null && !smtpPass.trim().isEmpty() && smtpPort != 587) {
            sent = sendViaStarttlsPort(toEmail, subject, htmlContent, 587);
        }

        if (!sent) {
            log.error("Failed to deliver email to {}. All SMTP and API channels failed or unconfigured.", toEmail);
            if (otpForLog != null) {
                log.warn("==================================================================");
                log.warn(">>> [LIVE OTP CODE FOR {}]: {} <<<", toEmail, otpForLog);
                log.warn("==================================================================");
            }
        }
    }

    private boolean sendViaStarttlsPort(String toEmail, String subject, String htmlContent, int port) {
        try {
            org.springframework.mail.javamail.JavaMailSenderImpl customSender = new org.springframework.mail.javamail.JavaMailSenderImpl();
            customSender.setHost(smtpHost != null && !smtpHost.trim().isEmpty() ? smtpHost.trim() : "smtp-relay.brevo.com");
            customSender.setPort(port);
            customSender.setUsername(smtpUser);
            customSender.setPassword(smtpPass);
            customSender.setDefaultEncoding("UTF-8");

            java.util.Properties props = customSender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.trust", "*");
            props.put("mail.smtp.connectiontimeout", "8000");
            props.put("mail.smtp.timeout", "8000");
            props.put("mail.smtp.writetimeout", "8000");

            MimeMessage message = customSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "UptimeForge Security");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            customSender.send(message);
            log.info("Email successfully dispatched via Brevo SMTP (Port {}) to {} [Subject: {}]", port, toEmail, subject);
            return true;
        } catch (Exception ex) {
            log.warn("Dispatch via Port {} failed for {}: {}", port, toEmail, ex.getMessage());
            return false;
        }
    }

    private boolean sendViaBrevoApi(String toEmail, String subject, String htmlContent) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(8))
                    .build();

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("sender", java.util.Map.of("name", "UptimeForge Security", "email", fromEmail));
            payload.put("to", java.util.List.of(java.util.Map.of("email", toEmail)));
            payload.put("subject", subject);
            payload.put("htmlContent", htmlContent);

            String requestBody = mapper.writeValueAsString(payload);

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("accept", "application/json")
                    .header("api-key", brevoApiKey.trim())
                    .header("content-type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody, java.nio.charset.StandardCharsets.UTF_8))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Email successfully dispatched via Brevo REST API (HTTPS port 443) to {}", toEmail);
                return true;
            } else {
                log.warn("Brevo REST API returned HTTP {}: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception ex) {
            log.warn("Brevo REST API dispatch failed for {}: {}", toEmail, ex.getMessage());
            return false;
        }
    }

    private boolean sendViaSmtpsFallback(String toEmail, String subject, String htmlContent) {
        try {
            org.springframework.mail.javamail.JavaMailSenderImpl sslSender = new org.springframework.mail.javamail.JavaMailSenderImpl();
            sslSender.setHost(smtpHost != null && !smtpHost.trim().isEmpty() ? smtpHost.trim() : "smtp-relay.brevo.com");
            sslSender.setPort(465);
            sslSender.setUsername(smtpUser);
            sslSender.setPassword(smtpPass);
            sslSender.setDefaultEncoding("UTF-8");

            java.util.Properties props = sslSender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", "*");
            props.put("mail.smtp.connectiontimeout", "8000");
            props.put("mail.smtp.timeout", "8000");
            props.put("mail.smtp.writetimeout", "8000");

            MimeMessage message = sslSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "UptimeForge Security");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            sslSender.send(message);
            log.info("Email successfully dispatched via Brevo SMTPS (Port 465 SSL) to {} [Subject: {}]", toEmail, subject);
            return true;
        } catch (Exception ex) {
            log.error("Port 465 SSL fallback dispatch also failed for {}: {}", toEmail, ex.getMessage(), ex);
            return false;
        }
    }

    private String buildOtpEmailTemplate(String recipientName, String title, String subtitle, String otpCode, String expiry) {
        return "<!DOCTYPE html>"
                + "<html lang='en'>"
                + "<head>"
                + "  <meta charset='UTF-8'>"
                + "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "  <title>" + title + "</title>"
                + "</head>"
                + "<body style='margin:0; padding:0; background-color:#0B0F19; font-family:-apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing:antialiased;'>"
                + "  <table width='100%' cellpadding='0' cellspacing='0' style='background-color:#0B0F19; padding:48px 16px;'>"
                + "    <tr>"
                + "      <td align='center'>"
                + "        <!-- Main Card Container -->"
                + "        <table width='100%' cellpadding='0' cellspacing='0' style='max-width:560px; background-color:#FFFFFF; border-radius:18px; overflow:hidden; box-shadow:0 25px 50px -12px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 255, 255, 0.1);'>"
                + "          <!-- Executive Header -->"
                + "          <tr>"
                + "            <td style='background:linear-gradient(135deg, #0F172A 0%, #1E293B 100%); padding:32px 40px; border-bottom:3px solid #2563EB;'>"
                + "              <table width='100%' cellpadding='0' cellspacing='0'>"
                + "                <tr>"
                + "                  <td>"
                + "                    <div style='font-size:24px; font-weight:800; color:#FFFFFF; letter-spacing:-0.5px; font-family:-apple-system, BlinkMacSystemFont, sans-serif;'>"
                + "                      Uptime<span style='color:#38BDF8;'>Forge</span>"
                + "                    </div>"
                + "                    <div style='font-size:12px; color:#94A3B8; margin-top:4px; font-weight:600; text-transform:uppercase; letter-spacing:1px;'>"
                + "                      Autonomous Synthetic Reliability"
                + "                    </div>"
                + "                  </td>"
                + "                  <td align='right'>"
                + "                    <span style='background:rgba(16, 185, 129, 0.15); border:1px solid rgba(16, 185, 129, 0.35); color:#34D399; font-size:11px; font-weight:700; padding:5px 12px; border-radius:20px; text-transform:uppercase; letter-spacing:0.8px;'>"
                + "                      ● Identity Guard"
                + "                    </span>"
                + "                  </td>"
                + "                </tr>"
                + "              </table>"
                + "            </td>"
                + "          </tr>"
                + "          <!-- Main Body -->"
                + "          <tr>"
                + "            <td style='padding:40px 40px 32px 40px; background-color:#FFFFFF; text-align:left;'>"
                + "              <h1 style='margin:0 0 14px 0; font-size:22px; font-weight:800; color:#0F172A; letter-spacing:-0.4px;'>"
                + "                " + title
                + "              </h1>"
                + "              <p style='margin:0 0 18px 0; font-size:15px; line-height:1.6; color:#334155;'>"
                + "                Hello <strong>" + recipientName + "</strong>,"
                + "              </p>"
                + "              <p style='margin:0 0 28px 0; font-size:14.5px; line-height:1.6; color:#475569;'>"
                + "                " + subtitle
                + "              </p>"
                + "              <!-- High-Contrast Passcode Box -->"
                + "              <div style='background:linear-gradient(180deg, #F8FAFC 0%, #F1F5F9 100%); border:1.5px solid #CBD5E1; border-radius:14px; padding:28px 20px; text-align:center; margin:28px 0; box-shadow:inset 0 2px 4px rgba(0, 0, 0, 0.02);'>"
                + "                <div style='font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:1.5px; color:#64748B; margin-bottom:12px;'>"
                + "                  Single-Use Verification Passcode"
                + "                </div>"
                + "                <div style='font-size:42px; font-weight:800; letter-spacing:14px; color:#1D4ED8; font-family:ui-monospace, Menlo, Monaco, Consolas, \"Liberation Mono\", monospace; padding-left:14px; margin:4px 0; text-shadow:0 1px 2px rgba(29, 78, 216, 0.15);'>"
                + "                  " + otpCode
                + "                </div>"
                + "                <div style='margin-top:14px;'>"
                + "                  <span style='display:inline-block; background:#EFF6FF; border:1px solid #BFDBFE; color:#1E40AF; font-size:12px; font-weight:600; padding:4px 14px; border-radius:20px;'>"
                + "                    ⏱ Valid for " + expiry + " · One-time access"
                + "                  </span>"
                + "                </div>"
                + "              </div>"
                + "              <!-- Security Callout Box -->"
                + "              <div style='background-color:#FFFBEB; border:1px solid #FDE68A; border-radius:12px; padding:16px 18px; margin:24px 0;'>"
                + "                <table cellpadding='0' cellspacing='0' width='100%'>"
                + "                  <tr>"
                + "                    <td width='24' valign='top' style='padding-top:2px; font-size:16px;'>🛡️</td>"
                + "                    <td style='padding-left:10px; font-size:13px; color:#92400E; line-height:1.5; font-weight:500;'>"
                + "                      <strong>Security Protection:</strong> Never share this code with anyone. UptimeForge engineers will never request your verification passcode by phone, email, or Slack."
                + "                    </td>"
                + "                  </tr>"
                + "                </table>"
                + "              </div>"
                + "              <p style='margin:0; font-size:13px; color:#94A3B8; line-height:1.5;'>"
                + "                If you did not initiate this request, you can safely disregard this email. Your account credentials remain protected."
                + "              </p>"
                + "            </td>"
                + "          </tr>"
                + "          <!-- Enterprise Footer -->"
                + "          <tr>"
                + "            <td style='background-color:#F8FAFC; border-top:1px solid #E2E8F0; padding:24px 40px; text-align:center; font-size:12px; color:#64748B; line-height:1.6;'>"
                + "              <div style='font-weight:600; color:#334155; margin-bottom:4px;'>"
                + "                UptimeForge Platform &bull; Autonomous Synthetic Reliability Engine"
                + "              </div>"
                + "              <div>"
                + "                Delivered securely via Brevo High-Deliverability Relays &bull; TLS 1.3 Encrypted"
                + "              </div>"
                + "              <div style='margin-top:8px; font-size:11px; color:#94A3B8;'>"
                + "                &copy; 2026 UptimeForge Inc. All rights reserved."
                + "              </div>"
                + "            </td>"
                + "          </tr>"
                + "        </table>"
                + "      </td>"
                + "    </tr>"
                + "  </table>"
                + "</body>"
                + "</html>";
    }

    private String buildSecurityNoticeTemplate(String recipientName, String formattedTime, String ipAddress) {
        return "<!DOCTYPE html>"
                + "<html lang='en'>"
                + "<head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>"
                + "<body style='margin:0; padding:0; background-color:#0B0F19; font-family:-apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif;'>"
                + "  <table width='100%' cellpadding='0' cellspacing='0' style='background-color:#0B0F19; padding:48px 16px;'>"
                + "    <tr>"
                + "      <td align='center'>"
                + "        <table width='100%' cellpadding='0' cellspacing='0' style='max-width:560px; background-color:#FFFFFF; border-radius:18px; overflow:hidden; box-shadow:0 25px 50px -12px rgba(0, 0, 0, 0.5);'>"
                + "          <tr>"
                + "            <td style='background:linear-gradient(135deg, #0F172A 0%, #1E293B 100%); padding:32px 40px; border-bottom:3px solid #DC2626;'>"
                + "              <table width='100%' cellpadding='0' cellspacing='0'>"
                + "                <tr>"
                + "                  <td>"
                + "                    <div style='font-size:24px; font-weight:800; color:#FFFFFF;'>Uptime<span style='color:#38BDF8;'>Forge</span></div>"
                + "                    <div style='font-size:12px; color:#94A3B8; margin-top:4px; font-weight:600; text-transform:uppercase; letter-spacing:1px;'>Security Operations Center</div>"
                + "                  </td>"
                + "                  <td align='right'>"
                + "                    <span style='background:rgba(220, 38, 38, 0.18); border:1px solid rgba(220, 38, 38, 0.4); color:#F87171; font-size:11px; font-weight:700; padding:5px 12px; border-radius:20px; text-transform:uppercase; letter-spacing:0.8px;'>"
                + "                      ● Security Alert"
                + "                    </span>"
                + "                  </td>"
                + "                </tr>"
                + "              </table>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style='padding:40px 40px 32px 40px; background-color:#FFFFFF; text-align:left;'>"
                + "              <h1 style='margin:0 0 14px 0; font-size:21px; font-weight:800; color:#0F172A;'>Your Account Password Was Changed</h1>"
                + "              <p style='margin:0 0 16px 0; font-size:15px; line-height:1.6; color:#334155;'>Hello <strong>" + recipientName + "</strong>,</p>"
                + "              <p style='margin:0 0 24px 0; font-size:14.5px; line-height:1.6; color:#475569;'>"
                + "                This email confirms that the password for your UptimeForge account was recently modified."
                + "              </p>"
                + "              <!-- Audit Details Box -->"
                + "              <table width='100%' cellpadding='0' cellspacing='0' style='background-color:#F8FAFC; border:1px solid #E2E8F0; border-radius:12px; margin:24px 0;'>"
                + "                <tr>"
                + "                  <td style='padding:14px 20px; border-bottom:1px solid #E2E8F0; font-size:13px; color:#64748B; font-weight:600; width:140px;'>Event Timestamp</td>"
                + "                  <td style='padding:14px 20px; border-bottom:1px solid #E2E8F0; font-size:13px; color:#0F172A; font-weight:600; font-family:monospace;'>" + formattedTime + "</td>"
                + "                </tr>"
                + "                <tr>"
                + "                  <td style='padding:14px 20px; font-size:13px; color:#64748B; font-weight:600;'>Originating IP</td>"
                + "                  <td style='padding:14px 20px; font-size:13px; color:#0F172A; font-weight:600; font-family:monospace;'>" + (ipAddress != null ? ipAddress : "Encrypted Client Session") + "</td>"
                + "                </tr>"
                + "              </table>"
                + "              <!-- Red Warning Box -->"
                + "              <div style='background-color:#FEF2F2; border:1px solid #FECACA; border-radius:12px; padding:16px 18px; margin:24px 0;'>"
                + "                <div style='font-size:13px; color:#991B1B; line-height:1.5; font-weight:600;'>"
                + "                  🚨 Did not make this change? Someone may have accessed your account. Reset your password immediately and contact security support."
                + "                </div>"
                + "              </div>"
                + "            </td>"
                + "          </tr>"
                + "          <tr>"
                + "            <td style='background-color:#F8FAFC; border-top:1px solid #E2E8F0; padding:22px 40px; text-align:center; font-size:12px; color:#64748B;'>"
                + "              UptimeForge Security Operations &bull; &copy; 2026 UptimeForge Inc."
                + "            </td>"
                + "          </tr>"
                + "        </table>"
                + "      </td>"
                + "    </tr>"
                + "  </table>"
                + "</body>"
                + "</html>";
    }
}
