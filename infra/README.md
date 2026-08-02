# Infrastructure configs

Configuration for the supporting infrastructure. These files are mounted into the
containers by the Compose stacks in [../compose](../compose); nothing here is built.

| Path | Configures |
|---|---|
| [`docker/postgres`](docker/postgres) | Postgres init script (creates the `credential` database) |
| [`docker/keycloak`](docker/keycloak) | Keycloak realm export (OAuth2/OIDC overlay) |
| [`docker/waltid`](docker/waltid) | walt.id issuer / verifier / wallet API configs |
| [`monitoring`](monitoring) | Prometheus, Grafana (dashboards + provisioning), Loki, Promtail |
| [`api-gateway`](api-gateway) | Kong declarative config (`kong.yml`) |
| [`kong-ui`](kong-ui) | Static Kong admin console (served by nginx) |

See [../docs/DEPLOYMENT.md](../docs/DEPLOYMENT.md) for how these are wired into the stack.
