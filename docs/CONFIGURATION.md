# Configuration

Reference for configuring the **ULHT Digital Credential System (DCS)**: environment variables, the `.env` workflow, Spring profiles, ports, per-service highlights, Kafka client config, and override mechanisms.

Related docs: [Getting Started](GETTING_STARTED.md), [Architecture](ARCHITECTURE.md), [Security](SECURITY.md), [Deployment](DEPLOYMENT.md), [API](API.md), [Mobile Apps](MOBILE_APPS.md), [Troubleshooting](TROUBLESHOOTING.md), and the [project README](../README.md).

## Environment variables

These are read from the root `.env` file (copied from `.env.example`).

| Variable | Required? | Default | Used by | Description |
| --- | --- | --- | --- | --- |
| `APP_API_KEY` | No | `ulht-dev-local-CHANGE-ME` | All 4 services (expected API key) | Shared client/gateway key. Every business endpoint expects this in the `apikey` header. |
| `WALLET_PASSWORD_SECRET` | **Yes** | _(none)_ | credential-service | Secret used to derive per-student wallet passwords. The app **fails to start** if unset. |
| `WALLET_PASSWORD_SALT` | **Yes** | _(none)_ | credential-service | Salt used together with the secret for wallet password derivation. The app **fails to start** if unset. |
| `APP_CORS_ALLOWED_ORIGINS` | No | `http://localhost:8000` | All 4 services | Comma-separated CORS allow-list. |
| `GRAFANA_ADMIN_USER` | No | `admin` | Grafana | Grafana admin username. |
| `GRAFANA_ADMIN_PASSWORD` | **Yes** | _(none)_ | Grafana | Grafana admin password. |
| `KAFKA_UI_USER` | No | `admin` | Kafka-UI | Kafka-UI login username. |
| `KAFKA_UI_PASSWORD` | **Yes** | _(none)_ | Kafka-UI | Kafka-UI login password. |

## The `.env` / `.env.example` workflow

1. Copy the template: `cp .env.example .env`
2. Fill in required variables (see table above). Missing required secrets cause fail-fast startup errors — see [Getting Started](GETTING_STARTED.md).
3. The `.env` file is **git-ignored** — never commit real secrets. Keep `.env.example` as the shared, placeholder-only template.

> Use placeholders like `<your-username>` and `<your-install-key>` in any documentation or examples — never real student credential values.

## Spring profiles

Two profiles are used:

| Profile | Where | Purpose |
| --- | --- | --- |
| `default` (local) | Local runs (Maven / IDE) | Local development defaults |
| `docker` | Inside containers | Container-specific wiring |

### How the `docker` profile differs

- **Kafka bootstrap:** `kafka:9092` (internal container network address).
- **Consul:** enabled (service discovery/config).
- **Management base-path:** `/actuator`, so with the `/api/v1` context path the health URL is **`/api/v1/actuator/health`**.
- **Health details:** `management.endpoint.health.show-details=when-authorized`.
- **Exposed endpoints:** `management.endpoints.web.exposure.include=health,info,prometheus`.

Public (unauthenticated) endpoints are `/api/v1/actuator/health`, `/api/v1/actuator/info`, `/api/v1/actuator/prometheus`, plus Swagger (`/api/v1/swagger-ui/**`, `/api/v1/v3/api-docs/**`). See [Security](SECURITY.md).

## Ports

All ports are bound to `127.0.0.1` (loopback only).

| Component | Port(s) | Notes |
| --- | --- | --- |
| student-service | 8084 | context-path `/api/v1` |
| lusofona-service | 8085 | context-path `/api/v1` |
| credential-service | 8086 | context-path `/api/v1` |
| fulfilment-service | 8087 | context-path `/api/v1` |
| Kong proxy | 8000 | API gateway proxy |
| Kong admin | 8001 | loopback admin API |
| Kong (TLS) | 8443 / 8444 | proxy TLS / admin TLS |
| Kong-UI | 8080 | |
| Kafka | 9092 & 29092 | KRaft; controller `29093` internal |
| Consul | 8500 & 8600 | HTTP / DNS |
| Kafka-UI | 8081 (remapped **8181** via override) | |
| Grafana | 3000 | |
| Prometheus | 9090 | |
| Loki | 3100 | |
| Promtail | 9080 | |
| Kafka Exporter | 9308 | |

> The external walt.id backend (issuer 7002 / verifier 7003 / wallet 7001) is **not** part of this compose project. See [Getting Started](GETTING_STARTED.md#walt-id-backend).

## Per-service configuration highlights

- **Context path:** all four services serve under `/api/v1`.
- **CORS:** the allow-list is driven by `APP_CORS_ALLOWED_ORIGINS` (comma-separated; default `http://localhost:8000`).
- **API key:** business endpoints require the `apikey` header matching `APP_API_KEY`.
- **Wallet password derivation (credential-service):** requires `WALLET_PASSWORD_SECRET` and `WALLET_PASSWORD_SALT`; both are mandatory and have no defaults.

## Kafka client configuration

Kafka runs in **KRaft** mode (`confluentinc/cp-kafka:8.3.0`, no ZooKeeper). Bootstrap addresses differ by network vantage point:

| Access path | Bootstrap address |
| --- | --- |
| From inside the container network (docker profile) | `kafka:9092` |
| From the host machine | `localhost:29092` |

The internal KRaft controller listener is `29093`.

> If migrating from a non-KRaft layout, wipe the data volume before the first KRaft start:
>
> ```bash
> docker volume rm ulht-dcs_kafka_data
> ```

## Overriding configuration

Spring Boot resolves configuration from multiple sources. You can override any property without editing files:

- **Environment variables** (relaxed binding), e.g. `APP_CORS_ALLOWED_ORIGINS=http://localhost:8000,http://localhost:3000`.
- **Command-line arguments**, e.g.:

  ```bash
  java -jar credential-service.jar \
    --spring.profiles.active=docker \
    --management.endpoint.health.show-details=when-authorized
  ```

- **`SPRING_APPLICATION_JSON`** for structured overrides:

  ```bash
  export SPRING_APPLICATION_JSON='{"spring":{"kafka":{"bootstrap-servers":"localhost:29092"}}}'
  ```

For the compose commands and profile activation used by the stack, see [Getting Started](GETTING_STARTED.md) and [Deployment](DEPLOYMENT.md).
