# ULHT Verifier Mobile App

A Flutter mobile application for bar/office staff to verify student credentials.

## Features

- **Verify Credentials**: Generate verification QR codes for students to scan
- **Select Credential Type**: Choose which credential type to verify (Educational ID, Identity, etc.)
- **Check Status**: Monitor verification status in real-time
- **History**: View past verification records

## Prerequisites

- Flutter SDK (>=3.0.0)
- Dart SDK
- Android Studio / Xcode (for mobile development)
- Backend services running (see main project README)

## Setup

1. **Install Flutter dependencies:**
   ```bash
   cd mobile-apps/verifier-app
   flutter pub get
   ```

2. **Configure API endpoints:**
   The app is configured to connect to:
   - Student Service: `http://localhost:8084/api/v1`
   - Fulfilment Service: `http://localhost:8087/api/v1`
   
   For Android emulator, use `10.0.2.2` instead of `localhost`.
   For iOS simulator, use `localhost` or your machine's IP address.

3. **Update API URLs (if needed):**
   Edit `lib/services/api_service.dart` to change the base URLs.

## Running the App

### Android
```bash
flutter run
```

### iOS
```bash
flutter run
```

### Web (for testing)
```bash
flutter run -d chrome
```

## Usage

1. **Generate Verification QR Code:**
   - Select the credential type you want to verify (e.g., Educational ID)
   - Optionally enter a student ID to verify a specific student
   - Tap "Generate Verification QR"
   - Wait for the QR code to appear

2. **Display QR Code:**
   - The QR code will be displayed on screen
   - Students can scan this QR code with their wallet app
   - The QR code contains a verification URL

3. **Check Verification Status:**
   - Tap "Check Status" to see if the student has presented their credentials
   - The status will show if verification was successful

4. **View History:**
   - Navigate to the History tab to see past verifications
   - Each record shows the credential type, status, and timestamp

## Supported Credential Types

- **EducationalID**: Student enrollment and academic status
- **IdentityCredential**: Digital identity verification
- **EuropeanStudentCard**: European Student Card credential
- **UniversityDegree**: University degree certificate

## Architecture

- **Screens**: UI screens (`verification_screen.dart`, `history_screen.dart`)
- **Services**: API communication (`api_service.dart`)
- **Providers**: State management (`verification_provider.dart`)
- **Main**: App entry point and navigation (`main.dart`)

## Verification Flow

1. Verifier initiates verification request → Backend generates verification URL
2. Backend returns verification URL → App displays QR code
3. Student scans QR code → Student's wallet app presents credentials
4. Backend validates credentials → Returns verification result
5. Verifier checks status → Sees verification result

## Troubleshooting

### Connection Issues
- Ensure backend services are running
- Check API URLs match your backend configuration
- For Android emulator, use `10.0.2.2` instead of `localhost`
- For iOS simulator, ensure you're using the correct IP address

### QR Code Not Generating
- Check backend logs for errors
- Ensure Kafka is running and processing messages
- Verify the verification workflow is properly configured

### Verification Not Completing
- Ensure students have issued credentials first
- Check that students are scanning the QR code correctly
- Verify the verification URL is accessible

## Notes

- This app is designed for staff/bar/office use
- Verification is selective - you choose which credential type to verify
- QR codes can be displayed on screens or printed
- Verification history is stored locally in the app

