# DCS Issuer App

A Flutter mobile application for conference organizers and event managers to issue session credentials. This app allows you to create conference sessions, generate QR codes for student registration, and track registered students.

## Features

- **Session Management**: Create and manage conference sessions
- **QR Code Generation**: Generate QR codes for each session that students can scan to register
- **Student Registration Tracking**: View all students registered for each session
- **Session Details**: View session information including time, location, and description

## Use Case

This app is designed for scenarios like:
- **Conference Sessions**: Create multiple sessions at a conference
- **Event Registration**: Students scan QR codes to get credentials for specific sessions
- **Access Control**: Conference verifiers can check if students have valid session credentials

## Quick Start

### Prerequisites

- Flutter SDK (>=3.0.0)
- Backend services running (see main project README)
- Credential Service with issuer endpoints (port 8086)

### Running the App

1. **Start Backend Services:**
   ```bash
   # From project root
   docker-compose up -d
   ```

2. **Install Dependencies:**
   ```bash
   cd mobile-apps/issuer-app
   flutter pub get
   ```

3. **Run the App:**
   ```bash
   flutter run
   ```

## Configuration

The app is configured to connect to:
- **Credential Service**: `http://localhost:8086/api/v1`

**Note:** For Android emulator, use `10.0.2.2` instead of `localhost`.

## Workflow

### Creating a Session

1. Open the Issuer App
2. Tap "New Session" (FAB button)
3. Fill in session details:
   - Conference Name (required)
   - Session Title (required)
   - Description (optional)
   - Location (optional)
   - Start/End Time (optional)
4. Tap "Create Session"

### Generating QR Code

1. Open a session from the home screen
2. The QR code is automatically generated when the session is created
3. Tap the refresh button to regenerate if needed
4. Display the QR code to students for scanning

### Viewing Registered Students

1. Open a session from the home screen
2. Tap "Registered Students" card
3. View list of all students who have scanned the QR code and registered

## Architecture

The app follows a clean architecture pattern:

```
lib/
├── main.dart              # App entry point
├── models/               # Data models
│   ├── session.dart
│   └── registered_student.dart
├── screens/              # UI screens
│   ├── home_screen.dart
│   ├── create_session_screen.dart
│   ├── session_detail_screen.dart
│   └── registered_students_screen.dart
├── services/             # API communication
│   └── api_service.dart
└── providers/            # State management
    └── session_provider.dart
```

## API Endpoints

The app communicates with the following backend endpoints:

- `POST /api/v1/issuer/sessions` - Create a new session
- `GET /api/v1/issuer/sessions` - Get all sessions
- `GET /api/v1/issuer/sessions/{id}` - Get a specific session
- `PUT /api/v1/issuer/sessions/{id}` - Update a session
- `DELETE /api/v1/issuer/sessions/{id}` - Delete a session
- `POST /api/v1/issuer/sessions/{id}/qr-code` - Generate/regenerate QR code
- `GET /api/v1/issuer/sessions/{id}/registrations` - Get registered students
- `POST /api/v1/issuer/sessions/{id}/issue` - Issue session credential
- `GET /api/v1/issuer/sessions/{id}/check-registration/{studentId}` - Check registration

## Integration with Student App

1. **Student scans QR code** from the issuer app using the student app
2. **Student app requests credential** for that session
3. **Backend issues session credential** to the student's wallet
4. **Student is registered** for the session

## Integration with Verifier App

1. **Verifier app checks** if a student has a valid session credential
2. **Session credential is verified** along with student's identity
3. **Access granted** if credentials are valid

## Troubleshooting

### Connection Refused

- Ensure backend services are running
- Check API URLs in `lib/services/api_service.dart`
- For Android: use `10.0.2.2` instead of `localhost`

### QR Code Not Generating

- Check if Credential Service is running on port 8086
- Verify issuer endpoints are implemented in the backend
- Check backend logs for errors

### Students Not Appearing

- Verify students have scanned the QR code
- Check if backend is processing registration requests
- Ensure session ID matches in the backend

## Development Notes

- This is a testing/demonstration app
- Real apps would integrate with authentication systems
- QR codes use standard protocols for credential issuance
- Session credentials are W3C Verifiable Credentials

## Next Steps

For production apps, consider:
- User authentication and login
- Session expiration and management
- Bulk registration features
- Export registered students list
- Analytics and reporting
- Push notifications for session updates

