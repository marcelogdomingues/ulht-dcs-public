import 'package:flutter/material.dart';

/// Represents a verification profile/use case (bar, office, conference, etc.)
class VerificationProfile {
  final String id;
  final String name;
  final String description;
  final IconData icon;
  final Color color;
  final List<String> defaultCredentialTypes;
  final Map<String, dynamic> settings;

  VerificationProfile({
    required this.id,
    required this.name,
    required this.description,
    required this.icon,
    required this.color,
    required this.defaultCredentialTypes,
    this.settings = const {},
  });

  /// Convert to JSON for storage
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'icon': icon.codePoint,
      'color': color.value,
      'defaultCredentialTypes': defaultCredentialTypes,
      'settings': settings,
    };
  }

  /// Create from JSON
  factory VerificationProfile.fromJson(Map<String, dynamic> json) {
    return VerificationProfile(
      id: json['id'] as String,
      name: json['name'] as String,
      description: json['description'] as String,
      icon: IconData(
        json['icon'] as int,
        fontFamily: 'MaterialIcons',
        fontPackage: null,
      ),
      color: Color(json['color'] as int),
      defaultCredentialTypes: List<String>.from(json['defaultCredentialTypes'] as List),
      settings: Map<String, dynamic>.from(json['settings'] as Map? ?? {}),
    );
  }

  /// Default profiles
  static List<VerificationProfile> getDefaultProfiles() {
    return [
      VerificationProfile(
        id: 'bar',
        name: 'Bar',
        description: 'Verify student age and enrollment for bar access',
        icon: Icons.local_bar_rounded,
        color: Colors.orange,
        defaultCredentialTypes: ['EducationalID', 'IdentityCredential'],
        settings: {
          'requireAgeVerification': true,
          'autoPoll': true,
          'soundEnabled': true,
        },
      ),
      VerificationProfile(
        id: 'office',
        name: 'Office',
        description: 'Verify student identity for office services',
        icon: Icons.business_rounded,
        color: Colors.blue,
        defaultCredentialTypes: ['EducationalID', 'IdentityCredential'],
        settings: {
          'requireAgeVerification': false,
          'autoPoll': true,
          'soundEnabled': false,
        },
      ),
      VerificationProfile(
        id: 'conference',
        name: 'Conference',
        description: 'Verify student enrollment for conference access',
        icon: Icons.event_rounded,
        color: Colors.purple,
        defaultCredentialTypes: ['EducationalID', 'EuropeanStudentCard'],
        settings: {
          'requireAgeVerification': false,
          'autoPoll': true,
          'soundEnabled': true,
        },
      ),
      VerificationProfile(
        id: 'library',
        name: 'Library',
        description: 'Verify student status for library access',
        icon: Icons.library_books_rounded,
        color: Colors.teal,
        defaultCredentialTypes: ['EducationalID'],
        settings: {
          'requireAgeVerification': false,
          'autoPoll': true,
          'soundEnabled': false,
        },
      ),
      VerificationProfile(
        id: 'custom',
        name: 'Custom',
        description: 'Create your own verification profile',
        icon: Icons.tune_rounded,
        color: Colors.grey,
        defaultCredentialTypes: ['EducationalID'],
        settings: {
          'requireAgeVerification': false,
          'autoPoll': true,
          'soundEnabled': true,
        },
      ),
    ];
  }

  VerificationProfile copyWith({
    String? id,
    String? name,
    String? description,
    IconData? icon,
    Color? color,
    List<String>? defaultCredentialTypes,
    Map<String, dynamic>? settings,
  }) {
    return VerificationProfile(
      id: id ?? this.id,
      name: name ?? this.name,
      description: description ?? this.description,
      icon: icon ?? this.icon,
      color: color ?? this.color,
      defaultCredentialTypes: defaultCredentialTypes ?? this.defaultCredentialTypes,
      settings: settings ?? this.settings,
    );
  }
}

