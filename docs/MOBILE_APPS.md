# Mobile Apps

The ULHT Digital Credential System ships three [Flutter](https://flutter.dev) applications, one per role in the credential lifecycle. They live under [`mobile-apps/`](../mobile-apps) and share the same networking design: each app talks **directly to the backend microservices by port** and authenticates every request with an `apikey` header.

See also: [Getting Started](GETTING_STARTED.md) · [Configuration](CONFIGURATION.md) · [Security](SECURITY.md) · [API](API.md) · [Architecture](ARCHITECTURE.md) · [Project README](../README.md)

## The three apps

| App | Role | Purpose |
| --- | --- | --- |
| **student-app** | Holder / Wallet | The student wallet. Log in, view academic data (schedule, enrolment, grades, course credits), request and hold Verifiable Credentials, and scan QR codes to present them. |
| **verifier-app** | Verifier | Verify credentials presented by a student. Scan/receive a presentation, validate it against the credential service, and record the result (export, share, print). |
| **issuer-app** | Issuer | Issuer / registration console. Create and issue session credentials for conferences and events. |

## Tech stack

All three apps are built with the same toolchain:

| Component | Version |
| --- | --- |
| Flutter SDK | 3.44.8 |
| Dart SDK | 3.12.2 |

### Key packages

| Package | Version | Used by | Purpose |
| --- | --- | --- | --- |
| `http` | 1.x | all | REST calls to backend services |
| `provider` | 6.x | all | State management |
| `mobile_scanner` | 7.x | student-app | QR / credential-offer scanning |
| `flutter_secure_storage` | 10.x | student-app, issuer-app | Encrypted storage for tokens / PII |
| `flutter_local_notifications` | 22.x | verifier-app | Local verification notifications |
| `share_plus` | 13.x | verifier-app | Share / export verification results |
| `qr_flutter` | 4.x | all | QR code rendering |
| `url_launcher` | 6.x | all | Open external / credential-exchange URLs |

> The exact dependency set differs per app — see each app's `pubspec.yaml`
> ([student-app](../mobile-apps/student-app/pubspec.yaml),
> [verifier-app](../mobile-apps/verifier-app/pubspec.yaml),
> [issuer-app](../mobile-apps/issuer-app/pubspec.yaml)).

## Networking design — direct service calls, not the gateway

**Important:** the apps do **not** call the Kong gateway (port `8000`). They call each backend service **directly on its own port**. There is no single working gateway route today — different features live on different services, and both `student-service` and `lusofona-service` serve `/api/v1/student/*` paths (a collision a gateway route cannot disambiguate).

Every request carries an `apikey` header. The backend services enforce this via Spring Security: only the actuator `health`/`info`/`prometheus` endpoints and Swagger are public — all business endpoints reject calls without a valid key with `401 Unauthorized`.

The service base URLs and the shared API key are **build-time configurable** via `--dart-define`.

## Build-time configuration (`--dart-define`)

| Key | Default | Description |
| --- | --- | --- |
| `API_KEY` | `ulht-dev-local-CHANGE-ME` | Shared API key sent as the `apikey` header on every request. |
| `STUDENT_SVC_URL` | `http://localhost:8084/api/v1` | student-service (`/student/issue`, `/student/status`, `/student/credentials`, `/student/verify`). |
| `LUSOFONA_SVC_URL` | `http://localhost:8085/api/v1` | lusofona-service (`/student/schedule`, `/student/enrolment`, `/student/grades`, `/student/course-credits`, `/student/login`). |
| `CREDENTIAL_SVC_URL` | `http://localhost:8086/api/v1` | credential-service (`/wallet/...`, `/issuer/...`). |
| `FULFILMENT_SVC_URL` | `http://localhost:8087/api/v1` | fulfilment-service (`/fulfilment/...`). |

### student-app only

| Key | Default | Description |
| --- | --- | --- |
| `STUDENT_USERNAME` | *(empty)* | Student login username. Replaces a previously hardcoded credential that was removed from the source. |
| `STUDENT_INSTALL_KEY` | *(empty)* | Student install key. Replaces a previously hardcoded credential that was removed from the source. |

> Use placeholder values in documentation and scripts. **Never commit real
> student credential values** — supply them at build time via `--dart-define`.

## Running an app

From the app directory (`mobile-apps/student-app`, `mobile-apps/verifier-app`, or `mobile-apps/issuer-app`):

```bash
# 1. Fetch dependencies
flutter pub get

# 2. Run on a connected device / emulator with the dev defaults
flutter run

# 3. Run with explicit configuration (recommended)
flutter run \
  --dart-define=API_KEY=ulht-dev-local-CHANGE-ME \
  --dart-define=STUDENT_SVC_URL=http://localhost:8084/api/v1 \
  --dart-define=LUSOFONA_SVC_URL=http://localhost:8085/api/v1 \
  --dart-define=CREDENTIAL_SVC_URL=http://localhost:8086/api/v1 \
  --dart-define=FULFILMENT_SVC_URL=http://localhost:8087/api/v1
```

For **student-app**, also pass the login credentials:

```bash
flutter run \
  --dart-define=API_KEY=ulht-dev-local-CHANGE-ME \
  --dart-define=STUDENT_USERNAME=<student-username> \
  --dart-define=STUDENT_INSTALL_KEY=<student-install-key>
```

### Running in the browser (web)

```bash
flutter run -d web-server --web-port=5001 --web-hostname=127.0.0.1
```

## Security features

The apps follow the same hardening principles as the backend (see [Security](SECURITY.md)):

- **Secure storage for secrets and PII.** Auth tokens, the install key, and student PII are stored via `flutter_secure_storage` (see [`secure_store.dart`](../mobile-apps/student-app/lib/services/secure_store.dart)). Non-sensitive UI preferences use `shared_preferences`.
- **QR / deep-link URL allowlist.** Before launching any scanned or arbitrary URL externally, it is validated against an allowlist so a malicious QR code cannot drive the wallet to an attacker-controlled site (see [`url_guard.dart`](../mobile-apps/student-app/lib/utils/url_guard.dart)). Allowed schemes: `openid4vp`, `openid4vci`, `haip`. Allowed http(s) hosts: `localhost`, `127.0.0.1`, `10.0.2.2` (Android emulator loopback), plus the `ensinolusofona.pt` / `ulusofona.pt` domain suffixes.
- **No hardcoded secrets.** API key and service URLs come from `--dart-define`; the previously hardcoded student credential was removed and is now injected at build time.
- **`debugPrint`, not `print`.** Diagnostic logging uses `debugPrint` so it is stripped in release builds.

## Production notes

- Pass **HTTPS** base URLs: `--dart-define=<SVC>_URL=https://<host>/api/v1`.
- Pass a **real** `--dart-define=API_KEY=<secret>` — never ship the dev key `ulht-dev-local-CHANGE-ME`.
- Update the `url_guard` allowlist to the real production host(s) over `https` only, and drop `localhost`.

## Known limitations

- **Per-user login UI is a TODO in student-app.** A real interactive login flow is not yet implemented; credentials are currently supplied via `--dart-define`.
- **TLS is required for production.** The dev defaults use plain `http://` on localhost. A production deployment must serve every backend over HTTPS.
- **No production gateway.** A gateway with correct, non-colliding routes is a future improvement; today the apps call services directly by port.
