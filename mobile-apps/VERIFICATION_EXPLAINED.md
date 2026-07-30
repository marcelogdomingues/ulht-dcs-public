# Understanding the Two QR Codes

## Overview

There are **two different QR codes** in the system, each serving a different purpose:

### 1. **Student App QR Code** (Credential Offer)
**Purpose:** When a student receives a credential offer, they can view it as a QR code.

**What it contains:**
- A credential offer URL (e.g., `openid4vci://...`)
- Used to **receive** credentials into the wallet

**When it appears:**
- After a student logs in and credentials are being issued
- The student can scan this QR code (or click "Open") to accept the credential offer
- This is a **one-time** process when credentials are first issued

**Example flow:**
```
1. Student logs in → System issues credentials
2. Student app shows credential offer as QR code
3. Student scans QR code → Credentials added to wallet
```

### 2. **Verifier App QR Code** (Verification Request)
**Purpose:** When a verifier wants to verify a student's credentials, they generate a QR code.

**What it contains:**
- A verification request URL (e.g., `openid4vp://authorize?...`)
- Used to **request** credentials from the student's wallet

**When it appears:**
- When a verifier (bar, event, etc.) wants to verify a student
- The verifier selects a credential type and a QR code appears immediately
- The student scans this QR code to **present** their credentials

**Example flow:**
```
1. Verifier opens app → Selects "Educational ID"
2. QR code appears immediately
3. Student scans QR code with wallet app
4. Student's wallet presents credentials
5. Verifier sees: "✓ Verified - Student ID: a12345678"
```

## How Scanning Works

### Current Implementation

**Student App:**
- ✅ Can **display** QR codes (credential offers)
- ❌ Cannot **scan** QR codes yet (needs to be added)

**Verifier App:**
- ✅ Can **display** QR codes (verification requests)
- ❌ Cannot scan (doesn't need to)

### How It Should Work

1. **Verifier generates QR code:**
   - Verifier app calls `/verifier/verify/basic`
   - Gets back: `openid4vp://authorize?response_type=vp_token&...`
   - Displays as QR code

2. **Student scans QR code:**
   - Student opens wallet app
   - Taps "Scan QR Code" button
   - Camera opens, scans the verifier's QR code
   - App extracts the `openid4vp://` URL

3. **Student presents credentials:**
   - Wallet app opens the URL using `url_launcher`
   - The `openid4vp://` protocol is handled by the walt.id wallet service
   - Wallet service presents the requested credentials
   - Verifier receives the credentials and verifies them

## Technical Details

### OID4VP Protocol

The verification QR code uses the **OpenID for Verifiable Presentations (OID4VP)** protocol:

- **URL Scheme:** `openid4vp://authorize`
- **Purpose:** Request specific credentials from a wallet
- **Response:** Wallet presents credentials via `direct_post` to walt.id verifier

### Walt.id Wallet Integration

The student app uses **walt.id wallet as an external service** (port 7001):

- Student app displays credentials from walt.id wallet API
- When student scans verification QR code, the `openid4vp://` URL is opened
- Walt.id wallet service handles the OID4VP flow
- Credentials are presented to the verifier

## Testing Flow

### Prerequisites
1. ✅ Credential Service running (port 8086)
2. ✅ Walt.id Verifier running (port 7003)
3. ✅ Walt.id Wallet running (port 7001)
4. ✅ Student has credentials in wallet

### Step-by-Step Test

1. **Open Verifier App:**
   ```
   - Select credential type: "EducationalID"
   - QR code appears immediately
   ```

2. **Open Student App:**
   ```
   - Go to Wallet tab
   - Tap "Scan QR Code" button (to be added)
   - Camera opens
   ```

3. **Scan QR Code:**
   ```
   - Point camera at verifier's QR code
   - App extracts openid4vp:// URL
   - Opens URL (handled by walt.id wallet)
   ```

4. **Present Credentials:**
   ```
   - Wallet shows: "Verifier wants to verify your EducationalID"
   - Student approves
   - Credentials are presented
   ```

5. **Verifier Sees Result:**
   ```
   - Verifier app polls for status
   - Status changes to "VERIFIED"
   - Student information is displayed
   ```

## Next Steps

To complete the flow, we need to:

1. ✅ Add QR scanner to student app
2. ✅ Handle `openid4vp://` URLs
3. ✅ Integrate with walt.id wallet for credential presentation

