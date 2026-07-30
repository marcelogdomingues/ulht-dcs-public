# ULHT Mobile Apps

This directory contains three Flutter mobile applications for testing the ULHT Digital Credential System backend.

## Applications

### 1. Student App (`student-app/`)
A mobile app for students to:
- Issue digital credentials
- View and manage their digital wallet
- Display credential QR codes
- View schedules and profile
- **NEW:** Scan QR codes to register for conference sessions

**See [student-app/README.md](student-app/README.md) for details.**

### 2. Verifier App (`verifier-app/`)
A mobile app for bar/office staff to:
- Generate verification QR codes
- Verify student credentials
- View verification history
- **NEW:** Check session credentials for conference access

**See [verifier-app/README.md](verifier-app/README.md) for details.**

### 3. Issuer App (`issuer-app/`) ⭐ NEW
A mobile app for conference organizers and event managers to:
- Create and manage conference sessions
- Generate QR codes for session registration
- Track registered students for each session
- Issue session-specific credentials

**See [issuer-app/README.md](issuer-app/README.md) for details.**

## Quick Start

### Prerequisites
- Flutter SDK (>=3.0.0)
- Backend services running (see main project README)

### Running All Apps

1. **Start Backend Services:**
   ```bash
   # From project root
   docker-compose up -d
   ```

2. **Run Student App:**
   ```bash
   cd student-app
   flutter pub get
   flutter run
   ```

3. **Run Verifier App:**
   ```bash
   cd verifier-app
   flutter pub get
   flutter run
   ```

4. **Run Issuer App:**
   ```bash
   cd issuer-app
   flutter pub get
   flutter run
   ```

## Conference Session Workflow

### Complete Flow Example

1. **Issuer App (Conference Organizer):**
   - Create a conference session (e.g., "Keynote: Future of Technology")
   - Generate a QR code for the session
   - Display QR code at the conference

2. **Student App:**
   - Student scans the QR code with their wallet app
   - Student requests credential for that session
   - Backend issues a session credential to the student's wallet
   - Student is now registered for the session

3. **Verifier App (At Conference Entry):**
   - Verifier scans student's session credential QR code
   - System verifies the student is registered for the session
   - Access granted if credentials are valid

## Configuration

All apps are pre-configured with:
- **Student Number**: `a12345678`
- **Install Key**: `00000_0000000000000`

API endpoints:
- Student Service: `http://localhost:8084/api/v1`
- Credential Service: `http://localhost:8086/api/v1`
- Fulfilment Service: `http://localhost:8087/api/v1`

**Note:** For Android emulator, use `10.0.2.2` instead of `localhost`.

## Architecture

All apps follow a similar structure:
```
lib/
├── main.dart              # App entry point
├── screens/              # UI screens
├── services/             # API communication
├── providers/            # State management
└── models/               # Data models
```

## Troubleshooting

### Common Issues

1. **Connection Refused:**
   - Ensure backend services are running
   - Check API URLs in `lib/services/api_service.dart`
   - For Android: use `10.0.2.2` instead of `localhost`

2. **Credentials Not Issuing:**
   - Check backend logs
   - Ensure Kafka is running
   - Verify student credentials are correct

3. **Verification Not Working:**
   - Ensure credentials are issued first
   - Check verification URL is accessible
   - Verify QR code is scanned correctly

4. **Session Registration Not Working:**
   - Verify issuer endpoints are implemented in the backend
   - Check if Credential Service is running on port 8086
   - Ensure students have valid base credentials first

## Development Notes

- These apps are simulation/testing apps
- They use static student credentials for testing
- Real apps would integrate with authentication systems
- QR codes use standard OpenID4VCI/OID4VP protocols

## Next Steps

For production apps, consider:
- User authentication and login
- Secure credential storage
- Biometric authentication
- Offline support
- Push notifications
- Better error handling
- Analytics and logging
