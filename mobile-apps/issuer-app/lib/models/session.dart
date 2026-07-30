class Session {
  final String id;
  final String title;
  final String? description;
  final String conferenceName;
  final DateTime? startTime;
  final DateTime? endTime;
  final String? location;
  final String? qrCodeUrl;
  final int registeredCount;
  final DateTime createdAt;
  final bool isActive;

  Session({
    required this.id,
    required this.title,
    this.description,
    required this.conferenceName,
    this.startTime,
    this.endTime,
    this.location,
    this.qrCodeUrl,
    this.registeredCount = 0,
    DateTime? createdAt,
    this.isActive = true,
  }) : createdAt = createdAt ?? DateTime.now();

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'description': description,
      'conferenceName': conferenceName,
      'startTime': startTime?.toIso8601String(),
      'endTime': endTime?.toIso8601String(),
      'location': location,
      'qrCodeUrl': qrCodeUrl,
      'registeredCount': registeredCount,
      'createdAt': createdAt.toIso8601String(),
      'isActive': isActive,
    };
  }

  factory Session.fromJson(Map<String, dynamic> json) {
    return Session(
      id: json['id'] as String,
      title: json['title'] as String,
      description: json['description'] as String?,
      conferenceName: json['conferenceName'] as String,
      startTime: json['startTime'] != null
          ? DateTime.parse(json['startTime'] as String)
          : null,
      endTime: json['endTime'] != null
          ? DateTime.parse(json['endTime'] as String)
          : null,
      location: json['location'] as String?,
      qrCodeUrl: json['qrCodeUrl'] as String?,
      registeredCount: json['registeredCount'] as int? ?? 0,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : DateTime.now(),
      isActive: json['isActive'] as bool? ?? true,
    );
  }

  Session copyWith({
    String? id,
    String? title,
    String? description,
    String? conferenceName,
    DateTime? startTime,
    DateTime? endTime,
    String? location,
    String? qrCodeUrl,
    int? registeredCount,
    DateTime? createdAt,
    bool? isActive,
  }) {
    return Session(
      id: id ?? this.id,
      title: title ?? this.title,
      description: description ?? this.description,
      conferenceName: conferenceName ?? this.conferenceName,
      startTime: startTime ?? this.startTime,
      endTime: endTime ?? this.endTime,
      location: location ?? this.location,
      qrCodeUrl: qrCodeUrl ?? this.qrCodeUrl,
      registeredCount: registeredCount ?? this.registeredCount,
      createdAt: createdAt ?? this.createdAt,
      isActive: isActive ?? this.isActive,
    );
  }
}

