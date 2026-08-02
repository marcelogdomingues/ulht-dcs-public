# DCS Credential Service

## 🎯 Overview

The DCS Credential Service is a microservice that issues W3C Verifiable Credentials for students. It integrates with:
- **WaltID Wallet** (external service) - Wallet API for user authentication and credential storage
- **WaltID Issuer** (external service) - Credential issuance via OID4VCI
- **WaltID Verifier** (external service) - Credential verification
- **SIS Service** - Receives real student data from DCS systems
- **Fulfilment Service** - Tracks progress and notifies students

**Note:** This service uses **WaltID Wallet as an external service** (similar to Issuer and Verifier). We do NOT build a custom wallet application. The `/wallet` endpoints are a wrapper/proxy around the WaltID Wallet API.

### Key Features

- ✅ **W3C Verifiable Credentials** - JWT & SD-JWT formats
- ✅ **European Educational Credentials** - SCHAC Educational ID & European Student Card
- ✅ **Automatic Issuance** - Triggered on student login with real DCS data
- ✅ **Kafka Integration** - Event-driven processing
- ✅ **Progress Tracking** - Real-time updates via Fulfilment Service
- ✅ **OpenAPI First** - API spec drives code generation
- ✅ **WaltID Wallet Integration** - Cookie-based authentication with external WaltID Wallet service

## 🚀 Quick Start

### Build & Run

```bash
cd credential-service
mvn clean generate-sources  # Generates API classes from openapi/api-spec.yaml
mvn clean package
mvn spring-boot:run
```

Service runs on: **http://localhost:8086**

## 🧪 Testing

### Real Integration Test (Recommended)

**Triggers complete flow with REAL student data:**

```bash
# 1. Start all services
docker-compose up -d

# 2. Trigger student login via SIS Service
curl -X POST http://localhost:8085/api/v1/studentLogin \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "a12345678",
    "installKey": "00000_0000000000000",
    "language": "PT",
    "platform": "ios"
  }'

# 3. Get correlationId from response, then check result
curl http://localhost:8087/api/v1/fulfilment/result/{correlationId}
```

**See `COMPLETE_INTEGRATION.md` for detailed end-to-end testing.**

### Postman Testing (Isolation)

Import collection: `Credential-Service-Postman-Collection.json`

**See `POSTMAN_GUIDE.md` for testing guide.**

## 📚 Supported Credentials

| Credential Type | Standard | Endpoint |
|----------------|----------|----------|
| Educational ID | SCHAC/eduPerson | `/wallet/issue-educational-id` |
| European Student Card | ESC Initiative | `/wallet/issue-european-student-card` |
| University Degree | W3C VC | `/wallet/issue-university-degree` |

## 🔗 Key Endpoints

### Manual Credential Issuance
- `POST /api/v1/wallet/onboard-issuer` - Create signing key
- `POST /api/v1/wallet/issue-university-degree` - Issue degree
- `POST /api/v1/wallet/issue-educational-id` - Issue Educational ID
- `POST /api/v1/wallet/issue-european-student-card` - Issue ESC

### Workflow (Automatic via Kafka)
- `POST /api/v1/api/flows/credential-issuance` - Trigger workflow (testing)
- `GET /api/v1/api/flows/result/{correlationId}` - Get results

### Health
- `GET /api/v1/api/health/ping` - Health check
- `GET /api/v1/api/health/status` - System status

## 🌊 Integration Flow

```
Student Login (Mobile App)
    ↓
SIS Service (8085)
    ↓ (calls real university SIS API)
Real Student Data
    ↓ (Kafka: credential.requests)
Credential Service (8086)
    ↓ (issues credentials)
    ├─> credential.progress (20%, 40%, 60%, 70%, 80%, 100%)
    └─> credential.completed (URLs)
    ↓ (Kafka)
Fulfilment Service (8087)
    ↓ (tracks & stores)
Student Gets Credentials
```

## 📡 Kafka Topics

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `credential.requests` | Sis | Credential | Student data input |
| `credential.progress` | Credential | Fulfilment | Progress updates |
| `credential.completed` | Credential | Fulfilment | Final results |
| `credential.error` | Credential | Fulfilment | Errors |

**All use `domain.action` format!**

## ⚙️ Configuration

### Prerequisites
- Java 25
- Maven 3.9+
- Docker & Docker Compose
- Kafka 3.x

### Environment Variables

```yaml
SPRING_KAFKA_BOOTSTRAP_SERVERS: localhost:29092  # or kafka:9092 in Docker
WALTID_ISSUER_URL: http://localhost:7002
WALTID_WALLET_URL: http://localhost:7001
```

## 📖 Documentation

- **README.md** (this file) - Quick start & overview
- **DEVELOPER.md** - Technical implementation details
- **COMPLETE_INTEGRATION.md** - End-to-end testing & integration
- **POSTMAN_GUIDE.md** - Postman collection usage
- **OpenAPI Spec:** `src/main/resources/openapi/api-spec.yaml`

### API Documentation
- Swagger UI: http://localhost:8086/api/v1/swagger-ui.html
- OpenAPI JSON: http://localhost:8086/api/v1/api-docs

## 📊 Standards Compliance

✅ W3C Verifiable Credentials Data Model  
✅ OpenID for Verifiable Credential Issuance (OID4VCI)  
✅ SCHAC 2.0 (Schema for Academia)  
✅ European Student Card Initiative  
✅ OpenAPI 3.0  

## 🐛 Troubleshooting

### No Kafka Messages?

```bash
# Check if Kafka is running
docker ps | grep kafka

# Check topics exist
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092 | grep credential

# Watch the topic
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic credential.requests \
  --from-beginning
```

### SIS Service Not Triggering?

```bash
# Check if CredentialWorkflowProducer is created
curl http://localhost:8085/api/v1/actuator/beans | jq '.contexts[].beans | keys[]' | grep Credential

# Check Kafka connection
# In sis-service logs, should see:
# "Sent credential workflow request for user: ..."
```

### Fulfilment Service Not Receiving?

```bash
# Verify topic names are updated
grep "credential.progress" fulfilment-service/src/main/java/pt/usis/dcs/fulfilment/kafka/WorkflowEventConsumer.java

# Should be "credential.progress" NOT "workflow.progress"
```

## 🎉 Summary

**Clean & Simple:**
- 4 core documentation files (was 24!)
- 1 flow type (was 6!)
- 6 Kafka topics, all used, all standardized
- Complete integration: Sis → Credential → Fulfilment
- OpenAPI Generator configured
- Real data from DCS systems

**Test the complete flow using COMPLETE_INTEGRATION.md!**

---

**Version:** 1.0.0  
**Status:** ✅ PRODUCTION READY  
**Integration:** ✅ COMPLETE
