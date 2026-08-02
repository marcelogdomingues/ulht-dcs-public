import 'dart:convert';
import 'package:http/http.dart' as http;
import 'secure_store.dart';

class ApiService {
  // The apps talk DIRECTLY to each backend service on its own port (there is
  // no single working gateway: different features live on different services,
  // and student-service and sis-service both serve '/api/v1/student/*'
  // paths, a collision a gateway cannot disambiguate). Each service base URL
  // and the shared API key are build-time configurable via --dart-define;
  // dev defaults point at the local per-service ports. Every request carries
  // the 'apikey' header (services now REQUIRE it via Spring Security).
  //
  // PRODUCTION: pass HTTPS bases via --dart-define=<SVC>_URL=https://<host>/api/v1
  // and a real --dart-define=API_KEY=<secret>. Never ship the dev key. A proper
  // gateway with correct, non-colliding routes is a future improvement.

  // student-service: /student/issue, /student/status, /student/credentials,
  // /student/verify
  static const String _studentSvcUrl = String.fromEnvironment(
      'STUDENT_SVC_URL',
      defaultValue: 'http://localhost:8084/api/v1');

  // sis-service: /student/schedule, /student/enrolment, /student/grades,
  // /student/course-credits, /student/login
  static const String _sisSvcUrl = String.fromEnvironment(
      'SIS_SVC_URL',
      defaultValue: 'http://localhost:8085/api/v1');

  // credential-service: /wallet/..., /issuer/...
  static const String _credentialSvcUrl = String.fromEnvironment(
      'CREDENTIAL_SVC_URL',
      defaultValue: 'http://localhost:8086/api/v1');

  // fulfilment-service: /fulfilment/...
  static const String _fulfilmentSvcUrl = String.fromEnvironment(
      'FULFILMENT_SVC_URL',
      defaultValue: 'http://localhost:8087/api/v1');

  // Getter names kept stable so existing call sites keep working; each now
  // points at the correct direct-service base.
  static String get apiV1 => _studentSvcUrl;
  static String get sisUrl => _sisSvcUrl;
  static String get credentialUrl => _credentialSvcUrl;
  static String get fulfilmentUrl => _fulfilmentSvcUrl;

  static const String _apiKey =
      String.fromEnvironment('API_KEY', defaultValue: 'dcs-dev-local-CHANGE-ME');

  // Centralized headers so every request carries the required api key.
  static Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        'apikey': _apiKey,
      };

  static Map<String, String> get _textHeaders => {
        'Content-Type': 'text/plain',
        'apikey': _apiKey,
      };

  // TODO(security): Per-user login flow required. These MUST come from a real
  // login screen and be persisted via secure storage (SecureStore), NOT
  // compiled into the app. The dart-define values below are a temporary dev
  // convenience with EMPTY defaults; production builds must supply credentials
  // at runtime via the login UI.
  static const String _envStudentUsername =
      String.fromEnvironment('STUDENT_USERNAME', defaultValue: '');
  static const String _envInstallKey =
      String.fromEnvironment('STUDENT_INSTALL_KEY', defaultValue: '');

  static const String _kUsernameKey = 'student_username';
  static const String _kInstallKeyKey = 'student_install_key';

  /// In-memory cache of credentials loaded from secure storage / dart-define.
  static String _studentUsername = _envStudentUsername;
  static String _installKey = _envInstallKey;

  /// Backwards-compatible accessors used throughout this file.
  static String get studentUsername => _studentUsername;
  static String get installKey => _installKey;

  /// Loads persisted credentials (from secure storage) falling back to the
  /// dart-define values. Call once at startup / after login.
  static Future<void> loadCredentials() async {
    final storedUser = await SecureStore.read(_kUsernameKey);
    final storedKey = await SecureStore.read(_kInstallKeyKey);
    if (storedUser != null && storedUser.isNotEmpty) {
      _studentUsername = storedUser;
    }
    if (storedKey != null && storedKey.isNotEmpty) {
      _installKey = storedKey;
    }
  }

  /// Persists credentials supplied by a login flow to secure storage.
  static Future<void> setCredentials({
    required String username,
    required String installKey,
  }) async {
    _studentUsername = username;
    _installKey = installKey;
    await SecureStore.write(_kUsernameKey, username);
    await SecureStore.write(_kInstallKeyKey, installKey);
  }

  static bool get hasCredentials =>
      _studentUsername.isNotEmpty && _installKey.isNotEmpty;

  Future<Map<String, dynamic>> issueCredentials() async {
    try {
      final response = await http.post(
        Uri.parse('$apiV1/student/issue'),
        headers: _headers,
        body: jsonEncode({
          'userName': studentUsername,
          'installKey': installKey,
          'language': 'PT',
          'platform': 'ios',
          'application': 'com.example.dcs.mobile',
          'versionCode': '1601206',
        }),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to issue credentials: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error issuing credentials: $e');
    }
  }

  Future<Map<String, dynamic>> getWorkflowStatus(String correlationId) async {
    try {
      final response = await http.get(
        Uri.parse('$apiV1/student/status/$correlationId'),
        headers: _headers,
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to get status: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error getting status: $e');
    }
  }

  Future<Map<String, dynamic>> getCredentials(String correlationId) async {
    try {
      final response = await http.get(
        Uri.parse('$apiV1/student/credentials/$correlationId'),
        headers: _headers,
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else if (response.statusCode == 202) {
        // Still processing
        return {'status': 'PROCESSING', 'message': 'Credentials are still being processed'};
      } else {
        throw Exception('Failed to get credentials: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error getting credentials: $e');
    }
  }

  Future<Map<String, dynamic>> initiateVerification({
    required String credentialType,
    String? userId,
  }) async {
    try {
      final response = await http.post(
        Uri.parse('$apiV1/student/verify'),
        headers: _headers,
        body: jsonEncode({
          'credentialType': credentialType,
          'format': 'jwt_vc_json',
          'userId': userId ?? studentUsername,
        }),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to initiate verification: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error initiating verification: $e');
    }
  }

  Future<Map<String, dynamic>> getVerificationResult(String correlationId) async {
    try {
      final response = await http.get(
        Uri.parse('$fulfilmentUrl/fulfilment/result/$correlationId'),
        headers: _headers,
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to get verification result: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error getting verification result: $e');
    }
  }

  Future<List<dynamic>> getWalletCredentials({String? email, String? studentId}) async {
    try {
      final walletCredentialsUrl = '$credentialUrl/wallet/credentials';
      final uri = Uri.parse(walletCredentialsUrl).replace(queryParameters: {
        if (email != null) 'email': email,
        if (studentId != null) 'studentId': studentId,
        'userName': studentUsername, // Always include userName as fallback
      });

      final response = await http.get(uri, headers: _headers);

      if (response.statusCode == 200) {
        final body = jsonDecode(response.body);
        return body is List ? body : [];
      } else {
        throw Exception('Failed to get wallet credentials: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error getting wallet credentials: $e');
    }
  }

  Future<List<dynamic>> getStudentSchedule() async {
    try {
      final response = await http.post(
        Uri.parse('$sisUrl/student/schedule'),
        headers: _headers,
        body: jsonEncode({
          'userName': studentUsername,
          'installKey': installKey,
          'language': 'PT',
          'platform': 'ios',
          'application': 'com.example.dcs.mobile',
          'versionCode': '1601206',
        }),
      );

      if (response.statusCode == 200) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        final schedule = body['schedule'];
        return schedule is List ? schedule : [];
      } else {
        throw Exception('Failed to get schedule: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error getting schedule: $e');
    }
  }

  Future<Map<String, dynamic>> getStudentEnrolments() async {
    try {
      final response = await http.post(
        Uri.parse('$sisUrl/student/enrolment'),
        headers: _headers,
        body: jsonEncode({
          'userName': studentUsername,
          'installKey': installKey,
          'language': 'PT',
          'platform': 'ios',
          'application': 'com.example.dcs.mobile',
          'versionCode': '1601206',
        }),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to get enrolments: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error getting enrolments: $e');
    }
  }

  Future<Map<String, dynamic>> getStudentGrades() async {
    try {
      final response = await http.post(
        Uri.parse('$sisUrl/student/grades'),
        headers: _headers,
        body: jsonEncode({
          'userName': studentUsername,
          'installKey': installKey,
          'language': 'PT',
          'platform': 'ios',
          'application': 'com.example.dcs.mobile',
          'versionCode': '1601206',
        }),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to get grades: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error getting grades: $e');
    }
  }

  Future<Map<String, dynamic>> getStudentCourseCredits() async {
    try {
      final response = await http.post(
        Uri.parse('$sisUrl/student/course-credits'),
        headers: _headers,
        body: jsonEncode({
          'userName': studentUsername,
          'installKey': installKey,
          'language': 'PT',
          'platform': 'ios',
          'application': 'com.example.dcs.mobile',
          'versionCode': '1601206',
        }),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to get course credits: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error getting course credits: $e');
    }
  }

  Future<Map<String, dynamic>> login() async {
    try {
      final response = await http.post(
        Uri.parse('$sisUrl/student/login'),
        headers: _headers,
        body: jsonEncode({
          'userName': studentUsername,
          'installKey': installKey,
          'language': 'PT',
          'platform': 'ios',
          'application': 'com.example.dcs.mobile',
          'versionCode': '1601206',
        }),
      );

      if (response.statusCode == 200 || response.statusCode == 202) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        // The login endpoint might return student data directly or we need to extract it
        // For now, return the response body - we'll need to check the actual response structure
        return body;
      } else {
        throw Exception('Failed to login: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error logging in: $e');
    }
  }

  /// Handle presentation request via wallet API (for web)
  /// Calls the credential service to resolve and match credentials
  Future<Map<String, dynamic>> handlePresentationRequest(String presentationRequestUrl) async {
    try {
      final walletUrl = '$credentialUrl/wallet/present';

      final uri = Uri.parse(walletUrl).replace(queryParameters: {
        'userName': studentUsername,
      });

      final response = await http.post(
        uri,
        headers: _textHeaders,
        body: presentationRequestUrl,
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to handle presentation request: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error handling presentation request: $e');
    }
  }

  /// Extract presentation definition from presentation request URL
  /// This method can be used to get the presentation definition from the credential service
  Future<Map<String, dynamic>?> extractPresentationDefinition(String presentationRequestUrl) async {
    try {
      // Try to get presentation definition from credential service
      final url = '$credentialUrl/wallet/presentation-definition';
      
      final response = await http.post(
        Uri.parse(url),
        headers: _textHeaders,
        body: presentationRequestUrl,
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      }
      
      return null;
    } catch (e) {
      // If endpoint doesn't exist, return null and use default
      return null;
    }
  }

  /// Register for a session by scanning QR code and issue session credential
  /// The URL format is: /api/v1/issuer/sessions/{sessionId}/register
  Future<Map<String, dynamic>> registerForSession(String sessionUrl) async {
    final uri = Uri.parse(sessionUrl);
    final sessionsIndex = uri.pathSegments.indexOf('sessions');
    
    if (sessionsIndex == -1 || sessionsIndex >= uri.pathSegments.length - 1) {
      throw Exception('Invalid session URL format. Could not find session ID.');
    }
    
    final sessionId = uri.pathSegments[sessionsIndex + 1];
    
    if (sessionId.isEmpty) {
      throw Exception('Session ID is empty in URL: $sessionUrl');
    }
    
    // Call the issue endpoint with student ID to get credential offer
    final issueUrl = '$credentialUrl/issuer/sessions/$sessionId/issue';
    
    final response = await http.post(
      Uri.parse(issueUrl),
      headers: _headers,
      body: jsonEncode({
        'studentId': studentUsername,
      }),
    ).timeout(
      const Duration(seconds: 30),
      onTimeout: () {
        throw Exception('Request timeout. Please check if the credential service is running.');
      },
    );

    if (response.statusCode == 200 || response.statusCode == 201) {
      final responseBody = jsonDecode(response.body) as Map<String, dynamic>;
      
      // Extract credential offer URL
      final credentialOfferUrl = responseBody['credentialOfferUrl']?.toString() ??
                                 responseBody['offerUrl']?.toString() ??
                                 responseBody['url']?.toString();
      
      return {
        'success': true,
        'sessionId': sessionId,
        'studentId': studentUsername,
        'credentialOfferUrl': credentialOfferUrl,
        'message': 'Successfully registered for session',
      };
    } else if (response.statusCode == 501) {
      // Not Implemented - backend endpoint exists but isn't implemented yet
      try {
        final errorBody = jsonDecode(response.body) as Map<String, dynamic>;
        final message = errorBody['message']?.toString() ?? 
                       'Session credential issuance endpoint is not yet implemented.';
        final details = errorBody['details']?.toString() ?? '';
        throw Exception(
          'Backend endpoint not implemented yet.\n\n'
          '$message\n\n'
          '${details.isNotEmpty ? "$details\n\n" : ""}'
          'Please implement the issuer endpoints according to the implementation guide:\n'
          'mobile-apps/issuer-app/IMPLEMENTATION_GUIDE.md'
        );
      } catch (e) {
        if (e is Exception && e.toString().contains('Backend endpoint')) {
          rethrow;
        }
        throw Exception(
          'Session credential issuance endpoint is not yet implemented.\n\n'
          'The backend endpoint exists but needs to be fully implemented.\n\n'
          'See: mobile-apps/issuer-app/IMPLEMENTATION_GUIDE.md'
        );
      }
    } else {
      String errorMessage = 'Failed to register for session: ${response.statusCode}';
      try {
        final errorBody = jsonDecode(response.body) as Map<String, dynamic>;
        errorMessage = errorBody['message']?.toString() ?? 
                      errorBody['errorMessage']?.toString() ?? 
                      errorMessage;
      } catch (_) {
        // Ignore JSON parsing errors
      }
      throw Exception(errorMessage);
    }
  }
  

  /// Get session information from a session URL
  Future<Map<String, dynamic>> getSessionInfo(String sessionUrl) async {
    try {
      final uri = Uri.parse(sessionUrl);
      final sessionsIndex = uri.pathSegments.indexOf('sessions');
      
      if (sessionsIndex == -1 || sessionsIndex >= uri.pathSegments.length - 1) {
        return {};
      }
      
      final sessionId = uri.pathSegments[sessionsIndex + 1];
      
      if (sessionId.isEmpty) {
        return {};
      }
      
      final sessionInfoUrl = '$credentialUrl/issuer/sessions/$sessionId';
      final response = await http.get(
        Uri.parse(sessionInfoUrl),
        headers: _headers,
      ).timeout(
        const Duration(seconds: 5),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      }
      
      return {};
    } catch (e) {
      return {};
    }
  }

  /// Match credentials for presentation definition
  /// Calls credential service to find matching credentials and get disclosure information
  Future<Map<String, dynamic>> matchCredentialsForPresentationDefinition({
    required String presentationRequestUrl,
  }) async {
    try {
      final url = '$credentialUrl/wallet/match-presentation';
      
      final response = await http.post(
        Uri.parse(url).replace(queryParameters: {
          'userName': studentUsername,
        }),
        headers: _textHeaders,
        body: presentationRequestUrl,
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to match credentials: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error matching credentials for presentation: $e');
    }
  }

  /// Submit presentation with selected disclosures
  /// Calls Waltid Wallet API to present credentials with selective disclosure
  Future<Map<String, dynamic>> submitPresentationWithDisclosures({
    required String walletId,
    required String presentationRequestUrl,
    required List<String> selectedDisclosures,
    required Map<String, dynamic> credentialData,
  }) async {
    try {
      // Extract credential ID from credential data
      final credentialId = credentialData['id']?.toString();
      if (credentialId == null) {
        throw Exception('Credential ID not found in credential data');
      }

      // For now, call through the credential service's existing endpoint
      // The credential service should handle the presentation
      // If selective disclosure support is needed, it may need to be added to the backend
      final url = '$credentialUrl/wallet/present';
      
      final response = await http.post(
        Uri.parse(url).replace(queryParameters: {
          'userName': studentUsername,
        }),
        headers: _headers,
        body: jsonEncode({
          'presentationRequestUrl': presentationRequestUrl,
          'selectedDisclosures': selectedDisclosures,
          'credentialId': credentialId,
        }),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        // If the endpoint doesn't support disclosures yet, try without them
        // This is a fallback for backward compatibility
        final fallbackResponse = await http.post(
          Uri.parse(url).replace(queryParameters: {
            'userName': studentUsername,
          }),
          headers: _textHeaders,
          body: presentationRequestUrl,
        );

        if (fallbackResponse.statusCode == 200) {
          return jsonDecode(fallbackResponse.body) as Map<String, dynamic>;
        }
        
        throw Exception('Failed to submit presentation: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error submitting presentation: $e');
    }
  }
}

