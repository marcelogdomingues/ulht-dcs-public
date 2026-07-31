# 5. Shared API-key authentication enforced at every service

## Status

Accepted

## Context

The DCS is a set of loopback-bound microservices (`:8084`–`:8087`) fronted by an
**optional** Kong gateway that is currently partial/aspirational — clients and
the mobile apps often call the services directly by port. The core security
principle is that **the gateway is untrusted middleware**: authentication must
never be delegated solely to the edge, because bypassing or removing Kong must
not weaken authentication.

At this stage the platform is a **hardened development / academic deployment**.
It needs a simple, stateless, machine-to-machine auth model that is easy to bring
up locally, works identically whether a request arrives through Kong or straight
at a service port, and is resistant to obvious attacks — while leaving a clear
path to stronger, per-caller authentication for production.

## Decision

Authenticate every caller with a single shared **API key**, enforced
**independently at every service** (defense-in-depth) via Spring Security and a
custom `ApiKeyAuthFilter`.

- The key travels in the `apikey` request header (exact, lowercase), configured
  through the `APP_API_KEY` environment variable (`app.security.api-key`).
- Comparison uses **constant-time** `java.security.MessageDigest.isEqual` so an
  attacker cannot infer the key byte-by-byte from response timing.
- On success the filter sets a `ROLE_SERVICE` authority in a **stateless**
  `SecurityContext` (no session, no cookie, CSRF disabled). On no/invalid key the
  request stays anonymous and Spring Security's entry point returns
  `401 Unauthorized` before any business logic runs.
- A small set of paths are public on every service (`/actuator/health`,
  `/actuator/info`, `/actuator/prometheus`, `swagger-ui`, `v3/api-docs`).
- Kong forwards the key rather than stripping it (`hide_credentials: false`)
  precisely because each backend validates the **same** `apikey` header.

## Consequences

### Positive

- **Defense in depth** — every service enforces the key itself, so dropping or
  bypassing Kong does not weaken authentication; the gateway is not a trust
  anchor.
- **Simple and stateless** — no sessions, cookies, or JWT plumbing on the
  machine-to-machine path; trivial local bring-up.
- **Timing-attack resistant** — constant-time comparison closes the obvious
  side-channel.
- **Consistent contract** — identical filter and public-path list across all four
  services.

### Negative / trade-offs

- **Shared credential, no per-caller identity** — all callers use one key; there
  is no per-consumer identity or fine-grained revocation.
- **Coarse authorization** — a single `ROLE_SERVICE` authority; no scopes or
  least-privilege between callers.
- **Interim model only** — a shared static key is not sufficient for shared or
  internet-facing deployment; it must be rotated and never shipped as the dev
  default (`ulht-dev-local-CHANGE-ME`).

## Path to production

Issue **distinct credentials per consumer** (Kong supports multiple
`keyauth_credentials`), move toward **OAuth2 / OIDC** for real caller identity
and scopes, add **mutual TLS (mTLS)** between services, terminate **HTTPS/TLS**
everywhere, and manage secrets in a dedicated secret manager. These steps are
enumerated in [Security — remaining manual / production steps](../SECURITY.md).

## Alternatives considered

- **Gateway-only auth (trust the edge)** — rejected outright: a direct call to a
  service port would bypass authentication entirely.
- **OAuth2 / JWT now** — the right long-term direction, but heavier to stand up
  for an academic deployment; deferred to the production path above.

See [Security — authentication](../SECURITY.md).
