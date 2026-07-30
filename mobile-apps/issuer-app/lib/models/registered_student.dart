class RegisteredStudent {
  final String studentId;
  final String? studentName;
  final String? email;
  final String sessionId;
  final String sessionTitle;
  final DateTime registeredAt;
  final bool hasCredential;

  RegisteredStudent({
    required this.studentId,
    this.studentName,
    this.email,
    required this.sessionId,
    required this.sessionTitle,
    DateTime? registeredAt,
    this.hasCredential = false,
  }) : registeredAt = registeredAt ?? DateTime.now();

  Map<String, dynamic> toJson() {
    return {
      'studentId': studentId,
      'studentName': studentName,
      'email': email,
      'sessionId': sessionId,
      'sessionTitle': sessionTitle,
      'registeredAt': registeredAt.toIso8601String(),
      'hasCredential': hasCredential,
    };
  }

  factory RegisteredStudent.fromJson(Map<String, dynamic> json) {
    return RegisteredStudent(
      studentId: json['studentId'] as String,
      studentName: json['studentName'] as String?,
      email: json['email'] as String?,
      sessionId: json['sessionId'] as String,
      sessionTitle: json['sessionTitle'] as String,
      registeredAt: json['registeredAt'] != null
          ? DateTime.parse(json['registeredAt'] as String)
          : DateTime.now(),
      hasCredential: json['hasCredential'] as bool? ?? false,
    );
  }
}

