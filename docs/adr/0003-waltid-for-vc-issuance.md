# 3. walt.id as the identity backend for W3C VC issuance and verification

## Status

Accepted

## Context

The core purpose of the DCS is to issue **W3C Verifiable Credentials** that are
cryptographically signed, tamper-proof, held in a student-controlled wallet, and
verifiable using open standards — **OID4VCI** (OpenID for Verifiable Credential
Issuance) for issuance and **OID4VP** (OpenID for Verifiable Presentations) for
verification. Delivering this ourselves would mean implementing issuer DIDs and
key management, credential signing, an SD-JWT/JWT-VC pipeline, a wallet, and a
standards-compliant verifier with policy evaluation — a large, security-critical
surface where subtle cryptographic mistakes are catastrophic and easy to make.

We want the platform team to focus on the **integration and workflow** logic
(SIS data, templates, the Kafka pipeline) rather than on building and maintaining
cryptographic identity infrastructure.

## Decision

Delegate all cryptographic credential work to **walt.id**, an open-source
identity stack, run as an external dependency alongside the DCS. It provides
three services the DCS integrates with:

- **issuer-api** (`:7002`) — signs and issues W3C VCs (OID4VCI).
- **wallet-api** (`:7001`) — student wallet creation, DID, and credential offers.
- **verifier-api** (`:7003`) — presentation requests and policy evaluation
  (OID4VP).

`credential-service` is the **only** service that talks to walt.id, via typed
Feign clients (`WaltidIssuerClient`, `WaltidWalletClient`, `WaltidVerifierClient`).
It assembles the VC JSON (`GenericCredentialBuilder`) from templates plus SIS
data — including the issuer as an object `{ "id": <DID> }` and the student DID as
`credentialSubject.id` — and hands it to walt.id to sign. Credential types are
**template-driven** in `application.yml`, each carrying a `waltidConfigId` that
maps to a walt.id issuance configuration.

## Consequences

### Positive

- **No home-grown crypto** — signing, DIDs, key handling, wallet, and the
  standards-compliant verifier come from a maintained, purpose-built stack.
- **Standards alignment** — OID4VCI / OID4VP and W3C VC Data Model out of the
  box, enabling interoperable credential types (SCHAC `EducationalID`,
  `EuropeanStudentCard`, `IdentityCredential`, `UniversityDegree`).
- **Clear boundary** — only `credential-service` depends on walt.id, keeping the
  rest of the system independent of the identity backend.
- **Focus** — the team invests in SIS integration, templates, and the pipeline.

### Negative / trade-offs

- **External dependency** — when walt.id is unavailable, issuance and verify
  return **HTTP 503** (authentication still succeeds; the 503 signals the
  downstream dependency). Its availability directly bounds the DCS's.
- **Coupling to walt.id's API and config model** — `waltidConfigId`s and client
  contracts must track walt.id's versions and behaviour.
- **Operational surface** — walt.id runs on its own `waltid_network` with its own
  datastore, which must be deployed, monitored, and upgraded.

## Alternatives considered

- **Build VC issuance/verification in-house** — maximum control, but a very large
  security-critical surface (crypto, DIDs, wallet, OID4VP verifier) that would
  dominate the project and risk subtle, dangerous flaws; rejected.
- **A hosted/proprietary credential SaaS** — would remove local control of keys
  and data and complicate an academic, self-hostable deployment; rejected in
  favour of a self-hosted open-source stack.

See [Architecture §3.3 (credential-service)](../ARCHITECTURE.md) and
[§6 (verification flow)](../ARCHITECTURE.md).
