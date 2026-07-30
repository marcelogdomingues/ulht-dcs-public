import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'secure_store.dart';

/// Verification history records can contain student PII (name, studentId,
/// full studentData) so the whole history is persisted via flutter_secure_storage
/// instead of shared_preferences.
class VerificationHistoryService {
  static const String _historyKey = 'verification_history';

  /// Save a verification record to history
  Future<void> saveVerification({
    required String credentialType,
    required String credentialId,
    required List<String> disclosedAttributes,
    required DateTime timestamp,
    String? verifierName,
    String? studentName,
    String? studentId,
    Map<String, dynamic>? studentData,
  }) async {
    try {
      final history = await getVerificationHistory();

      final record = {
        'id': DateTime.now().millisecondsSinceEpoch.toString(),
        'credentialType': credentialType,
        'credentialId': credentialId,
        'disclosedAttributes': disclosedAttributes,
        'timestamp': timestamp.toIso8601String(),
        'verifierName': verifierName,
        'studentName': studentName,
        'studentId': studentId,
        'studentData': studentData,
      };

      history.insert(0, record); // Add to beginning (most recent first)

      // Keep only last 100 records
      if (history.length > 100) {
        history.removeRange(100, history.length);
      }

      await SecureStore.write(_historyKey, jsonEncode(history));
    } catch (e) {
      // Silently fail - verification history is not critical.
      // Do not log the record contents (PII).
      debugPrint('Error saving verification history');
    }
  }

  /// Get all verification history
  Future<List<Map<String, dynamic>>> getVerificationHistory() async {
    try {
      final historyJson = await SecureStore.read(_historyKey);

      if (historyJson == null || historyJson.isEmpty) {
        return [];
      }

      final List<dynamic> historyList = jsonDecode(historyJson);
      return historyList.map((item) => item as Map<String, dynamic>).toList();
    } catch (e) {
      debugPrint('Error loading verification history');
      return [];
    }
  }

  /// Clear all verification history
  Future<void> clearHistory() async {
    try {
      await SecureStore.delete(_historyKey);
    } catch (e) {
      debugPrint('Error clearing verification history');
    }
  }

  /// Delete a specific verification record
  Future<void> deleteVerification(String recordId) async {
    try {
      final history = await getVerificationHistory();
      history.removeWhere((record) => record['id'] == recordId);
      await SecureStore.write(_historyKey, jsonEncode(history));
    } catch (e) {
      debugPrint('Error deleting verification record');
    }
  }
}
