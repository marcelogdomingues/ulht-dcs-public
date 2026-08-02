# DCS SIS Service

The integration layer to the university's **Student Information System (SIGES)**. It
consumes the login event, fetches real student academic data (enrolment, grades,
credits, evaluations) from the upstream SIS over HTTP, and publishes a
credential-issuance request onto Kafka — so the rest of the platform never talks to
the SIS directly.

> In the [demo profile](../../docs/DEMO.md) the SIS client is replaced by an in-memory
> mock, so the whole flow runs with no external SIS.

Part of the [Digital Credential System](../../README.md); see
[Architecture](../../docs/ARCHITECTURE.md) for the end-to-end event flow.

- **Port:** `8085` · **Context path:** `/api/v1`
- **Stack:** Java 25 · Spring Boot 4.1 · Spring Cloud 2025.1 · Apache Kafka (KRaft) · OpenFeign · Apache Avro · Resilience4j · Consul

## Responsibilities

- Consume `student.login.requested` and authenticate/fetch the student from SIGES.
- Map the SIS payload into strongly-typed **Avro** messages for the pipeline.
- Publish `credential.requests` for the Credential Service (or `credential.error`
  when the SIS lookup fails — issuance must never proceed on missing/fake data).
- Shield the platform from the SIS with **Resilience4j** circuit-breaker/retry.

## HTTP endpoints

Mostly event-driven; it also exposes direct endpoints for testing/health. All
business endpoints require the `apikey` header; `/api/v1/actuator/health|info|prometheus`
and the Swagger UI (`/api/v1/swagger-ui/index.html`) are public.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/studentLogin` | Fetch student data directly from the SIS (bypasses Kafka) |
| `GET`  | `/actuator/health` | Liveness/readiness |

## Kafka

- **Consumes:** `student.login.requested` (from the Student Service).
- **Produces:** `credential.requests` (to the Credential Service); `credential.error` on failure.

## Configuration

Full reference in [Configuration](../../docs/CONFIGURATION.md). Key variables:

- `APP_API_KEY` — required; the `apikey` accepted on business endpoints.
- `SIS_API_URL` — base URL of the upstream SIS (defaults to a placeholder
  `https://university-sis.example.edu/api`; usually injected via `SPRING_APPLICATION_JSON`).
- Kafka & Consul connection settings (defaulted for the Compose stack).

## Build & run

```bash
# 1. Install the shared module once (the Dockerfile does this automatically)
mvn -q -f ../dcs-commons/pom.xml -DskipTests install
# 2. Build + test this service (also generates Avro sources)
mvn -f pom.xml verify
```

Normally you run it as part of the stack — see the one-command
[demo](../../docs/DEMO.md) (`make demo`) or [Getting Started](../../docs/GETTING_STARTED.md).
