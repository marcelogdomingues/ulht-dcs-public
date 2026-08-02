# Digital Credential System

An event-driven microservices platform that issues and verifies **W3C Verifiable
Credentials** for university students, with privacy-preserving selective disclosure.
Students authenticate against the university Student Information System (SIS) and
receive standards-compliant credentials (Educational ID, Identity, European Student
Card, and — for graduates — University Degree) through a [walt.id](https://walt.id)
identity backend.

!!! note "Showcase edition"
    This is the public edition of the project. Institution-specific integration
    values (the SIS endpoint and sample credentials) are placeholders — configure
    them via environment variables (see [Configuration](CONFIGURATION.md)).

## Technology

| Area | Stack |
|---|---|
| Runtime | Java 25 · Spring Boot 4.1 · Spring Cloud 2025.1 |
| Messaging | Apache Kafka (Confluent CP 8.3, **KRaft** — no ZooKeeper) |
| Gateway / Discovery | Kong 3.9 · Consul 1.22 |
| Observability | Prometheus · Grafana · Loki · Promtail |
| Mobile | Flutter 3.44 / Dart 3.12 (student, verifier, issuer apps) |
| Identity backend | walt.id (issuer / verifier / wallet, run separately) |

## Documentation

| Guide | What it covers |
|---|---|
| [Getting Started](GETTING_STARTED.md) | Prerequisites, `.env` setup, build, run, first credential |
| [Architecture](ARCHITECTURE.md) | Services, event flow, Kafka topics, diagrams, selective verification |
| [Configuration](CONFIGURATION.md) | Environment variables, Spring profiles, ports |
| [Security](SECURITY.md) | API-key auth, secrets, CORS, hardening |
| [API Reference](API.md) | Every REST endpoint per service, with `apikey` examples |
| [Deployment](DEPLOYMENT.md) | Compose files, KRaft Kafka, images, volumes |
| [Deployment Checklist](DEPLOYMENT_CHECKLIST.md) | Pre-deployment / infrastructure readiness |
| [Mobile Apps](MOBILE_APPS.md) | The three Flutter apps, `--dart-define` config, secure storage |
| [CI/CD](CICD.md) | GitHub Actions pipelines and Dependabot |
| [Troubleshooting](TROUBLESHOOTING.md) | Common failures and fixes |

## Reading order

1. **New here?** → [Getting Started](GETTING_STARTED.md)
2. **Understand the system** → [Architecture](ARCHITECTURE.md)
3. **Configure & run it** → [Configuration](CONFIGURATION.md) + [Deployment](DEPLOYMENT.md)
4. **Integrate a client** → [API Reference](API.md) + [Mobile Apps](MOBILE_APPS.md)
5. **Harden it** → [Security](SECURITY.md)
6. **Deploy it** → [Deployment Checklist](DEPLOYMENT_CHECKLIST.md)
7. **Stuck?** → [Troubleshooting](TROUBLESHOOTING.md)
