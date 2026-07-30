import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/profile_provider.dart';
import '../models/verification_profile.dart';
import '../services/haptic_service.dart';
import 'profile_edit_screen.dart';

/// Helper function to darken a color
Color _darkenColor(Color color, double amount) {
  assert(amount >= 0 && amount <= 1);
  final hsl = HSLColor.fromColor(color);
  final lightness = (hsl.lightness - amount).clamp(0.0, 1.0);
  return hsl.withLightness(lightness).toColor();
}

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<ProfileProvider>(
      builder: (context, profileProvider, child) {
        final activeProfile = profileProvider.activeProfile;
        final appBarColors = activeProfile != null
            ? [_darkenColor(activeProfile.color, 0.2), _darkenColor(activeProfile.color, 0.1)]
            : [Colors.green.shade700, Colors.green.shade600];
        
        return Scaffold(
          appBar: AppBar(
            title: const Text(
              'Verification Profiles',
              style: TextStyle(
                fontWeight: FontWeight.bold,
                letterSpacing: -0.5,
              ),
            ),
            backgroundColor: appBarColors[0],
            foregroundColor: Colors.white,
            elevation: 0,
            flexibleSpace: Container(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: appBarColors,
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
              ),
            ),
            actions: [
              IconButton(
                icon: const Icon(Icons.add_rounded),
                tooltip: 'Create New Profile',
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const ProfileEditScreen(),
                    ),
                  );
                },
              ),
            ],
          ),
          body: Consumer<ProfileProvider>(
            builder: (context, provider, child) {
              if (provider.isLoading) {
                return const Center(
                  child: CircularProgressIndicator(),
                );
              }

              if (provider.profiles.isEmpty) {
                return Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.error_outline_rounded, size: 64, color: Colors.grey.shade400),
                      const SizedBox(height: 16),
                      Text(
                        'No profiles available',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          color: Colors.grey.shade800,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'Tap the + button to create a profile',
                        style: TextStyle(
                          color: Colors.grey.shade600,
                        ),
                      ),
                    ],
                  ),
                );
              }

              return ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  _buildHeader(context),
                  const SizedBox(height: 24),
                  ...provider.profiles.map((profile) => _buildProfileCard(
                        context,
                        profile,
                        provider,
                        isActive: provider.activeProfile?.id == profile.id,
                      )),
                  const SizedBox(height: 16),
                ],
              );
            },
          ),
        );
      },
    );
  }

  Widget _buildHeader(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            Colors.green.shade50,
            Colors.white,
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.green.shade200),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.green.shade100,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(
                  Icons.info_outline_rounded,
                  color: Colors.green.shade700,
                  size: 24,
                ),
              ),
              const SizedBox(width: 12),
              const Expanded(
                child: Text(
                  'Select a verification profile',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    letterSpacing: -0.5,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            'Profiles customize verification settings for different use cases. Each profile can have different credential types and behaviors.',
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey.shade700,
              height: 1.5,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildProfileCard(
    BuildContext context,
    VerificationProfile profile,
    ProfileProvider provider, {
    required bool isActive,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () async {
            HapticService().lightImpact();
            await provider.setActiveProfile(profile);
            if (context.mounted) {
              Navigator.pop(context);
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text('Switched to ${profile.name} profile'),
                  backgroundColor: profile.color,
                  behavior: SnackBarBehavior.floating,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(10),
                  ),
                ),
              );
            }
          },
          borderRadius: BorderRadius.circular(16),
          child: Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: isActive ? profile.color.withOpacity(0.1) : Colors.white,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(
                color: isActive
                    ? profile.color
                    : Colors.grey.shade200,
                width: isActive ? 2 : 1,
              ),
              boxShadow: isActive
                  ? [
                      BoxShadow(
                        color: profile.color.withOpacity(0.2),
                        blurRadius: 12,
                        offset: const Offset(0, 4),
                      ),
                    ]
                  : [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.05),
                        blurRadius: 4,
                        offset: const Offset(0, 2),
                      ),
                    ],
            ),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: isActive
                        ? profile.color
                        : profile.color.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(
                    profile.icon,
                    color: isActive ? Colors.white : profile.color,
                    size: 28,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Text(
                            profile.name,
                            style: TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                              color: isActive
                                  ? _darkenColor(profile.color, 0.3)
                                  : Colors.black87,
                              letterSpacing: -0.5,
                            ),
                          ),
                          if (isActive) ...[
                            const SizedBox(width: 8),
                            Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 8,
                                vertical: 4,
                              ),
                              decoration: BoxDecoration(
                                color: profile.color,
                                borderRadius: BorderRadius.circular(12),
                              ),
                              child: const Text(
                                'ACTIVE',
                                style: TextStyle(
                                  color: Colors.white,
                                  fontSize: 10,
                                  fontWeight: FontWeight.bold,
                                  letterSpacing: 0.5,
                                ),
                              ),
                            ),
                          ],
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        profile.description,
                        style: TextStyle(
                          fontSize: 13,
                          color: Colors.grey.shade600,
                          height: 1.4,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 6,
                        runSpacing: 6,
                        children: profile.defaultCredentialTypes
                            .take(3)
                            .map((type) => Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 8,
                                    vertical: 4,
                                  ),
                                  decoration: BoxDecoration(
                                    color: Colors.grey.shade100,
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Text(
                                    _getShortCredentialName(type),
                                    style: TextStyle(
                                      fontSize: 11,
                                      color: Colors.grey.shade700,
                                      fontWeight: FontWeight.w500,
                                    ),
                                  ),
                                ))
                            .toList(),
                      ),
                    ],
                  ),
                ),
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // Edit button for non-default profiles
                    if (profile.id != 'bar' && 
                        profile.id != 'office' && 
                        profile.id != 'conference' && 
                        profile.id != 'library' &&
                        profile.id != 'custom')
                      IconButton(
                        icon: Icon(Icons.edit_rounded, size: 20),
                        color: profile.color,
                        onPressed: () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (context) => ProfileEditScreen(profile: profile),
                            ),
                          );
                        },
                        tooltip: 'Edit Profile',
                      ),
                    Icon(
                      isActive
                          ? Icons.check_circle_rounded
                          : Icons.radio_button_unchecked_rounded,
                      color: isActive ? profile.color : Colors.grey.shade400,
                      size: 24,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  String _getShortCredentialName(String type) {
    switch (type) {
      case 'EducationalID':
        return 'Edu ID';
      case 'IdentityCredential':
        return 'Identity';
      case 'EuropeanStudentCard':
        return 'EU Card';
      case 'UniversityDegree':
        return 'Degree';
      default:
        return type;
    }
  }

  void _showProfileOptions(
    BuildContext context,
    VerificationProfile profile,
    ProfileProvider provider,
  ) {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: Icon(Icons.edit_rounded, color: profile.color),
              title: const Text('Edit Profile'),
              onTap: () {
                Navigator.pop(context);
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => ProfileEditScreen(profile: profile),
                  ),
                );
              },
            ),
            ListTile(
              leading: const Icon(Icons.delete_rounded, color: Colors.red),
              title: const Text('Delete Profile'),
              onTap: () {
                Navigator.pop(context);
                _confirmDeleteProfile(context, profile, provider);
              },
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }

  void _confirmDeleteProfile(
    BuildContext context,
    VerificationProfile profile,
    ProfileProvider provider,
  ) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete Profile'),
        content: Text('Are you sure you want to delete "${profile.name}"? This action cannot be undone.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () async {
              await provider.deleteProfile(profile.id);
              if (context.mounted) {
                Navigator.pop(context);
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('${profile.name} deleted'),
                    backgroundColor: Colors.red,
                  ),
                );
              }
            },
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
  }
}

