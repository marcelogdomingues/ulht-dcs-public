# DCS Student Service

The **entry point** of the Digital Credential System. It accepts a minimal student
request (username + install key), starts a credential-issuance workflow by publishing
an event to Kafka, and hands back a `correlationId` clients use to track progress.
It holds no business logic of its own — status and results are proxied from the
Fulfilment Service.

Part of the [Digital Credential System](../README.md); see
[Architecture](../docs/ARCHITECTURE.md) for the end-to-end event flow.

- **Port:** `8084` · **Context path:** `/api/v1`
- **Stack:** Java 25 · Spring Boot 4.1 · Spring Cloud 2025.1 · Apache Kafka (KRaft) · Consul

## Responsibilities

- Validate the inbound request and mint a `correlationId`.
- Publish `student.login.requested` to Kafka to kick off issuance (asynchronous — the
  HTTP call returns immediately with `PROCESSING`).
- Proxy status/progress/result queries to the Fulfilment Service so clients have a
  single endpoint to talk to.

## HTTP endpoints

All business endpoints require the `apikey` header. `/api/v1/actuator/health|info|prometheus`
and the Swagger UI (`/api/v1/swagger-ui/index.html`) are public.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/student/issue` | Start issuance; returns `correlationId` + `PROCESSING` |
| `GET`  | `/student/status/{correlationId}` | Current workflow status |
| `GET`  | `/student/credentials/{correlationId}` | Issued credential-offer URLs |
| `POST` | `/student/verify` | Start a verification workflow |
| `GET`  | `/student/verification/{correlationId}` | Verification result |

## Kafka

- **Produces:** `student.login.requested` (consumed by the SIS Service).
- **Consumes:** nothing directly — it reads workflow state from the Fulfilment Service over HTTP.

## Configuration

Full reference in [Configuration](../docs/CONFIGURATION.md). Key variables:

- `APP_API_KEY` — required; the `apikey` accepted on business endpoints.
- `SERVER_PORT` — defaults to `8084`.
- Kafka & Consul connection settings (defaulted for the Compose stack).

## Build & run

```bash
# 1. Install the shared module once (the Dockerfile does this automatically)
mvn -q -f ../dcs-commons/pom.xml -DskipTests install
# 2. Build + test this service
mvn -f pom.xml verify
```

Normally you run it as part of the stack — see the one-command
[demo](../docs/DEMO.md) (`make demo`) or [Getting Started](../docs/GETTING_STARTED.md).
