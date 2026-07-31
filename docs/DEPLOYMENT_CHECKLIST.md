# Deployment Checklist

A professional, actionable **pre-deployment and infrastructure-readiness checklist** for the **ULHT Digital Credential System (DCS)**. Work top to bottom; do not tick a box you cannot demonstrate. This complements the step-by-step [Deployment](DEPLOYMENT.md) guide, the [Security](SECURITY.md) hardening model, and the [CI/CD](CICD.md) pipeline.

> See also: [Deployment](DEPLOYMENT.md) · [Security](SECURITY.md) · [Configuration](CONFIGURATION.md) · [Architecture](ARCHITECTURE.md) · [CI/CD](CICD.md) · [Getting Started](GETTING_STARTED.md) · [Troubleshooting](TROUBLESHOOTING.md) · [Project README](index.md)

---

## The READY path

```mermaid
flowchart TD
    A[Start] --> B{Prerequisites met?<br/>Docker · walt.id up · SIS reachable}
    B -- No --> B1[Install / provision · fix] --> B
    B -- Yes --> C{All required secrets set?<br/>no ${VAR:?} failures}
    C -- No --> C1[Populate .env from a secret manager] --> C
    C -- Yes --> D{Security hardening done?<br/>TLS · admin API closed · CORS · Kafka SASL/TLS}
    D -- No --> D1[Apply hardening] --> D
    D -- Yes --> E{CI green?<br/>Backend · Mobile · Docker · CodeQL}
    E -- No --> E1[Fix failing checks] --> E
    E -- Yes --> F[Build & deploy]
    F --> G{Smoke tests pass?<br/>health 200 · authed issue}
    G -- No --> G1[Diagnose · rollback if needed] --> F
    G -- Yes --> H{Observability live?<br/>Prometheus targets UP · Grafana · Loki}
    H -- No --> H1[Fix scrape/log wiring] --> H
    H -- Yes --> I{Backup & rollback rehearsed?}
    I -- No --> I1[Configure backups · test restore] --> I
    I -- Yes --> J[Sign-off table complete] --> K([GO LIVE])
```

---

## 1. Prerequisites

- [ ] Docker Engine + Docker Compose v2 installed on the target host (`docker compose version`).
- [ ] Host has enough resources for the stack (allow ~6–8 GB RAM headroom for Kafka + four JVM services + Kong + observability).
- [ ] Repository checked out at the intended commit/tag on the host (or CI/CD delivery configured).
- [ ] Outbound network access from the host to GHCR (`ghcr.io`) if pulling pre-built images.
- [ ] **External walt.id stack is running and reachable** on the shared network (`waltid_network` → `docker-compose_default`):
    - [ ] issuer-api on `:7002`
    - [ ] verifier-api on `:7003`
    - [ ] wallet-api on `:7001`
    - [ ] issuer-api is a **patched/newer** build (no `notBefore cannot be in the past` time-bomb).
- [ ] The `waltid_network` external network exists (`docker network ls | grep docker-compose_default`).
- [ ] **University SIS endpoint reachable** from the host (set later via `LUSOFONA_API_URL`).
- [ ] Host ports free on `127.0.0.1`: `8000/8001/8443/8444` (Kong), `8082` (Kong-UI), `8084–8087` (services), `8181` (Kafka-UI via override), `9092/29092` (Kafka), `8500/8600` (Consul), and — for observability — `9090/3000/3100/9308`.

---

## 2. Required secrets & environment

Copy the template and populate it — ideally from a secret manager, not by hand:

```bash
cp .env.example .env
```

**Must be set (no defaults — Compose `${VAR:?}` fails startup if missing):**

- [ ] `WALLET_PASSWORD_SECRET` — long random secret (credential-service wallet derivation).
- [ ] `WALLET_PASSWORD_SALT` — random salt (credential-service wallet derivation).
- [ ] `KAFKA_UI_PASSWORD` — Kafka-UI login password.
- [ ] `GRAFANA_ADMIN_PASSWORD` — Grafana admin password (observability stack).

**Must be changed from the insecure default before any real exposure:**

- [ ] `APP_API_KEY` — default is `ulht-dev-local-CHANGE-ME`; replace with a strong, unique key.

**Should be reviewed / set for the environment:**

- [ ] `APP_CORS_ALLOWED_ORIGINS` — set to the exact front-end origin(s), not a wildcard.
- [ ] `LUSOFONA_API_URL` — your institution's SIS base URL (placeholder in the public repo).
- [ ] `KAFKA_UI_USER` / `GRAFANA_ADMIN_USER` — change from `admin` if policy requires.
- [ ] `JVM_XMS` / `JVM_XMX` — sized for the host (Kafka-UI JVM).

**Hygiene:**

- [ ] `.env` is **git-ignored** and never committed (`git check-ignore .env`).
- [ ] Secrets are stored in a secret manager / CI secret store, not in shell history or tickets.
- [ ] File permissions on `.env` are locked down (`chmod 600 .env`).

---

## 3. Security hardening (before exposure)

- [ ] **TLS enabled at the edge** — Kong proxy serves HTTPS (`:8443`); certificates valid and not self-signed for production.
- [ ] **Kong admin API (`:8001`) NOT exposed** — loopback-only or firewalled. Exposing it grants full gateway control.
- [ ] **CORS restricted** — `APP_CORS_ALLOWED_ORIGINS` is an explicit allow-list.
- [ ] **Kafka not PLAINTEXT on the wire** for non-loopback deployments — **SASL/TLS** configured (default listeners are PLAINTEXT).
- [ ] **Kafka-UI and Grafana** are behind auth (they are, by default) and not publicly reachable without need.
- [ ] **Only intended host ports are published** — everything binds to `127.0.0.1` by default; confirm nothing is on `0.0.0.0` unintentionally.
- [ ] **Secrets injected from a manager** (Vault / cloud secret store), not a plaintext file baked into an image.
- [ ] **Key rotation plan** documented for `APP_API_KEY` and wallet secrets.
- [ ] Reviewed [Security](SECURITY.md) — including which endpoints are intentionally unauthenticated (`/actuator/health`, `/info`, `/prometheus`, Swagger).

---

## 4. Build, deploy & smoke tests

**Build / deploy:**

- [ ] Validate the Compose config renders (catches YAML / `${VAR}` errors):
  ```bash
  docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml config >/dev/null
  ```
- [ ] Bring up the stack:
  ```bash
  docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml up -d --build
  ```
- [ ] All containers report **healthy**:
  ```bash
  docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml ps
  ```

**Smoke tests:**

- [ ] Each service health endpoint returns **HTTP 200** (no `apikey` needed):
  ```bash
  for p in 8084 8085 8086 8087; do
    curl -fsS "http://127.0.0.1:$p/api/v1/actuator/health" && echo " OK :$p"
  done
  ```
- [ ] Gateway routes are live through Kong (`http://127.0.0.1:8000/...`).
- [ ] **Authenticated write path** works — issuing a student credential with the API key returns success:
  ```bash
  curl -fsS -X POST "http://127.0.0.1:8084/api/v1/student/issue" \
    -H "apikey: $APP_API_KEY" \
    -H "Content-Type: application/json" \
    -d '{ ... issuance payload ... }'
  ```
- [ ] A request **without** the `apikey` header is rejected (401/403) — confirms auth is enforced.
- [ ] credential-service can reach walt.id (issuance/verify does **not** return 503).

---

## 5. Data & persistence

- [ ] `kafka_data` volume present and writable (KRaft log + metadata).
- [ ] `consul_data` volume present (service catalog / KV).
- [ ] For the observability stack: `prometheus_data`, `grafana_data`, `loki_data` present.
- [ ] **KRaft wipe caveat handled** — if this host previously ran Kafka in **ZooKeeper mode**, the old `kafka_data` is incompatible; it was wiped before first KRaft boot:
  ```bash
  docker volume ls | grep kafka_data          # confirm the name
  docker volume rm <project>_kafka_data        # only when migrating from ZooKeeper
  ```
- [ ] Volume backups configured for stateful volumes (see §7).

---

## 6. Observability

- [ ] Observability stack up (`docker-compose.infrastructure.yml`).
- [ ] **Prometheus targets all UP** at `http://127.0.0.1:9090/targets` — the four services (`/api/v1/actuator/prometheus`), `kafka-exporter:9308`, `consul:8500`, and Prometheus itself.
- [ ] **Grafana** reachable at `http://127.0.0.1:3000`, login works with `GRAFANA_ADMIN_PASSWORD`, provisioned dashboards render with data.
- [ ] **Loki + Promtail** shipping logs — logs visible in Grafana Explore / the logs dashboards.
- [ ] Alerting path decided (Alertmanager is commented out in `monitoring/prometheus.yml` — wire it up if alerts are required).

---

## 7. Backup, rollback & scaling

- [ ] **Backup** — scheduled backups of `kafka_data`, `consul_data`, and `grafana_data`; restore procedure tested at least once.
- [ ] **Rollback** — previous image tags (GHCR `:<sha>`) retained; documented steps to redeploy the last-good tag.
- [ ] **Rollback rehearsed** — a redeploy-to-previous-tag has actually been performed in a non-prod environment.
- [ ] **Scaling notes reviewed** — Kafka is **single-node** (`replication-factor=1`); horizontal scaling of the broker requires a multi-node KRaft quorum and higher replication factors. Stateless services can be scaled behind Kong, but confirm Kafka consumer-group semantics first.
- [ ] Resource limits (`deploy.resources`) reviewed against the host's capacity.

---

## 8. Go-live sign-off

| Area | Owner | Verified (date) | Status |
| --- | --- | --- | --- |
| Prerequisites (Docker, walt.id, SIS) |  |  | ☐ |
| Secrets & environment |  |  | ☐ |
| Security hardening |  |  | ☐ |
| CI green (Backend / Mobile / Docker / CodeQL) |  |  | ☐ |
| Build & deploy + smoke tests |  |  | ☐ |
| Data & persistence (KRaft caveat) |  |  | ☐ |
| Observability (Prometheus / Grafana / Loki) |  |  | ☐ |
| Backup / rollback rehearsed |  |  | ☐ |
| **Final approval to GO LIVE** |  |  | ☐ |

---

## Related documentation

- [Deployment](DEPLOYMENT.md) — full run guide, topology, healthchecks, KRaft
- [Security](SECURITY.md) — auth model & hardening
- [Configuration](CONFIGURATION.md) — environment variable reference
- [CI/CD](CICD.md) — pipelines, GHCR, required repo settings
- [Troubleshooting](TROUBLESHOOTING.md) — common failure modes
- [Project README](index.md)
