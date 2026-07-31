# 1. Event-driven microservices over a synchronous monolith

## Status

Accepted

## Context

Issuing a credential in the DCS is not a single, fast operation. A student login
triggers a chain of work: `lusofona-service` calls the university **SIS/SIGES**
to fetch the full academic record, `credential-service` builds and signs one or
more **W3C Verifiable Credentials** through the external **walt.id** stack and
offers them to the student's wallet, and `fulfilment-service` tracks the
lifecycle. These steps depend on external systems (the SIS and walt.id) that are
slow, sometimes unavailable, and not under our control. Doing all of this behind
one synchronous HTTP request would tie the caller up for seconds, couple the
services tightly, and let one failing dependency fail the whole request.

We also need clear domain boundaries: SIS integration, credential minting,
workflow tracking, and the public entry point are distinct responsibilities with
different failure modes, scaling needs, and rates of change.

## Decision

Build the DCS as **four Spring Boot microservices** — `student-service` (:8084,
entry point), `lusofona-service` (:8085, SIS integration), `credential-service`
(:8086, walt.id issuance/verification/wallet), and `fulfilment-service` (:8087,
workflow tracking) — communicating **asynchronously over an event bus** for the
credential pipeline.

The public API follows an **async 202-style pattern**. `POST /student/issue`
generates a random `correlationId` (UUID), publishes it to the pipeline, and
returns immediately with `status = PROCESSING` plus `monitorAt` / `credentialsAt`
URLs. The client then **polls** `GET /student/status/{id}` (which returns `202`
until the workflow is `COMPLETED`) or subscribes to **Server-Sent Events**. The
same `correlationId` is threaded through every hop as the message key and
`X-Correlation-ID` header, giving end-to-end traceability across services.

A small amount of **synchronous** communication remains where it fits: a Feign
REST call from `student-service` to `fulfilment-service` to initialise and poll
workflow status.

## Consequences

### Positive

- **Responsiveness** — the HTTP call returns instantly; slow SIS and walt.id work
  runs in the background.
- **Decoupling & independent failure** — services fail independently; a poison
  message or a down dependency does not cascade into an unrelated request.
- **Traceability** — one `correlationId` per workflow makes multi-service flows
  observable end to end.
- **Independent evolution & deployment** — each bounded context can change and
  scale on its own.

### Negative / trade-offs

- **More moving parts** — an event bus, consumer groups, and multiple services
  add operational and cognitive overhead versus one deployable unit.
- **Eventual consistency** — clients must poll or subscribe; there is no single
  synchronous "done" response.
- **Distributed debugging** — reasoning about a request means following events
  across services (mitigated by the correlation id and observability stack).

## Alternatives considered

- **Synchronous monolith** — simplest to build and debug, but a single slow SIS
  or walt.id call blocks the caller and a single failure fails everything;
  rejected for poor resilience and coupling.
- **Synchronous microservices (REST chains)** — keeps boundaries but reintroduces
  request-time coupling and cascading failures across the SIS/walt.id hops;
  rejected in favour of async events for the pipeline.

See [Architecture §5 (issuance flow)](../ARCHITECTURE.md) and
[§13 (resilience patterns)](../ARCHITECTURE.md).
