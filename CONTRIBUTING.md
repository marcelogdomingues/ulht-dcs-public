# Contributing to the ULHT Digital Credential System

Thanks for working on the **ULHT Digital Credential System (DCS)**. This guide is
the practical starting point for anyone building, testing, or extending the system.
It is written for the university team taking over maintenance of this thesis project.

> Documentation index: [`docs/README.md`](docs/README.md). Most links below point into
> `docs/`. Start with [Getting Started](docs/GETTING_STARTED.md) if you just want to
> run the stack; read on here if you intend to change code.

---

## Prerequisites

| Tool | Version | When you need it |
| --- | --- | --- |
| Docker Engine | Recent | Always — the whole stack runs in containers |
| Docker Compose | v2 (`docker compose`, not `docker-compose`) | Always — orchestrates the services |
| JDK (Java) | **25** | Building/testing a backend service with Maven |
| Maven | **3.9+** | Building/testing a backend service |
| Flutter / Dart | **Flutter 3.44 / Dart 3.12** | Building/running the mobile apps |

You do **not** need Java, Maven, or Flutter installed just to run the stack — the
service images are built inside the Compose build. You need the JDK/Maven toolchain
only when you build or test a service outside Docker, and Flutter only for the apps.

The backend targets **Java 25**, **Spring Boot 4.1.0**, and **Spring Cloud 2025.1.2**.
See [Getting Started](docs/GETTING_STARTED.md) and [Configuration](docs/CONFIGURATION.md)
for the full version matrix.

---

## Repository layout

The repository is a monorepo of **four independent Maven services** plus the mobile
apps, gateway config, observability config, and docs:

| Path | What it is |
| --- | --- |
| `student-service/` | Orchestrates issuance & verification (Maven service, port `8084`) |
| `lusofona-service/` | ULHT/SIGES academic data, proxies the external ULHT API (Maven service, port `8085`) |
| `credential-service/` | W3C issuance, wallet & verifier via walt.id (Maven service, port `8086`) |
| `fulfilment-service/` | Workflow / fulfilment tracking (Maven service, port `8087`) |
| `mobile-apps/` | Three Flutter apps: `student-app/`, `verifier-app/`, `issuer-app/` |
| `api-gateway/` | Kong declarative config (`kong.yml`) |
| `monitoring/` | Prometheus, Grafana, Loki, Promtail configuration |
| `docs/` | Markdown documentation set (source of truth) and static HTML site |
| `docker-compose.*.yml` | Compose files — see [Deployment](docs/DEPLOYMENT.md) |
| `.env.example` | Template for the git-ignored `.env` (secrets) |

Each of the four services is a **self-contained Maven project** with its own `pom.xml`;
there is no aggregator/parent POM. Build and test them one at a time.

For the design behind the layout — event-driven Kafka flow, topics, and the
walt.id integration — read [Architecture](docs/ARCHITECTURE.md).

---

## Building and testing a service

Each service builds and tests independently. From the repo root:

```bash
cd student-service        # or credential-service, lusofona-service, fulfilment-service
mvn -B verify             # compile, run unit tests, and package
```

`mvn -B verify` is exactly what CI runs (see [CI/CD](docs/CICD.md)). `-B` selects
batch/non-interactive mode so output is CI-friendly. To run only the tests:

```bash
mvn -B test
```

> **The backend tests do not require Docker.** There are no Testcontainers; unit and
> slice tests run against in-memory or mocked collaborators. You can run `mvn -B verify`
> on a machine with no Docker daemon.

To build all four services in one pass locally:

```bash
for svc in student-service lusofona-service credential-service fulfilment-service; do
  ( cd "$svc" && mvn -B verify ) || break
done
```

### Mobile apps

Each app under `mobile-apps/` is a standalone Flutter project:

```bash
cd mobile-apps/student-app   # or verifier-app, issuer-app
flutter pub get
flutter analyze              # must be clean — CI fails on any analyzer issue
flutter test                 # if the app has tests
```

Build-time configuration is injected with `--dart-define`; see
[Mobile Apps](docs/MOBILE_APPS.md) for the required defines and run commands.

---

## Running the whole stack locally

```bash
cp .env.example .env
# edit .env — set the REQUIRED secrets (no defaults): WALLET_PASSWORD_SECRET,
# WALLET_PASSWORD_SALT, GRAFANA_ADMIN_PASSWORD, KAFKA_UI_PASSWORD
docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml up -d --build
```

Always include **both** files: `docker-compose.microservices.yml` is the primary
stack and `docker-compose.override.yml` layers required local fixes (busybox `wget`
healthchecks, the Kafka-UI port remap, and the lusofona endpoint). Do **not** run the
bare root `docker-compose.yml` — it is stale and points at removed directories.

Verify health (public endpoints, no `apikey` needed):

```bash
curl http://127.0.0.1:8084/api/v1/actuator/health   # student-service
```

Full run/verify instructions, the walt.id dependency, and the KRaft
`kafka_data` caveat are in [Getting Started](docs/GETTING_STARTED.md) and
[Deployment](docs/DEPLOYMENT.md).

---

## API key for local testing

Every business endpoint is protected by a static API key sent in the `apikey`
header; only the health/info/prometheus/Swagger paths are public. Use your
configured `APP_API_KEY` when calling protected endpoints:

```bash
curl -X POST http://127.0.0.1:8084/api/v1/student/issue \
  -H "apikey: <APP_API_KEY>" \
  -H "Content-Type: application/json" \
  -d '{"userName":"<your-username>","installKey":"<your-install-key>"}'
```

The dev default is `APP_API_KEY=ulht-dev-local-CHANGE-ME` — fine for a local machine,
never for anything shared. See the [Security](docs/SECURITY.md) auth model and the
full [API Reference](docs/API.md).

---

## Secrets and `.env`

- **All secrets are supplied via environment variables.** Nothing sensitive is
  hard-coded in the services.
- **`.env` is git-ignored — never commit it.** Real values live only in your local,
  untracked `.env`.
- **`.env.example` is the committed template.** It documents every variable using
  placeholders. When you add a new configurable value, add it to `.env.example` too.
- Required variables with **no default** — the stack fails fast without them:
  `WALLET_PASSWORD_SECRET`, `WALLET_PASSWORD_SALT`, `GRAFANA_ADMIN_PASSWORD`,
  `KAFKA_UI_PASSWORD`.

See [Configuration](docs/CONFIGURATION.md) for the complete variable reference and
[Security](docs/SECURITY.md) for how secrets are handled.

---

## Code style & conventions

### Java (backend services)

- Follow standard **Spring Boot** conventions and idiomatic package-by-feature layout.
- Use **Lombok** for boilerplate (getters/setters/builders/constructors); prefer it
  over hand-written accessors, consistent with the existing code.
- **Keep controllers thin.** Controllers handle HTTP concerns only — validation,
  mapping, and delegation. Business logic belongs in services; persistence/integration
  in repositories and clients.
- Constructor injection over field injection.
- Public endpoints and DTOs should be documented for springdoc/OpenAPI so the Swagger
  UI stays accurate.
- Do not introduce Testcontainers or other Docker-dependent tests — keep the test
  suite runnable without a Docker daemon.

### Dart (mobile apps)

- **`flutter analyze` must be clean** — CI fails on any analyzer warning or error.
- Follow standard Dart/Flutter formatting (`dart format`).
- Configure endpoints and keys via `--dart-define`; never hard-code secrets in Dart.

---

## Branching, commits & pull requests

- **Work on feature branches**, e.g. `feature/<short-description>` or
  `fix/<short-description>`. Do not commit directly to `main`.
- **Use conventional-commit-style messages**, e.g.:
  - `feat(credential-service): add selective disclosure endpoint`
  - `fix(mobile): correct verifier deep-link handling`
  - `docs: expand deployment checklist`
  - `chore(ci): bump actions/setup-java`
- Open a **pull request** into `main`. **PRs must pass CI** before they can be merged —
  branch protection requires the CI checks to be green (see [CI/CD](docs/CICD.md)).
- Keep PRs focused and reasonably small; describe what changed and why, and note any
  new environment variables or manual steps.

---

## How CI works

Every push and pull request runs GitHub Actions:

- **Backend CI** — matrix `mvn -B verify` across the four services on Java 25.
- **Mobile CI** — `flutter pub get` + `flutter analyze` across the three apps.
- **Docker Build** — validates the Compose config and builds the four service images.
- **Dependabot** — automated dependency updates for Maven, pub, Docker, and Actions.

Run the same checks locally before pushing (`mvn -B verify`, `flutter analyze`,
`docker compose ... config`). The full pipeline reference — triggers, how to read a
failure, and required repo settings — is in [CI/CD](docs/CICD.md).

---

## Where to go next

- [Getting Started](docs/GETTING_STARTED.md) — run the stack, issue your first credential
- [Architecture](docs/ARCHITECTURE.md) — how the services fit together
- [Configuration](docs/CONFIGURATION.md) — every environment variable
- [CI/CD](docs/CICD.md) — the pipelines in detail
- [Deployment](docs/DEPLOYMENT.md) & [Deployment Checklist](docs/DEPLOYMENT_CHECKLIST.md)
- [Security](docs/SECURITY.md) — auth model and hardening
- [API Reference](docs/API.md) · [Mobile Apps](docs/MOBILE_APPS.md) · [Troubleshooting](docs/TROUBLESHOOTING.md)
