import 'package:flutter/foundation.dart';
import '../services/api_service.dart';

class CredentialProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();

  String? _correlationId;
  String? _status;
  int? _progress;
  String? _message;
  List<String>? _credentialOfferUrls;
  List<String>? _credentialTypes;
  int? _credentialsIssued;
  List<Map<String, dynamic>>? _walletCredentials;
  bool _isLoading = false;
  bool _isPolling = false; // Track if we're currently polling
  String? _error;

  String? get correlationId => _correlationId;
  String? get status => _status;
  int? get progress => _progress;
  String? get message => _message;
  List<String>? get credentialOfferUrls => _credentialOfferUrls;
  List<String>? get credentialTypes => _credentialTypes;
  int? get credentialsIssued => _credentialsIssued;
  List<Map<String, dynamic>>? get walletCredentials => _walletCredentials;
  bool get isLoading => _isLoading;
  String? get error => _error;

  Future<void> issueCredentials() async {
    // Prevent multiple simultaneous requests
    if (_isLoading || _isPolling) {
      return;
    }
    
    _isLoading = true;
    _isPolling = false;
    _error = null;
    notifyListeners();

    try {
      final response = await _apiService.issueCredentials();
      _correlationId = response['correlationId']?.toString();
      _status = response['status']?.toString();
      _message = response['message']?.toString();
      
      // Start polling for status
      if (_correlationId != null && _correlationId!.isNotEmpty) {
        await _pollStatus();
      } else {
        _isLoading = false;
        _error = 'No correlation ID received';
        notifyListeners();
      }
    } catch (e) {
      _error = e.toString();
      _isLoading = false;
      _isPolling = false;
      notifyListeners();
    }
  }

  Future<void> _pollStatus() async {
    if (_correlationId == null || _correlationId!.isEmpty) {
      _isLoading = false;
      _isPolling = false;
      notifyListeners();
      return;
    }

    // Prevent multiple polling instances
    if (_isPolling) {
      return;
    }

    _isPolling = true;
    int maxAttempts = 60; // Maximum 2 minutes (60 * 2 seconds)
    int attempts = 0;

    try {
      while (attempts < maxAttempts) {
        // Check if status is terminal
        final currentStatus = _status?.toUpperCase();
        if (currentStatus == 'COMPLETED' || currentStatus == 'FAILED') {
          break;
        }

        await Future.delayed(const Duration(seconds: 2));
        attempts++;
        
        try {
          final statusResponse = await _apiService.getWorkflowStatus(_correlationId!);
          final newStatus = statusResponse['status']?.toString();
          _status = newStatus;
          _progress = statusResponse['progress'] as int?;
          _message = statusResponse['message']?.toString();
          
          notifyListeners();

          final statusUpper = newStatus?.toUpperCase();
          if (statusUpper == 'COMPLETED') {
            await _fetchCredentials();
            _isLoading = false;
            _isPolling = false;
            notifyListeners();
            return;
          } else if (statusUpper == 'FAILED') {
            _error = statusResponse['error']?.toString() ?? 'Workflow failed';
            _isLoading = false;
            _isPolling = false;
            notifyListeners();
            return;
          }
        } catch (e) {
          _error = 'Error checking status: $e';
          _isLoading = false;
          _isPolling = false;
          notifyListeners();
          return;
        }
      }
      
      // If we exit the loop without completing
      if (attempts >= maxAttempts) {
        _error = 'Workflow timed out after ${maxAttempts * 2} seconds. Current status: $_status';
        _isLoading = false;
        _isPolling = false;
        notifyListeners();
      } else {
        final statusUpper = _status?.toUpperCase();
        if (statusUpper != 'COMPLETED' && statusUpper != 'FAILED') {
          _error = 'Unexpected workflow status: $_status';
          _isLoading = false;
          _isPolling = false;
          notifyListeners();
        }
      }
    } finally {
      _isPolling = false;
    }
  }

  Future<void> _fetchCredentials() async {
    if (_correlationId == null) return;

    try {
      final credentialsResponse = await _apiService.getCredentials(_correlationId!);
      
      if (credentialsResponse['status'] == 'COMPLETED') {
        _credentialOfferUrls = (credentialsResponse['credentialOfferUrls'] as List<dynamic>?)
            ?.map((e) => e.toString())
            .toList();
        _credentialTypes = (credentialsResponse['credentialTypes'] as List<dynamic>?)
            ?.map((e) => e.toString())
            .toList();
        _credentialsIssued = credentialsResponse['credentialsIssued'] as int?;
      }
      
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> refreshCredentials() async {
    if (_correlationId != null) {
      await _fetchCredentials();
    }
  }

  Future<void> loadWalletCredentials() async {
    // Don't interfere with credential issuance flow
    if (_isPolling) {
      return;
    }
    
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final credentials = await _apiService.getWalletCredentials(
        studentId: ApiService.studentUsername,
      );
      _walletCredentials = credentials.map((c) => c as Map<String, dynamic>).toList();
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      _isLoading = false;
      _walletCredentials = []; // Set empty list on error
      notifyListeners();
    }
  }

  void reset() {
    _correlationId = null;
    _status = null;
    _progress = null;
    _message = null;
    _credentialOfferUrls = null;
    _credentialTypes = null;
    _credentialsIssued = null;
    _walletCredentials = null;
    _isLoading = false;
    _isPolling = false;
    _error = null;
    notifyListeners();
  }
}

