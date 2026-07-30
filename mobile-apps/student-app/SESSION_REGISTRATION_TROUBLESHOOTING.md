# Session Registration Troubleshooting

## Common Error: "Registration Failed"

If you're getting a "Failed to register for session" error, this is likely because **the backend API endpoints for session registration are not yet implemented**.

## Why This Happens

The issuer app works in **mock mode** (using local storage), but the student app needs to communicate with **real backend endpoints** to register students for sessions. These endpoints need to be implemented in the credential service.

## Current Status

✅ **Working:**
- Issuer app can create sessions (mock mode)
- QR codes are generated and displayed
- Student app can scan QR codes
- QR code detection works

❌ **Not Working Yet:**
- Backend API endpoints for session registration
- Actual credential issuance for sessions
- Student registration tracking

## Error Messages Explained

### "Backend Not Available" or "Connection Error"

This means:
- The credential service is not running, OR
- The issuer endpoints (`/api/v1/issuer/sessions/*`) are not implemented yet

**Solution:** Implement the backend endpoints as described in `issuer-app/IMPLEMENTATION_GUIDE.md`

### "Invalid session URL format"

This means the QR code URL format is incorrect.

**Solution:** Check that the issuer app is generating QR codes correctly.

## What You Can Test Now

Even without backend endpoints, you can test:

1. ✅ **Issuer App:**
   - Create sessions
   - View QR codes
   - See session details

2. ✅ **Student App:**
   - Scan QR codes
   - See error messages (informative)
   - UI flow works

## Next Steps to Enable Full Functionality

To make session registration work, you need to:

1. **Implement Backend Endpoints** (see `issuer-app/IMPLEMENTATION_GUIDE.md`):
   - `POST /api/v1/issuer/sessions` - Create session
   - `GET /api/v1/issuer/sessions` - List sessions
   - `GET /api/v1/issuer/sessions/{id}` - Get session
   - `POST /api/v1/issuer/sessions/{id}/issue` - Issue session credential
   - `GET /api/v1/issuer/sessions/{id}/registrations` - Get registered students

2. **Start Backend Services:**
   ```bash
   docker-compose up -d
   ```

3. **Test Registration:**
   - Create session in issuer app
   - Scan QR code in student app
   - Should register successfully

## Expected Behavior After Backend is Ready

Once backend endpoints are implemented:

1. Student scans QR code
2. Student app calls `/api/v1/issuer/sessions/{id}/issue`
3. Backend issues session credential
4. Student sees success message
5. Credential appears in wallet
6. Student appears in registered students list

## Temporary Workaround (Testing UI Only)

For now, the error dialog will show a clear message explaining that the backend isn't available. This is expected behavior during development.

The app is designed to:
- Show informative error messages
- Guide users on what's needed
- Work seamlessly once backend is ready

## Getting Help

If you're still seeing issues after implementing backend endpoints:

1. Check backend logs
2. Verify endpoints are accessible
3. Check network connectivity
4. Verify QR code URL format
5. Check session ID is correct

