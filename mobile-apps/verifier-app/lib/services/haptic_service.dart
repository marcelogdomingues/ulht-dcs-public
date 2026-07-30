import 'package:vibration/vibration.dart';

class HapticService {
  static final HapticService _instance = HapticService._internal();
  factory HapticService() => _instance;
  HapticService._internal();

  bool _enabled = true;

  bool get enabled => _enabled;
  set enabled(bool value) => _enabled = value;

  /// Light haptic feedback
  Future<void> lightImpact() async {
    if (!_enabled) return;
    if (await Vibration.hasVibrator() ?? false) {
      await Vibration.vibrate(duration: 10);
    }
  }

  /// Medium haptic feedback
  Future<void> mediumImpact() async {
    if (!_enabled) return;
    if (await Vibration.hasVibrator() ?? false) {
      await Vibration.vibrate(duration: 20);
    }
  }

  /// Heavy haptic feedback
  Future<void> heavyImpact() async {
    if (!_enabled) return;
    if (await Vibration.hasVibrator() ?? false) {
      await Vibration.vibrate(duration: 30);
    }
  }

  /// Success pattern
  Future<void> success() async {
    if (!_enabled) return;
    if (await Vibration.hasVibrator() ?? false) {
      await Vibration.vibrate(pattern: [0, 50, 50, 50]);
    }
  }

  /// Error pattern
  Future<void> error() async {
    if (!_enabled) return;
    if (await Vibration.hasVibrator() ?? false) {
      await Vibration.vibrate(pattern: [0, 100, 50, 100]);
    }
  }

  /// Warning pattern
  Future<void> warning() async {
    if (!_enabled) return;
    if (await Vibration.hasVibrator() ?? false) {
      await Vibration.vibrate(pattern: [0, 30, 50, 30]);
    }
  }
}

