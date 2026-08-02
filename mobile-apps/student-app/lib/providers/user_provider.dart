import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import '../services/api_service.dart';

class UserProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  
  String? _studentCode;
  String? _email;
  String? _name;
  String? _fullName;
  String? _firstName;
  String? _lastName;
  String? _courseName;
  String? _institutionName;
  bool _isLoading = false;
  String? _error;

  // Getters
  String? get studentCode => _studentCode;
  String? get email => _email;
  String? get name => _name;
  String? get fullName => _fullName;
  String? get firstName => _firstName;
  String? get lastName => _lastName;
  String? get courseName => _courseName;
  String? get institutionName => _institutionName;
  bool get isLoading => _isLoading;
  String? get error => _error;
  
  // Get display name (prefer fullName, fallback to name, fallback to studentCode)
  String get displayName {
    if (_fullName != null && _fullName!.isNotEmpty) return _fullName!;
    if (_name != null && _name!.isNotEmpty) return _name!;
    return _studentCode ?? 'Student';
  }

  // Get username for API calls (studentCode)
  String get username => _studentCode ?? ApiService.studentUsername;

  Future<void> loadUserData() async {
    if (_isLoading) return;
    
    _isLoading = true;
    _error = null;
    
    // Set defaults first
    _studentCode = ApiService.studentUsername;
    _email = '${_studentCode}@alunos.usis.pt';
    _name = 'Student';
    _institutionName = 'DCS - Example University';
    
    notifyListeners();

    try {
      // Try to get student data from enrolments endpoint (it contains course info)
      try {
        final enrolmentsResponse = await _apiService.getStudentEnrolments();
        if (enrolmentsResponse['enrolmentList'] != null) {
          final enrolments = enrolmentsResponse['enrolmentList'] as List<dynamic>?;
          if (enrolments != null && enrolments.isNotEmpty) {
            final firstEnrolment = enrolments[0] as Map<String, dynamic>?;
            if (firstEnrolment != null) {
              _courseName = firstEnrolment['courseName'] as String?;
              // Extract course name from enrolment
              if (_courseName == null || _courseName!.isEmpty) {
                _courseName = firstEnrolment['curricularUnitName'] as String?;
              }
            }
          }
        }
      } catch (e) {
        debugPrint('Error loading enrolments: $e');
      }

      // Try to get name data from login endpoint
      try {
        final loginResponse = await _apiService.login();
        _name = loginResponse['name'] as String?;
        _fullName = loginResponse['fullName'] as String?;
        
        // Parse fullName into first and last name
        if (_fullName != null && _fullName!.isNotEmpty) {
          final nameParts = _parseFullName(_fullName!);
          _firstName = nameParts[0];
          _lastName = nameParts[1];
        } else if (_name != null && _name!.isNotEmpty) {
          // If fullName is not available, try to parse name
          final nameParts = _parseFullName(_name!);
          _firstName = nameParts[0];
          _lastName = nameParts[1];
        }
      } catch (e) {
        debugPrint('Error loading login data: $e');
      }

      // Try to get additional info from grades endpoint
      try {
        final gradesResponse = await _apiService.getStudentGrades();
        // Grades endpoint might have course info
        if (_courseName == null || _courseName!.isEmpty) {
          final grades = gradesResponse['grades'] as List<dynamic>?;
          if (grades != null && grades.isNotEmpty) {
            final firstGrade = grades[0] as Map<String, dynamic>?;
            if (firstGrade != null) {
              _courseName = firstGrade['courseName'] as String?;
            }
          }
        }
      } catch (e) {
        debugPrint('Error loading grades: $e');
      }
      
      _isLoading = false;
      notifyListeners();
    } catch (e) {
      // Keep defaults on error
      _isLoading = false;
      _error = e.toString();
      notifyListeners();
    }
  }

  void clearUserData() {
    _studentCode = null;
    _email = null;
    _name = null;
    _fullName = null;
    _firstName = null;
    _lastName = null;
    _courseName = null;
    _institutionName = null;
    _error = null;
    notifyListeners();
  }

  /// Parses a full name string into first name and last name
  /// Returns a list with [firstName, lastName]
  List<String> _parseFullName(String fullName) {
    if (fullName.isEmpty) {
      return ['', ''];
    }
    
    final parts = fullName.trim().split(RegExp(r'\s+'));
    
    if (parts.isEmpty) {
      return ['', ''];
    } else if (parts.length == 1) {
      // Only one name part, treat as first name
      return [parts[0], ''];
    } else {
      // First part is first name, rest is last name
      final firstName = parts[0];
      final lastName = parts.sublist(1).join(' ');
      return [firstName, lastName];
    }
  }
}

