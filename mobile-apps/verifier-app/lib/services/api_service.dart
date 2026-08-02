import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;

class ApiService {
  // Talks DIRECTLY to the credential-service (which serves /verifier) on its
  // own port; there is no single working gateway. The base URL and shared API
  // key are build-time configurable via --dart-define; dev defaults point at
  // the local credential-service. Every request carries the 'apikey' header
  // (the service now REQUIRES it via Spring Security).
  //
  // PRODUCTION: pass --dart-define=CREDENTIAL_SVC_URL=https://<host>/api/v1
  // (https only) and a real --dart-define=API_KEY=<secret>. Never ship the dev
  // key. A proper gateway with correct routes is a future improvement.
  static const String credentialBase = String.fromEnvironment(
      'CREDENTIAL_SVC_URL',
      defaultValue: 'http://localhost:8086/api/v1');

  static String get verifierUrl => '$credentialBase/verifier';

  static const String _apiKey =
      String.fromEnvironment('API_KEY', defaultValue: 'dcs-dev-local-CHANGE-ME');

  // Centralized headers so every request carries the required api key.
  static Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        'apikey': _apiKey,
      };

  /// Initiates a verification session and returns the QR code URL immediately
  /// This is independent of credential issuance - works for any student with credentials
  Future<Map<String, dynamic>> initiateVerification({
    required String credentialType,
    String? userId, // Optional - for future use if needed
  }) async {
    try {
      // Use the basic verification endpoint - simple and direct
      final uri = Uri.parse('$verifierUrl/verify/basic')
          .replace(queryParameters: {
        'credentialType': credentialType,
        'format': 'jwt_vc_json',
      });

      // Request explicit verification policies rather than relying on server
      // defaults: signature validity, expiry, and not-before must all be
      // enforced. If the backend ignores this body it must still enforce these
      // (see server-side TODO below); the client asks for them defensively.
      //
      // TODO(security): expiry AND revocation must be enforced server-side.
      // The client cannot be trusted to decide acceptance; the verifier
      // service must reject expired / revoked / not-yet-valid credentials
      // regardless of what the client requests.
      final response = await http.post(
        uri,
        headers: _headers,
        body: jsonEncode({
          'policies': ['signature', 'expired', 'not-before'],
        }),
      ).timeout(
        const Duration(seconds: 30),
        onTimeout: () {
          throw Exception('Request timeout: The server did not respond within 30 seconds. '
                         'Please check if the credential service is running on port 8086.');
        },
      );

      if (response.statusCode == 200) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        // Response contains: url, state, presentationId
        return body;
      } else {
        String errorMessage = 'Failed to initiate verification: ${response.statusCode}';
        try {
          final errorBody = jsonDecode(response.body) as Map<String, dynamic>;
          errorMessage = errorBody['message']?.toString() ?? 
                        errorBody['errorMessage']?.toString() ?? 
                        errorMessage;
        } catch (_) {
          // Ignore JSON parsing errors, use default message
        }
        throw Exception(errorMessage);
      }
    } on SocketException catch (e) {
      throw Exception('Network error: Unable to connect to the credential service (port 8086). '
                     'Please check if the service is running. Error: $e');
    } on TimeoutException catch (e) {
      throw Exception('Request timeout: The server did not respond in time. Error: $e');
    } catch (e) {
      throw Exception('Error initiating verification: $e');
    }
  }

  /// Checks if credentials have been presented and verified
  /// Returns verification status with policy results
  Future<Map<String, dynamic>> getVerificationStatus(String state) async {
    try {
      final response = await http.get(
        Uri.parse('$verifierUrl/session/$state'),
        headers: _headers,
      ).timeout(
        const Duration(seconds: 10),
        onTimeout: () {
          throw Exception('Request timeout: The server did not respond within 10 seconds.');
        },
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else if (response.statusCode == 404) {
        // Session not found or credentials not yet presented
        return {
          'verificationResult': false,
          'status': 'PENDING',
          'message': 'Waiting for student to present credentials...'
        };
      } else {
        String errorMessage = 'Failed to get verification status: ${response.statusCode}';
        try {
          final errorBody = jsonDecode(response.body) as Map<String, dynamic>;
          errorMessage = errorBody['message']?.toString() ?? 
                        errorBody['errorMessage']?.toString() ?? 
                        errorMessage;
        } catch (_) {
          // Ignore JSON parsing errors, use default message
        }
        throw Exception(errorMessage);
      }
    } on SocketException catch (e) {
      throw Exception('Network error: Unable to connect to the credential service. Error: $e');
    } on TimeoutException catch (e) {
      throw Exception('Request timeout: The server did not respond in time. Error: $e');
    } catch (e) {
      throw Exception('Error getting verification status: $e');
    }
  }

  /// Gets the presented credentials after successful verification
  /// Returns decoded credential data including student information
  Future<Map<String, dynamic>> getPresentedCredentials(String state) async {
    try {
      final response = await http.get(
        Uri.parse('$verifierUrl/session/$state/credentials')
            .replace(queryParameters: {'viewMode': 'simple'}),
        headers: _headers,
      ).timeout(
        const Duration(seconds: 10),
        onTimeout: () {
          throw Exception('Request timeout: The server did not respond within 10 seconds.');
        },
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else if (response.statusCode == 404) {
        // Credentials not yet presented
        return {
          'status': 'PENDING',
          'message': 'Credentials have not been presented yet'
        };
      } else {
        String errorMessage = 'Failed to get presented credentials: ${response.statusCode}';
        try {
          final errorBody = jsonDecode(response.body) as Map<String, dynamic>;
          errorMessage = errorBody['message']?.toString() ?? 
                        errorBody['errorMessage']?.toString() ?? 
                        errorMessage;
        } catch (_) {
          // Ignore JSON parsing errors, use default message
        }
        throw Exception(errorMessage);
      }
    } on SocketException catch (e) {
      throw Exception('Network error: Unable to connect to the credential service. Error: $e');
    } on TimeoutException catch (e) {
      throw Exception('Request timeout: The server did not respond in time. Error: $e');
    } catch (e) {
      throw Exception('Error getting presented credentials: $e');
    }
  }

  /// Quick check if verification was successful
  Future<bool> checkVerificationSuccess(String state) async {
    try {
      final response = await http.get(
        Uri.parse('$verifierUrl/session/$state/success'),
        headers: _headers,
      ).timeout(
        const Duration(seconds: 5),
        onTimeout: () {
          throw TimeoutException('Request timeout');
        },
      );

      if (response.statusCode == 200) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        return body['success'] == true;
      }
      return false;
    } on TimeoutException {
      return false; // Timeout means not successful yet
    } catch (e) {
      return false;
    }
  }
}

