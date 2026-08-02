# DCS Student Mobile App

A Flutter mobile application for students to manage their digital credentials and wallet.

## Features

- **Wallet Tab**: View and manage digital credentials
- **Issue Credentials**: Request new credentials from the backend
- **View Credentials**: Display issued credentials with QR codes
- **Schedules**: View class schedules (mock data)
- **Profile**: View student profile information

## Prerequisites

- Flutter SDK (>=3.0.0)
- Dart SDK
- Android Studio / Xcode (for mobile development)
- Backend services running (see main project README)

## Setup

1. **Install Flutter dependencies:**
   ```bash
   cd mobile-apps/student-app
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

## Static Configuration

The app is pre-configured with:
- **Student Number**: `a12345678`
- **Install Key**: `00000_0000000000000`

These values are hardcoded in `lib/services/api_service.dart` for testing purposes.

## Usage

1. **Issue Credentials:**
   - Navigate to the Wallet tab
   - Tap "Issue New Credentials"
   - Wait for the credentials to be issued (progress is shown)
   - Once completed, your digital cards will appear

2. **View Credentials:**
   - In the Wallet tab, you'll see all your issued credentials
   - Tap "Show QR Code" to display the credential QR code
   - Tap "Open" to open the credential URL in a browser

3. **Schedules:**
   - Navigate to the Schedules tab to view class schedules (mock data)

4. **Profile:**
   - Navigate to the Profile tab to view student information

## Architecture

- **Screens**: UI screens (`home_screen.dart`, `wallet_screen.dart`, etc.)
- **Services**: API communication (`api_service.dart`)
- **Providers**: State management (`credential_provider.dart`)
- **Main**: App entry point and navigation (`main.dart`)

## Troubleshooting

### Connection Issues
- Ensure backend services are running
- Check API URLs match your backend configuration
- For Android emulator, use `10.0.2.2` instead of `localhost`
- For iOS simulator, ensure you're using the correct IP address

### Credentials Not Appearing
- Check backend logs for errors
- Ensure Kafka is running and processing messages
- Verify the correlation ID is being tracked correctly

## Notes

- This is a simulation app for testing the backend credential system
- Credentials are issued asynchronously via Kafka
- The app polls for status updates every 2 seconds
- QR codes can be scanned by verifier apps or wallet applications

