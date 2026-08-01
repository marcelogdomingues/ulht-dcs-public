# Build Stage
#
# NOTE: the build context for this Dockerfile is the REPOSITORY ROOT (see
# docker-compose.*.yml -> build.context: . / dockerfile: credential-service/Dockerfile),
# because the service depends on the shared `ulht-commons` module which lives at
# the repo root and must be installed into the local Maven repo before the
# service is packaged.
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /build

# 1. Build & install the shared commons module into the local Maven repository.
COPY ulht-commons ./ulht-commons
RUN mvn -q -f ulht-commons/pom.xml -DskipTests install

# 2. Build the service (now that pt.ulusofona:ulht-commons is available).
COPY credential-service ./credential-service
RUN mvn -q -f credential-service/pom.xml clean package -DskipTests -Dmaven.test.skip=true

# Runtime Stage
FROM eclipse-temurin:25-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /build/credential-service/target/*.jar app.jar

# Create a non-root user and ensure app files are owned by it
RUN addgroup -S app && adduser -S app -G app && chown -R app:app /app
USER app

# Expose port and run the application with docker profile
EXPOSE 8086
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]

# Healthcheck to ensure service is running
HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8086/actuator/health || exit 1
