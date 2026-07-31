# Security

This document describes the security model of the ULHT Digital Credential System (DCS). The stack is a **hardened development / academic deployment**: sensible defaults are in place, but a number of manual steps (listed at the end) are required before it is safe to expose beyond `localhost`. This document is honest about that distinction.

See also: [Configuration](CONFIGURATION.md) · [Architecture](ARCHITECTURE.md) · [Getting Started](GETTING_STARTED.md) · [Deployment](DEPLOYMENT.md) · [Troubleshooting](TROUBLESHOOTING.md) · [Project README](index.md)

---

## Authentication model

Every service authenticates callers with a static **API key** enforced by Spring Security.

| Aspect | Detail |
| --- | --- |
| Header name | `apikey` (exact, lowercase) |
| Configured via | `APP_API_KEY` environment variable |
| Comparison | Constant-time comparison to prevent timing attacks |
| Granted authority | `ROLE_SERVICE` on successful authentication |
| Failure response | `401 Unauthorized` (no key, wrong key, or malformed key) |

A request that presents a valid `apikey` header is authenticated as a service principal holding `ROLE_SERVICE`; all protected endpoints require that role.

### Public vs protected paths

The following paths are **public** (no `apikey` header required) on every service:

- `GET /api/v1/actuator/health`
- `GET /api/v1/actuator/info`
- `GET /api/v1/actuator/prometheus`
- `GET /api/v1/swagger-ui/index.html` (Swagger UI)
- `GET /api/v1/v3/api-docs` (OpenAPI document)

**Every other endpoint is protected** and returns `401 Unauthorized` unless a valid `apikey` header is supplied.

### 401 behavior

A protected request without a valid `apikey` header is rejected with `401 Unauthorized` before any business logic runs. No credential data is returned in the error path.

### Defense in depth

Each service enforces the `apikey` requirement **independently**. This holds whether the service is reached directly on its own port (e.g. `http://localhost:8084`) or through the Kong API gateway (`:8000`). Because the gateway is treated as untrusted middleware, dropping or bypassing Kong does not weaken authentication — the service behind it still demands the key. This is deliberate: authentication is never delegated solely to the edge.

---

## Secrets management

- **All secrets are supplied through environment variables.** No secret is hard-coded in the services.
- **`.env` is git-ignored.** Real secrets live only in the local, untracked `.env` file.
- **`.env.example` is the committed template.** It documents required variables using placeholders, never real values.
- **`WALLET_PASSWORD_SECRET` and `WALLET_PASSWORD_SALT` are required and have no defaults.** The stack will not derive wallet passwords without them being explicitly set.
- **Wallet passwords are derived per student** from the combination of the secret and salt, so each student's wallet has a distinct, non-reversible password that is never stored in plaintext or committed.

> The dev default `APP_API_KEY=ulht-dev-local-CHANGE-ME` exists only to make local bring-up frictionless. It must never be shipped or reused outside a local machine. See the manual steps below.

---

## Transport & network

| Control | Detail |
| --- | --- |
| Port binding | Service and infrastructure ports are bound to `127.0.0.1` (loopback), not `0.0.0.0` |
| Kong admin API | Bound to loopback only — not reachable from the network |
| CORS | Origin allow-list configured via `APP_CORS_ALLOWED_ORIGINS` (no wildcard in shared use) |
| Container users | Containers run as **non-root** |
| Images | Container images are **pinned** to specific versions/digests |

Because ports are loopback-bound by default, the stack is not exposed to the LAN out of the box. Enabling remote access requires the production steps below (TLS, real CORS origins, real secrets).

---

## Kafka

- Runs in **KRaft** mode (no ZooKeeper).
- Internal broker traffic uses **PLAINTEXT**.
- The broker is **loopback-bound**, so it is not reachable off-host in the default configuration.

> Kafka currently has no authentication or encryption on the wire. **Add SASL authentication and TLS before any shared or multi-host deployment.**

---

## Monitoring authentication

- **Grafana** credentials are provided via environment variables — no defaults are committed.
- **Kafka-UI** credentials are provided via environment variables — no defaults are committed.

You must set these explicitly; the repository ships no baked-in monitoring passwords.

---

## Remaining manual / production steps

The stack is hardened for development but is **not production-ready** until the following are completed:

1. **Rotate the real ULHT install key** and **purge it from git history** (e.g. with `git filter-repo`), then force-push and invalidate the old key upstream.
2. **Enable HTTPS/TLS everywhere** for production — terminate TLS at Kong and/or per service, and disable plaintext listeners.
3. **Add SASL authentication and TLS to Kafka** before any shared deployment.
4. **Move real secrets into a dedicated secret manager** (e.g. Vault, AWS Secrets Manager, or the platform's secret store) instead of a local `.env` file.
5. **Restrict CORS** (`APP_CORS_ALLOWED_ORIGINS`) to the real front-end origins only — never `*`.
6. **Never ship the dev API key.** Replace `APP_API_KEY=ulht-dev-local-CHANGE-ME` with a strong, unique key generated per environment.

---

## Reporting

If you discover a security issue, please report it privately to the maintainers rather than opening a public issue. Include reproduction steps and the affected component, and allow time for a fix before any public disclosure.
