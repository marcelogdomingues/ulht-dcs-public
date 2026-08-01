# Build Stage
#
# NOTE: the build context for this Dockerfile is the REPOSITORY ROOT (see
# docker-compose.*.yml -> build.context: . / dockerfile: student-service/Dockerfile),
# because the service depends on the shared `ulht-commons` module which lives at
# the repo root and must be installed into the local Maven repo before the
# service is packaged.
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /build

# 1. Build & install the shared commons module into the local Maven repository.
COPY ulht-commons ./ulht-commons
RUN mvn -q -f ulht-commons/pom.xml -DskipTests install

# 2. Build the service (now that pt.ulusofona:ulht-commons is available).
COPY student-service ./student-service
RUN mvn -q -f student-service/pom.xml clean package -DskipTests -Dmaven.test.skip=true

# Runtime Stage
FROM eclipse-temurin:25-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /build/student-service/target/*.jar app.jar

# Create a non-root user and ensure app files are owned by it
RUN addgroup -S app && adduser -S app -G app && chown -R app:app /app
USER app

# Expose port
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/api/v1/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
