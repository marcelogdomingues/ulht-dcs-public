# Architecture Decision Records (ADRs)

This directory records the significant architectural decisions made for the
**Digital Credential System (DCS)** — the event-driven microservices
platform that issues, stores, and selectively verifies **W3C Verifiable
Credentials** for university students (see [Architecture](../ARCHITECTURE.md)).

## What is an ADR?

An **Architecture Decision Record** captures a single, significant decision:
the context that forced it, the decision itself, and the consequences (good and
bad) that flow from it. ADRs are **immutable** — once accepted, a record is not
rewritten. If a later decision changes course, a **new** ADR is written that
supersedes the old one, and the old one is marked accordingly. This gives future
contributors the *why* behind the system, not just the *what*.

These records follow the [MADR](https://adr.github.io/madr/) (Markdown Any
Decision Records) style: a numbered title and the sections **Status**,
**Context**, **Decision**, **Consequences**, and optionally **Alternatives
considered**.

## Index

| # | Decision | Status |
| --- | --- | --- |
| [0001](0001-event-driven-microservices.md) | Event-driven microservices over a synchronous monolith | Accepted |
| [0002](0002-kafka-kraft-over-zookeeper.md) | Apache Kafka in KRaft mode (no ZooKeeper) as the event backbone | Accepted |
| [0003](0003-waltid-for-vc-issuance.md) | walt.id as the identity backend for W3C VC issuance & verification | Accepted |
| [0004](0004-selective-disclosure-verification.md) | Selective / least-disclosure verification for privacy & GDPR | Accepted |
| [0005](0005-api-key-authentication.md) | Shared API-key authentication enforced at every service | Accepted |
| [0006](0006-spring-boot-4-java-25.md) | Spring Boot 4.1 on Java 25 as the runtime platform | Accepted |
| [0007](0007-credential-revocation.md) | Credential revocation via a status registry and W3C Bitstring Status List | Accepted |
| [0008](0008-auth-and-key-management-roadmap.md) | Authentication & key-management roadmap (OIDC, KMS/Vault, SD-JWT, mobile login) | Proposed |

## How to add a new ADR

1. Copy the number of the next free integer (zero-padded to four digits, e.g.
   `0007`).
2. Create `docs/adr/NNNN-short-kebab-title.md` using the MADR structure below.
3. Set the **Status** to `Proposed`, then `Accepted` once agreed (or
   `Superseded by [NNNN](NNNN-...md)` when replaced).
4. Add a row to the **Index** table above.
5. Wire the new file into the site navigation (`mkdocs.yml`) — done separately.

### Template

```markdown
# N. <short decision title>

## Status

Accepted

## Context

<the forces at play: requirements, constraints, the problem being solved>

## Decision

<the decision that was made, stated plainly>

## Consequences

### Positive

- <what improves as a result>

### Negative / trade-offs

- <what we accept or give up>

## Alternatives considered

- <option> — <why it was not chosen>
```

## Related documentation

- [Architecture](../ARCHITECTURE.md)
- [Security](../SECURITY.md)
- [Configuration](../CONFIGURATION.md)
- [Deployment](../DEPLOYMENT.md)
