# Build Stage
FROM maven:3-eclipse-temurin-26 AS builder

# Set the working directory
WORKDIR /build

# Copy the project files
COPY pom.xml ./pom.xml
COPY src ./src

# Build the project
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# Runtime Stage
FROM eclipse-temurin:25-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

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

