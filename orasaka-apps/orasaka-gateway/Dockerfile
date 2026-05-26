# ==============================================================================
# ORASAKA — Dockerfile (Gateway BFF Backend)
# ==============================================================================
# Multi-stage container build targeting Eclipse Temurin JRE 21.
# Compiles all internal framework dependencies hermetically.
# Respects zero-trust execution rules by enforcing a non-root user.
# ==============================================================================

# ── Build Stage ──────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy configuration and source files
COPY pom.xml .
COPY orasaka-framework orasaka-framework
COPY orasaka-apps/orasaka-gateway orasaka-apps/orasaka-gateway

# Compile the gateway module and its local framework dependencies
RUN mvn clean package -pl orasaka-apps/orasaka-gateway -am -DskipTests

# ── Runtime Stage ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-privileged system group and user
RUN addgroup -S orasaka && adduser -S orasaka -G orasaka

# Configure secure JVM options for containerized environments
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxRAMPercentage=75.0 -XX:MinRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"
ENV PORT=8080

# Expose HTTP port
EXPOSE 8080

# Copy executable jar from builder stage
COPY --from=builder /app/orasaka-apps/orasaka-gateway/target/orasaka-gateway-*.jar gateway.jar

# Enforce secure ownership on workspace files
RUN chown -R orasaka:orasaka /app

# Switch context to the non-root user
USER orasaka

# Launch the Spring Boot application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar gateway.jar"]
