import 'dart:async';
import 'dart:io';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:share_plus/share_plus.dart';
import 'package:printing/printing.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:screenshot/screenshot.dart';
import 'package:cross_file/cross_file.dart';
import 'package:path_provider/path_provider.dart';
import 'package:intl/intl.dart';
import '../providers/verification_provider.dart';
import '../providers/profile_provider.dart';
import '../services/notification_service.dart';
import '../services/haptic_service.dart';
import '../services/sound_service.dart';

class VerificationScreen extends StatefulWidget {
  const VerificationScreen({super.key});

  @override
  State<VerificationScreen> createState() => _VerificationScreenState();
}

class _VerificationScreenState extends State<VerificationScreen> with SingleTickerProviderStateMixin {
  String? _selectedCredentialType;
  final TextEditingController _userIdController = TextEditingController();
  late AnimationController _animationController;
  final ScreenshotController _screenshotController = ScreenshotController();
  Timer? _expirationTimer;
  int _remainingSeconds = 300; // 5 minutes default

  final List<String> _credentialTypes = [
    'EducationalID',
    'IdentityCredential',
    'EuropeanStudentCard',
    'UniversityDegree',
  ];

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..repeat();
  }

  @override
  void dispose() {
    _userIdController.dispose();
    _animationController.dispose();
    _expirationTimer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<ProfileProvider>(
      builder: (context, profileProvider, child) {
        final activeProfile = profileProvider.activeProfile;
        
    return Scaffold(
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
                _buildProfileBanner(activeProfile),
                const SizedBox(height: 16),
                _buildHeaderCard(activeProfile),
            const SizedBox(height: 24),
                _buildCredentialSelectionCard(activeProfile),
            const SizedBox(height: 20),
            _buildGenerateButton(),
            const SizedBox(height: 16),
            _buildErrorCard(),
            _buildQRCodeCard(),
            _buildVerificationResultCard(),
          ],
        ),
      ),
        );
      },
    );
  }

  Widget _buildProfileBanner(profile) {
    if (profile == null) return const SizedBox.shrink();
    
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: profile.color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: profile.color.withOpacity(0.3)),
      ),
      child: Row(
        children: [
          Icon(profile.icon, color: profile.color, size: 20),
          const SizedBox(width: 8),
          Text(
            'Active Profile: ${profile.name}',
            style: TextStyle(
              color: _darkenColor(profile.color, 0.3),
              fontWeight: FontWeight.w600,
              fontSize: 13,
            ),
          ),
        ],
      ),
    ).animate().fadeIn(duration: 300.ms).slideY(begin: -0.1);
  }

  Widget _buildHeaderCard(profile) {
    final List<Color> colors = profile != null
        ? [_darkenColor(profile.color, 0.2), _darkenColor(profile.color, 0.1)]
        : [Colors.green.shade700, Colors.green.shade600];
    
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: colors,
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: (profile?.color ?? Colors.green).withOpacity(0.3),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(28.0),
        child: Column(
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white.withOpacity(0.2),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.verified_user_rounded,
                size: 48,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 20),
            const Text(
              'Verify Student Credentials',
              style: TextStyle(
                fontSize: 26,
                fontWeight: FontWeight.bold,
                color: Colors.white,
                letterSpacing: -0.5,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Scan QR code or enter verification URL',
              style: TextStyle(
                fontSize: 15,
                color: Colors.white.withOpacity(0.9),
                height: 1.4,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCredentialSelectionCard(profile) {
    final credentialTypes = profile?.defaultCredentialTypes ?? _credentialTypes;
    
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.grey.shade200),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  Icons.credit_card_rounded,
                  color: profile?.color ?? Colors.green.shade700,
                  size: 24,
                ),
                const SizedBox(width: 12),
                const Text(
                  'Credential Selection',
                  style: TextStyle(
                    fontSize: 19,
                    fontWeight: FontWeight.bold,
                    letterSpacing: -0.5,
                  ),
                ),
              ],
            ),
            if (profile != null) ...[
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: profile.color.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  'Using ${profile.name} profile defaults',
                  style: TextStyle(
                    fontSize: 12,
                    color: _darkenColor(profile.color, 0.3),
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
            ],
            const SizedBox(height: 20),
            DropdownButtonFormField<String>(
              value: _selectedCredentialType,
              decoration: InputDecoration(
                labelText: 'Credential Type',
                hintText: 'Select a credential type',
                prefixIcon: const Icon(Icons.category_rounded),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                filled: true,
                fillColor: Colors.grey.shade50,
              ),
              items: credentialTypes.map<DropdownMenuItem<String>>((type) {
                return DropdownMenuItem<String>(
                  value: type,
                  child: Text(_getCredentialDisplayName(type)),
                );
              }).toList(),
              onChanged: (value) {
                HapticService().lightImpact();
                setState(() {
                  _selectedCredentialType = value;
                });
              },
            ),
            const SizedBox(height: 20),
            TextField(
              controller: _userIdController,
              decoration: InputDecoration(
                labelText: 'Student ID (Optional)',
                hintText: 'Leave empty to verify any student',
                prefixIcon: const Icon(Icons.person_rounded),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                filled: true,
                fillColor: Colors.grey.shade50,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildGenerateButton() {
    return Consumer2<VerificationProvider, ProfileProvider>(
      builder: (context, provider, profileProvider, child) {
        final activeProfile = profileProvider.activeProfile;
        final buttonColors = _selectedCredentialType == null || provider.isLoading
            ? [Colors.grey.shade400, Colors.grey.shade500]
            : activeProfile != null
                ? [_darkenColor(activeProfile.color, 0.2), _darkenColor(activeProfile.color, 0.1)]
                : [Colors.green.shade700, Colors.green.shade600];
        final shadowColor = activeProfile?.color ?? Colors.green;
        
        return Container(
          height: 56,
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: buttonColors,
            ),
            borderRadius: BorderRadius.circular(16),
            boxShadow: _selectedCredentialType == null || provider.isLoading
                ? null
                : [
                    BoxShadow(
                      color: shadowColor.withOpacity(0.4),
                      blurRadius: 12,
                      offset: const Offset(0, 6),
                    ),
                  ],
          ),
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              onTap: provider.isLoading || _selectedCredentialType == null
                  ? null
                  : () async {
                      HapticService().mediumImpact();
                      await provider.initiateVerification(
                        credentialType: _selectedCredentialType!,
                        userId: _userIdController.text.isEmpty
                            ? null
                            : _userIdController.text,
                      );
                      // Show notification
                      await NotificationService().showVerificationPending();
                    },
              borderRadius: BorderRadius.circular(16),
              child: Center(
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    if (provider.isLoading)
                      const SizedBox(
                        width: 24,
                        height: 24,
                        child: CircularProgressIndicator(
                          strokeWidth: 2.5,
                          valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                        ),
                      )
                    else
                      const Icon(
                        Icons.qr_code_scanner_rounded,
                        color: Colors.white,
                        size: 24,
                      ),
                    const SizedBox(width: 12),
                    Text(
                      provider.isLoading
                          ? 'Generating Verification...'
                          : 'Generate Verification QR',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 17,
                        fontWeight: FontWeight.w600,
                        letterSpacing: 0.3,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildErrorCard() {
    return Consumer<VerificationProvider>(
      builder: (context, provider, child) {
        if (provider.error != null) {
          return Padding(
            padding: const EdgeInsets.only(top: 16),
            child: Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.red.shade50,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.red.shade200),
              ),
              child: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.red.shade100,
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(Icons.error_outline_rounded, color: Colors.red, size: 20),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      provider.error!,
                      style: TextStyle(
                        color: Colors.red.shade900,
                        fontSize: 14,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          );
        }
        return const SizedBox.shrink();
      },
    );
  }

  Widget _buildQRCodeCard() {
    return Consumer2<VerificationProvider, ProfileProvider>(
      builder: (context, provider, profileProvider, child) {
        if (provider.verificationUrl != null) {
          final activeProfile = profileProvider.activeProfile;
          final qrColor = activeProfile != null
              ? _darkenColor(activeProfile.color, 0.4)
              : Colors.green.shade900;
          final iconColor = activeProfile?.color ?? Colors.green.shade700;
          
          return Column(
            children: [
              const SizedBox(height: 24),
              Card(
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(20),
                  side: BorderSide(color: Colors.grey.shade200),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(24.0),
                  child: Column(
                    children: [
                      Row(
                        children: [
                          Icon(
                            Icons.qr_code_2_rounded,
                            color: iconColor,
                            size: 24,
                          ),
                          const SizedBox(width: 12),
                          const Text(
                            'Verification QR Code',
                            style: TextStyle(
                              fontSize: 19,
                              fontWeight: FontWeight.bold,
                              letterSpacing: -0.5,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 24),
                      Consumer<VerificationProvider>(
                        builder: (context, verificationProvider, child) {
                          final isVerified = verificationProvider.currentStatus == 'VERIFIED';
                          final isFailed = verificationProvider.currentStatus == 'FAILED';
                          final shouldDim = isVerified || isFailed;
                          
                          return Opacity(
                            opacity: shouldDim ? 0.4 : 1.0,
                            child: Container(
                              padding: const EdgeInsets.all(20),
                              decoration: BoxDecoration(
                                color: Colors.white,
                                borderRadius: BorderRadius.circular(16),
                                border: Border.all(
                                  color: shouldDim ? Colors.grey.shade300 : Colors.grey.shade200,
                                  width: shouldDim ? 2 : 1,
                                ),
                              ),
                              child: Stack(
                                children: [
                                  Screenshot(
                                    controller: _screenshotController,
                                    child: Stack(
                                      alignment: Alignment.center,
                                      children: [
                                        QrImageView(
                                          data: provider.verificationUrl!,
                                          version: QrVersions.auto,
                                          size: 250.0,
                                          backgroundColor: Colors.white,
                                          foregroundColor: shouldDim ? Colors.grey.shade600 : qrColor,
                                          errorCorrectionLevel: QrErrorCorrectLevel.H,
                                        ),
                                        // Logo overlay
                                        if (activeProfile != null)
                                          Container(
                                            width: 60,
                                            height: 60,
                                            decoration: BoxDecoration(
                                              color: Colors.white,
                                              shape: BoxShape.circle,
                                              border: Border.all(
                                                color: shouldDim ? Colors.grey.shade400 : qrColor,
                                                width: 3,
                                              ),
                                            ),
                                            child: Icon(
                                              activeProfile.icon,
                                              color: shouldDim 
                                                  ? Colors.grey.shade600 
                                                  : activeProfile.color,
                                              size: 35,
                                            ),
                                          ),
                                      ],
                                    ),
                                  ),
                                  if (shouldDim)
                                    Positioned.fill(
                                      child: Container(
                                        decoration: BoxDecoration(
                                          color: isVerified 
                                              ? Colors.green.shade50.withOpacity(0.3)
                                              : Colors.red.shade50.withOpacity(0.3),
                                          borderRadius: BorderRadius.circular(16),
                                        ),
                                        child: Center(
                                          child: Column(
                                            mainAxisAlignment: MainAxisAlignment.center,
                                            children: [
                                              Icon(
                                                isVerified 
                                                    ? Icons.check_circle_rounded 
                                                    : Icons.cancel_rounded,
                                                size: 48,
                                                color: isVerified 
                                                    ? Colors.green.shade700 
                                                    : Colors.red.shade700,
                                              ),
                                              const SizedBox(height: 8),
                                              Text(
                                                isVerified 
                                                    ? 'Verification Complete' 
                                                    : 'Verification Failed',
                                                style: TextStyle(
                                                  fontSize: 16,
                                                  fontWeight: FontWeight.bold,
                                                  color: isVerified 
                                                      ? Colors.green.shade900 
                                                      : Colors.red.shade900,
                                                ),
                                              ),
                                            ],
                                          ),
                                        ),
                                      ),
                                    ),
                                ],
                              ),
                            ),
                          );
                        },
                      )
                          .animate()
                          .fadeIn(duration: 400.ms)
                          .scale(begin: const Offset(0.9, 0.9), duration: 400.ms),
                      const SizedBox(height: 20),
                      Text(
                        'Scan this QR code with student wallet app',
                        style: TextStyle(
                          color: Colors.grey.shade600,
                          fontSize: 14,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 24),
                      if (provider.currentStatus != null)
                        _buildStatusIndicator(provider),
                      const SizedBox(height: 20),
                      // Expiration timer
                      if (provider.verificationUrl != null)
                        _buildExpirationTimer(),
                      const SizedBox(height: 20),
                      Row(
                        children: [
                          Expanded(
                            child: Consumer<ProfileProvider>(
                              builder: (context, profileProvider, child) {
                                final activeProfile = profileProvider.activeProfile;
                                final buttonColor = activeProfile?.color ?? Colors.green.shade700;
                                return _buildActionButton(
                                  icon: provider.isPolling ? Icons.stop_rounded : Icons.refresh_rounded,
                                  label: provider.isPolling ? 'Stop Polling' : 'Check Status',
                                  color: buttonColor,
                                  onPressed: provider.isPolling
                                      ? () {
                                          provider.stopPolling();
                                          ScaffoldMessenger.of(context).showSnackBar(
                                            SnackBar(
                                              content: const Text('Stopped polling. Use "Check Status" to verify manually.'),
                                              backgroundColor: buttonColor,
                                              behavior: SnackBarBehavior.floating,
                                              shape: RoundedRectangleBorder(
                                                borderRadius: BorderRadius.circular(10),
                                              ),
                                            ),
                                          );
                                        }
                                      : () => _checkStatus(context, provider),
                                );
                              },
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: _buildUrlOptionsButton(provider),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      // Share/Print buttons
                      Row(
                        children: [
                          Expanded(
                            child: _buildShareButton(activeProfile),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: _buildPrintButton(activeProfile),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ],
          );
        }
        return const SizedBox.shrink();
      },
    );
  }

  Widget _buildStatusIndicator(VerificationProvider provider) {
    final status = provider.currentStatus!;
    final isVerified = status == 'VERIFIED';
    final isFailed = status == 'FAILED';
    final isPending = status == 'PENDING';

    Color statusColor;
    IconData statusIcon;
    String statusText;

    if (isVerified) {
      statusColor = Colors.green;
      statusIcon = Icons.check_circle_rounded;
      statusText = 'Credentials Verified ✓';
    } else if (isFailed) {
      statusColor = Colors.red;
      statusIcon = Icons.error_rounded;
      statusText = 'Verification Failed';
    } else {
      statusColor = Colors.orange;
      statusIcon = Icons.hourglass_empty_rounded;
      statusText = provider.isPolling
          ? 'Waiting for student to scan QR...'
          : 'Ready for scanning';
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      decoration: BoxDecoration(
        color: statusColor.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: statusColor.withOpacity(0.3)),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            statusIcon,
            color: statusColor,
            size: 24,
          ),
          const SizedBox(width: 12),
            Flexible(
              child: Text(
                statusText,
                style: TextStyle(
                  fontWeight: FontWeight.w600,
                  color: statusColor,
                  fontSize: 15,
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildActionButton({
    required IconData icon,
    required String label,
    required Color color,
    required VoidCallback onPressed,
  }) {
    return Container(
      height: 48,
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onPressed,
          borderRadius: BorderRadius.circular(12),
          child: Center(
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(icon, color: Colors.white, size: 20),
                const SizedBox(width: 8),
                Text(
                  label,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildUrlOptionsButton(VerificationProvider provider) {
    return PopupMenuButton<String>(
      onSelected: (value) {
        if (value == 'open') {
          _openUrl(provider.verificationUrl!);
        } else if (value == 'copy') {
          _copyUrl(context, provider.verificationUrl!);
        }
      },
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
      ),
      child: Container(
        height: 48,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        decoration: BoxDecoration(
          border: Border.all(color: Colors.grey.shade300),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.link_rounded, size: 18, color: Colors.grey.shade700),
            const SizedBox(width: 6),
            const Flexible(
              child: Text(
                'URL Options',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: Colors.black87,
                ),
                overflow: TextOverflow.ellipsis,
              ),
            ),
            const SizedBox(width: 4),
            Icon(Icons.arrow_drop_down_rounded, size: 18, color: Colors.grey.shade600),
          ],
        ),
      ),
      itemBuilder: (BuildContext context) => [
        const PopupMenuItem<String>(
          value: 'open',
          child: Row(
            children: [
              Icon(Icons.open_in_new_rounded, size: 20),
              SizedBox(width: 12),
              Text('Open URL'),
            ],
          ),
        ),
        const PopupMenuItem<String>(
          value: 'copy',
          child: Row(
            children: [
              Icon(Icons.copy_rounded, size: 20),
              SizedBox(width: 12),
              Text('Copy URL'),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildVerificationResultCard() {
    return Consumer<VerificationProvider>(
      builder: (context, provider, child) {
        if (provider.currentResult != null && provider.currentStatus == 'VERIFIED') {
          final credentials = provider.currentResult!;
          Map<String, dynamic>? credentialData;
          
          if (credentials['credentialsByFormat'] != null) {
            final byFormat = credentials['credentialsByFormat'] as Map<String, dynamic>;
            if (byFormat['jwt_vc_json'] != null) {
              final jwtCreds = byFormat['jwt_vc_json'] as List;
              if (jwtCreds.isNotEmpty) {
                final firstCred = jwtCreds[0] as Map<String, dynamic>;
                if (firstCred['verifiableCredentials'] != null) {
                  final vcs = firstCred['verifiableCredentials'] as List;
                  if (vcs.isNotEmpty) {
                    final vc = vcs[0] as Map<String, dynamic>;
                    if (vc['payload'] != null) {
                      credentialData = vc['payload'] as Map<String, dynamic>;
                    }
                  }
                }
              }
            }
          }
          
          return Column(
            children: [
              const SizedBox(height: 24),
              Card(
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(20),
                  side: BorderSide(color: Colors.green.shade200, width: 2),
                ),
                child: Container(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: [
                        Colors.green.shade50,
                        Colors.white,
                      ],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  padding: const EdgeInsets.all(24.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: Colors.green.shade100,
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: const Icon(
                              Icons.check_circle_rounded,
                              color: Colors.green,
                              size: 28,
                            ),
                          ),
                          const SizedBox(width: 12),
                          const Text(
                            'Verification Successful',
                            style: TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.bold,
                              letterSpacing: -0.5,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 24),
                      _buildResultRow('Status', 'VERIFIED', Icons.verified_rounded),
                      _buildResultRow('Credential Type', provider.currentCredentialType ?? 'Unknown', Icons.credit_card_rounded),
                      if (credentialData != null && credentialData['vc'] != null) ...[
                        const SizedBox(height: 8),
                        Builder(
                          builder: (context) {
                            final vc = credentialData!['vc'] as Map<String, dynamic>;
                            if (vc['credentialSubject'] != null) {
                              final subject = vc['credentialSubject'] as Map<String, dynamic>;
                              return Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  if (subject['studentId'] != null)
                                    _buildResultRow('Student ID', subject['studentId'].toString(), Icons.badge_rounded),
                                  if (subject['givenName'] != null)
                                    _buildResultRow('Given Name', subject['givenName'].toString(), Icons.person_rounded),
                                  if (subject['familyName'] != null)
                                    _buildResultRow('Family Name', subject['familyName'].toString(), Icons.person_outline_rounded),
                                  if (subject['email'] != null)
                                    _buildResultRow('Email', subject['email'].toString(), Icons.email_rounded),
                                ],
                              );
                            }
                            return const SizedBox.shrink();
                          },
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            ],
          )
              .animate()
              .fadeIn(duration: 500.ms)
              .slideY(begin: 0.1, duration: 500.ms)
              .scale(begin: const Offset(0.95, 0.95), duration: 500.ms);
        }
        return const SizedBox.shrink();
      },
    );
  }

  Widget _buildResultRow(String label, String value, IconData icon) {
    return Consumer<ProfileProvider>(
      builder: (context, profileProvider, child) {
        final activeProfile = profileProvider.activeProfile;
        final iconColor = activeProfile?.color ?? Colors.green.shade700;
        
        return Padding(
          padding: const EdgeInsets.only(bottom: 16),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(
                icon,
                size: 20,
                color: iconColor,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      label,
                      style: TextStyle(
                        fontWeight: FontWeight.w600,
                        fontSize: 13,
                        color: Colors.grey.shade700,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: Colors.grey.shade200),
                      ),
                      child: Text(
                        value,
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  String _getCredentialDisplayName(String type) {
    switch (type) {
      case 'EducationalID':
        return 'Educational ID';
      case 'IdentityCredential':
        return 'Identity Credential';
      case 'EuropeanStudentCard':
        return 'European Student Card';
      case 'UniversityDegree':
        return 'University Degree';
      default:
        return type;
    }
  }

  Future<void> _checkStatus(BuildContext context, VerificationProvider provider) async {
    await provider.checkVerificationStatus();
    
    if (provider.currentResult != null && context.mounted) {
      final profileProvider = Provider.of<ProfileProvider>(context, listen: false);
      final activeProfile = profileProvider.activeProfile;
      final snackColor = activeProfile?.color ?? Colors.green.shade700;
      
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('Verification completed!'),
          backgroundColor: snackColor,
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(10),
          ),
        ),
      );
    }
  }

  Future<void> _openUrl(String url) async {
    final uri = Uri.parse(url);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    } else {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Could not open URL: $url'),
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(10),
            ),
          ),
        );
      }
    }
  }

  Future<void> _copyUrl(BuildContext context, String url) async {
    await Clipboard.setData(ClipboardData(text: url));
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('URL copied to clipboard'),
          backgroundColor: Colors.green.shade700,
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(10),
          ),
        ),
      );
    }
  }

  /// Helper function to darken a color
  Color _darkenColor(Color color, double amount) {
    assert(amount >= 0 && amount <= 1);
    final hsl = HSLColor.fromColor(color);
    final lightness = (hsl.lightness - amount).clamp(0.0, 1.0);
    return hsl.withLightness(lightness).toColor();
  }

  /// Build expiration timer widget
  Widget _buildExpirationTimer() {
    return StatefulBuilder(
      builder: (context, setState) {
        // Start timer when QR code is generated
        if (_expirationTimer == null || !_expirationTimer!.isActive) {
          _remainingSeconds = 300; // 5 minutes
          _expirationTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
            if (_remainingSeconds > 0) {
              setState(() {
                _remainingSeconds--;
              });
            } else {
              timer.cancel();
            }
          });
        }

        final minutes = _remainingSeconds ~/ 60;
        final seconds = _remainingSeconds % 60;
        final isExpiring = _remainingSeconds < 60;

        return Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          decoration: BoxDecoration(
            color: isExpiring ? Colors.orange.shade50 : Colors.blue.shade50,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(
              color: isExpiring ? Colors.orange.shade200 : Colors.blue.shade200,
            ),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                Icons.timer_rounded,
                color: isExpiring ? Colors.orange.shade700 : Colors.blue.shade700,
                size: 20,
              ),
              const SizedBox(width: 8),
              Text(
                'QR Code expires in: ${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}',
                style: TextStyle(
                  color: isExpiring ? Colors.orange.shade900 : Colors.blue.shade900,
                  fontWeight: FontWeight.w600,
                  fontSize: 14,
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  /// Build share button
  Widget _buildShareButton(profile) {
    return Consumer<VerificationProvider>(
      builder: (context, provider, child) {
        if (provider.verificationUrl == null) return const SizedBox.shrink();
        
        final buttonColor = profile?.color ?? Colors.green.shade700;
        return _buildActionButton(
          icon: Icons.share_rounded,
          label: 'Share QR',
          color: buttonColor,
          onPressed: () async {
            HapticService().lightImpact();
            try {
              final image = await _screenshotController.capture();
              if (image != null) {
                final tempDir = await getTemporaryDirectory();
                final file = File('${tempDir.path}/verification_qr_${DateTime.now().millisecondsSinceEpoch}.png');
                await file.writeAsBytes(image);
                await SharePlus.instance.share(
                  ShareParams(
                    files: [XFile(file.path)],
                    text: 'Verification QR Code',
                  ),
                );
              }
            } catch (e) {
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Error sharing QR code: $e'),
                    backgroundColor: Colors.red,
                  ),
                );
              }
            }
          },
        );
      },
    );
  }

  /// Build print button
  Widget _buildPrintButton(profile) {
    return Consumer<VerificationProvider>(
      builder: (context, provider, child) {
        if (provider.verificationUrl == null) return const SizedBox.shrink();
        
        final buttonColor = profile?.color ?? Colors.green.shade700;
        return _buildActionButton(
          icon: Icons.print_rounded,
          label: 'Print QR',
          color: buttonColor,
          onPressed: () async {
            HapticService().lightImpact();
            try {
              final image = await _screenshotController.capture();
              if (image != null) {
                final pdf = pw.Document();
                final imageProvider = pw.MemoryImage(image);
                
                pdf.addPage(
                  pw.Page(
                    pageFormat: PdfPageFormat.a4,
                    margin: const pw.EdgeInsets.all(40),
                    build: (pw.Context context) {
                      return pw.Center(
                        child: pw.Column(
                          mainAxisAlignment: pw.MainAxisAlignment.center,
                          children: [
                            pw.Text(
                              'Verification QR Code',
                              style: pw.TextStyle(
                                fontSize: 24,
                                fontWeight: pw.FontWeight.bold,
                              ),
                            ),
                            pw.SizedBox(height: 30),
                            pw.Image(
                              imageProvider,
                              width: 250,
                              height: 250,
                              fit: pw.BoxFit.contain,
                            ),
                            pw.SizedBox(height: 30),
                            pw.Text(
                              'Scan this QR code with your wallet app',
                              style: pw.TextStyle(
                                fontSize: 14,
                                color: PdfColors.grey700,
                              ),
                            ),
                            pw.SizedBox(height: 10),
                            pw.Text(
                              'Generated: ${DateFormat('yyyy-MM-dd HH:mm:ss').format(DateTime.now())}',
                              style: pw.TextStyle(
                                fontSize: 10,
                                color: PdfColors.grey500,
                              ),
                            ),
                          ],
                        ),
                      );
                    },
                  ),
                );
                
                await Printing.layoutPdf(
                  onLayout: (format) async => pdf.save(),
                );
              } else {
                throw Exception('Failed to capture QR code image');
              }
            } catch (e) {
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Error printing QR code: ${e.toString()}'),
                    backgroundColor: Colors.red,
                    duration: const Duration(seconds: 3),
                  ),
                );
              }
            }
          },
        );
      },
    );
  }
}
