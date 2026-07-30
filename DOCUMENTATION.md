# ULHT Digital Credential System - Complete Documentation

**Version:** 2.0  
**Last Updated:** October 9, 2025  
**Status:** Production Ready

---

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Architecture](#architecture)
4. [Services](#services)
5. [Credential Types](#credential-types)
6. [Error Handling](#error-handling)
7. [API Reference](#api-reference)
8. [Development Guide](#development-guide)
9. [Testing](#testing)
10. [Deployment](#deployment)
11. [Troubleshooting](#troubleshooting)

---

## Overview

Complete microservices architecture for issuing W3C Verifiable Credentials to students automatically using a modern, event-driven approach.

### Key Features

- ✅ **W3C Verifiable Credentials** - Fully compliant with W3C VC Data Model
- ✅ **OID4VCI Protocol** - OpenID for Verifiable Credential Issuance
- ✅ **Generic Credential System** - Add credentials via YAML configuration
- ✅ **Event-Driven Architecture** - Kafka-based asynchronous processing
- ✅ **Standards Compliance** - SCHAC 2.0, European Student Card Initiative
- ✅ **walt.id Integration** - Professional credential issuance and wallet management
- ✅ **Comprehensive Error Handling** - Themed error codes with fun names

### Technology Stack

- **Java 25** - Latest Java version
- **Spring Boot 3.5.6** - Microservices framework
- **Spring Cloud 2025.0.0** - Service discovery
- **Kafka 3.x** - Event-driven messaging
- **walt.id** - W3C credential issuance
- **Docker** - Containerization

---

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 25
- Maven 3.9+
- Kafka (included in docker-compose)

### Start All Services

```bash
# Clone repository
git clone <repository-url>
cd ulht-dcs

# Option 1: Start infrastructure and microservices separately
docker-compose -f docker-compose.infrastructure.yml up -d
docker-compose -f docker-compose.microservices.yml up -d

# Option 2: Start everything at once
docker-compose up -d

# Option 3: Start services locally (for development)
cd credential-service && mvn spring-boot:run
cd lusofona-service && mvn spring-boot:run
cd fulfilment-service && mvn spring-boot:run
cd student-service && mvn spring-boot:run
```

### Verify Services

```bash
# Check health
curl http://localhost:8084/actuator/health  # Student Service
curl http://localhost:8085/api/v1/actuator/health  # Lusofona
curl http://localhost:8086/api/v1/actuator/health  # Credential
curl http://localhost:8087/api/v1/actuator/health  # Fulfilment
```

### Issue Your First Credential

```bash
# Via Kong API Gateway (recommended)
curl -X POST http://localhost:8000/api/v1/students/issue \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "a12345678",
    "installKey": "00000_0000000000000",
    "platform": "ios",
    "language": "PT"
  }'

# Or directly (bypassing Kong)
curl -X POST http://localhost:8084/student/issue \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "a12345678",
    "installKey": "00000_0000000000000",
    "platform": "ios",
    "language": "PT"
  }'
```

---

## Architecture

### System Overview

```
┌─────────────────┐
│  Mobile App     │
└────────┬────────┘
         │ POST /student/issue
         ↓
┌─────────────────────────────┐
│  Student Service (8084)     │  Entry point
│  - Validate request         │
│  - Generate correlation ID  │
│  - Publish to Kafka         │
└────────┬───────────────────┘
         │ Kafka: student.login.requested
         ↓
┌─────────────────────────────┐
│  Lusofona Service (8085)    │  ULHT Integration
│  - Authenticate student     │
│  - Fetch student data       │
│  - Publish to Kafka         │
└────────┬───────────────────┘
         │ Kafka: credential.requests
         ↓
┌─────────────────────────────┐
│  Credential Service (8086)  │  W3C Credential Issuance
│  - Load templates           │
│  - Build credentials        │
│  - Issue via walt.id        │
│  - Publish to Kafka         │
└────────┬───────────────────┘
         │ Kafka: credential.completed
         ↓
┌─────────────────────────────┐
│  Fulfilment Service (8087)  │  Progress Tracking
│  - Track workflow status    │
│  - Store results            │
│  - Notify client            │
└─────────────────────────────┘
         │
         ↓
    Credentials URLs
```

### Service Responsibilities

| Service | Port | Purpose | Technology |
|---------|------|---------|------------|
| **Student Service** | 8084 | Entry point, request validation | Spring Boot, Kafka Producer, Consul |
| **Lusofona Service** | 8085 | ULHT API integration, auth | Spring Boot, Feign, Kafka, Consul |
| **Credential Service** | 8086 | W3C credential issuance | Spring Boot, walt.id, Kafka, Consul |
| **Fulfilment Service** | 8087 | Workflow tracking, SSE | Spring Boot, Kafka Consumer, Consul |

### Infrastructure Services

| Service | Port | Purpose |
|---------|------|---------|
| **Kong API Gateway** | 8000, 8001 | API gateway, routing, rate limiting |
| **Kong UI** | 8082 | Kong management interface |
| **Kafka** | 9092, 29092 | Message broker |
| **Zookeeper** | 2181 | Kafka coordination |
| **Consul** | 8500, 8600 | Service discovery & configuration |
| **Kafka UI** | 8081 | Kafka management UI |

### Monitoring & Observability

| Service | Port | Purpose |
|---------|------|---------|
| **Prometheus** | 9090 | Metrics collection & storage |
| **Grafana** | 3000 | Visualization & dashboards |
| **Loki** | 3100 | Log aggregation |
| **Promtail** | 9080 | Log shipper |
| **Kafka Exporter** | 9308 | Kafka metrics |

---

## Services

### Student Service

**Purpose:** Entry point for credential issuance requests

**Key Features:**
- Request validation
- Correlation ID generation
- Kafka event publishing
- Progress monitoring

**Endpoints:**
- `POST /student/issue` - Issue credentials
- `GET /student/status/{correlationId}` - Check status
- `GET /student/credentials/{correlationId}` - Get credentials

**Error Codes:** `STUD-001` to `STUD-999`

---

### Lusofona Service

**Purpose:** ULHT API integration and student data retrieval

**Key Features:**
- Student authentication
- ULHT API integration
- Student data transformation
- Comprehensive error handling

**Error Codes:**
- `TE-001` to `TE-020` - Service errors
- `LUSOFONA-000` to `LUSOFONA-010` - ULHT API errors

**ULHT API Error Mapping:**

| Code | Name | HTTP Status | Description |
|:----:|------|:-----------:|-------------|
| 0 | All Good | 200 | Success |
| 1 | Mystery Box | 500 | Generic error |
| 2 | Wrong Password | 401 | Authentication failure |
| 3 | Déjà Vu Device | 500 | IMEI registered |
| 4 | Missing Membership | 403 | App not registered |
| 5 | Access Denied | 403 | Operation unavailable |
| 6 | Garbled Message | 400 | Incorrect format |
| 7 | Empty Locker | 404 | Data not found |
| 8 | Expired Ticket | 401 | Invalid key |
| 9 | Broken Package | 400 | Invalid request |
| 10 | Rule Breaker | 422 | Business rule validation |

---

### Credential Service

**Purpose:** W3C Verifiable Credential issuance using walt.id

**Key Features:**
- **Generic credential system** - Add credentials via YAML
- Dynamic field mapping
- Conditional issuance
- walt.id integration
- Multiple credential formats (JWT, SD-JWT)

**Error Codes:** `CRED-001` to `CRED-999`, `CRED-WALTID-XXX`

#### Generic Credential System

**No code changes needed!** Add new credential types via configuration:

```yaml
# credential-service/src/main/resources/application.yml
credentials:
  templates:
    - id: my-new-credential
      type: MyCredentialType
      displayName: "My New Credential"
      enabled: true
      priority: 50
      waltidConfigId: UniversityDegree_jwt_vc_json
      fieldMappings:
        field1: [sourceField1, alternativeField1]
        field2: [sourceField2]
      staticFields:
        constantField: "constant value"
```

**Restart service and done!** 🎉

##### Configuration Reference

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `id` | ✅ Yes | - | Unique identifier |
| `type` | ✅ Yes | - | W3C credential type |
| `displayName` | ❌ No | - | Human-readable name |
| `enabled` | ❌ No | `true` | Enable/disable |
| `priority` | ❌ No | `100` | Issuance order (lower first) |
| `required` | ❌ No | `false` | Fail workflow if fails |
| `waltidConfigId` | ✅ Yes | - | walt.id template ID |
| `fieldMappings` | ✅ Yes | - | Field to data mapping |
| `staticFields` | ❌ No | - | Constant values |
| `condition` | ❌ No | - | SpEL conditional expression |

##### Field Mapping

```yaml
fieldMappings:
  # Try multiple field names (in order)
  studentId: [studentId, studentCode, id, userCode]
  
  # Nested field access
  street: [address.street, location.street]
  
  # Common fields
  email: [email, studentEmail, emailAddress]
  firstName: [firstName, givenName, name]
```

##### Conditional Issuance

```yaml
# Only issue if student has graduated
- id: university-degree
  condition: "#studentData['graduationDate'] != null"
  
# Only issue for international students
- id: visa-support
  condition: "#studentData['nationality'] != 'PT'"
  
# Only issue for PhD students
- id: research-credential
  condition: "#studentData['academicLevel'] == 'phd'"
```

---

### Fulfilment Service

**Purpose:** Workflow progress tracking and result storage

**Key Features:**
- Real-time progress tracking
- Credential storage
- SSE notifications
- Workflow history

**Error Codes:** `FULF-001` to `FULF-999`

---

## Credential Types

### Currently Enabled

| Credential | Priority | Description | Condition |
|------------|----------|-------------|-----------|
| **Educational ID** | 10 | SCHAC-compliant student ID | Always |
| **Identity Credential** | 15 | Digital identity | Always |
| **European Student Card** | 20 | ESC Initiative | Always |
| **University Degree** | 30 | Degree certificate | Graduates only |

### Available (Disabled by Default)

Set `enabled: true` in configuration to activate:

- KYC Credential
- Boarding Pass
- Hotel Reservation
- ePassport
- Email Verification
- Enrollment Credential
- Legal Person
- And more...

---

## Error Handling

### Error Code Convention

**Format:** `SERVICE-XXX` where:
- `SERVICE` = Service identifier
- `XXX` = Error number

### Error Code Ranges

| Range | Purpose | Example |
|-------|---------|---------|
| XX-001 to XX-009 | Core/Request errors | STUD-001 |
| XX-010 series | Validation errors | STUD-010 |
| XX-020 series | Integration (Kafka, APIs) | STUD-020 |
| XX-030 series | Data/ID errors | STUD-030 |
| XX-040+ series | Service-specific | STUD-040 |
| XX-999 | Internal server error | STUD-999 |
| XX-000 | Unknown error | STUD-000 |

### Fun Error Names

All error codes have memorable "fun names" themed by service:

- **Student Service** (🎓 School): "Confused Student", "Incomplete Homework"
- **Lusofona Service** (🍪 Kitchen): "Wired Nutella", "Broken Biscuit"
- **Credential Service** (🎁 Factory): "Credential Factory Jam", "Grumpy External API"
- **Fulfilment Service** (📦 Delivery): "Lost Package", "Delivery Delayed"

**Example:**
```java
log.error("{}: {}", 
    ErrorCodes.INCOMPLETE_HOMEWORK.getFunName(),
    ErrorCodes.INCOMPLETE_HOMEWORK.getDescription());
// Logs: "Incomplete Homework: A required field is missing from the request"
```

---

## API Reference

### Student Service API

#### Issue Credentials
```http
POST /student/issue
Content-Type: application/json

{
  "userName": "a12345678",
  "installKey": "00000_0000000000000",
  "platform": "ios",
  "language": "PT",
  "application": "org.cofac.mobile.ulht",
  "versionCode": "1601206"
}

Response: 202 Accepted
{
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROCESSING",
  "message": "Credential issuance initiated, processing...",
  "monitorAt": "/student/status/550e8400-e29b-41d4-a716-446655440000",
  "credentialsAt": "/student/credentials/550e8400-e29b-41d4-a716-446655440000"
}
```

#### Check Status
```http
GET /student/status/{correlationId}

Response: 200 OK
{
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "progress": 100,
  "message": "Credentials issued successfully"
}
```

#### Get Credentials
```http
GET /student/credentials/{correlationId}

Response: 200 OK
{
  "result": {
    "studentId": "22212196",
    "credentialsIssued": 3,
    "issuedCredentialTypes": [
      "EducationalID",
      "IdentityCredential",
      "EuropeanStudentCard"
    ],
    "credentialOfferUrls": [
      "openid-credential-offer://..."
    ],
    "summary": {
      "total": 4,
      "issued": 3,
      "skipped": 1,
      "failed": 0
    }
  }
}
```

### API Documentation

**Direct Service Access:**
- Student: http://localhost:8084/swagger-ui.html
- Lusofona: http://localhost:8085/api/v1/swagger-ui.html
- Credential: http://localhost:8086/api/v1/swagger-ui.html
- Fulfilment: http://localhost:8087/api/v1/swagger-ui.html

**Via Kong API Gateway:**
- All services: http://localhost:8000/api/v1/*

**Management Interfaces:**
- Kong UI: http://localhost:8082
- Kafka UI: http://localhost:8081
- Consul UI: http://localhost:8500
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090

---

## Development Guide

### Adding a New Credential Type

**Time Required:** 5 minutes ⚡

1. Open `credential-service/src/main/resources/application.yml`
2. Add credential template:

```yaml
credentials:
  templates:
    - id: parking-permit
      type: ParkingPermit
      displayName: "Student Parking Permit"
      enabled: true
      priority: 60
      waltidConfigId: UniversityDegree_jwt_vc_json
      fieldMappings:
        studentName: [fullName, name]
        studentId: [studentId]
        vehiclePlate: [vehiclePlate]
      staticFields:
        institutionName: "ULHT"
        permitType: "Student Parking"
```

3. Restart credential service
4. Done! 🎉

### walt.id Compliance

The system is **fully compliant** with walt.id v0.15.0:

- ✅ OID4VCI Protocol Implementation
- ✅ Correct Endpoints (`/onboard/issuer`, `/openid4vc/jwt/issue`)
- ✅ W3C Credential Data Structure
- ✅ Issuer Key & DID Generation
- ✅ JWT & SD-JWT Support
- ✅ Dynamic Field Mapping

**Production Requirements:**
- ⚠️ **Required:** Integrate external KMS (Hashicorp Vault or Oracle KMS)
- 💡 Recommended: Accept student DIDs from wallets
- 💡 Optional: Add status callback URI

---

## Testing

### Postman Collections

Import collections from `postman/` directory:
- `ULHT-Digital-Credentials-Complete.postman_collection.json`
- `ULHT-Digital-Credential-Flow.postman_collection.json`

### Run Tests

```bash
# Credential Service
cd credential-service
mvn test

# Lusofona Service
cd lusofona-service
mvn test

# Student Service
cd student-service
mvn test

# Fulfilment Service
cd fulfilment-service
mvn test
```

### Integration Testing

```bash
# Start all services
docker-compose up -d

# Run complete workflow test
curl -X POST http://localhost:8084/student/issue \
  -H "Content-Type: application/json" \
  -d @postman/test-request.json

# Check logs
docker-compose logs -f credential-service
```

---

## Deployment

### Docker Deployment

```bash
# Build all services
mvn clean package -DskipTests

# Build Docker images
docker-compose build

# Start infrastructure first
docker-compose -f docker-compose.infrastructure.yml up -d

# Start microservices
docker-compose -f docker-compose.microservices.yml up -d

# Or start everything at once
docker-compose up -d

# Check logs
docker-compose logs -f

# Check specific service logs
docker-compose logs -f ulht-student-service

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Environment Variables

```bash
# Credential Service
WALTID_ISSUER_URL=http://walt-id:7002
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Lusofona Service
ULHT_API_URL=https://lusofona-api.example.com
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Fulfilment Service
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Student Service
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
FULFILMENT_SERVICE_URL=http://fulfilment-service:8087
```

### Production Checklist

- [ ] Configure external KMS for credential-service
- [ ] Set up proper SSL/TLS certificates
- [ ] Configure production Kafka cluster
- [ ] Enable authentication & authorization
- [ ] Set up monitoring & alerting
- [ ] Configure log aggregation
- [ ] Set up database backups
- [ ] Configure rate limiting
- [ ] Enable CORS properly
- [ ] Review and set resource limits

---

## Troubleshooting

### Common Issues

#### Issue: Credential Not Being Issued

**Symptoms:** Credential doesn't appear in issued list

**Solutions:**
1. Check template is enabled: `enabled: true`
2. Check condition in logs: `⏭️ Skipping CredentialType - condition not met`
3. Verify field mappings: `No value found for field 'X'`
4. Check walt.id service is running: `http://localhost:7002`

#### Issue: Kafka Connection Errors

**Symptoms:** `STUD-020: Message Not Sent`

**Solutions:**
1. Verify Kafka is running: `docker-compose ps kafka`
2. Check Kafka logs: `docker-compose logs kafka`
3. Verify bootstrap servers configuration
4. Check network connectivity

#### Issue: ULHT API Errors

**Symptoms:** `LUSOFONA-XXX` errors

**Solutions:**
1. Check credentials are valid
2. Verify ULHT API is accessible
3. Review error code meaning (see Error Handling section)
4. Check user permissions

#### Issue: walt.id Integration Errors

**Symptoms:** `CRED-WALTID-XXX` errors

**Solutions:**
1. Verify walt.id is running: `curl http://localhost:7002/health`
2. Check issuer onboarding: `GET /api/v1/waltid/issuer/status`
3. Review walt.id logs
4. Verify credential configuration ID exists

### Debug Logging

Enable debug logging in `application.yml`:

```yaml
logging:
  level:
    pt.ulusofona: DEBUG
    org.apache.kafka: DEBUG
```

### Monitoring Queries

```bash
# Count errors by service
errorCode:CRED-* | stats count by errorCode

# Track WaltID errors
errorCode:CRED-WALTID-* | timechart count by errorCode

# Monitor authentication failures
errorCode:LUSOFONA-002 OR errorCode:LUSOFONA-008

# Track workflow failures
errorCode:FULF-005 | timechart count
```

---

## Standards Compliance

- ✅ **W3C Verifiable Credentials Data Model** - Full compliance
- ✅ **OpenID for Verifiable Credential Issuance (OID4VCI)** - Implemented
- ✅ **SCHAC 2.0** (Schema for Academia) - Educational ID support
- ✅ **European Student Card Initiative** - ESC credential
- ✅ **Decentralized Identifiers (DIDs)** - DID-based issuance
- ✅ **walt.id v0.15.0** - Full compliance

---

## Project Structure

```
ulht-dcs/
├── credential-service/         # W3C credential issuance
│   ├── src/main/java/
│   │   └── pt/ulusofona/ulht/credential/
│   │       ├── builder/        # Generic credential builder
│   │       ├── config/         # Templates configuration
│   │       ├── kafka/          # Kafka consumers
│   │       └── service/        # walt.id integration
│   └── src/main/resources/
│       └── application.yml     # Credential templates
│
├── lusofona-service/          # ULHT integration
│   ├── src/main/java/
│   │   └── pt/ulusofona/digital/wallet/
│   │       ├── exception/      # Error handling
│   │       ├── kafka/          # Kafka producers/consumers
│   │       └── service/        # ULHT API client
│   └── src/main/resources/
│
├── fulfilment-service/        # Progress tracking
│   ├── src/main/java/
│   │   └── pt/ulusofona/ulht/fulfilment/
│   │       ├── domain/         # Event models
│   │       ├── kafka/          # Event consumers
│   │       └── service/        # Progress service
│   └── src/main/resources/
│
├── student-service/           # Entry point
│   ├── src/main/java/
│   │   └── pt/ulusofona/student/
│   │       ├── controller/     # REST endpoints
│   │       ├── kafka/          # Event producers
│   │       └── exception/      # Error codes
│   └── src/main/resources/
│       └── openapi/
│           └── api-spec.yaml   # OpenAPI specification
│
├── postman/                   # API testing
│   ├── ULHT-Digital-Credentials-Complete.postman_collection.json
│   └── ULHT-Digital-Credential-Environment.postman_environment.json
│
├── docker-compose.yml         # Service orchestration
└── DOCUMENTATION.md          # This file
```

---

## License

MIT License

---

## Contributors

Development Team - ULHT Digital Credential System

---

## Version History

- **2.0** (2025-10-09)
  - Generic credential system implementation
  - Unified documentation
  - Comprehensive error handling with fun names
  - walt.id compliance
  - Identity credential support
  
- **1.0** (2025-10-08)
  - Initial release
  - Basic credential issuance
  - Kafka integration

---

**For questions or support, refer to the specific sections above or check the service-specific documentation in each microservice directory.**

