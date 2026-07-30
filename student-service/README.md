# Student Service

Entry point service for student requests - minimal and simple

## Overview

The Student Service is the **primary gateway** for student interactions in the ULHT Digital Credential System. It provides a clean, minimal interface for triggering credential issuance workflows.

## Features

- 🚀 **Simple Entry Point**: Accepts minimal student data (username + installKey)
- 📨 **Event-Driven**: Publishes events to Kafka for asynchronous processing
- 📊 **Real-time Tracking**: Provides correlation IDs for workflow monitoring
- 🔄 **Smart Proxy**: Delegates status and result queries to Fulfilment Service

## Architecture

```
┌──────────────┐
│   Student    │
│  (Mobile App)│
└──────┬───────┘
       │ POST /student/login
       ▼
┌──────────────────┐
│ Student Service  │
│  (Port: 8085)    │
└──────┬───────────┘
       │ Publishes: student.login.requested
       ▼
   [Kafka Topics]
       │
       ├─► Lusofona Service (fetches real student data)
       │
       └─► Credential Service (issues credentials)
              │
              └─► Fulfilment Service (tracks & stores results)
```

## API Endpoints

### Student Operations

#### 1. Student Login
```http
POST /student/login
Content-Type: application/json

{
  "userName": "a12345678",
  "installKey": "00000_0000000000000",
  "platform": "ios",
  "language": "PT"
}
```

**Response:**
```json
{
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROCESSING",
  "message": "Student login received, processing...",
  "monitorAt": "/student/status/550e8400-e29b-41d4-a716-446655440000",
  "credentialsAt": "/student/credentials/550e8400-e29b-41d4-a716-446655440000"
}
```

#### 2. Check Workflow Status
```http
GET /student/status/{correlationId}
```

**Response:**
```json
{
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROCESSING",
  "progress": 60,
  "message": "Issuing credentials..."
}
```

#### 3. Get Issued Credentials
```http
GET /student/credentials/{correlationId}
```

**Response:**
```json
{
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "a12345678",
  "status": "COMPLETED",
  "credentialOfferUrls": [
    "openid-credential-offer://?credential_offer_uri=...",
    "openid-credential-offer://?credential_offer_uri=...",
    "openid-credential-offer://?credential_offer_uri=..."
  ],
  "credentialTypes": [
    "EducationalID",
    "EuropeanStudentCard",
    "UniversityDegree"
  ],
  "credentialsIssued": 3
}
```

### Health Check

```http
GET /actuator/health
```

## Technology Stack

- **Java:** 25
- **Spring Boot:** 3.5.6
- **Spring Cloud:** 2025.0.0
- **Spring Kafka:** For event publishing
- **Spring Cloud OpenFeign:** For service-to-service calls
- **Spring Cloud Consul:** For service discovery
- **SpringDoc OpenAPI:** API documentation (v2.8.13)
- **OpenAPI Generator:** Code generation (v7.10.0)
- **Lombok:** Boilerplate reduction

## Build & Run

### Prerequisites
- Java 25
- Maven 3.9+
- Kafka (running)
- Fulfilment Service (running for proxy endpoints)

### Build
```bash
mvn clean package
```

This will:
1. Generate OpenAPI code from spec
2. Compile all sources
3. Run tests
4. Package JAR: `target/student-service-1.0.0.jar`

### Run Locally
```bash
mvn spring-boot:run
```

Service will start on: `http://localhost:8085`

### Run with Docker
```bash
docker-compose up student-service
```

## Configuration

### Application Properties
Located in: `src/main/resources/application.yml`

Key configurations:
- Server port: `8085`
- Kafka brokers
- Feign client settings
- Actuator endpoints

### Environment Variables
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka broker addresses
- `FULFILMENT_SERVICE_URL`: Fulfilment service endpoint
- `SPRING_PROFILES_ACTIVE`: Active profile (dev, docker, prod)

## OpenAPI Documentation

### API Specification
Full OpenAPI 3.0.3 spec: `src/main/resources/openapi/api-spec.yaml`

### Swagger UI
Access interactive API documentation:
```
http://localhost:8085/swagger-ui.html
```

### OpenAPI JSON
Raw OpenAPI specification:
```
http://localhost:8085/v3/api-docs
```

## Code Generation

This service uses **OpenAPI Generator** to automatically generate:
- API interfaces (`StudentApi`, `HealthApi`)
- Model classes (DTOs with validation)
- Supporting utilities

Generated code location: `target/generated-sources/openapi/`

### Regenerate Code
Code is automatically generated during Maven `compile` phase. To manually trigger:
```bash
mvn clean compile
```

## Development

### Project Structure
```
student-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── pt/ulusofona/student/
│   │   │       ├── client/          # Feign clients
│   │   │       ├── config/          # Configuration
│   │   │       ├── controller/      # REST controllers
│   │   │       ├── dto/             # Data transfer objects
│   │   │       ├── kafka/           # Kafka producers
│   │   │       └── StudentServiceApplication.java
│   │   └── resources/
│   │       ├── openapi/
│   │       │   └── api-spec.yaml    # OpenAPI specification
│   │       ├── application.yml
│   │       └── banner.txt
│   └── test/
│       └── java/                    # Test classes
├── target/
│   └── generated-sources/
│       └── openapi/                 # Auto-generated code
├── pom.xml
├── Dockerfile
└── README.md
```

### Adding New Endpoints
1. Update `src/main/resources/openapi/api-spec.yaml`
2. Run `mvn compile` to regenerate code
3. Implement the generated interface in your controller
4. Test the endpoint

### Testing
```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=StudentControllerTest

# Skip tests during build
mvn package -DskipTests
```

## Integration

### Kafka Topics

**Publishes to:**
- `student.login.requested` - Student login events

### Service Dependencies

**Proxies to:**
- **Fulfilment Service** - For status and credentials retrieval

**Consumed by:**
- **Lusofona Service** - Processes login requests

## Workflow Example

1. **Student submits login:**
   ```bash
   curl -X POST http://localhost:8085/student/login \
     -H "Content-Type: application/json" \
     -d '{"userName":"a12345678","installKey":"00000_0000000000000"}'
   ```

2. **Service publishes to Kafka:**
   - Topic: `student.login.requested`
   - Includes: correlationId, userName, installKey

3. **Lusofona Service:**
   - Consumes event
   - Fetches real student data from ULHT API
   - Publishes enriched data to `credential.requests`

4. **Credential Service:**
   - Issues W3C Verifiable Credentials
   - Publishes progress updates

5. **Student checks status:**
   ```bash
   curl http://localhost:8085/student/status/{correlationId}
   ```

6. **Student retrieves credentials:**
   ```bash
   curl http://localhost:8085/student/credentials/{correlationId}
   ```

## Monitoring

### Health Check
```bash
curl http://localhost:8085/actuator/health
```

### Metrics
```bash
curl http://localhost:8085/actuator/metrics
```

### Logs
Logs include correlation IDs for tracing requests across services.

## Standardization

This service follows the **ULHT Digital Credential System** standardization:
- ✅ OpenAPI 3.0.3 specification
- ✅ Automated code generation
- ✅ Consistent dependency management
- ✅ Uniform build configuration
- ✅ Swagger UI integration

See `STANDARDIZATION.md` for details.

## Troubleshooting

### Common Issues

**Issue:** Service won't start
- Check if port 8085 is available
- Verify Kafka is running
- Check application.yml configuration

**Issue:** OpenAPI generation fails
- Verify `src/main/resources/openapi/api-spec.yaml` exists
- Check YAML syntax
- Run `mvn clean compile` to regenerate

**Issue:** Kafka connection errors
- Verify Kafka broker addresses in config
- Check Kafka is accessible
- Review network/firewall settings

## Contributing

1. Update OpenAPI spec for API changes
2. Regenerate code with `mvn compile`
3. Write/update tests
4. Follow existing code style
5. Update documentation

## License

MIT License - See LICENSE file for details

## Support

For issues and questions:
- Email: dev@ulusofona.pt
- Documentation: `/docs`
- API Reference: http://localhost:8085/swagger-ui.html

---

**Part of the ULHT Digital Credential System**
