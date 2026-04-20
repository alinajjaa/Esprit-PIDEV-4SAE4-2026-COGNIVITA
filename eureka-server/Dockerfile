# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache dependencies separately from source code.
# Docker will reuse this layer on rebuilds as long as pom.xml hasn't changed.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Now copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# curl is needed for the Docker health check
RUN apk add --no-cache curl \
 && addgroup -S appgroup \
 && adduser  -S appuser -G appgroup

COPY --from=builder /app/target/*.jar app.jar

USER appuser
EXPOSE 8761

# Tune JVM for container environments:
#   -XX:+UseContainerSupport   → respect Docker memory limits
#   -XX:MaxRAMPercentage=75.0  → use up to 75 % of container RAM
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
