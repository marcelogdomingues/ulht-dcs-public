# Verification Flow - Redesigned Architecture

## Overview

The verifier app has been redesigned to work independently of credential issuance. It now uses direct verifier endpoints that work for **any student with credentials**, regardless of when they were issued.

## Architecture

### Old Flow (❌ Removed)
- Verifier app → Student Service → Kafka → Credential Service → Fulfilment Service
- Required workflow tracking and correlation IDs
- Only worked if credentials were just issued
- Complex polling and status checking

### New Flow (✅ Current)
- Verifier app → Credential Service Verifier API (direct)
- Simple, immediate QR code generation
- Works for any student with credentials
- No workflow tracking needed

## API Endpoints

### 1. Initiate Verification
**Endpoint:** `POST http://localhost:8086/api/v1/verifier/verify/basic`

**Parameters:**
- `credentialType`: Type of credential to verify (e.g., "EducationalID")
- `format`: Credential format (default: "jwt_vc_json")

**Response:**
```json
{
  "url": "openid4vp://authorize?response_type=vp_token&...",
  "state": "X31C7TERsFzR",
  "presentationId": "HbIdo1BMxEwf"
}
```

**Returns immediately** - no waiting, no workflow tracking needed!

### 2. Check Verification Status
**Endpoint:** `GET http://localhost:8086/api/v1/verifier/session/{state}`

**Response:**
```json
{
  "id": "X31C7TERsFzR",
  "verificationResult": true,
  "policyResults": {...}
}
```

### 3. Get Presented Credentials
**Endpoint:** `GET http://localhost:8086/api/v1/verifier/session/{state}/credentials?viewMode=simple`

**Response:**
```json
{
  "viewMode": "simple",
  "credentialsByFormat": {
    "jwt_vc_json": [{
      "holder": "did:jwk:...",
      "verifiableCredentials": [{
        "payload": {
          "vc": {
            "credentialSubject": {
              "studentId": "a12345678",
              "givenName": "...",
              "familyName": "..."
            }
          }
        }
      }]
    }]
  }
}
```

## Flow Diagram

```
┌─────────────────┐
│  Verifier App   │
│  (Bar/Event)    │
└────────┬────────┘
         │
         │ 1. POST /verifier/verify/basic
         │    credentialType: "EducationalID"
         │
         ▼
┌─────────────────┐
│ Credential      │
│ Service         │──┐
│ (Port 8086)     │  │ Creates verification session
└────────┬────────┘  │ with walt.id
         │           │
         │ 2. Returns│
         │    URL +  │
         │    state  │
         │           │
         ▼           │
┌─────────────────┐ │
│  QR Code        │ │
│  Displayed      │ │
└────────┬────────┘ │
         │          │
         │ 3. Student│
         │    scans  │
         │    QR     │
         │          │
         ▼          │
┌─────────────────┐ │
│ Student Wallet  │ │
│ App             │ │
└────────┬────────┘ │
         │          │
         │ 4. Presents│
         │    credentials│
         │          │
         ▼          │
┌─────────────────┐ │
│  walt.id        │ │
│  Verifier       │◄┘
│  (Port 7003)    │
└────────┬────────┘
         │
         │ 5. Validates
         │    credentials
         │
         ▼
┌─────────────────┐
│ Verifier App    │
│ Polls status    │
│ GET /session/   │
│ {state}         │
└─────────────────┘
```

## Use Cases

### Use Case 1: School Bar
- Bar staff opens verifier app
- Selects "Educational ID" credential type
- QR code appears immediately
- Student scans QR code with wallet app
- Bar sees: "✓ Verified - Student ID: a12345678"
- Student gets access

### Use Case 2: Conference Registration
- Conference staff opens verifier app
- Selects "Educational ID" credential type
- QR code appears immediately
- Attendee scans QR code
- Conference sees: "✓ Verified - Student is enrolled"
- Attendee gets access

### Use Case 3: Generic Verification
- Any verifier opens app
- Selects credential type
- QR code appears
- Works for **any student** with that credential type
- No need to know student ID beforehand

## Key Benefits

1. **Independent of Issuance**: Works for students who got credentials days/weeks/months ago
2. **Immediate QR Code**: No waiting, no workflow tracking
3. **Simple Architecture**: Direct API calls, no Kafka complexity
4. **Scalable**: Can handle multiple verifiers and multiple students simultaneously
5. **Privacy-Preserving**: Verifier only sees what student presents

## Configuration

### Verifier App API Service
```dart
static const String verifierUrl = 'http://localhost:8086/api/v1/verifier';
```

### Credential Service
- Port: 8086
- Verifier endpoints: `/api/v1/verifier/*`
- Direct integration with walt.id verifier (port 7003)

## Error Handling

- **Network errors**: Clear messages about service connectivity
- **Timeout errors**: Helpful messages about service response times
- **Verification failures**: Shows which policies failed
- **No credentials presented**: Clear "waiting" status

## Testing

1. Start all services (especially Credential Service on 8086)
2. Ensure walt.id verifier is running on port 7003
3. Open verifier app
4. Select credential type (e.g., "EducationalID")
5. QR code should appear immediately
6. Scan with student wallet app
7. Status should update to "VERIFIED" when credentials are presented

