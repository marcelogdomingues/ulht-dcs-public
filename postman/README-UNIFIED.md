# ULHT Digital Credentials - Unified Postman Collection

## 📚 Overview

**ONE collection, ONE environment** - everything you need to test the ULHT Digital Credential System.

## 🚀 Quick Start

### 1. Import Files

Import these TWO files into Postman:

1. **Collection**: `ULHT-Unified.postman_collection.json`
2. **Environment**: `ULHT-Unified.postman_environment.json`

### 2. Select Environment

In Postman, select **"ULHT Digital Credentials - Unified"** from the environment dropdown (top right).

### 3. Run Your First Workflow

Navigate to: **🚀 Complete Workflows → 📝 Issuance Workflow → Run in order:**

1. **1. Issue Credentials** → Click Send
2. Wait 10-15 seconds ⏳
3. **3. Get Credentials** → Click Send

Done! ✅ You've just issued W3C Verifiable Credentials!

## 📂 Collection Structure

```
ULHT Digital Credentials - Unified Collection
│
├── 🚀 Complete Workflows (RECOMMENDED - Start here!)
│   ├── 📝 Issuance Workflow (Issue credentials)
│   ├── 🔍 Verification Workflow (Verify credentials)
│   └── 🔄 Issue → Verify Flow (Complete cycle)
│
├── 📊 Monitoring & Tracking
│   ├── Track Workflow (SSE)
│   ├── Get Workflow Status
│   └── Get Workflow Result
│
├── 🎫 Direct Credential APIs
│   └── (Bypass workflow, direct service calls)
│
├── 🔐 Direct Verifier APIs
│   └── (Bypass workflow, direct verifier calls)
│
└── 🏥 Health Checks
    └── (Check all services)
```

## 🎯 Common Workflows

### Workflow 1: Issue Credentials (Most Common)

**Folder**: 🚀 Complete Workflows → 📝 Issuance Workflow

1. **Issue Credentials** → Returns `correlationId`
2. Wait 10-15 seconds
3. **Get Credentials** → Returns credential URLs

**Use Case**: Student requests their digital credentials

---

### Workflow 2: Verify Credentials

**Folder**: 🚀 Complete Workflows → 🔍 Verification Workflow

1. **Initiate Verification** → Returns `correlationId`
2. Wait 2-3 seconds
3. **Get Verification URL** → Returns QR code URL
4. User scans QR code with wallet
5. **Check Verification Status** → See if credentials were presented

**Use Case**: Verifier wants to check student's credentials

---

### Workflow 3: Complete Cycle (Issue + Verify)

**Folder**: 🚀 Complete Workflows → 🔄 Issue → Verify Flow

Run all 4 steps in order - complete end-to-end test!

**Use Case**: Testing the complete system

---

## 🔧 Environment Variables

All variables are pre-configured! Just select the environment.

| Variable | Default Value | Description |
|----------|---------------|-------------|
| `student_url` | http://localhost:8084/api/v1 | Student Service |
| `lusofona_url` | http://localhost:8085/api/v1 | Lusofona Service |
| `credential_url` | http://localhost:8086/api/v1 | Credential Service |
| `fulfilment_url` | http://localhost:8087/api/v1 | Fulfilment Service |
| `waltid_verifier_url` | http://localhost:7003 | walt.id Verifier |
| `waltid_issuer_url` | http://localhost:7002 | walt.id Issuer |
| `waltid_wallet_url` | http://localhost:7001 | walt.id Wallet |
| `student_username` | a12345678 | Test student username |
| `student_install_key` | 00000_0000000000000 | Test install key |
| `correlationId` | (auto-filled) | Issuance workflow ID |
| `verificationCorrelationId` | (auto-filled) | Verification workflow ID |
| `verificationState` | (auto-filled) | walt.id verification state |

**Note**: Variables marked "auto-filled" are set automatically by test scripts.

## 📖 Service Ports Reference

| Service | Port | URL |
|---------|------|-----|
| Student Service | 8084 | http://localhost:8084/api/v1 |
| Lusofona Service | 8085 | http://localhost:8085/api/v1 |
| Credential Service | 8086 | http://localhost:8086/api/v1 |
| Fulfilment Service | 8087 | http://localhost:8087/api/v1 |
| walt.id Wallet | 7001 | http://localhost:7001 |
| walt.id Issuer | 7002 | http://localhost:7002 |
| walt.id Verifier | 7003 | http://localhost:7003 |

## 🎬 What Happens Behind the Scenes

### Issuance Workflow

```
Student App → Student Service (8084)
                    ↓
                  Kafka
                    ↓
            Lusofona Service (8085) ← Fetches student data
                    ↓
                  Kafka
                    ↓
          Credential Service (8086) ← Issues W3C credentials
                    ↓
          walt.id Issuer (7002)
                    ↓
                  Kafka
                    ↓
         Fulfilment Service (8087) ← Tracks & stores results
```

### Verification Workflow

```
Verifier App → Student Service (8084)
                    ↓
                  Kafka
                    ↓
          Credential Service (8086) ← Initiates verification
                    ↓
          walt.id Verifier (7003) ← Creates verification URL
                    ↓
                  Kafka
                    ↓
         Fulfilment Service (8087) ← Tracks results
```

## ⚡ Tips & Tricks

### Tip 1: Auto-Variable Extraction

The collection uses test scripts to automatically extract and save:
- `correlationId` from issuance responses
- `verificationCorrelationId` from verification responses
- `verificationState` from walt.id responses

**No manual copying needed!** ✨

### Tip 2: Check Console Output

Open Postman Console (View → Show Postman Console) to see helpful messages:
```
✅ Credential issuance initiated!
CorrelationId: 550e8400-e29b-41d4-a716-446655440000
⏳ Wait 10-15 seconds, then run step 2
```

### Tip 3: Real-Time Tracking

Use the **Track Workflow (SSE)** request to see real-time progress updates!

### Tip 4: Health Checks First

Before running workflows, check all services are up:
**🏥 Health Checks** folder → Run all 3 health checks

## 🐛 Troubleshooting

### ❌ Services Not Responding

**Check if services are running:**
```bash
docker-compose ps
```

**Restart services:**
```bash
docker-compose restart
```

### ❌ Variables Not Saving

1. Make sure environment is selected (dropdown, top right)
2. Check Postman Console for errors
3. Try manually setting a variable to test

### ❌ Workflow Takes Too Long

**Normal timings:**
- Issuance: 10-15 seconds (fetches real student data)
- Verification: 2-3 seconds (just creates URL)

If longer, check service logs:
```bash
docker-compose logs credential-service
docker-compose logs fulfilment-service
```

### ❌ Getting 404 Errors

**Check service ports:**
- Student: 8084 ✓
- Lusofona: 8085 ✓
- Credential: 8086 ✓
- Fulfilment: 8087 ✓

**Verify URLs include `/api/v1`:**
- ✅ http://localhost:8084/api/v1/student/issue
- ❌ http://localhost:8084/student/issue

## 📊 Expected Responses

### Issuance Success

```json
{
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "userId": "a12345678",
  "studentId": "511114706",
  "credentialOfferUrls": [
    "openid-credential-offer://?credential_offer=...",
    "openid-credential-offer://?credential_offer=...",
    "openid-credential-offer://?credential_offer=..."
  ],
  "credentialTypes": [
    "EducationalID",
    "IdentityCredential",
    "EuropeanStudentCard"
  ],
  "credentialsIssued": 3
}
```

### Verification Success

```json
{
  "correlationId": "660e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "result": {
    "verificationUrl": "openid4vp://authorize?...",
    "presentationId": "HbIdo1BMxEwf",
    "state": "X31C7TERsFzR",
    "credentialType": "VerifiableDiploma",
    "format": "jwt_vc_json"
  }
}
```

## 🗑️ Old Collections (Can Be Deleted)

You can safely **DELETE** these old collections:

- ❌ Credential-Service-W3C-VC.postman_collection.json
- ❌ Credential-Verifier.postman_collection.json
- ❌ ULHT-Digital-Credential-Flow.postman_collection.json
- ❌ ULHT-Digital-Credentials-Complete.postman_collection.json
- ❌ Verification-Workflow.postman_collection.json

And these old environments:

- ❌ Credential-Service-Environment.postman_environment.json
- ❌ ULHT-Digital-Credential-Environment.postman_environment.json
- ❌ ULHT-Digital-Credentials.postman_environment.json

**Keep ONLY:**
- ✅ ULHT-Unified.postman_collection.json
- ✅ ULHT-Unified.postman_environment.json

## 🎓 Learning Path

**New to the system?** Follow this order:

1. ✅ **Start Here**: Run Health Checks
2. ✅ **Learn Issuance**: Run Issuance Workflow
3. ✅ **Learn Verification**: Run Verification Workflow
4. ✅ **Complete Test**: Run Issue → Verify Flow
5. ✅ **Explore**: Try Direct APIs
6. ✅ **Monitor**: Use SSE Tracking

## 📞 Support

**Issues?**
- Check docker-compose logs
- Verify all services are running
- Ensure correct ports (8084-8087, 7001-7003)
- Check Postman Console for errors

**Questions?**
- See main project README.md
- Check service-specific READMEs
- Review API documentation at `/swagger-ui.html`

---

**Last Updated**: October 9, 2025  
**Version**: 1.0.0 - Unified Collection  
**Status**: ✅ Production Ready


