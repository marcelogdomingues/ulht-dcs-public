import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter/foundation.dart';

class NotificationService {
  static final NotificationService _instance = NotificationService._internal();
  factory NotificationService() => _instance;
  NotificationService._internal();

  final FlutterLocalNotificationsPlugin _notifications = FlutterLocalNotificationsPlugin();
  bool _initialized = false;

  /// Initialize the notification service
  Future<void> initialize() async {
    if (_initialized) return;

    try {
      const androidSettings = AndroidInitializationSettings('@mipmap/ic_launcher');
      const iosSettings = DarwinInitializationSettings(
        requestAlertPermission: true,
        requestBadgePermission: true,
        requestSoundPermission: true,
      );

      const initSettings = InitializationSettings(
        android: androidSettings,
        iOS: iosSettings,
      );

      await _notifications.initialize(
        settings: initSettings,
        onDidReceiveNotificationResponse: _onNotificationTapped,
      );

      // Request permissions for Android 13+
      if (defaultTargetPlatform == TargetPlatform.android) {
        await _notifications
            .resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()
            ?.requestNotificationsPermission();
      }

      _initialized = true;
    } catch (e) {
      // Handle missing plugin implementation gracefully
      // This can happen if the plugin isn't properly configured for the platform
      debugPrint('Notification service initialization failed: $e');
      debugPrint('Notifications will be disabled. This is normal for web/desktop platforms.');
      _initialized = false;
    }
  }

  /// Handle notification tap
  void _onNotificationTapped(NotificationResponse response) {
    debugPrint('Notification tapped: ${response.payload}');
    // You can navigate to a specific screen here if needed
  }

  /// Show a notification
  Future<void> showNotification({
    required String title,
    required String body,
    String? payload,
    int notificationId = 0,
  }) async {
    if (!_initialized) {
      await initialize();
    }
    
    // If still not initialized after trying, skip showing notification
    if (!_initialized) {
      debugPrint('Notifications not available. Skipping: $title - $body');
      return;
    }

    try {
      const androidDetails = AndroidNotificationDetails(
        'verification_channel',
        'Verification Notifications',
        channelDescription: 'Notifications for credential verification events',
        importance: Importance.high,
        priority: Priority.high,
        showWhen: true,
        enableVibration: true,
        playSound: true,
      );

      const iosDetails = DarwinNotificationDetails(
        presentAlert: true,
        presentBadge: true,
        presentSound: true,
      );

      const details = NotificationDetails(
        android: androidDetails,
        iOS: iosDetails,
      );

      await _notifications.show(
        id: notificationId,
        title: title,
        body: body,
        notificationDetails: details,
        payload: payload,
      );
    } catch (e) {
      // Handle notification errors gracefully
      debugPrint('Failed to show notification: $e');
    }
  }

  /// Show verification success notification
  Future<void> showVerificationSuccess({
    required String studentId,
    required String credentialType,
  }) async {
    await showNotification(
      title: '✓ Verification Successful',
      body: 'Student $studentId verified with $credentialType',
      notificationId: 1,
      payload: 'verification_success',
    );
  }

  /// Show verification failed notification
  Future<void> showVerificationFailed({
    String? reason,
  }) async {
    await showNotification(
      title: '✗ Verification Failed',
      body: reason ?? 'Credential verification failed',
      notificationId: 2,
      payload: 'verification_failed',
    );
  }

  /// Show verification pending notification
  Future<void> showVerificationPending() async {
    await showNotification(
      title: '⏳ Waiting for Verification',
      body: 'QR code generated. Waiting for student to scan...',
      notificationId: 3,
      payload: 'verification_pending',
    );
  }

  /// Cancel all notifications
  Future<void> cancelAll() async {
    await _notifications.cancelAll();
  }
}

