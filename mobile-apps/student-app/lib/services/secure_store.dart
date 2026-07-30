import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Small wrapper around flutter_secure_storage for sensitive values
/// (auth tokens, installKey, student PII). Non-sensitive UI preferences
/// should continue to use shared_preferences.
class SecureStore {
  static const FlutterSecureStorage _storage = FlutterSecureStorage(
    aOptions: AndroidOptions(),
  );

  static Future<void> write(String key, String? value) async {
    if (value == null) {
      await _storage.delete(key: key);
      return;
    }
    await _storage.write(key: key, value: value);
  }

  static Future<String?> read(String key) => _storage.read(key: key);

  static Future<void> delete(String key) => _storage.delete(key: key);

  static Future<Map<String, String>> readAll() => _storage.readAll();
}
