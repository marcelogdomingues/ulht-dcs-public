# Services

Backend Maven modules. Each service is standalone (its own `pom.xml`, `Dockerfile`,
and tests) — there is no parent aggregator. The shared `dcs-commons` module must be
installed first (`mvn -q -f services/dcs-commons/pom.xml -DskipTests install`); the
service Dockerfiles do this automatically.

| Module | Port | Role |
|---|---|---|
| [student-service](student-service/) | 8084 | Entry point — starts issuance, proxies status/result |
| [sis-service](sis-service/) | 8085 | University SIS (SIGES) integration |
| [credential-service](credential-service/) | 8086 | W3C VC issuance / wallet / verifier (walt.id) |
| [fulfilment-service](fulfilment-service/) | 8087 | Workflow tracking (REST + SSE) |
| [dcs-commons](dcs-commons/) | — | Shared auto-configured API-key / JWT security |

Each module's README documents its endpoints, Kafka topics, and configuration. For the
architecture and end-to-end event flow, see [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md).
