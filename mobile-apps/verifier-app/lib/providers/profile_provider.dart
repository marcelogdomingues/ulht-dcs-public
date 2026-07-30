import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import '../models/verification_profile.dart';

// NOTE(security): The data persisted here (verification profile presets:
// names, icons, colors, requested credential types) is non-sensitive UI
// configuration and contains no student PII or auth tokens, so it stays in
// shared_preferences. Verification results / student PII in this app are held
// in memory only (see verification_provider) and are not persisted. If PII or
// tokens are ever persisted, migrate those to flutter_secure_storage.
class ProfileProvider with ChangeNotifier {
  List<VerificationProfile> _profiles = [];
  VerificationProfile? _activeProfile;
  bool _isLoading = false;

  List<VerificationProfile> get profiles => _profiles;
  VerificationProfile? get activeProfile => _activeProfile;
  bool get isLoading => _isLoading;

  ProfileProvider() {
    _loadProfiles();
  }

  /// Load profiles from storage
  Future<void> _loadProfiles() async {
    _isLoading = true;
    notifyListeners();

    try {
      final prefs = await SharedPreferences.getInstance();
      final profilesJson = prefs.getString('verification_profiles');
      final activeProfileId = prefs.getString('active_profile_id');

      if (profilesJson != null) {
        final List<dynamic> profilesList = jsonDecode(profilesJson);
        _profiles = profilesList
            .map((p) => VerificationProfile.fromJson(p as Map<String, dynamic>))
            .toList();
      } else {
        // Initialize with default profiles
        _profiles = VerificationProfile.getDefaultProfiles();
        await _saveProfiles();
      }

      // Set active profile
      if (activeProfileId != null) {
        _activeProfile = _profiles.firstWhere(
          (p) => p.id == activeProfileId,
          orElse: () => _profiles.first,
        );
      } else {
        _activeProfile = _profiles.first;
      }

      _isLoading = false;
      notifyListeners();
    } catch (e) {
      debugPrint('Error loading profiles: $e');
      debugPrint('Falling back to default profiles. This is normal for web/desktop platforms.');
      // Fallback to default profiles - works without shared_preferences
      _profiles = VerificationProfile.getDefaultProfiles();
      _activeProfile = _profiles.isNotEmpty ? _profiles.first : null;
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Save profiles to storage
  Future<void> _saveProfiles() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final profilesJson = jsonEncode(
        _profiles.map((p) => p.toJson()).toList(),
      );
      await prefs.setString('verification_profiles', profilesJson);
      
      if (_activeProfile != null) {
        await prefs.setString('active_profile_id', _activeProfile!.id);
      }
    } catch (e) {
      // SharedPreferences may not be available on all platforms
      debugPrint('Error saving profiles: $e');
      debugPrint('Profiles will not persist. This is normal for web/desktop platforms.');
    }
  }

  /// Set active profile
  Future<void> setActiveProfile(VerificationProfile profile) async {
    _activeProfile = profile;
    try {
      await _saveProfiles();
    } catch (e) {
      // Continue even if save fails
      debugPrint('Could not save active profile: $e');
    }
    notifyListeners();
  }

  /// Add a new profile
  Future<void> addProfile(VerificationProfile profile) async {
    _profiles.add(profile);
    try {
      await _saveProfiles();
    } catch (e) {
      debugPrint('Could not save profile: $e');
    }
    notifyListeners();
  }

  /// Update an existing profile
  Future<void> updateProfile(VerificationProfile profile) async {
    final index = _profiles.indexWhere((p) => p.id == profile.id);
    if (index != -1) {
      _profiles[index] = profile;
      if (_activeProfile?.id == profile.id) {
        _activeProfile = profile;
      }
      try {
        await _saveProfiles();
      } catch (e) {
        debugPrint('Could not save profile update: $e');
      }
      notifyListeners();
    }
  }

  /// Delete a profile
  Future<void> deleteProfile(String profileId) async {
    _profiles.removeWhere((p) => p.id == profileId);
    if (_activeProfile?.id == profileId) {
      _activeProfile = _profiles.isNotEmpty ? _profiles.first : null;
    }
    try {
      await _saveProfiles();
    } catch (e) {
      debugPrint('Could not save profile deletion: $e');
    }
    notifyListeners();
  }

  /// Get profile by ID
  VerificationProfile? getProfileById(String id) {
    try {
      return _profiles.firstWhere((p) => p.id == id);
    } catch (e) {
      return null;
    }
  }
}

