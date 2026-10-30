# ==============================================================================
# BankEase - Multi-Stage Production Dockerfile
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Angular Frontend
# ------------------------------------------------------------------------------
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend

# Cache npm dependencies
COPY frontend/package*.json ./
RUN npm ci --prefer-offline --no-audit

# Copy frontend source and compile production bundle
COPY frontend/ ./
RUN npm run build

# ------------------------------------------------------------------------------
# Stage 2: Build Spring Boot Application
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS backend-builder
WORKDIR /app

# Copy Maven wrapper and POM for dependency pre-fetching
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x ./mvnw

# Cache Maven dependencies
RUN ./mvnw dependency:go-offline -B || true

# Copy backend source
COPY src ./src

# Integrate compiled Angular production bundle into Spring Boot static resources
COPY --from=frontend-builder /app/frontend/dist/frontend/browser/* ./src/main/resources/static/

# Package executable JAR (unit tests run in CI/CD pipeline)
RUN ./mvnw clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 3: Minimal, Secure Production Runtime Image
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre AS runner
WORKDIR /app

# Install curl for container health check and cleanup apt cache
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create non-root system user for security compliance
RUN useradd -r -u 1001 -g root bankease && \
    mkdir -p /app && \
    chown -R bankease:root /app

# Copy standalone executable JAR
COPY --from=backend-builder --chown=bankease:root /app/target/bankease-backend-0.0.1-SNAPSHOT.jar app.jar

USER bankease

# Default server port
EXPOSE 8080

# Production Health Check probe
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/api/health || exit 1

# Container-optimized JVM execution
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]

