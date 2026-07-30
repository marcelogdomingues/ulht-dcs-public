# W3C Verifiable Credential Verifier Implementation

## Overview

This implementation provides comprehensive W3C Verifiable Credential verification using the walt.id verifier API and OID4VP (OpenID for Verifiable Presentations) protocol.

## Architecture

### Components

1. **DTOs** (`dto/waltid/verifier/`)
   - Request/Response models for verification
   - Support for custom policies and presentation definitions
   - Relational constraints (subject_is_issuer, is_holder, same_subject)

2. **Client** (`client/WaltidVerifierClient.java`)
   - Feign client for walt.id verifier API
   - Three main endpoints:
     - `POST /openid4vc/verify` - Initiate verification
     - `GET /openid4vc/session/{state}` - Get verification status
     - `GET /openid4vc/session/{id}/presented-credentials` - Inspect credentials

3. **Service** (`service/VerifierService.java`)
   - Business logic for verification operations
   - Error handling and logging
   - Helper methods for common use cases

4. **Controller** (`controller/VerifierController.java`)
   - REST endpoints for verification
   - OpenAPI documentation
   - Exception handling via GlobalExceptionHandler

## API Endpoints

### 1. Initiate Verification

**Endpoint:** `POST /api/v1/verifier/verify`

**Headers:**
- `authorizeBaseUrl` (optional): Base URL for OID4VP authorization (default: "openid4vp://authorize")
- `responseMode` (optional): Response mode (default: "direct_post")
- `successRedirectUri` (optional): Redirect URL on success (can use `$id` placeholder)
- `errorRedirectUri` (optional): Redirect URL on error (can use `$id` placeholder)
- `statusCallbackUri` (optional): URL for status callbacks
- `statusCallbackApiKey` (optional): API key for callback authentication
- `stateId` (optional): Custom state identifier
- `openId4VPProfile` (optional): OID4VP profile (DEFAULT, EBSIV3, ISO_18013_7_MDOC)

**Request Body:**
```json
{
  "vp_policies": ["signature", "expired"],
  "vc_policies": ["signature", "expired", "not-before"],
  "request_credentials": [
    {
      "type": "VerifiableDiploma",
      "format": "jwt_vc_json"
    }
  ]
}
```

**Response:**
```json
{
  "url": "openid4vp://authorize?response_type=vp_token&...",
  "presentationId": "HbIdo1BMxEwf",
  "state": "X31C7TERsFzR"
}
```

### 2. Basic Verification (Simplified)

**Endpoint:** `POST /api/v1/verifier/verify/basic`

**Parameters:**
- `credentialType`: Type of credential to verify (e.g., "VerifiableDiploma")
- `format`: Credential format (default: "jwt_vc_json")

**Response:**
```json
{
  "url": "openid4vp://authorize?...",
  "presentationId": "...",
  "state": "..."
}
```

### 3. Get Verification Status

**Endpoint:** `GET /api/v1/verifier/session/{state}`

**Response:**
```json
{
  "id": "X31C7TERsFzR",
  "verificationResult": true,
  "policyResults": {
    "results": [
      {
        "credential": "VerifiablePresentation",
        "policyResults": [
          {
            "policy": "signature",
            "description": "Checks JWT signature",
            "is_success": true
          }
        ]
      }
    ],
    "policiesRun": 2
  }
}
```

### 4. Get Presented Credentials

**Endpoint:** `GET /api/v1/verifier/session/{sessionId}/credentials`

**Parameters:**
- `viewMode`: "simple" or "verbose" (default: "simple")

**Response (Simple Mode):**
```json
{
  "viewMode": "simple",
  "credentialsByFormat": {
    "jwt_vc_json": [
      {
        "holder": "did:jwk:...",
        "verifiableCredentials": [
          {
            "header": { "kid": "...", "typ": "JWT", "alg": "ES256" },
            "payload": { "iss": "...", "sub": "...", "vc": {...} }
          }
        ]
      }
    ]
  }
}
```

### 5. Check Verification Success

**Endpoint:** `GET /api/v1/verifier/session/{state}/success`

**Response:**
```json
{
  "success": true
}
```

## Usage Examples

### Example 1: Basic Verification

```bash
curl -X POST 'http://localhost:8086/api/v1/verifier/verify/basic' \
  -H 'Content-Type: application/json' \
  -d '{
    "credentialType": "VerifiableDiploma",
    "format": "jwt_vc_json"
  }'
```

### Example 2: Verification with Custom Policies

```bash
curl -X POST 'http://localhost:8086/api/v1/verifier/verify' \
  -H 'Content-Type: application/json' \
  -H 'successRedirectUri: https://example.com/success?id=$id' \
  -H 'errorRedirectUri: https://example.com/error?id=$id' \
  -d '{
    "vp_policies": ["signature", "expired", "not-before"],
    "vc_policies": ["signature", "expired"],
    "request_credentials": [
      {
        "type": "VerifiableDiploma",
        "format": "jwt_vc_json"
      }
    ]
  }'
```

### Example 3: Verification with Per-Credential Policies

```bash
curl -X POST 'http://localhost:8086/api/v1/verifier/verify' \
  -H 'Content-Type: application/json' \
  -d '{
    "vp_policies": ["signature"],
    "vc_policies": ["signature"],
    "request_credentials": [
      {
        "type": "VerifiableId",
        "format": "jwt_vc_json"
      },
      {
        "type": "OpenBadgeCredential",
        "format": "jwt_vc_json",
        "policies": [
          "signature",
          {
            "policy": "webhook",
            "args": "https://example.org/verify"
          }
        ]
      }
    ]
  }'
```

### Example 4: Custom Presentation Definition with Relational Constraints

```bash
curl -X POST 'http://localhost:8086/api/v1/verifier/verify' \
  -H 'Content-Type: application/json' \
  -d '{
    "vp_policies": ["presentation-definition"],
    "request_credentials": [
      {
        "input_descriptor": {
          "id": "OpenBadgeCredential",
          "format": {
            "jwt_vc_json": {
              "alg": ["EdDSA"]
            }
          },
          "constraints": {
            "subject_is_issuer": "required",
            "fields": [
              {
                "path": ["$.vc.type"],
                "filter": {
                  "type": "string",
                  "pattern": "OpenBadgeCredential"
                }
              }
            ]
          }
        }
      }
    ]
  }'
```

### Example 5: Check Verification Status

```bash
# Get the state from the verification initiation response
STATE="X31C7TERsFzR"

# Check status
curl -X GET "http://localhost:8086/api/v1/verifier/session/${STATE}"

# Simple success check
curl -X GET "http://localhost:8086/api/v1/verifier/session/${STATE}/success"

# Get presented credentials
curl -X GET "http://localhost:8086/api/v1/verifier/session/${STATE}/credentials?viewMode=simple"
```

## Supported Verification Policies

### VP Policies (Applied to Verifiable Presentation)
- `signature`: Verify VP signature
- `expired`: Check if VP is expired
- `not-before`: Check if VP is valid yet
- `presentation-definition`: Enforce presentation definition constraints

### VC Policies (Applied to Verifiable Credentials)
- `signature`: Verify VC signature
- `expired`: Check if VC is expired
- `not-before`: Check if VC is valid yet
- `webhook`: Custom webhook validation
  ```json
  {
    "policy": "webhook",
    "args": "https://example.org/verify"
  }
  ```

## Relational Constraints

### subject_is_issuer
Ensures the credential was self-issued (issuer DID matches credential subject).

```json
{
  "constraints": {
    "subject_is_issuer": "required"
  }
}
```

### is_holder
Binds specific fields to the holder's DID.

```json
{
  "constraints": {
    "is_holder": [
      {
        "field_id": ["achname"],
        "directive": "required"
      }
    ],
    "fields": [
      {
        "id": "achname",
        "path": ["$.vc.credentialSubject.achievement.name"]
      }
    ]
  }
}
```

### same_subject
Ensures multiple credentials refer to the same subject.

```json
{
  "constraints": {
    "same_subject": [
      {
        "field_id": ["obc_achievement", "udc_degree"],
        "directive": "required"
      }
    ]
  }
}
```

## Verification Flow

1. **Initiate Verification**
   - Client calls `/verifier/verify` with credential requirements
   - System generates presentation definition
   - Returns URL for wallet to fulfill request

2. **User Presents Credentials**
   - Display URL as QR code or share directly
   - User scans with wallet or opens URL
   - Wallet presents requested credentials

3. **Check Verification Status**
   - Client polls `/verifier/session/{state}`
   - Or receives callback at `statusCallbackUri`
   - Get verification result and policy outcomes

4. **Inspect Credentials (Optional)**
   - Call `/verifier/session/{id}/credentials`
   - View decoded credential data
   - Simple or verbose mode available

## Configuration

### application.yml

```yaml
waltid:
  verifier:
    url: http://localhost:7003
    defaults:
      authorize-base-url: openid4vp://authorize
      response-mode: direct_post
      vp-policies:
        - signature
      vc-policies:
        - signature
        - expired
        - not-before
```

### application-docker.yml

```yaml
waltid:
  verifier:
    url: http://verifier-api:7003
```

## Error Handling

All errors are handled by `GlobalExceptionHandler`:
- `ExternalServiceException`: Issues with walt.id verifier API
- `BadRequestException`: Invalid verification request
- `NotFoundException`: Session not found

Error Response Format:
```json
{
  "errorCode": "CRED-WALTID-001",
  "message": "Error calling WaltID Verifier service",
  "timestamp": "2025-10-09T10:00:00Z",
  "path": "/api/v1/verifier/verify"
}
```

## Integration with walt.id

This implementation is compatible with:
- walt.id Verifier API v7003
- OpenID4VP protocol
- W3C Verifiable Credentials Data Model v1.1 and v2.0
- JWT-VC, SD-JWT-VC, and mDoc formats

## Testing

Test with walt.id web wallet:
1. Initiate verification and get URL
2. Display as QR code or copy URL
3. Open walt.id wallet: https://wallet.demo.walt.id
4. Scan QR or paste URL
5. Select credentials to present
6. Check verification status

## Security Considerations

1. **HTTPS**: Always use HTTPS in production for callbacks and redirects
2. **API Keys**: Protect statusCallbackUri with statusCallbackApiKey
3. **State Validation**: Verify state parameter matches your session
4. **Policy Enforcement**: Always include signature policy as minimum
5. **DID Verification**: Ensure DIDs resolve and keys are valid

## Future Enhancements

- Support for batch verification
- Caching of verification results
- Webhook retry mechanism
- Verification templates/presets
- Integration with credential issuance flow
- Support for selective disclosure
- Advanced analytics and reporting

## References

- [walt.id Verifier API Documentation](https://docs.walt.id/v/web-wallet/verifier-api/overview)
- [OpenID for Verifiable Presentations](https://openid.net/specs/openid-4-verifiable-presentations-1_0.html)
- [W3C Verifiable Credentials](https://www.w3.org/TR/vc-data-model/)
- [Presentation Exchange](https://identity.foundation/presentation-exchange/)


