# Compose stacks

Docker Compose files for running the system. Use Compose **v2** (`docker compose`).
Paths inside these files are relative to the repository root (`context: ..`), so run
them **from the repo root**.

| File | Purpose |
|---|---|
| `microservices.yml` | **Primary** — Kafka, Consul, Kong, Kafka-UI, Kong-UI + the four services |
| `override.yml` | Local fixes layered on the primary (git-ignored; you create it) |
| `infrastructure.yml` | Observability + infra (adds Prometheus / Grafana / Loki / Promtail) |
| `demo.yml` | Self-contained demo — mock walt.id + SIS, no `.env` needed |
| `keycloak.yml` | OAuth2 / OIDC overlay (Keycloak) |
| `waltid.yml` | Real walt.id issuer / verifier / wallet backend |

Common commands (or use the [Makefile](../Makefile) targets):

```bash
# One-command demo               (make demo)
docker compose -f compose/demo.yml up -d --build

# Full stack                     (make up)
docker compose -f compose/microservices.yml -f compose/override.yml up -d --build
```

Full guide: [../docs/DEPLOYMENT.md](../docs/DEPLOYMENT.md).
