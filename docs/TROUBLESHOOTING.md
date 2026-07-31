# Troubleshooting

A practical problem → cause → fix guide for running the ULHT Digital Credential System.

The stack is **Spring Boot 4.1.0 / Java 25** microservices, **KRaft-mode Kafka** (`confluentinc/cp-kafka:8.3.0`, no ZooKeeper), Consul, Prometheus/Grafana/Loki, and three [Flutter apps](MOBILE_APPS.md). The credential service talks to an external [walt.id](https://walt.id) stack.

See also: [Getting Started](GETTING_STARTED.md) · [Configuration](CONFIGURATION.md) · [Security](SECURITY.md) · [API](API.md) · [Architecture](ARCHITECTURE.md) · [Mobile Apps](MOBILE_APPS.md) · [Project README](index.md)

## Primary run command

Use the microservices compose file **plus** the local override — do **not** use the root `docker-compose.yml` (it is stale):

```bash
docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml up -d
```

Before your **first** KRaft start, wipe any old ZooKeeper-mode Kafka volume (see below).

---

## Issues

### 401 Unauthorized on API calls

- **Cause:** the request is missing the `apikey` header (or sending the wrong key). Every business endpoint is protected by Spring Security.
- **Fix:** send the API key on every call.

```bash
curl -H "apikey: $APP_API_KEY" http://localhost:8084/api/v1/student/status/<credentialId>
```

Only `/api/v1/actuator/health`, `/api/v1/actuator/info`, `/api/v1/actuator/prometheus`, and the Swagger endpoints are public. Everything else requires the key.

### Service won't start — "WALLET_PASSWORD_SECRET required" (or `GRAFANA_ADMIN_PASSWORD` / `KAFKA_UI_PASSWORD`)

- **Cause:** a required environment variable is unset. Several vars have **no default** and the service fails fast without them.
- **Fix:** create your `.env` from the template and fill in the values.

```bash
cp .env.example .env
# then edit .env and set APP_API_KEY, WALLET_PASSWORD_SECRET, WALLET_PASSWORD_SALT,
# GRAFANA_ADMIN_PASSWORD, KAFKA_UI_PASSWORD, APP_CORS_ALLOWED_ORIGINS
```

### Kafka won't start after switching to KRaft ("log directory ... zookeeper" incompatibility)

- **Cause:** an old ZooKeeper-mode data volume is incompatible with KRaft's log format.
- **Fix:** wipe the stale Kafka volume, then bring the stack up again.

```bash
docker volume rm ulht-dcs_kafka_data
# or, to reset all volumes:
docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml down -v
```

### Container name conflict — "/kafka already in use"

- **Cause:** a stale container from a previous run still exists.
- **Fix:** remove it and re-run `up`.

```bash
docker rm -f kafka
docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml up -d
```

### Healthchecks never go healthy

- **Cause:** the `eclipse-temurin` alpine base images do **not** ship `curl`, so a `curl`-based healthcheck always fails.
- **Fix:** already handled — the override uses busybox `wget`. If you edit healthchecks, keep using `wget`. The health path is `/api/v1/actuator/health`.

### credential-service returns 503 on issue / verify

- **Cause:** the external walt.id backend is not running.
- **Fix:** start the walt.id stack. Expected ports: **issuer 7002**, **verifier 7003**, **wallet 7001**.

### walt.id issuer-api crashes at startup — "notBefore cannot be in the past"

- **Cause:** some issuer-api versions ship example certificates whose dates are now in the past.
- **Fix:** use a newer / patched issuer-api image (e.g. `waltid/issuer-api:0.22.0`).

### Kong gateway (8000) returns 503

- **Cause:** the gateway routes are partial / aspirational and not fully wired.
- **Fix:** call the services **directly by port** (`8084`–`8087`) with the `apikey` header. This is exactly what the [mobile apps](MOBILE_APPS.md) do.

### Consul unhealthy after long downtime

- **Cause:** a stale `consul_data` volume (goes stale after extended offline periods).
- **Fix:**

```bash
docker volume rm ulht-dcs_consul_data
```

### Prometheus not scraping a service

- **Cause:** Prometheus must scrape the metrics path `/api/v1/actuator/prometheus` (public, no key required).
- **Fix:** verify the target is configured to that path and is reachable.

```bash
curl http://localhost:8084/api/v1/actuator/prometheus
```

### Mobile app can't reach the backend

- **Cause:** wrong `--dart-define` base URL, missing `apikey`, or services bound to `127.0.0.1` and thus unreachable from a device/emulator.
- **Fix:**
  - Pass the correct `*_SVC_URL` values (see [Mobile Apps](MOBILE_APPS.md)).
  - Ensure `API_KEY` is set so the `apikey` header is sent.
  - From a physical device or emulator, use the host's reachable address (e.g. `10.0.2.2` for the Android emulator, or the host's LAN IP) rather than `localhost`.

### Root `docker-compose.yml` errors about missing build dirs

- **Cause:** the root `docker-compose.yml` is stale and references directories that no longer exist.
- **Fix:** use `docker-compose.microservices.yml` together with `docker-compose.override.yml` (see the primary run command above).

---

## Useful commands

```bash
# Follow logs for all services
docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml logs -f

# Follow logs for one service
docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml logs -f credential-service

# List running containers
docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml ps

# List Kafka topics (KRaft mode)
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check a service health endpoint (public, no key needed)
curl http://localhost:8084/api/v1/actuator/health

# Bring the stack down (add -v to also drop volumes)
docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml down
```
