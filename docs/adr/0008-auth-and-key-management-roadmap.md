# 8. Authentication & key-management roadmap

## Status

Partially implemented. **Item 1 (OAuth2 / OIDC) is implemented as an optional,
dual-auth capability** — services accept **either** a valid `apikey` **or** a
valid OIDC Bearer JWT, and the JWT leg is dormant unless an issuer is configured.
Items 2–4 (KMS/Vault key management, SD-JWT default, mobile OIDC login) remain
**proposed** (design only, not yet implemented).

## Context

The platform's security today is deliberately simple (see
[API-key authentication](0005-api-key-authentication.md)): a single shared
`apikey` header authenticates every service-to-service call, the issuer signing
key lives in memory, credentials are issued as `jwt_vc_json`, and the mobile apps
have no per-user login. This is the right trade-off for a hardened academic /
development deployment, but a shared or internet-facing deployment needs real
per-user identity, hardware-protected keys, true selective disclosure, and a user
login on mobile.

This ADR records the **intended design** for those four items and, honestly, why
each is **deferred**: every one of them introduces external infrastructure and a
migration with real risk, and none is required to demonstrate the system. It is a
plan of record, not an implemented decision — hence **Proposed**.

## Decision (target design)

### 1. OAuth2 / OIDC per-user authentication — IMPLEMENTED (optional, dual-auth)

- **Design.** Introduce an OpenID Connect provider — **Keycloak** (batteries
  included: user federation, admin UI, token issuance) or the lighter **Spring
  Authorization Server** — issuing short-lived JWT access tokens. Services
  validate tokens as an OAuth2 resource server (`spring-boot-starter-oauth2-
  resource-server`) via JWKS, mapping token scopes/roles to authorities for
  fine-grained, per-caller authorization.
- **Keep the api-key for service-to-service.** The shared `apikey` filter stays
  for machine-to-machine / internal traffic (defense-in-depth at the edge is
  unchanged); OIDC is added *alongside* it for human callers, so the two auth
  models coexist during and after migration.
- **What shipped.** The shared `dcs-commons`
  `ApiKeySecurityAutoConfiguration` now depends on
  `spring-boot-starter-oauth2-resource-server` and its single
  `SecurityFilterChain` runs the api-key filter first and **additionally** wires
  `http.oauth2ResourceServer(oauth2 -> oauth2.jwt(...))` — but only when a
  `JwtDecoder` bean exists, which Spring Boot creates only when
  `spring.security.oauth2.resourceserver.jwt.issuer-uri` is configured. A request
  authenticates with **either** a valid `apikey` **or** a valid Bearer JWT;
  neither ⇒ `401` (unchanged). With no issuer-uri set (the demo and base stack),
  the JWT leg is never wired and behaviour is byte-for-byte the api-key model of
  ADR 0005. An optional overlay `docker-compose.keycloak.yml` +
  `docker/keycloak/realm-export.json` stand up Keycloak and set the issuer-uri;
  see [Security → OAuth2 / OIDC (optional)](../SECURITY.md#oauth2-oidc-optional)
  and [Deployment → Running with Keycloak](../DEPLOYMENT.md#running-with-keycloak-optional-oauth2).
- **Still optional.** OIDC is opt-in infrastructure: the IdP (realm, clients,
  user store, key rotation) is only run when the overlay is used, so there is no
  operational cost or migration risk to the default api-key path.

### 2. Issuer-key management via KMS / Vault / HSM

- **Design.** Replace the in-memory `AtomicReference<OnboardIssuerResponse>` in
  `IssuerKeyService` (which holds the issuer signing material for the process
  lifetime and is lost on restart) with a managed key store: **HashiCorp Vault**
  (transit/secrets engine), a cloud **KMS**, or an **HSM/PKCS#11** device. The
  private key never leaves the boundary; the service requests signing operations
  or fetches a short-lived handle. This gives durable key identity across
  restarts, auditable access, and rotation.
- **Why deferred.** Needs external infrastructure to provision and secure, a key
  ceremony / DID re-registration so verifiers trust the managed key, and careful
  migration so existing issued credentials remain verifiable. Out of scope for a
  local demo where an ephemeral in-memory key is sufficient.

### 3. SD-JWT as the default credential format

- **Design.** Make **SD-JWT (`vc+sd-jwt`)** the default issuance format instead
  of `jwt_vc_json`, so credentials support real, holder-controlled selective
  disclosure at presentation time rather than relying on verifier-side least-
  disclosure. walt.id already supports the SD-JWT configurations; the change is to
  flip the default template `waltidConfigId` and issuance path and align the
  verifier presentation policies.
- **Why deferred.** Migrating the default format affects every issued credential,
  the wallet acceptance flow, and the verifier's presentation-definition/policy
  handling; it needs coordinated testing across issuer, wallet, and verifier and
  risks the currently-working `jwt_vc_json` happy path. Sequenced after the auth
  and key work so signing is on managed keys first.

### 4. Mobile OIDC login

- **Design.** Add an **OIDC Authorization Code + PKCE** login to the mobile apps
  against the IdP from item 1, storing tokens in the platform secure store
  (Keychain / Keystore) and attaching the access token to API calls — replacing
  the implicit trust the apps have today.
- **Why deferred.** Directly depends on item 1 (no IdP, no mobile login) and adds
  app-store release cycles and redirect-URI / deep-link plumbing. Deferred until
  the IdP exists.

## Migration path

1. Stand up the IdP (item 1) in a non-blocking, additive way — services accept
   **either** a valid `apikey` **or** a valid OIDC token; nothing breaks.
2. Move the issuer key into a KMS/Vault (item 2) with a re-registered issuer DID;
   verify old and new credentials both validate.
3. Flip the default credential format to SD-JWT (item 3) once signing is managed,
   testing issuer → wallet → verifier end to end.
4. Add mobile OIDC login (item 4) against the now-stable IdP.
5. Once callers have migrated, tighten authorization (scopes/roles) and consider
   retiring the shared api-key for human traffic while keeping it for internal
   service-to-service calls.

## Consequences

### Positive

- **Real identity and least privilege** — per-user OIDC identity with scopes
  replaces one coarse shared secret.
- **Protected signing keys** — durable, auditable, rotatable issuer keys instead
  of an ephemeral in-memory reference.
- **Genuine selective disclosure** — SD-JWT lets the holder choose what to reveal.
- **Authenticated mobile users** — no implicit trust from the apps.

### Negative / trade-offs

- **Operational weight** — an IdP and a KMS/Vault are new systems to run, secure,
  and monitor.
- **Migration risk** — each step touches a working path (security chain, signing,
  credential format, mobile release); hence the additive, staged sequence.
- **Partially implemented** — item 1 (OAuth2/OIDC) has landed as an optional
  dual-auth capability; the in-memory-key + `jwt_vc_json` model of ADR 0005
  otherwise remains in force until items 2–4 land. When no OIDC issuer is
  configured, the runtime behaviour is unchanged from ADR 0005.

## Alternatives considered

- **Do nothing / keep shared api-key only** — acceptable for the academic
  deployment but insufficient for shared or internet-facing use; this roadmap
  exists precisely to chart the way off it.
- **Custom JWT auth instead of an IdP** — reinvents token issuance, rotation, and
  user management that a standards-based OIDC provider gives for free; rejected.
- **Keys on disk / in env vars instead of a KMS** — marginally better than
  in-memory but still unprotected and unauditable; rejected in favour of managed
  key storage.

## Related documentation

- [API-key authentication](0005-api-key-authentication.md)
- [walt.id for VC issuance](0003-waltid-for-vc-issuance.md)
- [Selective disclosure](0004-selective-disclosure-verification.md)
- [Credential revocation](0007-credential-revocation.md)
- [Security](../SECURITY.md)
