# DCS SIS Service

## Overview

The DCS SIS Service is the integration layer to the university's Student Information System (SIGES). It fetches student academic data (enrolment, grades, credits, evaluations) on demand and feeds it into the credential-issuance pipeline over Kafka, so the rest of the platform never talks to the SIS directly.

## Key Features

- **Student Authentication**: Secure login and registration for students
- **Academic Data Retrieval**: Access to grades, enrollments, schedules, and evaluations from SIGES
- **Digital Credentials**: Integration with WaltID for digital credential management
- **Real-time Updates**: Kafka-based messaging for real-time data synchronization
- **OpenAPI Documentation**: Comprehensive API documentation with Swagger UI
- **Resilient Integration**: Circuit breakers and retry patterns for external services
- **Avro Schema**: Strongly-typed messaging with Apache Avro

## Technology Stack

- **Java 25**: Latest Java version with enhanced features
- **Spring Boot 3.5.6**: Modern Spring Boot framework
- **Spring Cloud 2025.0.0**: Cloud-native patterns and service discovery
- **Kafka**: Event-driven messaging with Avro serialization
- **OpenFeign**: Declarative HTTP client for external APIs
- **Apache Avro**: Schema-based serialization
- **Resilience4j**: Circuit breakers and retry patterns
- **Consul**: Service discovery and configuration management
- **OpenAPI 3.0**: API documentation and client generation

## Architecture

The service follows a layered architecture:

```
├── Controller Layer: REST API endpoints
├── Service Layer: Business logic and validation
├── Client Layer: External service integration (SIGES)
├── Kafka Layer: Event-driven messaging
├── Converter Layer: Data transformation and mapping
└── Configuration Layer: Application configuration
```

## Quick Start

### Prerequisites

- Java 25
- Maven 3.9+
- Docker (optional)
- Kafka 3.x with Schema Registry
- Consul (for service discovery)

### Running Locally

1. **Build the project:**
   ```bash
   mvn clean package
   ```

2. **Run the application:**
   ```bash
   java -jar target/sis-service-0.0.1-SNAPSHOT.jar
   ```

3. **Access the API documentation:**
   - Swagger UI: http://localhost:8085/api/v1/swagger-ui.html
   - OpenAPI Docs: http://localhost:8085/api/v1/api-docs

### Running with Docker

1. **Build the Docker image:**
   ```bash
   docker build -t sis-service:latest .
   ```

2. **Run the container:**
   ```bash
   docker run -p 8085:8085 sis-service:latest
   ```

## Configuration

The service can be configured through `application.yml`. Key configuration properties:

```yaml
server:
  port: 8085
  servlet:
    context-path: /api/v1

spring:
  application:
    name: sis-service
  kafka:
    bootstrap-servers: localhost:29092
  cloud:
    openfeign:
      client:
        config:
          sisClient:
            url: https://university-sis.example.edu/api
```

## API Endpoints

### Student Services

- `POST /student/enrolment` - Get student enrollments
- `POST /student/schedule` - Get student class schedule
- `POST /student/grades` - Get student grades
- `POST /student/evaluations` - Get student evaluations
- `POST /student/course-credits` - Get student course credits

### Authentication

- `POST /student/login` - Student login and credential workflow
- `POST /student/registration` - Student registration

### System Endpoints

- `GET /health` - Service health check
- `GET /actuator/health` - Spring Boot actuator health

## Data Models

### Student Service Request

```json
{
  "userName": "a12345678",
  "installKey": "00000_0000000000000",
  "language": "PT",
  "platform": "ios",
  "application": "com.example.dcs.mobile",
  "versionCode": "1601206"
}
```

### Student Enrollments Response

```json
{
  "count": 1,
  "enrolmentList": [
    {
      "academicYear": "2023/2024",
      "courseName": "Licenciatura em Engenharia Informática",
      "curricularUnitName": "Programação I",
      "curricularUnitCode": 1001,
      "className": "LEI001",
      "curricularYear": 1,
      "ects": 6,
      "programme": "LEI"
    }
  ],
  "errorCode": "SUCCESS"
}
```

## Kafka Integration

The service uses Kafka for event-driven processing with Avro serialization:

### Topics

- `credential-workflow-events`: Credential workflow requests
- `student-events`: Student data change events
- `university-registration-events`: Registration events

### Avro Schemas

Located in `src/main/resources/avro/`:

- `StudentEvent.avsc`: Student event schema
- `UniversityRegistrationRequest.avsc`: Registration request schema
- `UniversityServiceCommonRequest.avsc`: Common service request schema

### Message Production

```java
@Autowired
private CredentialWorkflowProducer producer;

public void initiateWorkflow(StudentServiceRequest request) {
    CredentialWorkflowRequest workflowRequest = CredentialWorkflowRequest.builder()
        .correlationId(UUID.randomUUID().toString())
        .userName(request.getUserName())
        .build();
    
    producer.sendWorkflowRequest(workflowRequest);
}
```

## External Service Integration

### SIGES Integration

The service integrates with SIGES (Sistema de Gestão de Estudantes) through OpenFeign:

```java
@FeignClient(name = "sisClient", configuration = ClientConfiguration.class)
public interface SisClient {
    
    @PostMapping("/students/enrolment")
    StudentEnrolments getEnrolments(@RequestBody StudentServiceRequest request);
    
    @PostMapping("/students/schedule")
    StudentSchedule getSchedule(@RequestBody StudentServiceRequest request);
}
```

### Resilience Patterns

- **Circuit Breaker**: Prevents cascading failures
- **Retry**: Automatic retry with exponential backoff
- **Timeout**: Request timeout protection
- **Fallback**: Graceful degradation

## Monitoring and Observability

### Health Checks

- Spring Boot Actuator endpoints
- Custom health indicators
- Circuit breaker status
- Kafka connectivity

### Logging

- Structured logging with correlation IDs
- Feign request/response logging
- Configurable log levels
- Pattern: `%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n`

## Development

### Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── pt/usis/digital/wallet/
│   │       ├── controller/      # REST controllers
│   │       ├── service/         # Business services
│   │       ├── clients/         # Feign clients
│   │       ├── kafka/           # Kafka producers/consumers
│   │       ├── config/          # Configuration classes
│   │       ├── domain/          # Domain models
│   │       ├── dto/             # Data transfer objects
│   │       ├── converter/       # Data converters
│   │       ├── exception/       # Exception handlers
│   │       └── validation/      # Custom validators
│   └── resources/
│       ├── application.yml      # Main configuration
│       ├── application-docker.yml # Docker configuration
│       ├── avro/                # Avro schemas
│       ├── openapi/
│       │   └── api-spec.yaml    # OpenAPI specification
│       └── banner.txt           # Application banner
└── test/
    ├── java/                    # Test classes
    └── resources/
        └── application-test.yaml # Test configuration
```

### Testing

Run tests with:
```bash
mvn test
```

### OpenAPI Generator

The service uses OpenAPI Generator to generate API interfaces from the YAML specification:

```bash
mvn clean compile
```

Generated code is placed in `target/generated-sources/openapi/`.

## Deployment

### Docker Deployment

The service includes a multi-stage Dockerfile for optimized builds:

```dockerfile
# Build stage
FROM maven:3.9.9-amazoncorretto-25 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM amazoncorretto:25
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Environment Variables

Key environment variables:

- `SPRING_PROFILES_ACTIVE`: Active Spring profile (default, docker, prod)
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers
- `SIS_SERVICE_URL`: SIGES service URL
- `CONSUL_HOST`: Consul host for service discovery

## Security

- Student credentials authentication
- Input validation on all endpoints
- HTTPS support for production deployments
- Rate limiting capabilities
- Secure cookie handling

## Performance

- Connection pooling for external services
- Async processing with Kafka
- Circuit breakers for external dependencies
- Efficient data serialization with Avro
- Caching for frequently accessed data

## Error Handling

### Error Codes

Comprehensive error code system:

- `TE-001`: Student not found
- `TE-002`: Validation failed
- `TE-003`: Invalid student credentials
- `TE-005`: Student already registered
- `TE-008`: Unexpected error
- `TE-013`: External service unavailable

### Error Response Format

```json
{
  "status": "error",
  "message": "Student not found",
  "errorCode": "TE-001",
  "statusCode": 404,
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/student/enrolment"
}
```

## Troubleshooting

### Common Issues

1. **SIGES connection failed**
   - Verify SIGES service URL
   - Check network connectivity
   - Review circuit breaker status

2. **Kafka Schema Registry error**
   - Verify Schema Registry is running
   - Check schema compatibility
   - Review Avro schema definitions

3. **Student not found**
   - Verify student credentials
   - Check installKey validity
   - Review SIGES system status

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

MIT License - see LICENSE file for details

## Support

For technical support or questions:
- Email: dev@usis.pt
- Documentation: https://docs.usis.pt/digital-wallet

## Related Services

- **Credential Service**: Digital credential issuance and management
- **Fulfilment Service**: Workflow tracking and notifications
- **SIGES**: University information system
