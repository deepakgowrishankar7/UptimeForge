<div align="center">

  <img src="frontend/assets/logo.png" alt="UptimeForge Logo" width="96" height="96" style="border-radius: 16px; margin-bottom: 12px;">

  # UptimeForge
  ### Enterprise-Grade Synthetic Observability, Multi-Protocol Health Probing & Incident Defense

  [![Java](https://img.shields.io/badge/Java-25%20(Loom%20Virtual%20Threads)-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
  [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
  [![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
  [![Redis](https://img.shields.io/badge/Redis-Cache%20%26%20Locking-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
  [![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)
  [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=for-the-badge)](https://github.com/deepakgowrishankar7/UptimeForge/pulls)

  <p align="center">
    <strong>A high-throughput, developer-first uptime monitoring platform engineered from first principles.</strong><br>
    Continuous synthetic probing, sub-second latency percentiles (p50/p95/p99), autonomous SSRF defense, multi-channel alert deduplication, and branded public status pages.
  </p>

  <p align="center">
    <a href="#-architecture-overview">Architecture</a> •
    <a href="#-key-features">Key Features</a> •
    <a href="#-quick-start">Quick Start</a> •
    <a href="#-docker-deployment">Docker</a> •
    <a href="#-api-documentation">API Docs</a> •
    <a href="#-meet-the-creator">Meet the Creator</a>
  </p>

</div>

---

## 📖 The Vision: Why I Built UptimeForge

> *"During my journey as a **Computer Science and Engineering (CSE)** student, I encountered a common frustration in application monitoring: traditional tools felt unnecessarily complex and bloated, while basic uptime ping scripts offered little visibility beyond whether a service responded with HTTP 200.*
>
> *That inspired me to engineer **UptimeForge** — an autonomous, developer-first uptime monitoring and incident defense platform designed to make application reliability intuitive, precise, and transparent.*
>
> *Built with **Java 25** and **Spring Boot 3**, UptimeForge brings together automated multi-protocol health checks, sub-second latency percentiles, tenant-aware data access, cryptographic OTP verification, and instant incident dispatches into a cohesive SRE workspace.*
>
> ***Built for Developers. Designed for Reliability.***
> *Whether you are monitoring a personal API, an e-commerce backend, or a distributed microservices cluster, UptimeForge aims to give you a clearer view of service health and a faster path from detection to investigation."*
>
> — **Deepak Gowri Shankar Enduri**, Creator & Lead Architect

---

## ⚡ Key Features

| Category | Capability | Description |
| :--- | :--- | :--- |
| 🌐 **Probing Engine** | **Multi-Protocol Probes** | Monitor HTTP, HTTPS REST APIs, and raw TCP ports with custom headers, JSON body payloads, keyword assertions, and status code matchers. |
| 🛡️ **Security Defense** | **Autonomous SSRF Shield** | Deep network validator blocking RFC 1918 private subnets, loopbacks, link-local addresses, and AWS/GCP cloud metadata endpoints (`169.254.169.254`). |
| 📊 **High-Res Analytics** | **Latency Percentiles** | Real-time calculation of **p50, p90, p95, and p99** response times across sliding windows (24h, 7d, 30d) via Chart.js histograms. |
| 🔔 **Alert Engine** | **Smart Cooldown & Deduplication** | Dispatches to **Email (Brevo SMTP)**, **Slack Incoming Webhooks**, and **Custom Webhooks** with threshold hysteresis to eliminate alert storms. |
| 🏢 **Multi-Tenancy** | **Tenant-Aware Isolation** | Complete data scoping per organization with true 0% uptime, 0s MTTR, and clean empty states for brand-new accounts. |
| 📢 **Customer Trust** | **Public Status Pages** | Deploy customizable, public-facing status pages with custom slugs, live monitor telemetry, and real-time maintenance announcements. |
| 🔐 **Identity & Auth** | **Cryptographic OTP & JWT** | Role-Based Access Control (`ADMIN`, `USER`), secure BCrypt hashing, stateless JWT authentication, and 6-digit email OTPs. |

---

## 🏛️ Architecture Overview

UptimeForge is structured around domain-driven, layered clean architecture principles with complete separation of concerns:

```
+-----------------------------------------------------------------------------------+
|                              Web Browser Client                                   |
|   Vanilla ES6 Modules (api.js, auth.js, common.js), Responsive CSS3, Chart.js     |
+-----------------------------------------------------------------------------------+
                                         |
                                (HTTPS / REST API)
                                         v
+-----------------------------------------------------------------------------------+
|                        Nginx Reverse Proxy & Static Server                        |
|   SSL Termination, Gzip/Brotli, Security Headers, Static Asset Caching            |
+-----------------------------------------------------------------------------------+
                                         |
                           (Proxy pass to localhost:8080)
                                         v
+-----------------------------------------------------------------------------------+
|                        Spring Boot 3 Application Layer                           |
|                                                                                   |
|  [Security & Rate Limiting]                                                       |
|    - JwtAuthenticationFilter, JwtTokenProvider, SecurityConfig                    |
|    - RateLimiterService (Redis Token Bucket with In-Memory ConcurrentMap fallback)|
|    - SsrfValidator (Private IP, loopback, link-local, cloud metadata blocking)    |
|                                                                                   |
|  [REST Controllers (/api/v1/*)]                                                   |
|    - Auth, Users, Monitors, Incidents, Alerts, Notifications, Analytics,           |
|      Teams, StatusPages, PublicStatus, Admin                                      |
|                                                                                   |
|  [Service & Business Logic Layer]                                                 |
|    - MonitorService, TeamService, IncidentService, NotificationService            |
|    - AnalyticsService (p50/p90/p95/p99 latency calculations, uptime ratios)        |
|                                                                                   |
|  [Monitoring & Scheduling Engine]                                                 |
|    - MonitoringScheduler (Distributed Redis lock / atomic concurrency guard)      |
|    - MonitoringEngine (Virtual threads & bounded pools, timeout enforcement)      |
|    - HealthCheckers: HttpHealthChecker (Java 11+ HttpClient), TcpHealthChecker    |
|                                                                                   |
|  [Data Access Layer]                                                              |
|    - Spring Data JPA, Hibernate, 20 Normalized Relational Repositories            |
+-----------------------------------------------------------------------------------+
                           |                                |
            (JDBC Connection Pool)               (Lettuce Client)
                           v                                v
+---------------------------------------+    +--------------------------------------+
|            MySQL 8 Database           |    |             Redis Cache              |
| 20 Normalized Tables, FKs, Indexes,   |    | Rate Limiting, Distributed Locking,  |
| Flyway Automated Schema Migrations    |    | Session Coordination                 |
+---------------------------------------+    +--------------------------------------+
```

### Backend Package Layout
```
com.pulsewatch
├── PulseWatchApplication.java       # Application Bootstrapper
├── config/                          # Security, CORS, Redis, Async & Flyway configs
├── security/                        # JWT Provider, Auth Filter, UserPrincipal
├── validation/                      # SsrfValidator, URL, Header, and Port guards
├── entity/                          # 20 JPA Entities (User, Monitor, Incident, etc.)
├── entity/enums/                    # MonitorType, MonitorStatus, IncidentStatus, etc.
├── repository/                      # 20 Spring Data JPA Repositories
├── dto/                             # Request/Response DTOs & ApiResponse envelopes
├── service/                         # Monitor, Incident, Notification, Analytics
├── monitoring/                      # MonitoringEngine, HttpHealthChecker, TcpChecker
├── scheduler/                       # MonitoringScheduler with distributed locking
├── controller/                      # REST API Endpoints under /api/v1
└── exception/                       # GlobalExceptionHandler & domain exceptions
```

---

## 🛠️ Technology Stack

| Domain | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Java (OpenJDK) | 25 / 21 LTS / 17 | Core runtime leveraging virtual threads (Project Loom) |
| **Framework** | Spring Boot | 3.3.4 | Dependency injection, MVC, Scheduling, Async |
| **Security** | Spring Security & JJWT | 0.12.6 | Stateless JWT authentication, role guards, BCrypt |
| **Database** | MySQL Server | 8.0+ | High-durability transactional relational storage |
| **Migrations** | Flyway | 10.x | Idempotent, automated schema versioning (`V1__init_schema.sql`) |
| **Cache & Locks** | Redis / Redisson | 7.x | Distributed scheduler locks & token-bucket rate limiting |
| **Connection Pool**| HikariCP | 5.1.0 | High-performance JDBC connection pooling |
| **Email Relay** | Brevo (Sendinblue) | Cloud SMTP | Cryptographic OTP delivery & responsive HTML alert dispatches |
| **Frontend** | Vanilla ES6 Modules | Modern Web | Zero-bundle SPA navigation, reactive DOM updates, Chart.js |
| **API Spec** | OpenAPI & Swagger UI | 3.0 | Interactive API documentation at `/swagger-ui.html` |
| **Container** | Docker & Compose | Multi-Stage | One-click localized and cloud infrastructure orchestration |

---

## 🚀 Quick Start

### Prerequisites
- **JDK 17, 21, or 25** installed (`java -version`)
- **Maven 3.8+** installed (`mvn -version`)
- **MySQL 8.0+** running locally on port `3306`
- **Redis** running on port `6379` *(Optional: in-memory fallback enabled automatically if Redis is unavailable)*

### 1. Clone the Repository
```bash
git clone https://github.com/deepakgowrishankar7/UptimeForge.git
cd UptimeForge
```

### 2. Configure Environment Variables
Copy the sample environment file:
```bash
cp .env.example .env
```
Edit `.env` or set your environment variables:
```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=uptimeforge
DB_USER=root
DB_PASSWORD=your_mysql_password

REDIS_HOST=localhost
REDIS_PORT=6379

JWT_SECRET=YourUltraSecure64CharacterSecretKeyGoesHereForJwtSignatureVerification!
MAIL_HOST=smtp-relay.brevo.com
MAIL_PORT=587
MAIL_USERNAME=your_brevo_smtp_login
MAIL_PASSWORD=your_brevo_smtp_key
```

### 3. Build & Run
```bash
# Package the JAR skipping tests
mvn clean package -DskipTests

# Run with development profile
java -jar target/pulsewatch-1.0.0.jar --spring.profiles.active=dev
```

### 4. Access the Application
- **Landing Page & Creator Showcase:** [http://localhost:8080/index.html](http://localhost:8080/index.html)
- **Architectural Story:** [http://localhost:8080/about.html](http://localhost:8080/about.html)
- **Live Console / Dashboard:** [http://localhost:8080/dashboard.html](http://localhost:8080/dashboard.html)
- **Interactive Swagger Docs:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 🐳 Docker Deployment

Run the complete multi-tier production stack (Spring Boot + MySQL 8 + Redis + Nginx) with a single command:

```bash
docker-compose up -d --build
```

To stop the containers:
```bash
docker-compose down
```

---

## 📡 API Documentation

UptimeForge adheres strictly to RESTful conventions under `/api/v1/*`. Interactive documentation is available via OpenAPI 3.0 at `/swagger-ui.html`.

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :---: |
| `POST` | `/api/v1/auth/register` | Register new account & dispatch 6-digit OTP | No |
| `POST` | `/api/v1/auth/verify-registration` | Verify OTP and activate account | No |
| `POST` | `/api/v1/auth/login` | Authenticate with credentials and return JWT | No |
| `POST` | `/api/v1/auth/forgot-password` | Dispatch password reset verification OTP | No |
| `POST` | `/api/v1/auth/reset-password` | Reset password using verified OTP | No |
| `GET` | `/api/v1/monitors` | List tenant-scoped monitors with status & latency | Yes |
| `POST` | `/api/v1/monitors` | Create HTTP/TCP monitor with SSRF validation | Yes |
| `GET` | `/api/v1/monitors/{id}` | Retrieve monitor details, uptime ratio, and logs | Yes |
| `GET` | `/api/v1/analytics/overview` | Aggregated p50, p95, p99 percentiles & MTTR | Yes |
| `GET` | `/api/v1/incidents` | List active, acknowledged, and resolved incidents | Yes |
| `POST` | `/api/v1/alerts/{id}/test` | Trigger live verification alert to Email/Slack | Yes |
| `GET` | `/api/v1/public/status/{slug}` | Public status page telemetry feed | No |

---

## 👤 Meet the Creator

<div align="center">
  <img src="frontend/assets/deepak.jpg" alt="Deepak Gowri Shankar Enduri" width="160" height="160" style="border-radius: 20px; object-fit: cover; box-shadow: 0 8px 24px rgba(0,0,0,0.12); margin-bottom: 12px;">

  ### **Deepak Gowri Shankar Enduri**
  **Creator & Lead Architect** • *B.Tech in Computer Science & Engineering (CSE)*

  [![Portfolio](https://img.shields.io/badge/Personal%20Portfolio-2563EB?style=for-the-badge&logo=googlechrome&logoColor=white)](https://deepakgowrishankar7.github.io/Portfolio/)
  [![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/deepakgowrishankar7)
  [![Email](https://img.shields.io/badge/Direct%20Email-EA4335?style=for-the-badge&logo=gmail&logoColor=white)](mailto:deepakgowrishankar7@gmail.com)

  <p align="center">
    Passionate about distributed systems architecture, resilient backend microservices, and sub-second synthetic telemetry.<br>
    Engineered <strong>UptimeForge</strong> from first principles to provide developers with dependable, enterprise-grade observability without the enterprise bloat.
  </p>
</div>

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) — see the LICENSE file for details.
