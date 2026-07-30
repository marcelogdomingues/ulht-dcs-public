import 'dart:io';
import 'package:csv/csv.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import 'package:flutter/foundation.dart';
import 'package:intl/intl.dart';
import '../providers/verification_provider.dart';

class ExportService {
  /// Export verification history to CSV
  static Future<void> exportHistoryToCSV(List<VerificationRecord> history) async {
    try {
      // Create CSV data
      final csvData = <List<dynamic>>[
        ['Timestamp', 'Credential Type', 'Status', 'Student ID', 'Given Name', 'Family Name', 'Email'],
      ];

      for (final record in history) {
        String studentId = '';
        String givenName = '';
        String familyName = '';
        String email = '';

        // Extract student data from result
        if (record.result != null) {
          try {
            final credentials = record.result!;
            if (credentials['credentialsByFormat'] != null) {
              final byFormat = credentials['credentialsByFormat'] as Map<String, dynamic>;
              if (byFormat['jwt_vc_json'] != null) {
                final jwtCreds = byFormat['jwt_vc_json'] as List;
                if (jwtCreds.isNotEmpty) {
                  final firstCred = jwtCreds[0] as Map<String, dynamic>;
                  if (firstCred['verifiableCredentials'] != null) {
                    final vcs = firstCred['verifiableCredentials'] as List;
                    if (vcs.isNotEmpty) {
                      final vc = vcs[0] as Map<String, dynamic>;
                      if (vc['payload'] != null) {
                        final payload = vc['payload'] as Map<String, dynamic>;
                        if (payload['vc'] != null) {
                          final vcData = payload['vc'] as Map<String, dynamic>;
                          if (vcData['credentialSubject'] != null) {
                            final subject = vcData['credentialSubject'] as Map<String, dynamic>;
                            studentId = subject['studentId']?.toString() ?? '';
                            givenName = subject['givenName']?.toString() ?? '';
                            familyName = subject['familyName']?.toString() ?? '';
                            email = subject['email']?.toString() ?? '';
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          } catch (e) {
            debugPrint('Error extracting student data: $e');
          }
        }

        csvData.add([
          DateFormat('yyyy-MM-dd HH:mm:ss').format(record.timestamp),
          record.credentialType,
          record.status,
          studentId,
          givenName,
          familyName,
          email,
        ]);
      }

      // Convert to CSV string
      final csvString = Csv().encode(csvData);

      // Get temporary directory
      final directory = await getTemporaryDirectory();
      final file = File('${directory.path}/verification_history_${DateTime.now().millisecondsSinceEpoch}.csv');

      // Write to file
      await file.writeAsString(csvString);

      // Share the file
      await SharePlus.instance.share(
        ShareParams(
          files: [XFile(file.path)],
          text: 'Verification History Export',
          subject: 'Verification History',
        ),
      );
    } catch (e) {
      debugPrint('Error exporting history: $e');
      rethrow;
    }
  }
}

