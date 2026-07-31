# Deployment

How to build and run the **ULHT Digital Credential System (DCS)** locally with Docker Compose. For the system design behind these components, see [Architecture](ARCHITECTURE.md).

> See also: [Architecture](ARCHITECTURE.md) · [Configuration](CONFIGURATION.md) · [Security](SECURITY.md) · [Getting Started](GETTING_STARTED.md) · [Troubleshooting](TROUBLESHOOTING.md) · [Project README](index.md)

---

## Compose files

The repository ships several compose files. Use the right combination:

| File | Role |
| --- | --- |
| **`docker-compose.microservices.yml`** | **PRIMARY** — the full application stack (services + infra + observability). |
| `docker-compose.override.yml` | Local fixes, applied on top of the primary (see [Override file](#override-file)). |
| `docker-compose.infrastructure.yml` | **Infrastructure only** (Kafka, Consul, gateway, observability). |
| `docker-compose.dev.yml` | Development variant. |
| `docker-compose.yml` (root) | **STALE — do not use.** References removed directories. |

> Do **not** run the bare root `docker-compose.yml`; it will fail because it points at directories that no longer exist.

---

## Run the stack

Start the full stack (primary + override), building images as needed:

```bash
docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml up -d --build
```

Before configuring, copy the example environment file and fill in the required secrets (there are variables with **no defaults**):

```bash
cp .env.example .env
# then edit .env — see the environment table below and the Configuration doc
```

### Environment variables

| Variable | Default | Required |
| --- | --- | --- |
| `APP_API_KEY` | `ulht-dev-local-CHANGE-ME` | Change for anything real |
| `WALLET_PASSWORD_SECRET` | *(none)* | **Yes** |
| `WALLET_PASSWORD_SALT` | *(none)* | **Yes** |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:8000` | No |
| `GRAFANA_ADMIN_USER` | `admin` | No |
| `GRAFANA_ADMIN_PASSWORD` | *(none)* | **Yes** |
| `KAFKA_UI_USER` | `admin` | No |
| `KAFKA_UI_PASSWORD` | *(none)* | **Yes** |

See [Configuration](CONFIGURATION.md) for the full reference.

---

## Kafka (KRaft mode)

Kafka runs as **Confluent `cp-kafka:8.3.0` in KRaft mode** — **no ZooKeeper**. It is a single-node combined **broker + controller**.

| Setting | Value |
| --- | --- |
| Image | `confluentinc/cp-kafka:8.3.0` |
| Mode | KRaft (combined broker + controller) |
| `CLUSTER_ID` | `MkU3OEVBNTcwNTJENDM2Qk` |
| Internal listener | `CLIENT://kafka:9092` |
| Host listener | `EXTERNAL://localhost:29092` |
| Controller listener | `CONTROLLER://kafka:29093` (internal only) |
| Auto-create topics | Enabled |

Kafka data is persisted in the **`kafka_data`** volume.

> **⚠️ `kafka_data` wipe caveat.** If you are **migrating from a previous ZooKeeper-based setup**, the old metadata is incompatible with KRaft. You must **wipe the volume before the first KRaft start**:
>
> ```bash
> docker volume rm ulht-dcs_kafka_data
> ```
>
> Failing to do this causes the broker to refuse to start against pre-existing ZooKeeper-era metadata.

---

## Container images

| Component | Image | Version |
| --- | --- | --- |
| Kafka | `confluentinc/cp-kafka` | `8.3.0` |
| Consul | `hashicorp/consul` | `1.22` |
| API gateway | `kong` | `3.9` |
| Kafka-UI | `kafbat/kafka-ui` | `v1.5.0` |
| Prometheus | `prom/prometheus` | `v3.13.1` |
| Grafana | `grafana/grafana` | `13.1.1` |
| Loki | `grafana/loki` | `3.7.4` |
| Promtail | `grafana/promtail` | `3.7.4` |
| Reverse proxy | `nginx` | `1.29-alpine` |
| Kafka exporter | `danielqsj/kafka-exporter` | `v1.9.0` |

### Exposed ports

All service and infra ports are bound to **`127.0.0.1` (loopback)**.

| Component | Port(s) |
| --- | --- |
| student-service | `8084` |
| lusofona-service | `8085` |
| credential-service | `8086` |
| fulfilment-service | `8087` |
| Kong proxy / admin / TLS | `8000` / `8001` (loopback only) / `8443` / `8444` |
| Kong-UI | `8080` |
| Kafka | `9092`, `29092` |
| Consul | `8500`, `8600` |
| Kafka-UI | `8081` → remapped to **`8181`** by the override |
| Grafana | `3000` |
| Prometheus | `9090` |
| Loki | `3100` |
| Promtail | `9080` |
| Kafka exporter | `9308` |

---

## Dockerfile build

The microservices use a **multi-stage** Docker build that produces a small, non-root runtime image.

```dockerfile
# --- build stage ---
FROM maven:3.9-eclipse-temurin-25 AS build
# ... mvn package ...

# --- runtime stage ---
FROM eclipse-temurin:25-jre-alpine
USER app          # runs as a non-root user
# ... copy jar, ENTRYPOINT ...
```

| Aspect | Value |
| --- | --- |
| Build stage image | `maven:3.9-eclipse-temurin-25` |
| Runtime image | `eclipse-temurin:25-jre-alpine` |
| Runs as | non-root `USER app` |

---

## Volumes

| Volume | Contents |
| --- | --- |
| `kafka_data` | Kafka (KRaft) log + metadata |
| `consul_data` | Consul service catalog / KV state |

---

## Healthchecks

Compose healthchecks gate startup ordering. The runtime images are **alpine-based and do not ship `curl`**, so the override redefines the healthchecks to use **busybox `wget`** instead.

```yaml
# override healthcheck pattern (busybox wget — curl is absent in alpine)
healthcheck:
  test: ["CMD", "wget", "--spider", "-q", "http://localhost:8084/api/v1/actuator/health"]
  interval: 10s
  timeout: 5s
  retries: 5
```

The public health endpoints (`/api/v1/actuator/health`, `/info`, `/prometheus`, and Swagger) do not require the `apikey` header — see [Security](SECURITY.md).

---

## Override file

`docker-compose.override.yml` layers **local fixes** on top of the primary compose file. It is applied automatically when you include it with `-f`. Its purpose:

- **Healthchecks** — switch to **busybox `wget`** because the alpine runtime images lack `curl`.
- **Kafka-UI port remap** — moves Kafka-UI from `8081` to **`8181`** to avoid a local port conflict.
- **lusofona `SPRING_APPLICATION_JSON`** — injects the **real ULHT API endpoint** into `lusofona-service` at runtime.

Always include it in the run command alongside the primary file.

---

## Infra-only vs full stack

**Infrastructure only** (Kafka, Consul, gateway, observability — no application services):

```bash
docker compose -f docker-compose.infrastructure.yml up -d
```

Useful when running the microservices from your IDE while pointing them at containerised Kafka/Consul.

**Full stack** (primary + override, build included):

```bash
docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml up -d --build
```

---

## walt.id dependency

The [walt.id](ARCHITECTURE.md#waltid-integration) components (issuer `:7002`, verifier `:7003`, wallet `:7001`, plus vc-repo + postgres) are **external to this repo** and run on a shared Docker network joined by `credential-service`.

> Some `issuer-api` versions crash with `notBefore cannot be in the past` (expired example certs). Use a patched / newer issuer-api (e.g. **0.22.0**). Without walt.id, `credential-service` returns **503** on issuance/verify (auth still passes). See [Troubleshooting](TROUBLESHOOTING.md).

---

## Production hardening

The defaults are tuned for **local development**. Before any real deployment:

- **Enable TLS** everywhere — terminate HTTPS at the edge (Kong `8443`/`8444`) and use encrypted listeners.
- **Use real secrets** — replace `APP_API_KEY` (default `ulht-dev-local-CHANGE-ME`), and set strong `WALLET_PASSWORD_SECRET`, `WALLET_PASSWORD_SALT`, `GRAFANA_ADMIN_PASSWORD`, and `KAFKA_UI_PASSWORD`. Never commit `.env`.
- **Do not expose the Kong admin API** (`:8001`) — keep it loopback-only or firewalled; exposing it hands over full gateway control.
- **Rotate keys** regularly — API keys and wallet secrets.
- Review [Security](SECURITY.md) for the full authentication model and the endpoints exposed without auth.

---

## Tear down

Stop and remove containers (volumes **retained**):

```bash
docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml down
```

Stop and remove containers **and volumes** (destroys `kafka_data` and `consul_data`):

```bash
docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml down -v
```

> Use `down -v` to get a completely clean slate — this also removes the Kafka log/metadata, which is what you want when reinitialising KRaft.

---

## Related documentation

- [Architecture](ARCHITECTURE.md)
- [Configuration](CONFIGURATION.md)
- [Security](SECURITY.md)
- [Getting Started](GETTING_STARTED.md)
- [Troubleshooting](TROUBLESHOOTING.md)
- [Project README](index.md)
