#
# activities-service - multi-stage build (no Java/Maven needed locally)
#

########## Stage 1: Build with Maven ##########
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

# Cache dependencies first
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Then copy sources
COPY src ./src

# Optional build args (useful for CI / tuning)
ARG MAVEN_ARGS="-DskipTests"
RUN mvn -q -e package ${MAVEN_ARGS}


########## Stage 2: Run with lightweight JRE ##########
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl \
  && addgroup -S appgroup \
  && adduser -S appuser -G appgroup

# Copy the built jar (boot fat jar)
COPY --from=build /workspace/target/*.jar /app/app.jar

USER appuser

EXPOSE 8087

# Optional runtime args
#
# Astuce perf JVM en conteneur:
# - UseContainerSupport: prise en compte des limites CPU/RAM du conteneur
# - MaxRAMPercentage: évite de réserver trop de heap par défaut
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE=""

#
# Healthcheck: Spring Boot Actuator (avec context-path=/api => /api/actuator/health)
# NB: nécessite spring-boot-starter-actuator (déjà présent dans le pom)
#
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=10 \
  CMD curl -fsS "http://localhost:8087/api/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar ${SPRING_PROFILES_ACTIVE:+--spring.profiles.active=$SPRING_PROFILES_ACTIVE}"]
