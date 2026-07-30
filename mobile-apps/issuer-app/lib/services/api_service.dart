import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../models/session.dart';
import '../models/registered_student.dart';
import 'secure_store.dart';

class ApiService {
  // Talks DIRECTLY to the credential-service (which serves /issuer) on its own
  // port; there is no single working gateway. The base URL and shared API key
  // are build-time configurable via --dart-define; dev defaults point at the
  // local credential-service. Every request carries the 'apikey' header (the
  // service now REQUIRES it via Spring Security).
  //
  // PRODUCTION: pass --dart-define=CREDENTIAL_SVC_URL=https://<host>/api/v1
  // (https only) and a real --dart-define=API_KEY=<secret>. Never ship the dev
  // key. A proper gateway with correct routes is a future improvement.
  static const String baseUrl = String.fromEnvironment(
      'CREDENTIAL_SVC_URL',
      defaultValue: 'http://localhost:8086/api/v1');

  static const String _apiKey =
      String.fromEnvironment('API_KEY', defaultValue: 'ulht-dev-local-CHANGE-ME');

  // Centralized headers so every request carries the required api key.
  static Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        'apikey': _apiKey,
      };
  
  // Mock mode flag - set to true to use mock data instead of real API calls
  // Default to true since backend endpoints may not be implemented yet
  static bool _mockMode = true;
  static bool get mockMode => _mockMode;
  static bool _initialized = false;
  
  // Shared preferences key for storing sessions locally in mock mode
  static const String _sessionsKey = 'issuer_app_sessions';
  static const String _registrationsKey = 'issuer_app_registrations';
  
  // Initialize mock mode and default sessions
  Future<void> _ensureInitialized() async {
    if (_initialized) return;
    
    // Try to check if backend is available (quick timeout)
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/issuer/sessions'),
        headers: _headers,
      ).timeout(const Duration(seconds: 2));
      
      // If we get a response (even 404), backend is available
      if (response.statusCode == 200 || response.statusCode == 404) {
        // Backend is available, try using it
        _mockMode = false;
      } else {
        // Other error, use mock mode
        _mockMode = true;
      }
    } catch (e) {
      // Backend not available, use mock mode (default)
      _mockMode = true;
    }
    
    // Always initialize mock sessions in case we need them
    await _initializeMockSessions();
    
    _initialized = true;
  }
  
  Future<void> _initializeMockSessions() async {
    final prefs = await SharedPreferences.getInstance();
    final existing = prefs.getString(_sessionsKey);
    if (existing == null || existing.isEmpty) {
      // Create 3 default sessions
      final defaultSessions = _createDefaultSessions();
      await prefs.setString(_sessionsKey, jsonEncode(
        defaultSessions.map((s) => s.toJson()).toList(),
      ));
    }
  }
  
  List<Session> _createDefaultSessions() {
    final now = DateTime.now();
    return [
      Session(
        id: 'session-1',
        title: 'Keynote: Future of Technology',
        description: 'Join us for an inspiring keynote presentation on emerging technologies and their impact on society.',
        conferenceName: 'International Tech Conference 2024',
        startTime: now.add(const Duration(days: 1)),
        endTime: now.add(const Duration(days: 1, hours: 2)),
        location: 'Main Hall, Room 101',
        qrCodeUrl: '$baseUrl/issuer/sessions/session-1/register',
        registeredCount: 15,
        isActive: true,
      ),
      Session(
        id: 'session-2',
        title: 'AI and Machine Learning Workshop',
        description: 'Hands-on workshop on building machine learning models with practical examples.',
        conferenceName: 'International Tech Conference 2024',
        startTime: now.add(const Duration(days: 2)),
        endTime: now.add(const Duration(days: 2, hours: 3)),
        location: 'Workshop Room A',
        qrCodeUrl: '$baseUrl/issuer/sessions/session-2/register',
        registeredCount: 8,
        isActive: true,
      ),
      Session(
        id: 'session-3',
        title: 'Blockchain Innovation Panel',
        description: 'Panel discussion with industry leaders on blockchain technology and its real-world applications.',
        conferenceName: 'International Tech Conference 2024',
        startTime: now.add(const Duration(days: 3)),
        endTime: now.add(const Duration(days: 3, hours: 1, minutes: 30)),
        location: 'Conference Center, Room 205',
        qrCodeUrl: '$baseUrl/issuer/sessions/session-3/register',
        registeredCount: 22,
        isActive: true,
      ),
    ];
  }
  
  Future<List<Session>> _getMockSessions() async {
    final prefs = await SharedPreferences.getInstance();
    final sessionsJson = prefs.getString(_sessionsKey);
    if (sessionsJson != null && sessionsJson.isNotEmpty) {
      final List<dynamic> decoded = jsonDecode(sessionsJson);
      final sessions = decoded.map((json) => Session.fromJson(json as Map<String, dynamic>)).toList();
      // Sort sessions by creation date (newest first)
      sessions.sort((a, b) => b.createdAt.compareTo(a.createdAt));
      return sessions;
    }
    return [];
  }
  
  Future<void> _saveMockSessions(List<Session> sessions) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_sessionsKey, jsonEncode(
      sessions.map((s) => s.toJson()).toList(),
    ));
  }
  
  Future<Session> _saveMockSession(Session session) async {
    final sessions = await _getMockSessions();
    final index = sessions.indexWhere((s) => s.id == session.id);
    if (index >= 0) {
      sessions[index] = session;
    } else {
      sessions.add(session);
    }
    await _saveMockSessions(sessions);
    return session;
  }

  /// Creates a new session and generates a QR code for credential issuance
  Future<Session> createSession({
    required String title,
    String? description,
    required String conferenceName,
    DateTime? startTime,
    DateTime? endTime,
    String? location,
  }) async {
    await _ensureInitialized();
    
    // Use mock mode if backend is not available
    if (_mockMode) {
      return _createMockSession(
        title: title,
        description: description,
        conferenceName: conferenceName,
        startTime: startTime,
        endTime: endTime,
        location: location,
      );
    }

    try {
      final response = await http.post(
        Uri.parse('$baseUrl/issuer/sessions'),
        headers: _headers,
        body: jsonEncode({
          'title': title,
          'description': description,
          'conferenceName': conferenceName,
          'startTime': startTime?.toIso8601String(),
          'endTime': endTime?.toIso8601String(),
          'location': location,
        }),
      ).timeout(const Duration(seconds: 5));

      if (response.statusCode == 200 || response.statusCode == 201) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        return Session.fromJson(body);
      } else {
        String errorMessage = 'Failed to create session: ${response.statusCode}';
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
    } catch (e) {
      // Fall back to mock mode if connection fails
      _mockMode = true;
      await _initializeMockSessions();
      return _createMockSession(
        title: title,
        description: description,
        conferenceName: conferenceName,
        startTime: startTime,
        endTime: endTime,
        location: location,
      );
    }
  }
  
  Future<Session> _createMockSession({
    required String title,
    String? description,
    required String conferenceName,
    DateTime? startTime,
    DateTime? endTime,
    String? location,
  }) async {
    final sessionId = 'session-${DateTime.now().millisecondsSinceEpoch}';
    final session = Session(
      id: sessionId,
      title: title,
      description: description,
      conferenceName: conferenceName,
      startTime: startTime,
      endTime: endTime,
      location: location,
      qrCodeUrl: '$baseUrl/issuer/sessions/$sessionId/register',
      registeredCount: 0,
      isActive: true,
    );
    return await _saveMockSession(session);
  }

  /// Gets all sessions
  Future<List<Session>> getSessions() async {
    await _ensureInitialized();
    
    // Use mock mode if backend is not available
    if (_mockMode) {
      return await _getMockSessions();
    }

    try {
      final response = await http.get(
        Uri.parse('$baseUrl/issuer/sessions'),
        headers: _headers,
      ).timeout(const Duration(seconds: 5));

      if (response.statusCode == 200) {
        final body = jsonDecode(response.body);
        if (body is List) {
          final sessions = body.map((json) => Session.fromJson(json as Map<String, dynamic>)).toList();
          // Sort sessions by creation date (newest first)
          sessions.sort((a, b) => b.createdAt.compareTo(a.createdAt));
          return sessions;
        }
        return [];
      } else {
        throw Exception('Failed to get sessions: ${response.statusCode}');
      }
    } catch (e) {
      // Fall back to mock mode if connection fails
      _mockMode = true;
      await _initializeMockSessions();
      return await _getMockSessions();
    }
  }

  /// Gets a specific session by ID
  Future<Session> getSession(String sessionId) async {
    await _ensureInitialized();
    
    // Use mock mode if backend is not available
    if (_mockMode) {
      final sessions = await _getMockSessions();
      final session = sessions.firstWhere(
        (s) => s.id == sessionId,
        orElse: () => throw Exception('Session not found: $sessionId'),
      );
      return session;
    }

    try {
      final response = await http.get(
        Uri.parse('$baseUrl/issuer/sessions/$sessionId'),
        headers: _headers,
      ).timeout(const Duration(seconds: 5));

      if (response.statusCode == 200) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        return Session.fromJson(body);
      } else if (response.statusCode == 404) {
        // Session doesn't exist - throw specific exception
        throw Exception('Session not found: $sessionId');
      } else {
        throw Exception('Failed to get session: ${response.statusCode}');
      }
    } on Exception catch (e) {
      // Check if it's a "not found" exception - don't fall back to mock mode for that
      if (e.toString().contains('not found') || e.toString().contains('404')) {
        rethrow;
      }
      // Fall back to mock mode only for connection errors
      _mockMode = true;
      await _initializeMockSessions();
      final sessions = await _getMockSessions();
      final session = sessions.firstWhere(
        (s) => s.id == sessionId,
        orElse: () => throw Exception('Session not found: $sessionId'),
      );
      return session;
    }
  }

  /// Updates a session
  Future<Session> updateSession(Session session) async {
    await _ensureInitialized();
    
    // Use mock mode if backend is not available
    if (_mockMode) {
      return await _saveMockSession(session);
    }

    try {
      final response = await http.put(
        Uri.parse('$baseUrl/issuer/sessions/${session.id}'),
        headers: _headers,
        body: jsonEncode(session.toJson()),
      ).timeout(const Duration(seconds: 5));

      if (response.statusCode == 200) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        return Session.fromJson(body);
      } else {
        throw Exception('Failed to update session: ${response.statusCode}');
      }
    } catch (e) {
      // Fall back to mock mode if connection fails
      _mockMode = true;
      await _initializeMockSessions();
      return await _saveMockSession(session);
    }
  }

  /// Deletes a session
  Future<void> deleteSession(String sessionId) async {
    await _ensureInitialized();
    
    // Use mock mode if backend is not available
    if (_mockMode) {
      await _deleteMockSession(sessionId);
      return;
    }

    try {
      final response = await http.delete(
        Uri.parse('$baseUrl/issuer/sessions/$sessionId'),
        headers: _headers,
      ).timeout(const Duration(seconds: 5));

      if (response.statusCode != 200 && response.statusCode != 204) {
        throw Exception('Failed to delete session: ${response.statusCode}');
      }
    } catch (e) {
      // Fall back to mock mode if connection fails
      _mockMode = true;
      await _initializeMockSessions();
      await _deleteMockSession(sessionId);
    }
  }
  
  Future<void> _deleteMockSession(String sessionId) async {
    final sessions = await _getMockSessions();
    sessions.removeWhere((s) => s.id == sessionId);
    await _saveMockSessions(sessions);
  }

  /// Gets the QR code URL for a session (or regenerates it)
  Future<String> getSessionQrCode(String sessionId) async {
    await _ensureInitialized();
    
    // Use mock mode if backend is not available
    if (_mockMode) {
      final session = await getSession(sessionId);
      final qrCodeUrl = '$baseUrl/issuer/sessions/$sessionId/register';
      // Update session with QR code URL
      await _saveMockSession(session.copyWith(qrCodeUrl: qrCodeUrl));
      return qrCodeUrl;
    }

    try {
      final response = await http.post(
        Uri.parse('$baseUrl/issuer/sessions/$sessionId/qr-code'),
        headers: _headers,
      ).timeout(const Duration(seconds: 5));

      if (response.statusCode == 200) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        return body['qrCodeUrl'] as String? ?? '';
      } else {
        throw Exception('Failed to get QR code: ${response.statusCode}');
      }
    } catch (e) {
      // Fall back to mock mode if connection fails
      _mockMode = true;
      await _initializeMockSessions();
      final session = await getSession(sessionId);
      final qrCodeUrl = '$baseUrl/issuer/sessions/$sessionId/register';
      await _saveMockSession(session.copyWith(qrCodeUrl: qrCodeUrl));
      return qrCodeUrl;
    }
  }

  /// Gets all registered students for a session
  Future<List<RegisteredStudent>> getRegisteredStudents(String sessionId) async {
    await _ensureInitialized();
    
    // Use mock mode if backend is not available
    if (_mockMode) {
      return await _getMockRegisteredStudents(sessionId);
    }

    try {
      final response = await http.get(
        Uri.parse('$baseUrl/issuer/sessions/$sessionId/registrations'),
        headers: _headers,
      ).timeout(const Duration(seconds: 5));

      if (response.statusCode == 200) {
        final body = jsonDecode(response.body);
        if (body is List) {
          return body.map((json) => RegisteredStudent.fromJson(json as Map<String, dynamic>)).toList();
        }
        return [];
      } else {
        throw Exception('Failed to get registered students: ${response.statusCode}');
      }
    } catch (e) {
      // Fall back to mock mode if connection fails
      _mockMode = true;
      await _initializeMockSessions();
      return await _getMockRegisteredStudents(sessionId);
    }
  }
  
  Future<List<RegisteredStudent>> _getMockRegisteredStudents(String sessionId) async {
    // Registered students contain PII (id, name, email) so they are read from
    // secure storage rather than shared_preferences. Session/conference
    // metadata (non-sensitive) remains in shared_preferences above.
    final registrationsJson = await SecureStore.read(_registrationsKey);
    if (registrationsJson != null && registrationsJson.isNotEmpty) {
      final List<dynamic> decoded = jsonDecode(registrationsJson);
      final allRegistrations = decoded.map((json) => RegisteredStudent.fromJson(json as Map<String, dynamic>)).toList();
      return allRegistrations.where((r) => r.sessionId == sessionId).toList();
    }
    return [];
  }

  /// Issues a session credential for a student
  /// This is typically called when a student scans the QR code
  Future<Map<String, dynamic>> issueSessionCredential({
    required String sessionId,
    required String studentId,
  }) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/issuer/sessions/$sessionId/issue'),
        headers: _headers,
        body: jsonEncode({
          'studentId': studentId,
        }),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        String errorMessage = 'Failed to issue credential: ${response.statusCode}';
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
    } on SocketException catch (e) {
      throw Exception('Network error: Unable to connect to the credential service. Error: $e');
    } catch (e) {
      throw Exception('Error issuing session credential: $e');
    }
  }

  /// Checks if a student is registered for a session
  Future<bool> checkStudentRegistration({
    required String sessionId,
    required String studentId,
  }) async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/issuer/sessions/$sessionId/check-registration/$studentId'),
        headers: _headers,
      );

      if (response.statusCode == 200) {
        final body = jsonDecode(response.body) as Map<String, dynamic>;
        return body['isRegistered'] as bool? ?? false;
      } else {
        return false;
      }
    } catch (e) {
      return false;
    }
  }
}

