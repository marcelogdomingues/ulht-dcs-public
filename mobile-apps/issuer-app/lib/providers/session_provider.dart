import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import '../models/session.dart';
import '../models/registered_student.dart';
import '../services/api_service.dart';

class SessionProvider with ChangeNotifier {
  final ApiService _apiService;

  SessionProvider(this._apiService);

  List<Session> _sessions = [];
  Session? _selectedSession;
  List<RegisteredStudent> _registeredStudents = [];
  bool _isLoading = false;
  String? _errorMessage;

  List<Session> get sessions => _sessions;
  Session? get selectedSession => _selectedSession;
  List<RegisteredStudent> get registeredStudents => _registeredStudents;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;

  /// Load all sessions
  Future<void> loadSessions() async {
    _setLoading(true);
    _errorMessage = null;
    try {
      final sessions = await _apiService.getSessions();
      // Sort sessions by creation date (newest first)
      sessions.sort((a, b) => b.createdAt.compareTo(a.createdAt));
      _sessions = sessions;
      notifyListeners();
    } catch (e) {
      _errorMessage = e.toString();
      notifyListeners();
    } finally {
      _setLoading(false);
    }
  }

  /// Create a new session
  Future<Session?> createSession({
    required String title,
    String? description,
    required String conferenceName,
    DateTime? startTime,
    DateTime? endTime,
    String? location,
  }) async {
    _setLoading(true);
    _errorMessage = null;
    try {
      final session = await _apiService.createSession(
        title: title,
        description: description,
        conferenceName: conferenceName,
        startTime: startTime,
        endTime: endTime,
        location: location,
      );
      // Add new session at the beginning (newest first)
      _sessions.insert(0, session);
      // Sort to ensure correct order (newest first)
      _sessions.sort((a, b) => b.createdAt.compareTo(a.createdAt));
      notifyListeners();
      return session;
    } catch (e) {
      _errorMessage = e.toString();
      notifyListeners();
      return null;
    } finally {
      _setLoading(false);
    }
  }

  /// Select a session and load its registered students
  Future<void> selectSession(Session session) async {
    _selectedSession = session;
    _errorMessage = null;
    notifyListeners();

    // Refresh session data to get latest registered count
    await refreshSession(session.id);
    
    // Load registered students for this session
    await loadRegisteredStudents(session.id);
  }
  
  /// Refresh session data from backend
  Future<void> refreshSession(String sessionId) async {
    try {
      final updatedSession = await _apiService.getSession(sessionId);
      
      // Update session in sessions list
      final index = _sessions.indexWhere((s) => s.id == sessionId);
      if (index != -1) {
        _sessions[index] = updatedSession;
      }
      
      // Update selected session if it's the one being refreshed
      if (_selectedSession?.id == sessionId) {
        _selectedSession = updatedSession;
      }
      
      notifyListeners();
    } catch (e) {
      // If session not found or other error, try to at least refresh registered count
      // This handles cases where the session exists but getSession fails
      final errorMsg = e.toString().toLowerCase();
      final isNotFound = errorMsg.contains('not found') || errorMsg.contains('404');
      
      if (isNotFound) {
        debugPrint('Session not found during refresh: $sessionId');
        // Try to refresh from all sessions list instead
        try {
          await loadSessions();
          // Find the session in the refreshed list
          final refreshedSession = _sessions.firstWhere(
            (s) => s.id == sessionId,
            orElse: () => _selectedSession ?? _sessions.first,
          );
          if (_selectedSession?.id == sessionId) {
            _selectedSession = refreshedSession;
          }
          notifyListeners();
        } catch (e2) {
          debugPrint('Failed to refresh from sessions list: $e2');
        }
      } else {
        // For other errors, try to update registered count from registered students
        debugPrint('Failed to refresh session from backend: $e');
        try {
          final students = await _apiService.getRegisteredStudents(sessionId);
          final index = _sessions.indexWhere((s) => s.id == sessionId);
          if (index != -1) {
            // Update registered count if we have the students list
            _sessions[index] = _sessions[index].copyWith(registeredCount: students.length);
            if (_selectedSession?.id == sessionId) {
              _selectedSession = _selectedSession!.copyWith(registeredCount: students.length);
            }
            notifyListeners();
          }
        } catch (e2) {
          // Silently fail - keep existing data
          debugPrint('Failed to refresh registered count: $e2');
        }
      }
    }
  }

  /// Load registered students for a session
  Future<void> loadRegisteredStudents(String sessionId) async {
    _setLoading(true);
    _errorMessage = null;
    try {
      _registeredStudents = await _apiService.getRegisteredStudents(sessionId);
      notifyListeners();
    } catch (e) {
      _errorMessage = e.toString();
      notifyListeners();
    } finally {
      _setLoading(false);
    }
  }

  /// Get QR code URL for a session
  Future<String?> getSessionQrCode(String sessionId) async {
    _setLoading(true);
    _errorMessage = null;
    try {
      final qrCodeUrl = await _apiService.getSessionQrCode(sessionId);
      
      // Update the session in the list with the new QR code URL
      final index = _sessions.indexWhere((s) => s.id == sessionId);
      if (index != -1) {
        _sessions[index] = _sessions[index].copyWith(qrCodeUrl: qrCodeUrl);
        if (_selectedSession?.id == sessionId) {
          _selectedSession = _selectedSession!.copyWith(qrCodeUrl: qrCodeUrl);
        }
        notifyListeners();
      }
      
      return qrCodeUrl;
    } catch (e) {
      _errorMessage = e.toString();
      notifyListeners();
      return null;
    } finally {
      _setLoading(false);
    }
  }

  /// Update a session
  Future<bool> updateSession(Session session) async {
    _setLoading(true);
    _errorMessage = null;
    try {
      final updatedSession = await _apiService.updateSession(session);
      final index = _sessions.indexWhere((s) => s.id == session.id);
      if (index != -1) {
        _sessions[index] = updatedSession;
        if (_selectedSession?.id == session.id) {
          _selectedSession = updatedSession;
        }
        notifyListeners();
      }
      return true;
    } catch (e) {
      _errorMessage = e.toString();
      notifyListeners();
      return false;
    } finally {
      _setLoading(false);
    }
  }

  /// Delete a session
  Future<bool> deleteSession(String sessionId) async {
    _setLoading(true);
    _errorMessage = null;
    try {
      await _apiService.deleteSession(sessionId);
      _sessions.removeWhere((s) => s.id == sessionId);
      if (_selectedSession?.id == sessionId) {
        _selectedSession = null;
        _registeredStudents = [];
      }
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = e.toString();
      notifyListeners();
      return false;
    } finally {
      _setLoading(false);
    }
  }

  /// Clear selected session
  void clearSelection() {
    _selectedSession = null;
    _registeredStudents = [];
    notifyListeners();
  }

  /// Clear error message
  void clearError() {
    _errorMessage = null;
    notifyListeners();
  }

  void _setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }
}

