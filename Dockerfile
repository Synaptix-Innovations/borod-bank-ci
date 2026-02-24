# Multi-stage build for local development
FROM eclipse-temurin:17-jdk-alpine AS builder

LABEL maintainer="Vlad Vesninsky <Vlad774>"

WORKDIR /build

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Copy source code
COPY src src

# Build the application (skip tests for faster builds)
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Upgrade Alpine packages to pick up latest security patches
# Fixes: CVE-2026-1584 (gnutls), CVE-2026-25646 (libpng)
RUN apk upgrade --no-cache

EXPOSE 8080

ENV APP_HOME=/usr/src/app

WORKDIR $APP_HOME

# Copy the built JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

CMD ["java", "-jar", "app.jar"]
