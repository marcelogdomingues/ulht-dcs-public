# Mock Mode - Issuer App

## Overview

The issuer app now includes **mock mode** functionality, which allows it to work without requiring backend API endpoints to be implemented. This is perfect for development and testing before the backend is ready.

## How It Works

### Automatic Detection

The app automatically detects if the backend is available:

1. **On first API call**, it tries to connect to the backend
2. **If connection succeeds** → Uses real backend API
3. **If connection fails** → Automatically switches to mock mode
4. **Mock mode uses local storage** (SharedPreferences) to persist data

### Default Sessions

When mock mode is initialized for the first time, it automatically creates **3 default sessions**:

1. **Keynote: Future of Technology**
   - Conference: International Tech Conference 2024
   - Location: Main Hall, Room 101
   - 15 registered students

2. **AI and Machine Learning Workshop**
   - Conference: International Tech Conference 2024
   - Location: Workshop Room A
   - 8 registered students

3. **Blockchain Innovation Panel**
   - Conference: International Tech Conference 2024
   - Location: Conference Center, Room 205
   - 22 registered students

## Features in Mock Mode

All functionality works in mock mode:

- ✅ **Create new sessions** - Stored locally
- ✅ **View all sessions** - Loaded from local storage
- ✅ **View session details** - Full information available
- ✅ **Generate QR codes** - Mock QR code URLs generated
- ✅ **View registered students** - Empty by default (can be extended)
- ✅ **Update sessions** - Changes saved locally
- ✅ **Delete sessions** - Removed from local storage

## Data Persistence

- **Sessions** are stored in SharedPreferences under key `issuer_app_sessions`
- **Registrations** are stored under key `issuer_app_registrations`
- Data persists across app restarts
- Data is cleared when app is uninstalled

## Switching to Real Backend

Once backend endpoints are implemented:

1. The app will automatically detect the backend is available
2. It will switch from mock mode to real API calls
3. Existing mock data will remain available but won't be synced
4. You can manually clear SharedPreferences to start fresh

## Benefits

- **Develop frontend independently** - No need to wait for backend
- **Test UI/UX** - See how the app looks and feels
- **Demo ready** - Show the app with sample data
- **Smooth transition** - Automatically switches when backend is ready

## Manual Override

To force mock mode (even if backend is available), you can modify `api_service.dart`:

```dart
static bool _mockMode = true; // Force mock mode
```

To force real backend mode:

```dart
static bool _mockMode = false; // Force backend mode
```

## Troubleshooting

### Sessions not loading

- Check if SharedPreferences is initialized
- Clear app data and restart to regenerate default sessions
- Check console for error messages

### Can't create sessions

- Verify mock mode is enabled
- Check SharedPreferences permissions
- Look for storage errors in console

### Want to reset data

- Clear app data from device settings
- Or delete SharedPreferences keys programmatically

## Future Enhancements

- Add ability to import/export sessions
- Sync mock data with backend when available
- Add more default sample data
- Implement local database for better performance

