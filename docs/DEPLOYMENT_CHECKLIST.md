# Deployment Checklist

A pre-deployment and infrastructure-readiness checklist for the **ULHT Digital
Credential System (DCS)**. Work through it top to bottom before exposing the stack
beyond a local machine. Every item is a checkbox — nothing is optional for a shared
or production deployment.

> This is an operational checklist. For the reasoning behind each control see
> [Security](SECURITY.md) and [Deployment](DEPLOYMENT.md).
>
> See also: [Configuration](CONFIGURATION.md) · [Architecture](ARCHITECTURE.md) · [Getting Started](GETTING_STARTED.md) · [Troubleshooting](TROUBLESHOOTING.md) · [CI/CD](CICD.md) · [Contributing](https://github.com/marcelogdomingues/ulht-dcs-public/blob/main/CONTRIBUTING.md) · [Project README](index.md)

> **Never put real credentials in this file, in `.env.example`, in Compose files, or in
> git.** Use placeholders like `<generate-a-strong-value>` everywhere. Real secrets live
> only in the untracked `.env` or a secret manager.

---

## 1. Prerequisites (infrastructure)

- [ ] **Docker Engine** (recent) installed and running on the host.
- [ ] **Docker Compose v2** available as `docker compose` (not legacy `docker-compose`).
- [ ] Host has adequate **resources** — CPU, RAM, and disk for the four services plus
      Kafka, Consul, Kong, and the observability stack (Prometheus, Grafana, Loki,
      Promtail). Budget generously; Kafka and the JVMs are the heaviest tenants.
- [ ] **Network / DNS** planned — hostname(s) for the public entry point resolve, and
      firewall rules are defined for exactly the ports you intend to expose.
- [ ] The **external walt.id backend** is reachable and healthy. It is **not** part of
      this repo and must run on the shared Docker network joined by `credential-service`:
  - [ ] Issuer API on `7002`
  - [ ] Verifier API on `7003`
  - [ ] Wallet API on `7001`
  - [ ] A **patched / newer `issuer-api`** (e.g. `0.22.0`) is used to avoid the
        `notBefore cannot be in the past` crash. Without walt.id, `credential-service`
        returns **503** on issuance/verify (auth still passes). See
        [Troubleshooting](TROUBLESHOOTING.md).

---

## 2. Required secrets & environment

Copy the template and fill in strong, unique values: `cp .env.example .env`.
Confirm **none** of the following are left at their defaults or placeholders.

- [ ] `APP_API_KEY` set to a **strong, unique, non-default** value
      (not `ulht-dev-local-CHANGE-ME`).
- [ ] `WALLET_PASSWORD_SECRET` set to a long random secret (**no default** — service
      fails fast without it).
- [ ] `WALLET_PASSWORD_SALT` set to a random salt (**no default** — service fails fast
      without it).
- [ ] `GRAFANA_ADMIN_PASSWORD` set to a strong value (**no default**).
- [ ] `KAFKA_UI_PASSWORD` set to a strong value (**no default**).
- [ ] `APP_CORS_ALLOWED_ORIGINS` set to the **real** front-end origin(s) — never `*`.
- [ ] `.env` is present on the host, is **not** committed to git, and file permissions
      restrict it to the deploy user.
- [ ] Every variable in `.env.example` has been reviewed and given an appropriate value.

See [Configuration](CONFIGURATION.md) for the full variable reference.

---

## 3. Security hardening (before any exposure)

- [ ] **TLS / HTTPS termination** in place at the edge — terminate at Kong
      (`8443`/`8444`) and/or a reverse proxy; disable plaintext listeners for
      externally reachable traffic.
- [ ] **Kong admin API (`8001`) is NOT publicly exposed** — keep it loopback-bound or
      firewalled. Exposing it hands over full gateway control.
- [ ] **CORS restricted** via `APP_CORS_ALLOWED_ORIGINS` to the real origins only
      (no wildcard).
- [ ] **Kafka secured** with **SASL authentication and TLS** before any shared or
      multi-host deployment (default is loopback-bound PLAINTEXT, no auth).
- [ ] **University install key rotated** and **purged from git history**
      (e.g. `git filter-repo`), then the old key invalidated upstream.
- [ ] **Loopback-published ports bound or firewalled** — service/infra ports default to
      `127.0.0.1`; if the host is multi-tenant or the ports must move to `0.0.0.0`,
      protect them with host firewall rules.
- [ ] **Secrets in a manager** — real secrets moved into a dedicated secret store
      (Vault, AWS Secrets Manager, or the platform's secret store) rather than a plain
      `.env` for production.
- [ ] Containers confirmed running **non-root**; images **pinned** to known versions.

Full rationale: [Security](SECURITY.md) and
[Deployment → Production hardening](DEPLOYMENT.md#production-hardening).

---

## 4. Build & deploy

- [ ] **Obtain images** — either build locally or **pull from GHCR** (`ghcr.io`) if the
      Docker Build pipeline published them (see [CI/CD](CICD.md)).
- [ ] **Bring up the stack** with both Compose files:
  ```bash
  docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml up -d --build
  ```
  (Never run the stale root `docker-compose.yml`.)
- [ ] **All containers healthy** — every service reports `healthy`:
  ```bash
  docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml ps
  ```
- [ ] **Smoke test — health** (public endpoint, no key needed):
  ```bash
  curl http://127.0.0.1:8084/api/v1/actuator/health   # expect {"status":"UP"}
  ```
- [ ] **Smoke test — authenticated issuance** with the `apikey` header:
  ```bash
  curl -X POST http://127.0.0.1:8084/api/v1/student/issue \
    -H "apikey: <APP_API_KEY>" \
    -H "Content-Type: application/json" \
    -d '{"userName":"<test-username>","installKey":"<test-install-key>"}'
  # expect 202 Accepted with a correlationId and status PROCESSING
  ```
- [ ] A protected endpoint **without** a valid `apikey` returns **401** (auth is enforced).

---

## 5. Data & persistence

- [ ] Named volumes present and backed up according to policy:
  - [ ] **`kafka_data`** — Kafka (KRaft) log + metadata.
  - [ ] **`consul_data`** — Consul service catalog / KV state.
- [ ] **KRaft migration caveat understood.** Kafka runs in **KRaft mode (no ZooKeeper)**.
      Only **wipe `kafka_data`** when migrating from an **old ZooKeeper-based** volume —
      the legacy metadata is incompatible and the broker will refuse to start:
  ```bash
  docker volume rm ulht-dcs_kafka_data   # ONLY when migrating off ZooKeeper
  ```
      Do **not** wipe it on a normal restart — you would lose event history.
- [ ] Volume storage sized and monitored so Kafka logs do not fill the disk.

---

## 6. Observability

- [ ] **Prometheus targets healthy** — check `http://<host>:9090/targets`; all service
      scrape targets are `UP`.
- [ ] **Grafana reachable** and dashboards load; logged in with the configured
      `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` (default admin password changed).
- [ ] **Loki receiving logs** — Promtail is shipping and log queries return results in
      Grafana.
- [ ] Alerting / notification channels configured if required by the operating team.

---

## 7. Backup, rollback & scaling

- [ ] **Backup** — `kafka_data` and `consul_data` volumes and the host `.env` (or secret
      manager entries) are backed up on a defined schedule; restores have been tested.
- [ ] **Rollback plan** — the previous known-good image tags are recorded so you can
      redeploy them; `docker compose ... down` then `up -d` with pinned tags is
      rehearsed. Volumes are **retained** on `down` (use `down -v` only for a clean slate).
- [ ] **Scaling notes reviewed** — Kafka is a **single-node** KRaft broker/controller and
      the services are stateless behind the gateway. Horizontal scaling of the services
      is possible, but Kafka would need a proper multi-broker cluster (and SASL/TLS)
      before it is production-grade. Document the intended topology.

---

## 8. Go-live sign-off

Confirm and record who signed off and when.

- [ ] All sections above complete; no secret left at a default/placeholder value.
- [ ] Security hardening (Section 3) reviewed and approved.
- [ ] Smoke tests (Section 4) passed against the target environment.
- [ ] Backup and rollback (Section 7) verified.
- [ ] Monitoring and alerting (Section 6) confirmed operational.

| Field | Value |
| --- | --- |
| Environment | `<staging / production>` |
| Release / image tag | `<tag or commit>` |
| Deployed by | `<name>` |
| Reviewed by | `<name>` |
| Date | `<YYYY-MM-DD>` |
| Notes | `<known limitations / follow-ups>` |

---

## Related documentation

- [Security](SECURITY.md) — authentication model and remaining production steps
- [Deployment](DEPLOYMENT.md) — Compose files, images, KRaft, volumes, healthchecks
- [CI/CD](CICD.md) — building and publishing images
- [Configuration](CONFIGURATION.md) — environment variables
- [Troubleshooting](TROUBLESHOOTING.md) — common failures, walt.id `notBefore` issue
