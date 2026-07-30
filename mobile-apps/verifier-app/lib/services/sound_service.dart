import 'package:flutter/foundation.dart';

class SoundService {
  static final SoundService _instance = SoundService._internal();
  factory SoundService() => _instance;
  SoundService._internal();

  bool _enabled = true;

  bool get enabled => _enabled;
  set enabled(bool value) => _enabled = value;

  /// Play success sound
  Future<void> playSuccess() async {
    if (!_enabled) return;
    // Sound effects disabled for now to avoid platform compatibility issues
    // In production, add sound files to assets/sounds/ and uncomment
    debugPrint('Success sound (disabled - add sound files to enable)');
  }

  /// Play error sound
  Future<void> playError() async {
    if (!_enabled) return;
    debugPrint('Error sound (disabled - add sound files to enable)');
  }

  /// Play notification sound
  Future<void> playNotification() async {
    if (!_enabled) return;
    debugPrint('Notification sound (disabled - add sound files to enable)');
  }
}

