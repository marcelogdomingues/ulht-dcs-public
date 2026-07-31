# API Reference

Complete REST reference for the ULHT Digital Credential System (DCS) services.

See also: [Security](SECURITY.md) · [Configuration](CONFIGURATION.md) · [Architecture](ARCHITECTURE.md) · [Getting Started](GETTING_STARTED.md) · [Deployment](DEPLOYMENT.md) · [Troubleshooting](TROUBLESHOOTING.md) · [Project README](index.md)

---

## Base URLs

Clients may call each service directly on its own port. The Kong API gateway (`http://localhost:8000`) is **partial / aspirational** — use direct ports for reliable access.

| Service | Base URL | Responsibility |
| --- | --- | --- |
| student-service | `http://localhost:8084/api/v1` | Orchestrates credential issuance & verification |
| lusofona-service | `http://localhost:8085/api/v1` | ULHT/SIGES academic data (proxies external ULHT API) |
| credential-service | `http://localhost:8086/api/v1` | W3C issuance, wallet & verifier via walt.id |
| fulfilment-service | `http://localhost:8087/api/v1` | Workflow / fulfilment tracking |

All service paths use the `/api/v1` context path.

---

## Authentication

All endpoints require the `apikey` header **except** the public endpoints listed below.

```
apikey: $APP_API_KEY
```

Missing or invalid keys are rejected with `401 Unauthorized`. See [Security](SECURITY.md) for the full authentication model.

Set the key in your shell for the examples below:

```bash
export APP_API_KEY="<your-api-key>"   # dev default is ulht-dev-local-CHANGE-ME
```

---

## Public endpoints (no `apikey`)

Available on **every** service under its base URL:

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/v1/actuator/health` | Liveness/readiness health check |
| GET | `/api/v1/actuator/info` | Build & service info |
| GET | `/api/v1/actuator/prometheus` | Prometheus metrics scrape endpoint |
| GET | `/api/v1/swagger-ui/index.html` | Swagger UI |
| GET | `/api/v1/v3/api-docs` | OpenAPI 3 document |

### Swagger UI per service

| Service | Swagger UI URL |
| --- | --- |
| student-service | `http://localhost:8084/api/v1/swagger-ui/index.html` |
| lusofona-service | `http://localhost:8085/api/v1/swagger-ui/index.html` |
| credential-service | `http://localhost:8086/api/v1/swagger-ui/index.html` |
| fulfilment-service | `http://localhost:8087/api/v1/swagger-ui/index.html` |

---

## Standard responses

| Status | Meaning |
| --- | --- |
| `202 Accepted` | Async issuance accepted; body includes a `correlationId` and `status: PROCESSING`. Poll for progress. |
| `401 Unauthorized` | Missing or invalid `apikey` header on a protected endpoint. |
| `503 Service Unavailable` | The external walt.id backend is unavailable (issuer `:7002`, verifier `:7003`, or wallet `:7001`). Returned by credential-service. |

---

## student-service

Base URL: `http://localhost:8084/api/v1` · All endpoints require the `apikey` header.

| Method | Path | Description |
| --- | --- | --- |
| POST | `/student/issue` | Start async credential issuance → `202` with `{correlationId, status: PROCESSING}` |
| GET | `/student/status/{correlationId}` | Get processing status for an issuance |
| GET | `/student/credentials/{correlationId}` | Fetch issued credential(s) for a correlation id |
| POST | `/student/verify` | Verify a submitted credential |

---

## lusofona-service

Base URL: `http://localhost:8085/api/v1` · All endpoints require the `apikey` header. These endpoints proxy the external ULHT/SIGES API.

| Method | Path | Description |
| --- | --- | --- |
| POST | `/student/login` | Authenticate against the ULHT academic backend |
| GET | `/student/grades` | Retrieve the student's grades |
| GET | `/student/enrolment` | Retrieve enrolment information |
| GET | `/student/schedule` | Retrieve the student's schedule |
| GET | `/student/course-credits` | Retrieve course credit information |

---

## credential-service

Base URL: `http://localhost:8086/api/v1` · All endpoints require the `apikey` header. W3C issuance, wallet, and verifier operations are backed by walt.id. Returns `503` when the walt.id backend is down.

### Wallet

| Method | Path | Description |
| --- | --- | --- |
| GET | `/wallet/credentials` | List credentials held in the wallet |
| POST | `/wallet/present` | Create/submit a verifiable presentation |
| GET | `/wallet/presentation-definition` | Retrieve the presentation definition |
| GET | `/wallet/match-presentation` | Match wallet credentials against a presentation definition |
| POST | `/wallet/register` | Register a wallet |
| POST | `/wallet/login` | Log in to a wallet |

### Verifier

| Method | Path | Description |
| --- | --- | --- |
| POST | `/verifier/verify` | Verify a presentation/credential |
| POST | `/verifier/verify/basic` | Basic verification with explicit policies (see note) |
| GET | `/verifier/session/{state}` | Get a verification session by state |
| GET | `/verifier/session/{state}/success` | Get the success result for a verification session |

> `POST /verifier/verify/basic?credentialType=...&format=jwt_vc_json` requests explicit policies: **signature**, **expired**, and **not-before**.

### Issuer

| Method | Path | Description |
| --- | --- | --- |
| POST | `/issuer/sessions` | Create an issuance session |
| GET | `/issuer/sessions` | List issuance sessions |
| GET | `/issuer/sessions/{sessionId}` | Get an issuance session |
| PUT | `/issuer/sessions/{sessionId}` | Update an issuance session |
| DELETE | `/issuer/sessions/{sessionId}` | Delete an issuance session |
| POST | `/issuer/sessions/{sessionId}/issue` | Issue credential(s) for a session |
| GET | `/issuer/sessions/{sessionId}/registrations` | List registrations for a session |
| GET | `/issuer/session/{sessionId}/credentials` | Get credentials for a session |

### Other

| Method | Path | Description |
| --- | --- | --- |
| GET | `/credentials` | List credentials |
| GET | `/flows` | List available flows |
| POST | `/presentations/holder-initiated` | Start a holder-initiated presentation |

---

## fulfilment-service

Base URL: `http://localhost:8087/api/v1` · All endpoints require the `apikey` header. Tracks workflow fulfilment.

| Method | Path | Description |
| --- | --- | --- |
| GET | `/fulfilment/status/{correlationId}` | Get fulfilment status |
| GET | `/fulfilment/progress/{correlationId}` | Get fulfilment progress |
| GET | `/fulfilment/result/{correlationId}` | Get the fulfilment result |
| GET | `/fulfilment/results` | List fulfilment results |
| POST | `/fulfilment/initialize/{correlationId}` | Initialize a fulfilment workflow |

---

## Examples

All examples use the `apikey` header and placeholders. Never use real student credential values.

### 1. Issue a credential (async)

```bash
curl -X POST http://localhost:8084/api/v1/student/issue \
  -H "apikey: $APP_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "userName": "<your-username>",
    "installKey": "<your-install-key>"
  }'
```

Response (`202 Accepted`):

```json
{
  "correlationId": "<correlation-id>",
  "status": "PROCESSING"
}
```

### 2. Poll issuance status

```bash
curl http://localhost:8084/api/v1/student/status/<correlation-id> \
  -H "apikey: $APP_API_KEY"
```

### 3. Fetch issued credentials

```bash
curl http://localhost:8084/api/v1/student/credentials/<correlation-id> \
  -H "apikey: $APP_API_KEY"
```

### 4. Run a basic verification

```bash
curl -X POST "http://localhost:8086/api/v1/verifier/verify/basic?credentialType=<credential-type>&format=jwt_vc_json" \
  -H "apikey: $APP_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{ "credential": "<verifiable-credential>" }'
```

This requests explicit **signature**, **expired**, and **not-before** policies. If the walt.id verifier backend (`:7003`) is unavailable, the service returns `503 Service Unavailable`.
