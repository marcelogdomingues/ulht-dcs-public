# How to Test the Verification Flow

## Overview

This guide explains how to test the complete verification flow with both the **Student App** (wallet) and **Verifier App**.

## Prerequisites

### 1. Services Must Be Running

```bash
# Check if services are running
lsof -i :8086 -i :7003 -i :7001 | grep LISTEN
```

**Required services:**
- ✅ **Credential Service** (port 8086) - Handles verification requests
- ✅ **Walt.id Verifier** (port 7003) - Validates credentials
- ✅ **Walt.id Wallet** (port 7001) - Stores and presents credentials

**Start services if needed:**
```bash
# Start infrastructure (Kafka, Consul, etc.)
docker-compose -f docker-compose.infrastructure.yml up -d

# Start walt.id services
cd /path/to/waltid-identity/docker-compose
docker-compose up -d

# Start Credential Service (in IntelliJ or terminal)
cd credential-service
mvn spring-boot:run
```

### 2. Student Must Have Credentials

The student needs to have credentials in their wallet before verification can work.

**To issue credentials:**
1. Open Student App
2. Go to Wallet tab
3. Tap "Issue New Credentials"
4. Wait for credentials to be issued
5. Credentials should appear in the wallet

## Testing Flow

### Step 1: Open Verifier App

1. **Launch the Verifier App** (on a device or simulator)
2. **Select Credential Type:**
   - Choose from dropdown: "EducationalID", "IdentityCredential", etc.
   - Example: Select "EducationalID"
3. **Tap "Generate Verification QR"**
4. **QR Code Appears Immediately:**
   - No waiting, no workflow tracking
   - QR code contains: `openid4vp://authorize?response_type=vp_token&...`

### Step 2: Open Student App

1. **Launch the Student App** (on a different device or simulator)
2. **Go to Wallet Tab**
3. **Tap "Scan QR Code" Button**
   - This opens the camera scanner
   - Camera view appears

### Step 3: Scan the QR Code

1. **Point Student App Camera at Verifier App's QR Code**
   - Make sure both devices are visible
   - Or use a screenshot/display method
2. **App Detects QR Code:**
   - Extracts the `openid4vp://` URL
   - Opens the URL using `url_launcher`
3. **Walt.id Wallet Handles the Request:**
   - Wallet service receives the OID4VP request
   - Shows student what credentials are being requested
   - Student approves/rejects

### Step 4: Present Credentials

1. **Wallet Presents Credentials:**
   - Credentials are sent to walt.id verifier
   - Verifier validates signatures, expiration, etc.
2. **Verifier App Polls for Status:**
   - App automatically polls `/verifier/session/{state}`
   - Status updates from "PENDING" → "VERIFIED"

### Step 5: See Verification Result

1. **Verifier App Shows Result:**
   - Status: "VERIFIED" (green checkmark)
   - Student information displayed:
     - Student ID
     - Name
     - Email
     - Credential Type
2. **Verification Complete!**

## Expected Behavior

### ✅ Success Flow

```
Verifier App:
1. Select "EducationalID" → QR code appears
2. Status: "Waiting for credentials..."
3. Student scans QR code
4. Status: "VERIFIED" ✓
5. Student info displayed

Student App:
1. Tap "Scan QR Code"
2. Camera opens
3. Scan verifier's QR code
4. URL opens (handled by walt.id wallet)
5. Credentials presented
```

### ❌ Common Issues

**Issue: QR code not appearing**
- **Check:** Credential Service (8086) is running
- **Check:** Walt.id Verifier (7003) is running
- **Fix:** Restart services

**Issue: "Network error" in verifier app**
- **Check:** API service URL is correct (`http://localhost:8086/api/v1/verifier`)
- **Check:** Services are accessible
- **Fix:** Update API service base URL if needed

**Issue: Student can't scan QR code**
- **Check:** Camera permissions granted
- **Check:** `mobile_scanner` package installed
- **Fix:** Run `flutter pub get` in student-app directory

**Issue: "Verification failed"**
- **Check:** Student has the requested credential type
- **Check:** Credential is not expired
- **Check:** Credential signature is valid
- **Fix:** Issue new credentials if needed

**Issue: URL doesn't open**
- **Check:** `url_launcher` package installed
- **Check:** Walt.id Wallet (7001) is running
- **Fix:** Ensure wallet service is accessible

## Testing with Two Devices

### Option 1: Two Physical Devices
1. **Device 1:** Run Verifier App
2. **Device 2:** Run Student App
3. Point Device 2 camera at Device 1 screen

### Option 2: Simulator + Physical Device
1. **Simulator:** Run Verifier App
2. **Physical Device:** Run Student App
3. Point physical device camera at simulator screen

### Option 3: Screenshot Method
1. **Device 1:** Run Verifier App, take screenshot of QR code
2. **Device 2:** Run Student App, scan screenshot
3. Works if QR code is clear enough

## Troubleshooting

### Check Service Health

```bash
# Credential Service
curl http://localhost:8086/api/v1/actuator/health

# Walt.id Verifier
curl http://localhost:7003/health

# Walt.id Wallet
curl http://localhost:7001/health
```

### Check Logs

**Credential Service logs:**
- Look for verification initiation
- Check for errors with walt.id verifier

**Verifier App logs:**
- Check polling status
- Check network errors

**Student App logs:**
- Check QR code detection
- Check URL launching

### Test API Directly

```bash
# Test verification initiation
curl -X POST "http://localhost:8086/api/v1/verifier/verify/basic?credentialType=EducationalID&format=jwt_vc_json"

# Response should include:
# {
#   "url": "openid4vp://authorize?...",
#   "state": "...",
#   "presentationId": "..."
# }
```

## Next Steps

Once basic flow works:
1. Test with different credential types
2. Test with expired credentials (should fail)
3. Test with invalid credentials (should fail)
4. Test multiple students simultaneously
5. Test verifier rejecting credentials

