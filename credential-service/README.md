# DCS Credential Service

The heart of the platform: it issues, stores the session state for, and verifies
**W3C Verifiable Credentials**. It consumes issuance requests from the pipeline and
drives the external **walt.id** stack (Issuer / Wallet / Verifier) over OID4VCI /
OID4VP, then reports progress back over Kafka. It also serves the **Bitstring Status
List** used for credential revocation.

> walt.id Wallet/Issuer/Verifier are treated as **external services** — this service
> is a thin, resilient proxy around their APIs, not a custom wallet. In the
> [demo profile](../docs/DEMO.md) those clients are mocked.

Part of the [Digital Credential System](../README.md); see
[Architecture](../docs/ARCHITECTURE.md) and [Standards](../docs/STANDARDS.md) for the
VC/OID4VCI/OID4VP details.

- **Port:** `8086` · **Context path:** `/api/v1`
- **Stack:** Java 25 · Spring Boot 4.1 · Spring Cloud 2025.1 · Apache Kafka (KRaft) · PostgreSQL + JPA + Flyway · OpenFeign · Resilience4j · Consul

## Responsibilities

- Consume `credential.requests` and issue the applicable credentials (EducationalID,
  IdentityCredential, EuropeanStudentCard, and conditionally UniversityDegree).
- Ensure a wallet, obtain the subject DID, and produce OID4VCI **credential-offer URLs**.
- Embed a `credentialStatus` (Bitstring Status List entry) in issued VCs and serve the
  status list / revocation endpoints.
- Handle verification requests (OID4VP) and publish progress/results.
- Persist issuer sessions and credential status to PostgreSQL (Flyway-managed schema).

## HTTP endpoints

All business endpoints require the `apikey` header; `/api/v1/actuator/health|info|prometheus`
and the Swagger UI (`/api/v1/swagger-ui/index.html`) are public. Highlights:

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/issuer/start` | Begin an issuance session |
| `POST` | `/sessions` · `GET` `/sessions/{id}` | Manage issuer sessions |
| `POST` | `/verify`, `/verify/basic` | Verify a presentation (OID4VP) |
| `GET`  | `/status-list/{listId}` | W3C Bitstring Status List credential |
| `POST` | `/credentials/{id}/revoke` · `GET` `/credentials/{id}/status` | Revocation |

Full surface is in the [API Reference](../docs/API.md) and the live Swagger UI.

## Kafka

- **Consumes:** `credential.requests`, `verification.requested`, `wallet.requests`,
  `wallet.completed`, `wallet.error`.
- **Produces:** `credential.progress` / `credential.completed` / `credential.error`,
  `verification.progress` / `verification.completed` / `verification.error`,
  `wallet.requests` / `wallet.progress` / `wallet.completed` / `wallet.error`.

Idempotency is enforced via a `processed_event` table; failures are routed to
`<topic>.DLT` by the Kafka error handler.

## Configuration

Full reference in [Configuration](../docs/CONFIGURATION.md). Key variables:

- `APP_API_KEY` — required; the `apikey` accepted on business endpoints.
- `WALLET_PASSWORD_SECRET` / `WALLET_PASSWORD_SALT` — required; wallet credential derivation.
- `SPRING_DATASOURCE_*` — PostgreSQL connection (uses a dedicated `credential` database).
- walt.id Issuer/Wallet/Verifier base URLs; Kafka & Consul settings (defaulted for Compose).

## Build & run

```bash
# 1. Install the shared module once (the Dockerfile does this automatically)
mvn -q -f ../dcs-commons/pom.xml -DskipTests install
# 2. Build + test (generates API classes from openapi/api-spec.yaml)
mvn -f pom.xml verify
```

Full issuance needs the external **walt.id** stack — see
[Getting Started → walt.id backend](../docs/GETTING_STARTED.md#walt-id-backend). For a
zero-dependency run, use the [demo](../docs/DEMO.md) (`make demo`).
