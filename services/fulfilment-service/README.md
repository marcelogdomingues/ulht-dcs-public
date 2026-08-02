# DCS Fulfilment Service

The workflow **tracker**. It listens to the progress/completion/error events emitted
across the pipeline, persists the current state of each workflow, and exposes it for
polling (REST) or live updates (Server-Sent Events). The Student Service proxies its
status/result endpoints, so clients get a single place to follow a request.

Part of the [Digital Credential System](../../README.md); see
[Architecture](../../docs/ARCHITECTURE.md) for the end-to-end event flow.

- **Port:** `8087` · **Context path:** `/api/v1`
- **Stack:** Java 25 · Spring Boot 4.1 · Spring Cloud 2025.1 · Apache Kafka (KRaft) · PostgreSQL + JPA + Flyway · Server-Sent Events · Consul

## Responsibilities

- Consume issuance and verification progress/completion/error events and fold them into
  a single per-`correlationId` workflow record (persisted to PostgreSQL).
- Serve workflow status/progress/result over REST for polling.
- Stream real-time updates to clients over **SSE**.

## HTTP endpoints

All business endpoints require the `apikey` header; `/api/v1/actuator/health|info|prometheus`
and the Swagger UI (`/api/v1/swagger-ui/index.html`) are public.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/fulfilment/initialize/{correlationId}` | Register a new workflow |
| `GET`  | `/fulfilment/status/{correlationId}` | Current status |
| `GET`  | `/fulfilment/progress/{correlationId}` | Live progress (SSE stream) |
| `GET`  | `/fulfilment/result/{correlationId}` | Final result |
| `GET`  | `/fulfilment/results` | List tracked workflows |

## Kafka

- **Consumes:** `credential.progress` / `credential.completed` / `credential.error`,
  `verification.progress` / `verification.completed` / `verification.error`.
- **Produces:** nothing — it is a terminal sink for workflow state. Consumption is
  naturally idempotent (last-write-wins per `correlationId`).

## Configuration

Full reference in [Configuration](../../docs/CONFIGURATION.md). Key variables:

- `APP_API_KEY` — required; the `apikey` accepted on business endpoints.
- `SPRING_DATASOURCE_*` — PostgreSQL connection (workflow state).
- Kafka & Consul connection settings (defaulted for the Compose stack).

## Build & run

```bash
# 1. Install the shared module once (the Dockerfile does this automatically)
mvn -q -f ../dcs-commons/pom.xml -DskipTests install
# 2. Build + test this service
mvn -f pom.xml verify
```

Normally you run it as part of the stack — see the one-command
[demo](../../docs/DEMO.md) (`make demo`) or [Getting Started](../../docs/GETTING_STARTED.md).
