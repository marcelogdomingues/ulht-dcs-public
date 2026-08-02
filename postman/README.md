# Postman Collections

## 🎯 Single Unified Collection

**Import ONLY these 2 files:**
1. ✅ **Collection**: `DCS-Unified.postman_collection.json`
2. ✅ **Environment**: `DCS-Unified.postman_environment.json`

---

## 🚀 Quick Start

### 1. Import Files

Import these TWO files into Postman:

1. **Collection**: `DCS-Unified.postman_collection.json`
2. **Environment**: `DCS-Unified.postman_environment.json`

### 2. Select Environment

In Postman, select **"DCS Digital Credentials - Unified"** from the environment dropdown (top right).

### 3. Run Your First Workflow

Navigate to: **🚀 Complete Workflows → 📝 Issuance Workflow → Run in order:**

1. **1. Issue Credentials** → Click Send
2. Wait 10-15 seconds ⏳
3. **3. Get Credentials** → Click Send

Done! ✅ You've just issued W3C Verifiable Credentials!

---

## 📂 Collection Structure

The unified collection includes:

- 🚀 **Complete Workflows** - Issuance, Verification, and Complete Flow
- 📊 **Monitoring & Tracking** - Real-time progress tracking
- 🎫 **Direct Credential APIs** - Direct service calls
- 🔐 **Direct Verifier APIs** - Advanced verification
- 🏥 **Health Checks** - Service health monitoring
- 💼 **Wallet Operations** - Wallet management (Kafka integrated)
- 🎓 **W3C Credential Issuance** - Manual credential issuance
- 🔄 **Credential Issuance Flow** - Kafka-based workflow
- 🛠️ **Flow Utilities** - Workflow management

---

## 📋 Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `student_url` | http://localhost:8084/api/v1 | Student service |
| `sis_url` | http://localhost:8085/api/v1 | Sis service |
| `credential_url` | http://localhost:8086/api/v1 | Credential service |
| `fulfilment_url` | http://localhost:8087/api/v1 | Fulfilment service |
| `waltid_verifier_url` | http://localhost:7003 | WaltID Verifier |
| `waltid_issuer_url` | http://localhost:7002 | WaltID Issuer |
| `waltid_wallet_url` | http://localhost:7001 | WaltID Wallet |
| `student_username` | a12345678 | Test username |
| `student_install_key` | 00000_0000000000000 | Test install key |

Auto-managed variables (set by test scripts):
- `correlationId` - Workflow tracking ID
- `verificationCorrelationId` - Verification tracking ID
- `verificationState` - Verification state
- `walletSessionCookie` - Wallet session
- `walletId` - Wallet identifier
- `issuerDid` - Issuer DID
- `issuerKeyJson` - Issuer key

---

## ✅ What's Included

**All previous collections have been merged into this single unified collection:**

- ✅ Complete workflows (from DCS-Digital-Credentials-Complete)
- ✅ Verification workflows (from Verification-Workflow)
- ✅ Credential service APIs (from Credential-Service-Postman-Collection)
- ✅ Verifier APIs (from Credential-Verifier)
- ✅ W3C VC issuance (from Credential-Service-W3C-VC)
- ✅ Wallet operations with Kafka integration
- ✅ Health checks
- ✅ Direct API endpoints

**One collection, everything you need!** 🎉

---

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

**Last Updated**: January 2025  
**Version**: 2.0.0 - Unified Collection  
**Status**: ✅ Production Ready
