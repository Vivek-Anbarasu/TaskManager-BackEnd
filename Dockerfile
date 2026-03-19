# ─────────────────────────────────────────────────────────────
# Local Development Dockerfile
# Uses the pre-built JAR from target/ to avoid needing JDK 25
# inside Docker. Build the JAR first on the host:
#   mvn clean package -DskipTests
#
# For CI/CD full-build, switch to the multi-stage section below.
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Copy the pre-built fat JAR
COPY target/tasks-management-application-1.0-RELEASE.jar app.jar

EXPOSE 8080

# JVM tuning for containers (respects cgroup memory limits)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]


# ─────────────────────────────────────────────────────────────
# CI/CD multi-stage build (uncomment to use):
# Requires eclipse-temurin:25-jdk-alpine + Maven on build host
# ─────────────────────────────────────────────────────────────
# FROM eclipse-temurin:25-jdk-alpine AS builder
# RUN apk add --no-cache maven
# WORKDIR /app
# COPY pom.xml .
# RUN mvn dependency:go-offline -q
# COPY src ./src
# RUN mvn clean package -DskipTests -q
#
# FROM eclipse-temurin:25-jre-alpine
# WORKDIR /app
# COPY --from=builder /app/target/*.jar app.jar
# EXPOSE 8080
# ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-jar","app.jar"]
