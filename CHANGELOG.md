# Changelog

All notable changes to the **ULHT Digital Credential System** will be documented
in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

- No unreleased changes yet.

## [1.0.0] - 2026-07-31

First public release of the ULHT Digital Credential System — an event-driven
microservices platform for issuing and verifying W3C Verifiable Credentials.

### Added

- **Verifiable Credentials**: End-to-end issuance and verification of W3C
  Verifiable Credentials using the OID4VCI and OID4VP protocols, powered by
  [walt.id](https://walt.id).
- **Microservices**: Four Spring Boot 4.1 services running on Java 25 —
  the **student** service, the **lusofona/SIS** (Student Information System)
  service, the **credential** service, and the **fulfilment** service.
- **Event-driven architecture**: Asynchronous inter-service communication over
  **Apache Kafka** running in **KRaft mode** (no ZooKeeper dependency).
- **API gateway & service discovery**: **Kong** as the API gateway with
  **Consul** for service discovery.
- **Authentication**: API-key based authentication enforced at the gateway.
- **Observability**: Metrics, dashboards and logs via
  **Prometheus**, **Grafana**, and **Loki**.
- **Mobile applications**: Three **Flutter** apps — **student**, **verifier**,
  and **issuer**.
- **CI/CD**: Full continuous integration and delivery pipelines with
  **GitHub Actions**, plus automated dependency updates via **Dependabot**.
- **Documentation**: A **MkDocs Material** documentation site covering
  architecture, setup, and the security model.

[Unreleased]: https://github.com/marcelogdomingues/ulht-dcs-public/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/marcelogdomingues/ulht-dcs-public/releases/tag/v1.0.0
