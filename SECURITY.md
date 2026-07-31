# Security Policy

Thank you for helping keep the **ULHT Digital Credential System** and its users safe.

This document describes how to report security vulnerabilities in this
repository. It is the coordinated-disclosure policy for the project. A more
detailed description of the system's security model, threat considerations and
cryptographic design lives in the documentation site under
[`docs/SECURITY.md`](docs/SECURITY.md).

## Supported Versions

Security fixes are provided for the following release lines:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

Only the latest patch release within a supported line receives fixes. Please
upgrade to the newest `1.0.x` release before reporting an issue.

## Reporting a Vulnerability

**Please do not open a public GitHub issue for security vulnerabilities.**

Instead, report privately through GitHub Security Advisories:

1. Go to the repository's **Security** tab:
   <https://github.com/marcelogdomingues/ulht-dcs-public/security/advisories>
2. Click **"Report a vulnerability"** to open a private advisory.
3. Fill in the details described below.

If you are unable to use GitHub Security Advisories, you may reach the
maintainer by opening a minimal private channel via the project's GitHub
profile ([@marcelogdomingues](https://github.com/marcelogdomingues)) and
requesting a secure contact. Do **not** include sensitive details in public
comments. *(Security contact via GitHub; no email address is published.)*

### What to Include

To help us triage quickly, please provide as much of the following as you can:

- A clear description of the vulnerability and its potential impact.
- The affected component(s) — e.g. student service, lusofona/SIS service,
  credential service, fulfilment service, API gateway, or a Flutter app.
- The affected version, commit SHA, or Docker image tag.
- Step-by-step reproduction instructions or a proof of concept.
- Any relevant logs, request/response captures, or configuration
  (with **all secrets, tokens, and personal data redacted**).
- Suggested remediation, if you have one.

### Our Commitment

- **Acknowledgement:** we aim to acknowledge your report within **72 hours**.
- **Assessment:** we aim to provide an initial assessment and severity
  triage within **7 calendar days**.
- **Resolution:** we will work with you on a remediation timeline appropriate
  to the severity and will keep you informed of progress.
- **Disclosure:** we follow coordinated disclosure. We ask that you give us a
  reasonable opportunity to release a fix before any public disclosure, and we
  are happy to credit you in the advisory unless you prefer to remain anonymous.

## Scope

This policy covers the source code and configuration published in this
repository. Please **do not** perform testing that could affect other users,
degrade service availability, or access data that is not yours. Never use real
student credentials, production endpoints, or real personal data when preparing
a report — use placeholder and synthetic data only.

Thank you for practising responsible disclosure.
