# 7. Credential revocation via a status registry and W3C Bitstring Status List

## Status

Accepted

## Context

The DCS issues W3C Verifiable Credentials to students, but a VC is a long-lived,
holder-held artifact: once issued, the issuer no longer controls the copy in the
wallet. Real deployments must be able to **revoke** a credential after issuance —
a student withdraws, a degree is rescinded, a card is reported lost — and let a
verifier discover that state at verification time. Some cases are temporary
(a hold on an account), which calls for a **suspend / reactivate** state distinct
from permanent revocation.

The interoperable, privacy-preserving mechanism for this is the
[W3C Bitstring Status List](https://www.w3.org/TR/vc-bitstring-status-list/): the
issuer publishes a single compressed bitstring where each credential is assigned
an index; a set bit means that credential is revoked (or suspended). Verifiers
fetch one small list and check one bit, so the issuer learns nothing about *which*
credential is being verified, and a large minimum list size gives herd privacy.

`credential-service` already owns a PostgreSQL database (`credential`) with JPA +
Flyway from earlier phases, which is the natural home for the authoritative status
of every credential it issues.

## Decision

Implement a **credential status registry** inside `credential-service`, backed by
the existing `credential` database, and expose it plus a Bitstring Status List
over the existing apikey-secured REST surface.

- **Schema / entity** — Flyway migration `V3__create_credential_status.sql`
  creates `credential_status` with columns `id` (PK — the VC id or a generated
  UUID), `status_list_index` (the credential's bit position), `credential_type`,
  `subject_id` (nullable), `status` (`VALID` / `REVOKED` / `SUSPENDED`), `reason`
  (nullable), `issued_at`, `updated_at`. The `status_list_index` is assigned by
  the application as `MAX(index)+1` so it stays portable across PostgreSQL
  (production) and H2 (tests) without a native sequence. Mapped by
  `CredentialStatusEntity` + `CredentialStatusRepository`.
- **Service** — `CredentialStatusService` provides `record(credentialId, type,
  subjectId)` (idempotent, → `VALID`), `revoke(id, reason)`, `suspend` /
  `reactivate`, and `getStatus(id)`. `buildEncodedList()` renders the registry as
  the W3C `encodedList`: it sets the index bit of every revoked/suspended row in a
  bitstring (bits numbered from the most-significant bit of each byte, per spec),
  pads to the recommended **131,072-bit (16 KB)** herd-privacy minimum,
  GZIP-compresses it, and base64url-encodes it (unpadded).
- **REST** — `CredentialStatusController` under the service context-path
  `/api/v1` (all routes require the shared `apikey`; none are public):
  - `POST /credentials/{id}/revoke` — optional body `{ "reason": "..." }`, marks
    the credential `REVOKED`.
  - `GET /credentials/{id}/status` — `{ id, status, reason, updatedAt }` (404 if
    unknown).
  - `GET /status-list/{listId}` — a `BitstringStatusListCredential` Verifiable
    Credential JSON (`credentialSubject.type = BitstringStatusList`,
    `statusPurpose = revocation`, `encodedList = <gzip+base64url>`). A single
    default list is published.
- **Issuance hook** — the credential workflow
  (`CredentialWorkflowConsumer.issueCredentials`) calls
  `CredentialStatusService.record(...)` for each successfully issued credential so
  it appears in the registry as `VALID`. walt.id returns an offer URL rather than
  a stable VC id, so a UUID is generated for the registry id. In DEMO mode walt.id
  is mocked but a row is still recorded per issued type. The call is
  **failure-isolated**: any registry error is caught and logged only — issuance
  must never fail because the registry hiccuped.

## Consequences

### Positive

- **Real revocation** — the issuer can revoke or suspend a credential after
  issuance, and a verifier can discover it.
- **Standards-based & privacy-preserving** — a spec-conformant Bitstring Status
  List with herd-privacy padding; interoperable with any compliant verifier.
- **Authoritative & durable** — status lives in the existing Postgres database
  with the rest of the issuer's durable state; no new infrastructure.
- **Non-invasive** — the issuance happy path is unchanged and cannot be broken by
  a registry failure.

### Negative / trade-offs

- **Unsigned status list** — `GET /status-list` returns the list as a VC-shaped
  JSON object but it is not cryptographically signed by the real issuer key yet.
- **Single default list** — one list/index space; sharding across multiple lists
  is deferred until scale requires it.
- **Generated ids** — because walt.id returns an offer URL, the registry id is a
  generated UUID rather than the VC's own id, so cross-referencing an in-wallet VC
  to its registry row is indirect for now.

## Follow-up

**Done — `credentialStatus` is now embedded in issued VCs.**
`CredentialWorkflowConsumer.issueCredentials` records the credential in the
registry *before* issuance (obtaining a stable id + `statusListIndex`), then
embeds a `credentialStatus` entry
(`type: BitstringStatusListEntry`, `statusPurpose: revocation`,
`statusListIndex`, `statusListCredential = {baseUrl}/status-list/{listId}`) into
the credential data walt.id signs, so the issued VC points a verifier at
`GET /api/v1/status-list/{listId}`. The base URL is configurable via
`credentials.status.base-url`. This was verified end-to-end against the bundled
real walt.id issuer (`waltid/issuer-api:0.22.0`, see
[Deployment → Running with real walt.id](../DEPLOYMENT.md)): the issuer accepts
the entry and returns a valid OID4VCI pre-authorized credential offer. The
embedding is **failure-isolated** — if the registry or embedding step fails,
issuance still proceeds (just without the status entry) and a fallback
`record(...)` still tracks the credential.

Remaining smaller increments:

- **Use the credential's real VC id as the registry id** — walt.id returns an
  offer URL rather than a stable VC id, so a generated UUID is still used as both
  the registry id and the `statusListIndex` anchor.
- **Sign the published status list** with the issuer key (currently returned as
  unsigned VC-shaped JSON).

## Alternatives considered

- **RevocationList2020 / bespoke "is-revoked?" endpoint** — a per-credential
  lookup leaks which credential a verifier is checking and is superseded by the
  Bitstring Status List; rejected on privacy and interoperability grounds.
- **On-chain / status registry service** — heavier infrastructure for no benefit
  at this scale; the issuer's own database is the authoritative source.

## Related documentation

- [walt.id for VC issuance](0003-waltid-for-vc-issuance.md)
- [Selective disclosure](0004-selective-disclosure-verification.md)
- [Auth & key-management roadmap](0008-auth-and-key-management-roadmap.md)
- [W3C Bitstring Status List](https://www.w3.org/TR/vc-bitstring-status-list/)
