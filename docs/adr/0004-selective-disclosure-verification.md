# 4. Selective / least-disclosure verification for privacy and GDPR

## Status

Accepted

## Context

Traditional identity checks over-collect: a plastic card or PDF shown at a
library desk, a border, or an employer reveals far more than the interaction
needs — full name, date of birth, address, grades, everything on the document.
The DCS holds sensitive **student PII** sourced from the SIS (identity, grades,
enrolment, schedule) and issues several distinct credential types
(`EducationalID`, `EuropeanStudentCard`, `IdentityCredential`, `UniversityDegree`)
into a student-controlled wallet.

Under **GDPR data minimisation (Art. 5(1)(c))**, a verifier should receive only
the data adequate, relevant, and limited to what its purpose requires. A library
kiosk confirming student status has no legitimate need for a degree
classification or a home address.

## Decision

Adopt **selective / least-disclosure verification**: a verifier requests **only
the specific credential type it actually needs** for a given interaction, and
nothing else is disclosed.

`student-service` publishes `verification.requested`; `credential-service` drives
the walt.id **verifier-api** over **OID4VP**, declaring
`request_credentials: [{ type, format }]` so the wallet presents **only** the
matching credential. Unrelated credentials and attributes are never transmitted.
The verifier evaluates per-credential and presentation-level **policies**
(e.g. `signature`, `expired`, `not-before`). Credentials can additionally be
issued as **SD-JWT** (Selective-Disclosure JWT), letting the holder reveal only
the specific claims within a credential (for example an `is_over_18` flag instead
of an exact date of birth).

| Verifier scenario | Credential requested | Not disclosed |
| --- | --- | --- |
| Library / campus door | `EducationalID` | Degree, GPA, home address |
| Cross-border discount | `EuropeanStudentCard` | Grades, phone number |
| Age-gated service | `IdentityCredential` (`is_over_18`) | Exact date of birth, address |
| Employer diploma check | `UniversityDegree` | Identity address, phone |

## Consequences

### Positive

- **Data minimisation by design** — verifiers see only the credential their
  purpose requires, aligning with GDPR Art. 5(1)(c).
- **Reduced over-collection & liability** — less PII crosses each boundary, so
  there is less to leak or misuse.
- **Student control** — the wallet presents only what is asked for; with SD-JWT
  the student reveals only the needed claims within a credential.
- **Purpose-bound trust** — each interaction is scoped to a single credential
  type, making the trust relationship explicit.

### Negative / trade-offs

- **Per-scenario modelling** — someone must decide which credential (and which
  claims) each verifier scenario legitimately needs.
- **Dependence on the verifier stack** — selective presentation and SD-JWT
  behaviour rely on walt.id's OID4VP verifier and policy engine
  (see [ADR 0003](0003-waltid-for-vc-issuance.md)).
- **Multiple exchanges** — a verifier needing two distinct facts must make two
  scoped requests rather than reading one all-encompassing document.

## Alternatives considered

- **Present the full credential set / a monolithic identity document** — simplest
  for verifiers, but over-discloses and conflicts with data minimisation;
  rejected.
- **Verifier-side filtering after full disclosure** — the wallet would still
  transmit everything and rely on the verifier to discard the rest; rejected
  because the data has already left the student's control.

See [Architecture §8 (selective verification)](../ARCHITECTURE.md) and
[Security — data protection & privacy](../SECURITY.md).
