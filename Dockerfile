# PulseWatch Multi-Stage Dockerfile
# Stage 1: Build application using Maven
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache dependency layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production jar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal, secure JRE runtime container
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create non-root system user for security
RUN groupadd -r pulsewatch && useradd -r -g pulsewatch -d /app -s /sbin/nologin pulsewatch

COPY --from=builder /app/target/pulsewatch-*.jar app.jar
RUN chown -R pulsewatch:pulsewatch /app

USER pulsewatch

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
