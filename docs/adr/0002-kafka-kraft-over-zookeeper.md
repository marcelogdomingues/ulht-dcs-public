# 2. Apache Kafka in KRaft mode (no ZooKeeper) as the event backbone

## Status

Accepted

## Context

[ADR 0001](0001-event-driven-microservices.md) commits the DCS to
event-driven microservices, which requires a durable, ordered, replayable event
bus. The credential pipeline emits a defined set of topics
(`student.login.requested`, `credential.requests`, `credential.progress/completed/error`,
`verification.*`, `wallet.*`) consumed by dedicated consumer groups
(`sis-service-group`, `credential-service-*-group`,
`fulfilment-service-workflow-group`).

Two properties matter. First, **per-workflow ordering**: all events for one
credential workflow must be processed in order, which Kafka gives us by keying
every message on the `correlationId` so it lands on a single partition. Second,
**operational simplicity** for a single-node academic/dev deployment — we did not
want to run and babysit a separate coordination service.

Historically Kafka required an external **ZooKeeper** ensemble for cluster
metadata and controller election, adding a second distributed system to operate.
Modern Kafka replaces this with **KRaft** (Kafka Raft), where the broker itself
hosts the controller quorum.

## Decision

Use **Apache Kafka as the event backbone**, running the Confluent
`confluentinc/cp-kafka:8.3.0` image in **KRaft mode — no ZooKeeper**.

In this single-node deployment the one broker plays **both** roles:
`KAFKA_PROCESS_ROLES = broker,controller` with
`KAFKA_CONTROLLER_QUORUM_VOTERS = 1@kafka:29093` and a fixed `CLUSTER_ID`.
Replication factor is `1` (single node), `KAFKA_AUTO_CREATE_TOPICS_ENABLE = true`
so topics appear on first use, and default log retention is 7 days. Listeners
expose `CLIENT://kafka:9092` in-cluster and `EXTERNAL://localhost:29092` for
host/dev access, with `CONTROLLER` on `:29093`. Messages are **JSON** (Spring
Kafka `JsonSerializer`/`JsonDeserializer`) with `JsonDeserializer` restricted to
trusted packages (`com.example.dcs.*,java.util,java.lang`), `ErrorHandlingDeserializer`
wrapping the deserializer, manual acknowledgement, and retry + `.DLT`
dead-letter topics.

## Consequences

### Positive

- **One fewer system to run** — KRaft removes the ZooKeeper ensemble; the broker
  self-manages metadata and controller election.
- **Ordered, replayable streams** — keying on `correlationId` guarantees
  in-order, per-workflow processing; `earliest` offset reset allows replay.
- **Faster start-up and simpler compose** — a single container covers broker and
  controller for the dev/academic footprint.
- **Standard, well-supported image** — pinned Confluent `cp-kafka:8.3.0`.

### Negative / trade-offs

- **Single point of failure in dev** — one broker, replication factor 1: no
  redundancy; a broker loss means downtime until restart. Acceptable for the
  academic deployment, not for production.
- **No wire security yet** — the broker runs **PLAINTEXT** and is loopback-bound;
  SASL authentication, TLS, and per-topic ACLs are required before any shared
  deployment (see [Security](../SECURITY.md)).
- **KRaft operational familiarity** — KRaft is newer than the ZooKeeper model
  many operators know; recovery procedures differ.

## Alternatives considered

- **Kafka with ZooKeeper** — the legacy topology; rejected because it adds a
  second distributed system to operate for no benefit at this scale, and
  ZooKeeper mode is being retired upstream.
- **RabbitMQ / other brokers** — capable message brokers, but Kafka's partitioned
  log with keyed ordering, retention, and replay fits the correlation-id workflow
  model best.

See [Architecture §7 (Kafka topology)](../ARCHITECTURE.md).
