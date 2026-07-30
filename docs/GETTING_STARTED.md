# Getting Started

This guide walks you through running the **ULHT Digital Credential System (DCS)** locally: from prerequisites to issuing your first credential end-to-end.

For deeper reference material, see [Configuration](CONFIGURATION.md), [Architecture](ARCHITECTURE.md), [Security](SECURITY.md), [Deployment](DEPLOYMENT.md), [API](API.md), [Mobile Apps](MOBILE_APPS.md), and [Troubleshooting](TROUBLESHOOTING.md). See also the [project README](../README.md).

## Prerequisites

| Tool | Version | When you need it |
| --- | --- | --- |
| Docker | Recent | Always — the whole stack runs in containers |
| Docker Compose | v2 (`docker compose`) | Always — orchestrates the services |
| Java (JDK) | 25 | Only if building a service locally with Maven |
| Maven | 3.9 | Only if building a service locally |
| Flutter / Dart | Flutter 3.44.8 / Dart 3.12.2 | Only if building/running the mobile apps |

The backend stack is **Java 25**, **Spring Boot 4.1.0**, **Spring Cloud 2025.1.2**, **springdoc 3.0.3**, and **resilience4j-spring-boot4 2.4.0**. You do **not** need Java or Maven installed to run the stack via Docker — the images are built inside the compose build.

## 1. Clone the repository

```bash
git clone <repository-url> ulht-dcs
cd ulht-dcs
```

## 2. Configure your environment (`.env`)

The stack reads secrets and configuration from a `.env` file at the repo root. Start from the example:

```bash
cp .env.example .env
```

Then open `.env` and fill in the required variables. **This step is not optional**: several services **fail fast on startup** if required secrets are missing. In particular, `credential-service` will refuse to start unless both `WALLET_PASSWORD_SECRET` and `WALLET_PASSWORD_SALT` are set, because they are used to derive per-student wallet passwords. Grafana and Kafka-UI also require passwords.

At minimum, set:

| Variable | Required | Notes |
| --- | --- | --- |
| `APP_API_KEY` | Recommended | Shared client/gateway key; dev default is `ulht-dev-local-CHANGE-ME` |
| `WALLET_PASSWORD_SECRET` | **Yes** | No default — `credential-service` won't start without it |
| `WALLET_PASSWORD_SALT` | **Yes** | No default — `credential-service` won't start without it |
| `GRAFANA_ADMIN_PASSWORD` | **Yes** | Grafana admin password |
| `KAFKA_UI_PASSWORD` | **Yes** | Kafka-UI login password |

See [Configuration](CONFIGURATION.md) for the complete variables table and defaults.

## 3. Build and run the full stack

Use the **microservices** compose file plus the override file. Do **not** use the root `docker-compose.yml` — it is stale.

```bash
docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml up -d --build
```

> **Kafka note:** the stack uses `confluentinc/cp-kafka:8.3.0` in **KRaft** mode (no ZooKeeper). If you are migrating from an older layout, wipe the Kafka data volume before the first KRaft start:
>
> ```bash
> docker volume rm ulht-dcs_kafka_data
> ```

## 4. Verify health

Check that the containers are up:

```bash
docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml ps
```

All services expose a **public** health endpoint (no `apikey` required). Note the context path `/api/v1` and, in the docker profile, the actuator base-path `/actuator`:

```bash
curl http://127.0.0.1:8084/api/v1/actuator/health   # student-service
curl http://127.0.0.1:8085/api/v1/actuator/health   # lusofona-service
curl http://127.0.0.1:8086/api/v1/actuator/health   # credential-service
curl http://127.0.0.1:8087/api/v1/actuator/health   # fulfilment-service
```

A healthy service returns `{"status":"UP"}`. The Swagger UI is also public, e.g. `http://127.0.0.1:8084/api/v1/swagger-ui/`.

## 5. Issue your first credential (end-to-end)

Every business endpoint requires the `apikey` header. Use your configured `APP_API_KEY` value.

**Step 1 — request issuance.** This returns `202 Accepted` with a `correlationId` and `status: PROCESSING`.

```bash
curl -X POST http://127.0.0.1:8084/api/v1/student/issue \
  -H "apikey: <APP_API_KEY>" \
  -H "Content-Type: application/json" \
  -d '{"userName":"<your-username>","installKey":"<your-install-key>"}'
```

Response:

```json
{ "correlationId": "1234-abcd", "status": "PROCESSING" }
```

**Step 2 — poll the status** with the returned `correlationId`:

```bash
curl http://127.0.0.1:8084/api/v1/student/status/<correlationId> \
  -H "apikey: <APP_API_KEY>"
```

**Step 3 — fetch the issued credential** once processing completes:

```bash
curl http://127.0.0.1:8084/api/v1/student/credentials/<correlationId> \
  -H "apikey: <APP_API_KEY>"
```

> Never commit or share real student credential values. Use placeholders such as `<your-username>` and `<your-install-key>` in examples.

See the [API](API.md) reference for the full endpoint catalog.

## walt-id-backend

Real credential issuance depends on an **external walt.id stack**, which is **not** part of this compose project. It provides:

| Component | Port |
| --- | --- |
| Issuer API | 7002 |
| Verifier API | 7003 |
| Wallet API | 7001 |

Without a running walt.id backend, `credential-service` authentication still passes (so a valid `apikey` is accepted), but issuance calls return **HTTP 503** because the downstream issuer/verifier/wallet APIs are unavailable.

> **Known issue:** some `issuer-api` versions crash with `notBefore cannot be in the past`. Use a newer/patched `issuer-api` (for example, **0.22.0**). See [Troubleshooting](TROUBLESHOOTING.md) for details and the certificate/`notBefore` workaround.

## Building a single service locally

If you want to build just one service with Maven (Java 25 + Maven 3.9 required):

```bash
cd credential-service && mvn -q clean package
```

Swap the directory for `student-service`, `lusofona-service`, or `fulfilment-service` as needed.

## Running the mobile apps

The Flutter apps (student and verifier) call the services directly by port and are configured at build time via `--dart-define` values. Full setup, run commands, and the required defines are documented in [Mobile Apps](MOBILE_APPS.md).

## Where to go next

- [Configuration](CONFIGURATION.md) — every environment variable, ports, and Spring profiles
- [Architecture](ARCHITECTURE.md) — how the services fit together
- [API](API.md) — endpoint reference
- [Security](SECURITY.md) — auth model and secrets handling
- [Deployment](DEPLOYMENT.md) — deploying beyond local
- [Mobile Apps](MOBILE_APPS.md) — building and running the Flutter clients
- [Troubleshooting](TROUBLESHOOTING.md) — common failures, including the walt.id `notBefore` issue
