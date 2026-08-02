# DCS Fulfilment Service

## Overview

The DCS Fulfilment Service provides workflow fulfilment and progress tracking capabilities for the DCS digital credential ecosystem. It enables real-time monitoring of credential workflow processes through Server-Sent Events (SSE) and REST endpoints for status checking.

## Key Features

- **Real-time Progress Tracking**: SSE-based real-time workflow updates
- **Workflow Status Monitoring**: REST endpoints for polling-based status checking
- **Event-Driven Architecture**: Kafka-based event processing
- **High Availability**: Scalable SSE connection management
- **Health Monitoring**: Comprehensive health check endpoints
- **OpenAPI Documentation**: Complete API documentation with Swagger UI

## Technology Stack

- **Java 25**: Latest Java version with enhanced features
- **Spring Boot 3.5.6**: Modern Spring Boot framework
- **Spring Cloud 2025.0.0**: Cloud-native patterns and service discovery
- **Kafka**: Event-driven messaging
- **Server-Sent Events (SSE)**: Real-time browser communication
- **OpenAPI 3.0**: API documentation and client generation
- **Consul**: Service discovery and configuration management

## Architecture

The service follows a simple, focused architecture:

```
├── Controller Layer: REST API and SSE endpoints
├── Service Layer: Workflow tracking and SSE management
├── Kafka Layer: Event consumption
└── Configuration Layer: Application configuration
```

## Quick Start

### Prerequisites

- Java 25
- Maven 3.9+
- Docker (optional)
- Kafka 3.x
- Consul (for service discovery)

### Running Locally

1. **Build the project:**
   ```bash
   mvn clean package
   ```

2. **Run the application:**
   ```bash
   java -jar target/fulfilment-service-1.0.0.jar
   ```

3. **Access the API documentation:**
   - Swagger UI: http://localhost:8087/api/v1/swagger-ui.html
   - OpenAPI Docs: http://localhost:8087/api/v1/api-docs

### Running with Docker

1. **Build the Docker image:**
   ```bash
   docker build -t fulfilment-service:latest .
   ```

2. **Run the container:**
   ```bash
   docker run -p 8087:8087 fulfilment-service:latest
   ```

## Configuration

The service can be configured through `application.yml`. Key configuration properties:

```yaml
server:
  port: 8087
  servlet:
    context-path: /api/v1

spring:
  application:
    name: dcs-fulfilment-service
  kafka:
    bootstrap-servers: localhost:29092

fulfilment-service:
  sse:
    timeout: 30000  # 30 seconds
    cleanup-delay: 300  # 5 minutes
  workflow:
    max-concurrent: 1000
    retention-hours: 24
```

## API Endpoints

### Workflow Tracking

- `GET /fulfilment/track/{correlationId}` - Subscribe to real-time workflow updates (SSE)
- `GET /fulfilment/status/{correlationId}` - Get current workflow status (REST)

### System Health

- `GET /fulfilment/health` - Get service health and statistics

## Server-Sent Events (SSE)

### Connecting to SSE

```javascript
const eventSource = new EventSource(
  'http://localhost:8087/api/v1/fulfilment/track/{correlationId}'
);

eventSource.onmessage = function(event) {
  const data = JSON.parse(event.data);
  console.log('Workflow status:', data.status);
  console.log('Progress:', data.progress + '%');
  console.log('Message:', data.message);
  
  if (data.status === 'COMPLETED') {
    console.log('Result:', data.result);
    eventSource.close();
  } else if (data.status === 'FAILED') {
    console.error('Error:', data.error);
    eventSource.close();
  }
};

eventSource.onerror = function(error) {
  console.error('SSE connection error:', error);
  eventSource.close();
};
```

### Event Format

Events are sent as JSON with the following structure:

```json
{
  "status": "PROCESSING",
  "progress": 60,
  "message": "Creating digital credential",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Workflow States

- `INITIATED`: Workflow has been initiated
- `PROCESSING`: Workflow is currently being processed
- `COMPLETED`: Workflow completed successfully
- `FAILED`: Workflow failed with errors
- `CANCELLED`: Workflow was cancelled

## Kafka Integration

The service consumes events from Kafka topics:

### Topics

- `flow-completion`: Workflow completion events
- `flow-errors`: Workflow error events
- `workflow-progress`: Workflow progress updates

### Event Handling

- Automatic retry with exponential backoff
- Dead letter queue for failed messages
- Consumer group: `fulfilment-service-group`

## Monitoring and Observability

### Health Checks

- Spring Boot Actuator endpoints
- Custom health indicators with connection counts
- Prometheus metrics

### Metrics

- Active SSE connections
- Tracked workflows
- Event processing rates
- Connection lifecycle metrics

### Logging

- Structured logging
- Configurable log levels
- Correlation ID tracking

## Development

### Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── pt/usis/dcs/fulfilment/
│   │       ├── controller/      # REST and SSE controllers
│   │       ├── service/         # Business services
│   │       ├── kafka/           # Kafka consumers
│   │       ├── domain/          # Domain models
│   │       └── FulfilmentServiceApplication.java
│   └── resources/
│       ├── application.yml      # Main configuration
│       ├── application-docker.yml # Docker configuration
│       ├── openapi/
│       │   └── api-spec.yaml    # OpenAPI specification
│       └── banner.txt           # Application banner
└── test/
    └── java/                    # Test classes
```

### Testing

Run tests with:
```bash
mvn test
```

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
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Environment Variables

Key environment variables:

- `SPRING_PROFILES_ACTIVE`: Active Spring profile (default, docker, prod)
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers
- `CONSUL_HOST`: Consul host for service discovery
- `SSE_TIMEOUT`: SSE connection timeout (milliseconds)
- `WORKFLOW_RETENTION_HOURS`: Workflow data retention period

## Performance

- Connection pooling for SSE
- Efficient event streaming
- Async processing with Kafka
- Automatic connection cleanup
- Memory-efficient workflow tracking

## Security

- CORS configuration for browser clients
- Connection timeout protection
- Input validation on all endpoints
- HTTPS support for production deployments

## Scalability

The service is designed to scale horizontally:

- Stateless architecture
- Kafka consumer groups for load distribution
- In-memory workflow tracking (can be externalized to Redis)
- Connection management per instance

## Troubleshooting

### Common Issues

1. **SSE connection timeout**
   - Check SSE timeout configuration
   - Verify network connectivity
   - Review firewall/proxy settings

2. **Kafka connection failed**
   - Verify Kafka is running
   - Check bootstrap servers configuration
   - Verify consumer group configuration

3. **Missing workflow updates**
   - Check Kafka topic configuration
   - Verify event producers are working
   - Review consumer lag metrics

## Browser Compatibility

SSE is supported in:
- Chrome 6+
- Firefox 6+
- Safari 5+
- Edge 79+
- Opera 11+

**Note**: Internet Explorer does not support SSE. Use polyfills for IE support.

## API Usage Examples

### Track Workflow (REST Polling)

```bash
curl http://localhost:8087/api/v1/fulfilment/status/{correlationId}
```

### Track Workflow (SSE)

```bash
curl -N http://localhost:8087/api/v1/fulfilment/track/{correlationId}
```

### Get Service Health

```bash
curl http://localhost:8087/api/v1/fulfilment/health
```

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
- Documentation: https://docs.usis.pt/fulfilment-service

## Related Services

- **Credential Service**: Digital credential issuance and management
- **Sis Service**: Student data and university integration
