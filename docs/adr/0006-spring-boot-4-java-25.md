# 6. Spring Boot 4.1 on Java 25 as the runtime platform

## Status

Accepted

## Context

All four DCS microservices share a common runtime platform, so the choice of
framework and JDK is a cross-cutting decision. The system is I/O-heavy: services
spend most of their time waiting on Kafka, the SIS, walt.id, and inter-service
Feign calls. That workload benefits directly from **virtual threads** (Project
Loom), which let a large number of blocking calls be served cheaply without a
reactive rewrite.

We also want to start the project on a **current, long-supported platform**
rather than an older baseline we would soon have to migrate off. Java 25 is the
latest LTS-era release, and Spring Boot 4.1 is the matching current-generation
framework with first-class virtual-thread support and an up-to-date dependency
set (Spring Cloud 2025.1.2, resilience4j-spring-boot4, springdoc-openapi 3.x).

## Decision

Standardise every service on **Spring Boot 4.1.0** running on **Java 25**
(`<java.version>25</java.version>`, `spring-boot-starter-parent:4.1.0`), built
with Maven. Companion versions: **Spring Cloud 2025.1.2** (OpenFeign, Consul),
**resilience4j-spring-boot4 2.4.0**, and **springdoc-openapi 3.0.3**.

## Migration notes (Spring Boot 3 → 4)

Adopting Boot 4 required accounting for platform changes:

- **Jackson 3 is the default** JSON stack in Spring Boot 4 (packages/artifacts
  moved from `com.fasterxml.jackson` toward `tools.jackson`). Serialization
  config and any explicit Jackson dependencies were reviewed against the Boot 4
  default.
- **`KafkaProperties` moved** — Kafka auto-configuration (including
  `KafkaProperties`) was extracted out of `spring-boot-autoconfigure` into its own
  **`spring-boot-kafka`** module in Boot 4, which each service now declares
  explicitly.
- **Aligned Spring ecosystem** — Spring Cloud, resilience4j (the
  `-spring-boot4` variant), and springdoc were moved to their Boot 4–compatible
  releases.
- **Virtual threads** — the I/O-bound listeners and REST clients are a natural fit
  for Loom virtual threads on Java 25.

## Consequences

### Positive

- **Current, long-supported base** — starting on Java 25 / Boot 4.1 maximises the
  supported lifetime before the next forced migration.
- **Virtual threads** — cheap concurrency for the blocking Kafka/SIS/walt.id/Feign
  workload without a reactive rewrite.
- **Modern, aligned dependencies** — Jackson 3, Spring Cloud 2025.1.2,
  resilience4j-spring-boot4, springdoc 3.x kept consistent across all services.

### Negative / trade-offs

- **Bleeding-edge exposure** — being on the newest platform means a smaller pool
  of community answers and third-party libraries fully validated against Boot 4 /
  Java 25, and a higher chance of hitting early-adopter issues.
- **Migration cost** — the Jackson 3 default and the `KafkaProperties`/module move
  required deliberate changes and testing rather than a drop-in upgrade.
- **Toolchain floor** — every build and deployment environment must provide a
  Java 25 toolchain.

## Alternatives considered

- **Spring Boot 3.x on Java 21** — a very well-trodden, stable combination, but it
  starts the project on an older baseline with a nearer migration horizon;
  rejected in favour of the current platform.
- **Reactive stack (WebFlux) for concurrency** — could handle the I/O load, but
  imposes a reactive programming model project-wide; virtual threads deliver the
  concurrency benefit while keeping straightforward blocking code.

See [Architecture §16 (technology stack)](../ARCHITECTURE.md).
