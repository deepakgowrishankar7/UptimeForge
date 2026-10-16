package com.pulsewatch.config;

import com.pulsewatch.entity.*;
import com.pulsewatch.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final MonitorRepository monitorRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final AlertConfigRepository alertConfigRepository;
    private final StatusPageRepository statusPageRepository;
    private final StatusPageMonitorRepository statusPageMonitorRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           MonitorRepository monitorRepository,
                           TeamRepository teamRepository,
                           TeamMemberRepository teamMemberRepository,
                           AlertConfigRepository alertConfigRepository,
                           StatusPageRepository statusPageRepository,
                           StatusPageMonitorRepository statusPageMonitorRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.monitorRepository = monitorRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.alertConfigRepository = alertConfigRepository;
        this.statusPageRepository = statusPageRepository;
        this.statusPageMonitorRepository = statusPageMonitorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            initUsersAndData();
        } catch (Exception e) {
            log.warn("Data initialization encountered non-fatal error: {}", e.getMessage());
        }
    }

    private void initUsersAndData() {
        // Admin user: admin@pulsewatch.io / Admin@123456
        User admin = userRepository.findByEmail("admin@pulsewatch.io").orElseGet(() -> {
            User u = new User();
            u.setName("System Administrator");
            u.setEmail("admin@pulsewatch.io");
            u.setPasswordHash(passwordEncoder.encode("Admin@123456"));
            u.setRole(Role.ADMIN);
            u.setEnabled(true);
            u.setEmailVerified(true);
            return userRepository.save(u);
        });
        // Ensure password matches Admin@123456
        if (!passwordEncoder.matches("Admin@123456", admin.getPasswordHash())) {
            admin.setPasswordHash(passwordEncoder.encode("Admin@123456"));
            userRepository.save(admin);
        }

        // Demo user: user@pulsewatch.io / User@123456
        User user = userRepository.findByEmail("user@pulsewatch.io").orElseGet(() -> {
            User u = new User();
            u.setName("Alex Turner");
            u.setEmail("user@pulsewatch.io");
            u.setPasswordHash(passwordEncoder.encode("User@123456"));
            u.setRole(Role.USER);
            u.setEnabled(true);
            u.setEmailVerified(true);
            return userRepository.save(u);
        });
        if (!passwordEncoder.matches("User@123456", user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode("User@123456"));
            userRepository.save(user);
        }

        // Initialize default team if none exists
        Team team;
        Optional<Team> existingTeam = teamRepository.findByOwner(user).stream().findFirst();
        if (existingTeam.isEmpty()) {
            team = new Team("Core Infrastructure", user);
            team = teamRepository.save(team);
            teamMemberRepository.save(new TeamMember(team, user, TeamRole.OWNER));
            teamMemberRepository.save(new TeamMember(team, admin, TeamRole.ADMIN));
        } else {
            team = existingTeam.get();
        }

        // Seed initial monitors if empty
        if (monitorRepository.count() == 0) {
            log.info("Seeding initial production health monitors...");

            Monitor m1 = new Monitor();
            m1.setOwner(user);
            m1.setTeam(team);
            m1.setName("Cloudflare DNS HTTP");
            m1.setDescription("Cloudflare public DNS over HTTPS probe");
            m1.setUrl("https://1.1.1.1");
            m1.setType(MonitorType.HTTPS);
            m1.setHttpMethod("GET");
            m1.setIntervalSeconds(60);
            m1.setTimeoutMs(5000);
            m1.setExpectedStatusCodes("200,301,302");
            m1.setCurrentStatus(MonitorStatus.UP);
            m1.setNextCheckAt(Instant.now());
            m1 = monitorRepository.save(m1);

            Monitor m2 = new Monitor();
            m2.setOwner(user);
            m2.setTeam(team);
            m2.setName("GitHub Public API");
            m2.setDescription("Checks GitHub REST API status");
            m2.setUrl("https://api.github.com");
            m2.setType(MonitorType.HTTPS);
            m2.setHttpMethod("GET");
            m2.setIntervalSeconds(60);
            m2.setTimeoutMs(8000);
            m2.setExpectedStatusCodes("200");
            m2.setCurrentStatus(MonitorStatus.UP);
            m2.setNextCheckAt(Instant.now());
            m2 = monitorRepository.save(m2);

            Monitor m3 = new Monitor();
            m3.setOwner(user);
            m3.setName("Google Public DNS TCP");
            m3.setDescription("TCP handshake check to Google DNS on port 53 (DNS)");
            m3.setUrl("8.8.8.8:53");
            m3.setType(MonitorType.TCP);
            m3.setIntervalSeconds(60);
            m3.setTimeoutMs(5000);
            m3.setCurrentStatus(MonitorStatus.UP);
            m3.setNextCheckAt(Instant.now());
            m3 = monitorRepository.save(m3);

            // Default Alert Config
            if (alertConfigRepository.findByOwner(user).isEmpty()) {
                AlertConfig alert = new AlertConfig(
                        user,
                        team,
                        AlertChannel.EMAIL,
                        "alerts@pulsewatch.io",
                        true,
                        3,
                        900,
                        true
                );
                alertConfigRepository.save(alert);
            }

            // Default Public Status Page
            if (statusPageRepository.findBySlug("system-status").isEmpty()) {
                StatusPage sp = new StatusPage(
                        user,
                        team,
                        "system-status",
                        "UptimeForge Platform Status",
                        "Real-time service health, uptime, and incident updates for public infrastructure.",
                        true
                );
                sp = statusPageRepository.save(sp);
                statusPageMonitorRepository.save(new StatusPageMonitor(sp, m1));
                statusPageMonitorRepository.save(new StatusPageMonitor(sp, m2));
            }
        }

        log.info("PulseWatch data initialization complete. Ready for requests.");
    }
}
