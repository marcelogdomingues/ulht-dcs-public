# Standards & Conformance

This document is the **standards reference** for the **Digital Credential System (DCS)**. It records the open, published identity standards the platform builds on, exactly where each one is used in the code, and — honestly — what is fully implemented versus what is partial or on the roadmap.

> See also: [Architecture](ARCHITECTURE.md) · [API](API.md)

!!! note "Public repository — placeholders only"
    This is the **public** repository. Institution-specific values (the real SIS base URL, student identifiers, installation keys, API keys) are **never** committed here. Wherever a real endpoint would appear, this document uses the placeholder `https://university-sis.example.edu/api`. Real values are injected privately at runtime.

---

## Why a standards-based approach

The DCS deliberately builds on **open, published standards** rather than a proprietary credential format. This choice has three concrete pay-offs:

- **Interoperability** — credentials issued by the DCS can be read, stored, and verified by any conformant wallet or verifier, not only by DCS tooling. Verification runs over **OpenID for Verifiable Presentations (OID4VP)**, and credentials follow the **W3C Verifiable Credentials Data Model**, so a third-party verifier (a library, an employer, a cross-border service) needs no DCS-specific SDK.
- **Portability** — a credential lives in a **student-controlled wallet** and is bound to the student's own **Decentralized Identifier (DID)**. It is not locked inside a university database; the student carries it and presents it wherever it is accepted.
- **No vendor lock-in** — the issuing/verification engine is [walt.id](https://walt.id), an open-source implementation of these same standards. Because the wire formats and protocols are standardised, the underlying engine could be swapped for another conformant implementation without changing the credential contract seen by wallets and verifiers.

Credential types themselves are **template-driven** (declared in `application.yml` under `credentials.templates`), so aligning a new credential to a standard schema is a configuration change, not a code change.

---

## Conformance matrix

| Standard | What it provides | Where it's used in this project | Status / notes | Spec link |
| --- | --- | --- | --- | --- |
| **W3C Verifiable Credentials Data Model 2.0** | Credential structure — `@context`, `type`, `issuer` (as an object with `id`), `credentialSubject`, issuance/expiration dates | `GenericCredentialBuilder` / `CredentialDataBuilder` assemble the VC JSON that walt.id signs; `issuer` is emitted as an object `{ "id": <DID> }` and `credentialSubject.id` is the student DID | **Implemented.** Data model is 2.0-aligned; the default `@context` list still references the widely deployed `credentials/v1` context. See notes below. | https://www.w3.org/TR/vc-data-model-2.0/ |
| **W3C Decentralized Identifiers (DIDs)** | Cryptographically verifiable issuer & subject identifiers with no central registry | Issuer DID configured via `waltid.issuer.defaults.did-method: jwk` (`did:jwk` default; `did:key` also supported); student subject DIDs generated as `did:jwk:<suffix>` in `CredentialWorkflowConsumer` | **Implemented** for `did:jwk` and `did:key`. `did:web` / `did:cheqd` are listed as options but not exercised by default. | https://www.w3.org/TR/did-core/ |
| **OpenID for Verifiable Credential Issuance (OID4VCI)** | Standard protocol for offering and delivering a credential to a wallet (credential offer + pre-authorized code flow) | `credential-service` issues through walt.id's issuer-api (`:7002`); `IssueCredentialRequest` documents the OID4VCI flow; `authentication-method: PRE_AUTHORIZED`, `standard-version: DRAFT13`; wallet receives a credential offer URL | **Implemented** via walt.id (Draft 13, pre-authorized flow). | https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html |
| **OpenID for Verifiable Presentations (OID4VP)** | Standard protocol for requesting and presenting credentials (selective, purpose-bound disclosure) | `VerifierController` + `VerifierService` drive walt.id's verifier-api (`:7003`); `authorize-base-url: openid4vp://authorize`, `response-mode: direct_post`; verifier requests a single credential type and receives a QR/URL the wallet responds to | **Implemented** via walt.id. Powers [selective verification](ARCHITECTURE.md). | https://openid.net/specs/openid-4-verifiable-presentations-1_0.html |
| **SD-JWT VC / JWT VC** | Credential serialization formats — a signed JWT VC, or an SD-JWT enabling selective disclosure of individual claims | Enabled credential templates use `format: jwt_vc_json`; walt.id config ids for `vc+sd-jwt` exist (e.g. `UniversityDegree_vc+sd-jwt`) and DTOs recognise `sd_jwt_vc` | **Partial.** `jwt_vc_json` is the active format for all four default types. SD-JWT is wired at the walt.id config level but not the default issuance path. | https://datatracker.ietf.org/doc/draft-ietf-oauth-sd-jwt-vc/ |
| **SCHAC 2.0** (Schema for Academia) | Standard attribute schema for the academic sector (e.g. `schacPersonalUniqueCode`, `schacHomeOrganization`, `schacPersonalUniqueID`) | The `EducationalID` credential is SCHAC-aligned — student identity + institution attributes mapped from the SIS record; `@context` includes the European `esi/v1` context | **Partial / aligned.** Attributes are modelled in the SCHAC spirit; formal SCHAC URN attribute naming is a roadmap item. | https://wiki.refeds.org/display/STAN/SCHAC |
| **European Student Card Initiative (ESC)** | Pan-European interoperability for student cards — the European Student Identifier (ESI) and cross-border student status | The `EuropeanStudentCard` credential maps an `esi` (European Student Identifier), institution PIC/code, and study level; `@context` includes `esc/v1`; `staticFields.cardType: "ESC"` | **Partial / aligned.** ESI + ESC context are emitted; full ESC card-number / router-registry integration is roadmap. | https://europeanstudentcard.eu/ |

!!! note "About the `@context` version"
    The default templates ship the stable, widely deployed `https://www.w3.org/2018/credentials/v1` context alongside European sector contexts (`esi/v1`, `esc/v1`, `degree/v1`). The **structure** of the emitted credential (issuer-as-object, `credentialSubject`, validity dates) follows the VC Data Model 2.0. Moving the `@context` itself to the `credentials/v2` URI is a low-risk configuration change tracked on the roadmap.

---

## Credential types and their standard alignment

The system enables **four** W3C Verifiable Credential types by default (declared in `credentials.templates`). Additional types (KYC, boarding pass, hotel reservation, …) ship **disabled** and are activated with `enabled: true`.

| Credential type | Standard alignment | What it carries | Notes |
| --- | --- | --- | --- |
| **EducationalID** | **SCHAC 2.0** (+ W3C VC, European `esi/v1` context) | Student identity, course/programme, institution, enrolment status | Priority credential for active students; the credential most verifiers request to confirm student status |
| **IdentityCredential** | **W3C VC** identity | Personal identity attributes plus derived `is_over_18 / is_over_21 / is_over_65` flags | Enables age-gated checks without revealing the exact date of birth |
| **EuropeanStudentCard** | **ESC Initiative** (+ W3C VC, European `esc/v1` context) | European Student Identifier (`esi`), institution PIC, study level, validity dates | Cross-border student interoperability; `cardType: ESC` |
| **UniversityDegree** | **W3C VC** (issued conditionally) | Degree name/type, major, graduation date, classification, accrediting body | Issued **only** for graduates — gated by a SpEL condition on `graduationDate` / `degreeAwarded` in the SIS record |

All four are signed by the issuer DID via walt.id and offered into the student's wallet over **OID4VCI**; all four are verifiable over **OID4VP**.

---

## Implemented vs. partial / roadmap

**Fully implemented today**

- W3C VC Data Model credential structure (issuer-as-object, `credentialSubject`, issuance/expiration dates).
- Decentralized Identifiers for both issuer and subject (`did:jwk` default, `did:key` supported).
- End-to-end **OID4VCI** issuance and **OID4VP** verification through walt.id, including selective, purpose-bound presentation of a single credential type.
- `jwt_vc_json` credential format for all four default credential types.
- Per-credential and presentation-level verification **policies** (`signature`, `expired`, `not-before`).

**Partial / roadmap (honest status)**

- **SD-JWT VC** — walt.id config ids and DTO support exist for `vc+sd-jwt`, but the default issuance path uses `jwt_vc_json`. Native claim-level selective disclosure via SD-JWT is not yet the default.
- **SCHAC / ESC formal conformance** — attributes are modelled in the spirit of SCHAC and ESC (including the ESI and European sector contexts), but formal SCHAC URN attribute naming and full ESC router/registry integration are not yet complete.
- **`@context` v2 URI** — structure is 2.0-aligned; the `@context` still references `credentials/v1`.
- **Additional DID methods** — `did:web` and `did:cheqd` are configuration options but are not exercised by default.
- **Revocation & credential status** — no status-list mechanism (e.g. W3C Bitstring Status List / StatusList2021) is implemented yet. Credentials rely on a 365-day expiry rather than active revocation. This is a known roadmap item.
- **Formal conformance test suites** — the platform is standards-*aligned* by construction (via walt.id), but it has not been run through the official W3C VC / OID4VC conformance test suites. Independent conformance certification is roadmap.

---

## Related documentation

- [Architecture](ARCHITECTURE.md) — full system design, credential pipeline, and selective verification
- [API](API.md) — endpoint reference for issuance and verification
