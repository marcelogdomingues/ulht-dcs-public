import 'package:flutter/foundation.dart';
import 'dart:developer' as developer;
import '../services/api_service.dart';
import '../services/notification_service.dart';
import '../services/haptic_service.dart';
import '../services/sound_service.dart';

// Helper functions for logging
void _logInfo(String message) => developer.log(message, name: 'VerificationProvider');
void _logWarning(String message) => developer.log(message, name: 'VerificationProvider', level: 900);

class VerificationRecord {
  final String correlationId;
  final String credentialType;
  final String status;
  final DateTime timestamp;
  final Map<String, dynamic>? result;
  final List<String>? disclosedAttributes; // Attributes that were disclosed

  VerificationRecord({
    required this.correlationId,
    required this.credentialType,
    required this.status,
    required this.timestamp,
    this.result,
    this.disclosedAttributes,
  });
}

class VerificationProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  final List<VerificationRecord> _history = [];

  String? _currentState; // Verification session state (from walt.id)
  String? _currentStatus; // Verification status (PENDING, VERIFIED, FAILED)
  String? _currentCredentialType;
  Map<String, dynamic>? _currentResult; // Presented credentials data
  bool _isLoading = false;
  String? _error;
  String? _verificationUrl; // QR code URL
  bool _isPolling = false;

  List<VerificationRecord> get history => _history;
  String? get currentState => _currentState;
  String? get currentStatus => _currentStatus;
  String? get currentCredentialType => _currentCredentialType;
  Map<String, dynamic>? get currentResult => _currentResult;
  bool get isLoading => _isLoading;
  String? get error => _error;
  String? get verificationUrl => _verificationUrl;
  bool get isPolling => _isPolling;

  /// Initiates a verification session and immediately gets the QR code URL
  /// This works for ANY student with credentials - no correlation with issuance
  Future<void> initiateVerification({
    required String credentialType,
    String? userId, // Optional - not used currently but kept for future
  }) async {
    _isLoading = true;
    _error = null;
    _currentStatus = null;
    _currentResult = null;
    _verificationUrl = null;
    _currentState = null;
    _currentCredentialType = credentialType;
    notifyListeners();

    try {
      // Call verifier endpoint directly - returns URL immediately
      final response = await _apiService.initiateVerification(
        credentialType: credentialType,
        userId: userId,
      );
      
      // Response contains: url, state, presentationId
      _verificationUrl = response['url']?.toString();
      _currentState = response['state']?.toString();
      _currentStatus = 'PENDING'; // Waiting for student to present credentials
      _isLoading = false;
      
      _logInfo('Verification session created: state=$_currentState, url=$_verificationUrl');
      notifyListeners();

      // Start polling for credential presentation
      if (_currentState != null) {
        _pollForCredentialPresentation();
      }
    } catch (e) {
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Polls for credential presentation - checks if student has scanned QR and presented credentials
  Future<void> _pollForCredentialPresentation() async {
    if (_currentState == null) return;

    _isPolling = true;
    int attempts = 0;
    const maxAttempts = 150; // 5 minutes total (150 * 2 seconds)
    String? lastError;

    while (attempts < maxAttempts && _isPolling) {
      await Future.delayed(const Duration(seconds: 2));
      attempts++;

      try {
        // Check verification status
        final statusResult = await _apiService.getVerificationStatus(_currentState!);
        
        // Check if verification was successful
        final verificationResult = statusResult['verificationResult'] as bool?;
        
        if (verificationResult == true) {
          // Credentials were presented and verified successfully!
          _currentStatus = 'VERIFIED';
          
          // Haptic feedback
          HapticService().success();
          // Sound feedback
          SoundService().playSuccess();
          
          // Get the presented credentials data
          String? studentId;
          try {
            final credentials = await _apiService.getPresentedCredentials(_currentState!);
            _currentResult = credentials;
            
            // Extract student ID for notification
            try {
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
                              studentId = subject['studentId']?.toString();
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            } catch (e) {
              _logWarning('Could not extract student ID: $e');
            }
            
            // Show success notification
            await NotificationService().showVerificationSuccess(
              studentId: studentId ?? 'Unknown',
              credentialType: _currentCredentialType ?? 'Unknown',
            );
            
            // Extract disclosed attributes from credentialSubject
            List<String> disclosedAttributes = [];
            try {
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
                              // Extract all attributes from credentialSubject (these are the disclosed ones)
                              subject.forEach((key, value) {
                                // Skip internal fields
                                if (key != 'id' && !key.startsWith('_')) {
                                  disclosedAttributes.add(key);
                                }
                              });
                            }
                            // Also check for issuanceDate at top level
                            if (vcData['issuanceDate'] != null) {
                              disclosedAttributes.add('issuanceDate');
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            } catch (e) {
              _logWarning('Could not extract disclosed attributes: $e');
            }
            
            // Add to history if not already there
            if (!_history.any((r) => r.correlationId == _currentState)) {
              _history.insert(0, VerificationRecord(
                correlationId: _currentState!,
                credentialType: _currentCredentialType ?? 'Unknown',
                status: 'VERIFIED',
                timestamp: DateTime.now(),
                result: _currentResult,
                disclosedAttributes: disclosedAttributes.isNotEmpty ? disclosedAttributes : null,
              ));
              _logInfo('Added verification to history: ${_currentState}');
            }
          } catch (e) {
            _logWarning('Failed to get presented credentials: $e');
            // Still mark as verified and add to history even if we can't get credential details
            if (!_history.any((r) => r.correlationId == _currentState)) {
              _history.insert(0, VerificationRecord(
                correlationId: _currentState!,
                credentialType: _currentCredentialType ?? 'Unknown',
                status: 'VERIFIED',
                timestamp: DateTime.now(),
                result: null,
              ));
            }
            // Show notification even without details
            await NotificationService().showVerificationSuccess(
              studentId: 'Unknown',
              credentialType: _currentCredentialType ?? 'Unknown',
            );
          }
          
          _isPolling = false;
          notifyListeners();
          return;
        } else if (verificationResult == false) {
          // Verification failed (policies didn't pass)
          _currentStatus = 'FAILED';
          _error = 'Credential verification failed. Policies did not pass.';
          
          // Haptic feedback
          HapticService().error();
          // Sound feedback
          SoundService().playError();
          
          // Show failure notification
          await NotificationService().showVerificationFailed(
            reason: 'Policies did not pass',
          );
          
          // Add failed verification to history
          if (!_history.any((r) => r.correlationId == _currentState)) {
            _history.insert(0, VerificationRecord(
              correlationId: _currentState!,
              credentialType: _currentCredentialType ?? 'Unknown',
              status: 'FAILED',
              timestamp: DateTime.now(),
              result: null,
              disclosedAttributes: null,
            ));
          }
          
          _isPolling = false;
          notifyListeners();
          return;
        }
        
        // Still pending - continue polling
        _currentStatus = 'PENDING';
        lastError = null;
        notifyListeners();
      } catch (e) {
        lastError = e.toString();
        // Continue polling unless we've exhausted attempts
        if (attempts >= maxAttempts) {
          _error = 'Timeout waiting for credential presentation after ${maxAttempts * 2} seconds. '
                   'Student may not have scanned the QR code yet. '
                   'Last error: ${lastError ?? "Unknown error"}';
          _isPolling = false;
          notifyListeners();
          return;
        }
      }
    }

    // If we exit the loop without completing
    if (_isPolling) {
      _error = 'Stopped polling after ${maxAttempts * 2} seconds. '
               'Student may still present credentials - use "Check Status" to verify.';
      _isPolling = false;
      notifyListeners();
    }
  }

  /// Manually check verification status (useful if polling stopped)
  Future<void> checkVerificationStatus() async {
    if (_currentState == null) return;

    try {
      final statusResponse = await _apiService.getVerificationStatus(_currentState!);
      final verificationResult = statusResponse['verificationResult'] as bool?;
      
      if (verificationResult == true) {
        _currentStatus = 'VERIFIED';
        HapticService().success();
        SoundService().playSuccess();
        
        // Get presented credentials
        try {
          final credentials = await _apiService.getPresentedCredentials(_currentState!);
          _currentResult = credentials;
          
          // Extract student ID for notification
          String? studentId;
          try {
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
                            studentId = subject['studentId']?.toString();
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          } catch (e) {
            _logWarning('Could not extract student ID: $e');
          }
          
          // Extract disclosed attributes from credentialSubject
          List<String> disclosedAttributes = [];
          try {
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
                            // Extract all attributes from credentialSubject (these are the disclosed ones)
                            subject.forEach((key, value) {
                              // Skip internal fields
                              if (key != 'id' && !key.startsWith('_')) {
                                disclosedAttributes.add(key);
                              }
                            });
                          }
                          // Also check for issuanceDate at top level
                          if (vcData['issuanceDate'] != null) {
                            disclosedAttributes.add('issuanceDate');
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          } catch (e) {
            _logWarning('Could not extract disclosed attributes: $e');
          }
          
          await NotificationService().showVerificationSuccess(
            studentId: studentId ?? 'Unknown',
            credentialType: _currentCredentialType ?? 'Unknown',
          );
          
          // Add to history if not already there
          if (!_history.any((r) => r.correlationId == _currentState)) {
            _history.insert(0, VerificationRecord(
              correlationId: _currentState!,
              credentialType: _currentCredentialType ?? 'Unknown',
              status: 'VERIFIED',
              timestamp: DateTime.now(),
              result: _currentResult,
              disclosedAttributes: disclosedAttributes.isNotEmpty ? disclosedAttributes : null,
            ));
          }
        } catch (e) {
          _logWarning('Failed to get presented credentials: $e');
        }
      } else if (verificationResult == false) {
        _currentStatus = 'FAILED';
        _error = 'Credential verification failed.';
        HapticService().error();
        SoundService().playError();
        
        await NotificationService().showVerificationFailed();
        
        // Add failed verification to history if not already there
        if (!_history.any((r) => r.correlationId == _currentState)) {
          _history.insert(0, VerificationRecord(
            correlationId: _currentState!,
            credentialType: _currentCredentialType ?? 'Unknown',
            status: 'FAILED',
            timestamp: DateTime.now(),
            result: null,
            disclosedAttributes: null,
          ));
        }
      } else {
        _currentStatus = 'PENDING';
      }
      
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }

  /// Stop polling for credential presentation
  void stopPolling() {
    _isPolling = false;
    notifyListeners();
  }

  void reset() {
    _currentState = null;
    _currentStatus = null;
    _currentCredentialType = null;
    _currentResult = null;
    _isLoading = false;
    _error = null;
    _verificationUrl = null;
    _isPolling = false;
    notifyListeners();
  }

  /// Clear all history (for testing/debugging)
  void clearHistory() {
    _history.clear();
    notifyListeners();
    _logInfo('History cleared');
  }

  /// Remove a specific record from history by correlation ID
  void removeFromHistory(String correlationId) {
    _history.removeWhere((record) => record.correlationId == correlationId);
    notifyListeners();
    _logInfo('Removed record from history: $correlationId');
  }

  /// Get history count (for debugging)
  int get historyCount => _history.length;
}

