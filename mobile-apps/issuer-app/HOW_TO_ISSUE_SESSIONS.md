# How to Issue Session Credentials

## Overview

This guide explains how conference organizers can issue session credentials that students can scan to register for conference sessions.

## Step-by-Step Process

### 1. Create a Session (Issuer App)

1. **Open the Issuer App**
2. **Tap the "New Session" button** (orange floating action button)
3. **Fill in the session details:**
   - Conference Name (required)
   - Session Title (required)
   - Description (optional)
   - Location (optional)
   - Start/End Time (optional)
4. **Tap "Create Session"**

### 2. View the QR Code

1. **Tap on any session** from the home screen
2. **The QR code is automatically displayed** in the Session Detail screen
3. The QR code contains the registration URL for that session

### 3. Display QR Code for Students

**Option 1: Display on Screen**
- Open the session detail screen
- Display the QR code on a screen or projector
- Students scan directly from the screen

**Option 2: Print or Export**
- Take a screenshot of the QR code
- Print it and place it at the conference entrance
- Or share it digitally (email, website, etc.)

### 4. Students Scan QR Code

1. **Student opens Student App**
2. **Navigates to QR Scanner** (from wallet or home screen)
3. **Scans the QR code** from the issuer app
4. **Registration happens automatically:**
   - Student sees a confirmation dialog
   - Session credential is issued to their wallet
   - They are registered for that session

## QR Code Location

The QR code is displayed in:
- **Screen:** Session Detail Screen
- **Path:** Tap any session → View QR Code section
- **Features:**
  - Large, scannable QR code
  - Auto-generated when session is created
  - Can be refreshed if needed

## Technical Details

### QR Code Format

The QR code contains a URL like:
```
https://localhost:8086/api/v1/issuer/sessions/{sessionId}/register
```

### Registration Flow

1. Student scans QR code
2. Student app detects session registration URL
3. App calls backend API to register student
4. Backend issues session credential
5. Student receives confirmation
6. Session credential added to wallet

### What Happens When Student Scans

1. **Automatic Detection:** Student app recognizes session registration URL
2. **Session Info:** Fetches session details (title, conference name)
3. **Registration:** Registers student for the session
4. **Credential Issuance:** Backend issues a session-specific credential
5. **Confirmation:** Student sees success dialog with session info
6. **Wallet Update:** Credential appears in student's wallet

## Tips

- **Display QR code prominently** at conference entrance or session room
- **Test scanning** before the event
- **Have backup** - screenshot or print multiple QR codes
- **Refresh QR code** if needed using the refresh button
- **Check registered students** from the session detail screen

## Troubleshooting

### QR Code Not Scanning
- Ensure good lighting
- QR code must be clearly visible
- Check if QR code is properly generated

### Student Not Registered
- Verify student scanned the correct QR code
- Check backend is running
- Verify session is active

### Credential Not Issued
- Check backend logs
- Ensure student has base credentials first
- Verify API endpoints are working

## Next Steps

After students are registered:
- They can view their session credentials in the wallet
- Verifiers can check session credentials at entry points
- View registered students list in the issuer app

