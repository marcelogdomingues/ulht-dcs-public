import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/user_provider.dart';

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              const Color(0xFF1976D2).withOpacity(0.08),
              const Color(0xFF03A9F4).withOpacity(0.05),
              Colors.grey.shade50,
            ],
            stops: const [0.0, 0.5, 1.0],
          ),
        ),
        child: SingleChildScrollView(
          child: Column(
            children: [
              // Modern Header
              Container(
                padding: const EdgeInsets.fromLTRB(20, 60, 20, 32),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [
                      const Color(0xFF1976D2),
                      const Color(0xFF1565C0),
                      const Color(0xFF0D47A1),
                    ],
                    stops: const [0.0, 0.6, 1.0],
                  ),
                  borderRadius: const BorderRadius.only(
                    bottomLeft: Radius.circular(32),
                    bottomRight: Radius.circular(32),
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.blue.withOpacity(0.3),
                      blurRadius: 20,
                      offset: const Offset(0, 10),
                    ),
                  ],
                ),
                child: Column(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(4),
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        gradient: LinearGradient(
                          colors: [
                            Colors.white.withOpacity(0.3),
                            Colors.white.withOpacity(0.1),
                          ],
                        ),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black.withOpacity(0.2),
                            blurRadius: 15,
                            offset: const Offset(0, 5),
                          ),
                        ],
                      ),
                      child: Container(
                        padding: const EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: Colors.white,
                        ),
                        child: const Icon(
                          Icons.person_rounded,
                          size: 64,
                          color: Color(0xFF1976D2),
                        ),
                      ),
                    ),
                    const SizedBox(height: 20),
                    Consumer<UserProvider>(
                      builder: (context, userProvider, child) {
                        return Column(
                          children: [
                            Text(
                              userProvider.displayName,
                              style: const TextStyle(
                                fontSize: 28,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                                letterSpacing: -0.5,
                              ),
                            ),
                            const SizedBox(height: 8),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                              decoration: BoxDecoration(
                                color: Colors.white.withOpacity(0.2),
                                borderRadius: BorderRadius.circular(20),
                                border: Border.all(
                                  color: Colors.white.withOpacity(0.3),
                                  width: 1.5,
                                ),
                              ),
                              child: Text(
                                userProvider.studentCode ?? 'a12345678',
                                style: const TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w600,
                                  color: Colors.white,
                                  letterSpacing: 0.5,
                                ),
                              ),
                            ),
                          ],
                        );
                      },
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              // Profile Information Card
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Container(
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(24),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.06),
                        blurRadius: 20,
                        offset: const Offset(0, 8),
                      ),
                    ],
                  ),
                  child: Consumer<UserProvider>(
                    builder: (context, userProvider, child) {
                      return Column(
                        children: [
                          Padding(
                            padding: const EdgeInsets.all(20),
                            child: Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(12),
                                  decoration: BoxDecoration(
                                    color: Colors.blue.shade50,
                                    borderRadius: BorderRadius.circular(16),
                                  ),
                                  child: Icon(
                                    Icons.info_outline_rounded,
                                    color: Colors.blue.shade700,
                                    size: 24,
                                  ),
                                ),
                                const SizedBox(width: 16),
                                const Text(
                                  'Profile Information',
                                  style: TextStyle(
                                    fontSize: 20,
                                    fontWeight: FontWeight.bold,
                                    letterSpacing: -0.5,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const Divider(height: 1),
                          _buildModernProfileRow(
                            Icons.badge_rounded,
                            'Student Number',
                            userProvider.studentCode ?? 'a12345678',
                            Colors.blue,
                          ),
                          if (userProvider.email != null && userProvider.email!.isNotEmpty) ...[
                            const Divider(height: 1, indent: 60),
                            _buildModernProfileRow(
                              Icons.email_rounded,
                              'Email',
                              userProvider.email!,
                              Colors.orange,
                            ),
                          ],
                          if (userProvider.courseName != null && userProvider.courseName!.isNotEmpty) ...[
                            const Divider(height: 1, indent: 60),
                            _buildModernProfileRow(
                              Icons.school_rounded,
                              'Course',
                              userProvider.courseName!,
                              Colors.purple,
                            ),
                          ],
                          const Divider(height: 1, indent: 60),
                          _buildModernProfileRow(
                            Icons.business_rounded,
                            'Institution',
                            userProvider.institutionName ?? 'ULHT - Universidade Lusófona',
                            Colors.teal,
                          ),
                          if (userProvider.firstName != null && userProvider.firstName!.isNotEmpty) ...[
                            const Divider(height: 1, indent: 60),
                            _buildModernProfileRow(
                              Icons.person_outline_rounded,
                              'First Name',
                              userProvider.firstName!,
                              Colors.indigo,
                            ),
                          ],
                          if (userProvider.lastName != null && userProvider.lastName!.isNotEmpty) ...[
                            const Divider(height: 1, indent: 60),
                            _buildModernProfileRow(
                              Icons.person_outline_rounded,
                              'Last Name',
                              userProvider.lastName!,
                              Colors.indigo,
                            ),
                          ],
                          if (userProvider.fullName != null && userProvider.fullName!.isNotEmpty) ...[
                            const Divider(height: 1, indent: 60),
                            _buildModernProfileRow(
                              Icons.badge_rounded,
                              'Full Name',
                              userProvider.fullName!,
                              Colors.blue,
                            ),
                          ],
                        ],
                      );
                    },
                  ),
                ),
              ),
              const SizedBox(height: 20),
              // Settings Card
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20),
                child: Container(
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(24),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.06),
                        blurRadius: 20,
                        offset: const Offset(0, 8),
                      ),
                    ],
                  ),
                  child: Column(
                    children: [
                      Padding(
                        padding: const EdgeInsets.all(20),
                        child: Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: Colors.grey.shade50,
                                borderRadius: BorderRadius.circular(16),
                              ),
                              child: Icon(
                                Icons.settings_rounded,
                                color: Colors.grey.shade700,
                                size: 24,
                              ),
                            ),
                            const SizedBox(width: 16),
                            const Text(
                              'Settings',
                              style: TextStyle(
                                fontSize: 20,
                                fontWeight: FontWeight.bold,
                                letterSpacing: -0.5,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const Divider(height: 1),
                      _buildModernListTile(
                        context,
                        Icons.settings_outlined,
                        'Settings',
                        Colors.blue,
                        () {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Settings feature coming soon')),
                          );
                        },
                      ),
                      const Divider(height: 1, indent: 60),
                      _buildModernListTile(
                        context,
                        Icons.help_outline_rounded,
                        'Help & Support',
                        Colors.green,
                        () {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Help & Support feature coming soon')),
                          );
                        },
                      ),
                      const Divider(height: 1, indent: 60),
                      _buildModernListTile(
                        context,
                        Icons.info_outline_rounded,
                        'About',
                        Colors.orange,
                        () {
                          showAboutDialog(
                            context: context,
                            applicationName: 'ULHT Student App',
                            applicationVersion: '1.0.0',
                            applicationLegalese: '© 2024 ULHT',
                          );
                        },
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 32),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildModernProfileRow(IconData icon, String label, String value, Color color) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 16.0),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: color.withOpacity(0.1),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(icon, color: color, size: 20),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: TextStyle(
                    fontSize: 12,
                    color: Colors.grey.shade600,
                    fontWeight: FontWeight.w500,
                    letterSpacing: 0.3,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  value,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 0.2,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildModernListTile(
    BuildContext context,
    IconData icon,
    String title,
    Color color,
    VoidCallback onTap,
  ) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(0),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: color.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(icon, color: color, size: 20),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              Icon(
                Icons.chevron_right_rounded,
                color: Colors.grey.shade400,
                size: 24,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
