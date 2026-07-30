# ULHT DCS — Documentation

Documentation for the **ULHT Digital Credential System**. Start with the root
[`../README.md`](../README.md) for the project overview and quick start, then use the
guides below.

## Guides (Markdown)

| Document | What it covers |
|---|---|
| [Getting Started](GETTING_STARTED.md) | Prerequisites, `.env` setup, build, run, first credential, mobile apps |
| [Architecture](ARCHITECTURE.md) | Services, event-driven Kafka flow, topics, diagrams, network topology, selective verification |
| [Configuration](CONFIGURATION.md) | Environment variables, Spring profiles, ports, per-service settings |
| [Security](SECURITY.md) | API-key auth, secrets, CORS, network hardening, remaining production steps |
| [API Reference](API.md) | Every REST endpoint per service, with `apikey` examples |
| [Deployment](DEPLOYMENT.md) | Compose files, KRaft Kafka, images, volumes, healthchecks, production notes |
| [Mobile Apps](MOBILE_APPS.md) | The three Flutter apps, `--dart-define` config, secure storage |
| [Troubleshooting](TROUBLESHOOTING.md) | Common failures and fixes |
| [CI/CD](CICD.md) | GitHub Actions pipelines (backend, mobile, Docker build), Dependabot, required settings, running checks locally |
| [Deployment checklist](DEPLOYMENT_CHECKLIST.md) | Actionable pre-deployment / infrastructure-readiness checklist with go-live sign-off |

## Reading order

1. **New here?** → [Getting Started](GETTING_STARTED.md)
2. **Understand the system** → [Architecture](ARCHITECTURE.md)
3. **Configure & run it** → [Configuration](CONFIGURATION.md) + [Deployment](DEPLOYMENT.md)
4. **Integrate a client** → [API Reference](API.md) + [Mobile Apps](MOBILE_APPS.md)
5. **Harden it** → [Security](SECURITY.md)
6. **Contribute code** → [Contributing](../CONTRIBUTING.md) + [CI/CD](CICD.md)
7. **Deploy it** → [Deployment checklist](DEPLOYMENT_CHECKLIST.md)
8. **Stuck?** → [Troubleshooting](TROUBLESHOOTING.md)

## HTML documentation site

This directory also contains a static, browsable HTML site (dark mode, search,
interactive diagrams):

```bash
cd docs
./serve.sh            # or: python3 -m http.server 8000
# then open http://localhost:8000
```

Pages: `index.html` (home), `getting-started.html`, `architecture.html`,
`api-reference.html`, `card-demo.html`. The Markdown guides above are the source of
truth and are kept current with the codebase; regenerate or cross-check the HTML site
against them when it drifts.

## Diagrams & assets

- `CompleteArchitecture.png`, `SimpleArchitecture.png` — architecture diagrams
- `search-index.json`, `js/`, `styles/` — assets for the HTML site
