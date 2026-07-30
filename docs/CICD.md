# CI/CD

Continuous integration and delivery for the **ULHT Digital Credential System (DCS)**,
implemented with **GitHub Actions**. This document explains what each pipeline does,
what triggers it, how to read a failure, the repository settings and secrets it needs,
and how to reproduce every check locally.

> See also: [Contributing](../CONTRIBUTING.md) · [Deployment](DEPLOYMENT.md) · [Deployment Checklist](DEPLOYMENT_CHECKLIST.md) · [Security](SECURITY.md) · [Configuration](CONFIGURATION.md) · [Getting Started](GETTING_STARTED.md) · [Project README](../README.md)

---

## Overview

| Workflow | Purpose | Typical triggers |
| --- | --- | --- |
| **Backend CI** | `mvn -B verify` (matrix) over the four Maven services on Java 25 | push, pull request |
| **Mobile CI** | `flutter pub get` + `flutter analyze` over the three Flutter apps | push, pull request |
| **Docker Build** | Validate Compose config and build the four service images; optionally push to GHCR | push, pull request, tags |
| **Dependabot** | Automated dependency update PRs (maven, pub, docker, github-actions) | scheduled by GitHub |

The workflow definitions live in `.github/workflows/` and the Dependabot config in
`.github/dependabot.yml`. (That directory is owned by the CI setup task; this document
describes the intended behaviour and how to work with it.)

> **The backend tests do not need Docker.** There are no Testcontainers in the suite,
> so Backend CI runs `mvn -B verify` on a plain JDK 25 runner with no Docker daemon.

---

## Backend CI

**What it does.** Runs a build matrix — one job per service — over:

- `student-service`
- `lusofona-service`
- `credential-service`
- `fulfilment-service`

Each matrix job checks out the repo, sets up **JDK 25** (`actions/setup-java`,
Temurin) with Maven dependency caching, changes into the service directory, and runs:

```bash
mvn -B verify
```

This compiles the code, runs the unit/slice tests, and packages the JAR. Because the
four services are independent Maven projects (no parent/aggregator POM), each is built
in isolation; a failure in one service does not mask the others.

**Triggers.** Pushes and pull requests. Path filters may scope it to backend changes,
but treat it as running on every PR that touches service code.

**What it checks.** Compilation on Java 25, passing tests, and a successful `package`
phase for every service.

---

## Mobile CI

**What it does.** Runs a matrix over the three Flutter apps:

- `mobile-apps/student-app`
- `mobile-apps/verifier-app`
- `mobile-apps/issuer-app`

Each job sets up Flutter (`subosito/flutter-action` or equivalent, Flutter 3.44 /
Dart 3.12), then in each app directory runs:

```bash
flutter pub get
flutter analyze
```

**Triggers.** Pushes and pull requests.

**What it checks.** Dependencies resolve cleanly and the Dart analyzer is **clean** —
any analyzer warning or error fails the job. Fix analyzer findings rather than
suppressing them.

---

## Docker Build

**What it does.**

1. **Validates the Compose configuration** — `docker compose ... config` parses and
   resolves the primary and override files, catching syntax and interpolation errors
   before anything is built.
2. **Builds the four service images** using the multi-stage Dockerfiles
   (`maven:3.9-eclipse-temurin-25` build stage → `eclipse-temurin:25-jre-alpine`
   runtime, non-root). See [Deployment](DEPLOYMENT.md#dockerfile-build).
3. **Optionally pushes to GHCR** — on the **default branch** and on **version tags**,
   the built images are pushed to the **GitHub Container Registry** (`ghcr.io`).
   On pull requests from feature branches the images are built but **not** pushed.

**Triggers.** Pushes, pull requests, and tags. The push-to-registry step is gated on
`github.ref` being the default branch or a tag.

**What it checks.** That the Compose files are valid and that every service image
builds end-to-end.

---

## Dependabot

`.github/dependabot.yml` enables automated dependency-update pull requests across four
ecosystems:

| Ecosystem | Covers |
| --- | --- |
| `maven` | Backend service dependencies (one entry per service directory) |
| `pub` | Flutter/Dart packages in each mobile app |
| `docker` | Base images in the service Dockerfiles |
| `github-actions` | Action versions used by the workflows |

Dependabot opens PRs on its schedule. Each PR runs the normal CI checks, so you can
merge an update only once Backend/Mobile/Docker CI pass against it. Review changelogs
for majors before merging.

---

## Reading a failure

1. Open the **Actions** tab (or the **Checks** section of the PR) and click the red job.
2. Expand the failed step; the last lines usually name the cause.

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `mvn -B verify` compile error | Java 25 syntax/API issue or missing dependency | Reproduce locally with `mvn -B verify` in that service; fix and re-push |
| Test failures in Backend CI | A unit/slice test regressed | Run `mvn -B test` locally; no Docker needed |
| `flutter analyze` non-zero exit | Analyzer warning/error introduced | Run `flutter analyze` locally; resolve, don't suppress |
| `docker compose ... config` error | Broken Compose YAML or unresolved `${VAR}` | Validate locally (see below); check `.env` interpolation |
| Image build fails | Dockerfile/build-stage issue | Build the image locally to reproduce |
| GHCR push denied | Missing package-write permission or packages disabled | See required settings below |

Re-run a single failed job from the Actions UI ("Re-run failed jobs") once fixed;
avoid re-running the whole matrix unnecessarily.

---

## Required repository settings & secrets

| Item | Needed for | Notes |
| --- | --- | --- |
| `GITHUB_TOKEN` | All workflows, GHCR push | **Automatic** — GitHub injects it per run; no manual secret to create |
| GHCR package writes | Docker Build push step | Grant Actions **`packages: write`** (workflow `permissions:` block and/or repo → Settings → Actions → Workflow permissions). The first push creates the package; then set its visibility |
| Branch protection | Enforcing green CI on `main` | Settings → Branches → protect `main` → **require status checks to pass** (Backend CI, Mobile CI, Docker Build) before merge |
| Dependabot | Update PRs | Enabled by committing `.github/dependabot.yml`; ensure Dependabot is allowed in repo/org settings |

No third-party secrets are required for the core pipeline — GHCR uses the built-in
`GITHUB_TOKEN`. If you later push to an external registry, add its credentials as
**encrypted repository secrets** (Settings → Secrets and variables → Actions) and never
place them in workflow YAML. See [Security](SECURITY.md).

---

## Run the same checks locally

Reproduce each CI gate before pushing:

```bash
# Backend — per service (no Docker required)
cd student-service && mvn -B verify        # repeat for the other three services

# Mobile — per app
cd mobile-apps/student-app && flutter pub get && flutter analyze

# Docker — validate compose config, then build images
docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml config
docker compose -f docker-compose.microservices.yml -f docker-compose.override.yml build
```

Matching CI locally is the fastest way to keep PRs green. For the day-to-day workflow
and conventions, see [Contributing](../CONTRIBUTING.md).

---

## Related documentation

- [Contributing](../CONTRIBUTING.md) — build, test, branch & PR conventions
- [Deployment](DEPLOYMENT.md) — Compose files, images, KRaft, healthchecks
- [Deployment Checklist](DEPLOYMENT_CHECKLIST.md) — pre-deployment readiness
- [Security](SECURITY.md) — secrets handling and hardening
- [Configuration](CONFIGURATION.md) — environment variables
