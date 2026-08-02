# Demo mode (no setup, no external services)

The demo runs the **full issue → verify pipeline with a single command** and **no
external dependencies**. There is no real walt.id and no real university Student
Information System (SIS): both are replaced by in-memory mocks that return canned,
obviously-fake data.

!!! warning "Illustrative only"
    Demo mode uses **mock walt.id** (issuer / wallet / verifier) and a **mock SIS**.
    The credentials it produces are **not real, not cryptographically verifiable, and
    not tied to any person or institution**. All demo data is fake
    (`demo-student`, `example.edu`, ...). Use it to explore the workflow, not for
    anything production-related.

## One command

```bash
docker compose -f compose/demo.yml up -d --build
```

This builds and starts a lean, self-contained stack:

- **Kafka** (KRaft, single node) — the event backbone
- **Consul** — service discovery
- **student-service** (`:8084`) — the entry point
- **sis-service** (`:8085`) — SIS proxy (**mocked**)
- **credential-service** (`:8086`) — walt.id issuer/wallet/verifier (**mocked**)
- **fulfilment-service** (`:8087`) — workflow status/result tracking

It does **not** include walt.id, the Kong API gateway, or the monitoring stack —
those are intentionally left out to keep the demo minimal.

The API key is baked in as `demo-key`, and the wallet password secret/salt have
inline demo defaults, so **no `.env` file is required**.

## What it does

1. `POST /api/v1/student/issue` publishes a `student.login.requested` Kafka event.
2. **sis-service** consumes it and (in demo mode) returns canned student data
   from the mock `SisClient` instead of calling a real SIS, then emits a
   `credential.requests` event.
3. **credential-service** consumes it, ensures a wallet, and issues credentials via
   the mock walt.id clients — producing realistic OID4VCI **credential-offer URLs**
   (`openid-credential-offer://...`) — then emits `credential.completed`.
4. **fulfilment-service** tracks the workflow so status/result can be polled.

Verification follows the same pattern: the mock verifier returns a verification URL
and a result whose `verificationResult == true` ("verified").

## Try it: issue → poll → fetch

Wait until all services report healthy:

```bash
docker compose -f compose/demo.yml ps
```

Then run the flow (the API key is `demo-key`, sent in the `apikey` header):

```bash
KEY=demo-key

# 1. Start issuance and capture the correlationId
CID=$(curl -s -X POST http://localhost:8084/api/v1/student/issue \
  -H "Content-Type: application/json" \
  -H "apikey: $KEY" \
  -d '{"userName":"demo-student","installKey":"demo-key"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["correlationId"])')
echo "correlationId: $CID"

# 2. Poll status until COMPLETED (or ERROR/FAILED)
for i in $(seq 1 30); do
  STATUS=$(curl -s -H "apikey: $KEY" \
    http://localhost:8084/api/v1/student/status/$CID \
    | python3 -c 'import sys,json;print(json.load(sys.stdin).get("status"))')
  echo "status: $STATUS"
  [ "$STATUS" = "COMPLETED" ] || [ "$STATUS" = "FAILED" ] && break
  sleep 3
done

# 3. Fetch the issued credential-offer URLs
curl -s -H "apikey: $KEY" \
  http://localhost:8084/api/v1/student/credentials/$CID | python3 -m json.tool
```

You should see a `COMPLETED` status and a `credentialOfferUrls` array of
`openid-credential-offer://...` URLs.

### Example output

A real run against the demo stack (mock walt.id + SIS):

```jsonc
// POST /api/v1/student/issue  -> 200
{
  "correlationId": "58a4ab9c-7471-4a81-bd40-6ac6498f3c07",
  "status": "PROCESSING",
  "message": "Credential issuance initiated, processing...",
  "monitorAt": "/student/status/58a4ab9c-...",
  "credentialsAt": "/student/credentials/58a4ab9c-..."
}

// GET /api/v1/student/status/{correlationId}  (after ~1s)  -> 200
{
  "correlationId": "58a4ab9c-7471-4a81-bd40-6ac6498f3c07",
  "status": "COMPLETED",
  "progress": 100,
  "message": "Workflow completed successfully",
  "result": {
    "summary": { "total": 4, "issued": 3, "skipped": 1, "failed": 0 },
    "issuedCredentialTypes": ["EducationalID", "IdentityCredential", "EuropeanStudentCard"],
    "userId": "demo-student"
  }
}
```

(`UniversityDegree` is skipped for a non-graduate — that's the conditional-issuance rule in action.)

### Verify (basic)

```bash
curl -s -X POST http://localhost:8084/api/v1/student/verify \
  -H "Content-Type: application/json" \
  -H "apikey: $KEY" \
  -d '{"credentialType":"DemoStudentCredential","userId":"demo-student"}' \
  | python3 -m json.tool
```

The mock verifier returns a verification URL and a successful ("verified") result.

## Debugging

```bash
docker compose -f compose/demo.yml logs credential-service sis-service
docker compose -f compose/demo.yml ps
```

## Tear down

```bash
docker compose -f compose/demo.yml down
```

## How the mocks are wired

- **sis-service** — `DemoConfiguration` (`@Profile("demo")`) registers a
  `@Primary` mock `SisClient` returning canned enrolment/grade/eval/credit data
  and a login payload with a fake `studentId`/`email`/`fullName`.
- **credential-service** — `DemoConfiguration` (`@Profile("demo")`) registers
  `@Primary` mock `WaltidIssuerClient`, `WaltidWalletClient`, and
  `WaltidVerifierClient` returning a `did:jwk:...` issuer DID, credential-offer URLs,
  a wallet session cookie, a subject DID, and a `verificationResult == true` result.

The real Feign clients are left in place; the `@Primary` demo beans simply win while
the `demo` profile is active. The default and `docker` profiles behave exactly as
before. The demo profile is activated via `SPRING_PROFILES_INCLUDE=demo` alongside
the `docker` profile, so real Kafka + Consul wiring is still used — only the walt.id
and SIS calls are mocked.
