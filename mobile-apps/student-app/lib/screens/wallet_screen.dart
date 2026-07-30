import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:provider/provider.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:url_launcher/url_launcher.dart';
import '../providers/credential_provider.dart';
import '../utils/url_guard.dart';
import 'qr_scanner_screen.dart';
import 'verification_history_screen.dart';

// Helper functions for credential card
String _formatDateHelper(String dateString) {
  try {
    final date = DateTime.parse(dateString);
    return '${date.day}/${date.month}/${date.year}';
  } catch (e) {
    return dateString;
  }
}

String _getCredentialNameHelper(String credentialType, [Map<String, dynamic>? credentialData]) {
  // Check if it's a conference session credential - use conference name
  if ((credentialType == 'ConferenceSessionCredential' || credentialType.startsWith('ConferenceSession_')) && credentialData != null) {
    final credentialSubject = credentialData['credentialSubject'] as Map<String, dynamic>?;
    final conferenceName = credentialSubject?['conferenceName']?.toString();
    final sessionTitle = credentialSubject?['sessionTitle']?.toString();
    
    if (conferenceName != null && conferenceName.isNotEmpty) {
      return conferenceName;
    }
    if (sessionTitle != null && sessionTitle.isNotEmpty) {
      return sessionTitle;
    }
  }
  
  switch (credentialType) {
    case 'EducationalID':
      return 'Educational ID';
    case 'IdentityCredential':
      return 'Identity Credential';
    case 'EuropeanStudentCard':
      return 'European Student Card';
    case 'UniversityDegree':
      return 'University Degree';
    case 'ConferenceSessionCredential':
      return 'Conference Session';
    default:
      if (credentialType.startsWith('ConferenceSession_')) {
        return credentialType.replaceFirst('ConferenceSession_', '').replaceAll(RegExp(r'([A-Z])'), ' \$1').trim();
      }
      return credentialType;
  }
}

class WalletScreen extends StatefulWidget {
  const WalletScreen({super.key});

  @override
  State<WalletScreen> createState() => _WalletScreenState();
}

class _WalletScreenState extends State<WalletScreen> {
  @override
  void initState() {
    super.initState();
    // Load wallet credentials when screen loads
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final provider = Provider.of<CredentialProvider>(context, listen: false);
      provider.loadWalletCredentials();
      if (provider.correlationId != null) {
        provider.refreshCredentials();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Consumer<CredentialProvider>(
        builder: (context, provider, child) {
          return Container(
            decoration: BoxDecoration(
              color: Colors.grey.shade50,
            ),
            child: CustomScrollView(
              slivers: [
                // Clean minimal header
                SliverToBoxAdapter(
                  child: Container(
                    padding: const EdgeInsets.fromLTRB(24, 60, 24, 24),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Wallet',
                          style: TextStyle(
                            fontSize: 34,
                            fontWeight: FontWeight.w600,
                            color: Colors.black87,
                            letterSpacing: -0.5,
                            height: 1.0,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          provider.walletCredentials != null && provider.walletCredentials!.isNotEmpty
                              ? '${provider.walletCredentials!.length} ${provider.walletCredentials!.length == 1 ? 'card' : 'cards'}'
                              : 'No cards yet',
                          style: TextStyle(
                            fontSize: 15,
                            color: Colors.grey.shade600,
                            fontWeight: FontWeight.w400,
                            letterSpacing: 0,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                // Clean action buttons
                SliverToBoxAdapter(
                  child: Container(
                    margin: const EdgeInsets.fromLTRB(24, 8, 24, 0),
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(16),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withOpacity(0.04),
                          blurRadius: 10,
                          offset: const Offset(0, 2),
                        ),
                      ],
                    ),
                    child: Column(
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: _buildModernActionButton(
                                context,
                                icon: Icons.qr_code_scanner_rounded,
                                label: 'Scan QR',
                                color: const Color(0xFF9C27B0),
                                onPressed: () async {
                                  try {
                                    final result = await Navigator.push(
                                      context,
                                      MaterialPageRoute(
                                        builder: (context) => const QRScannerScreen(),
                                      ),
                                    );
                                    
                                    // If session registration was successful, refresh wallet
                                    if (result != null && result['success'] == true) {
                                      if (mounted) {
                                        final credentialProvider = Provider.of<CredentialProvider>(context, listen: false);
                                        await credentialProvider.loadWalletCredentials();
                                        
                                        ScaffoldMessenger.of(context).showSnackBar(
                                          const SnackBar(
                                            content: Text('✅ Session credential added to wallet!'),
                                            backgroundColor: Colors.green,
                                            duration: Duration(seconds: 2),
                                          ),
                                        );
                                      }
                                    }
                                  } catch (e) {
                                    if (mounted) {
                                      ScaffoldMessenger.of(context).showSnackBar(
                                        SnackBar(
                                          content: Text('QR Scanner not available: $e'),
                                          backgroundColor: Colors.orange,
                                          duration: const Duration(seconds: 3),
                                        ),
                                      );
                                    }
                                  }
                                },
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: _buildModernActionButton(
                                context,
                                icon: Icons.refresh_rounded,
                                label: 'Refresh',
                                color: const Color(0xFF43A047),
                                onPressed: provider.isLoading
                                    ? null
                                    : () async {
                                        await provider.loadWalletCredentials();
                                      },
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        Row(
                          children: [
                            Expanded(
                              child: _buildModernActionButton(
                                context,
                                icon: Icons.history_rounded,
                                label: 'History',
                                color: const Color(0xFF9E9E9E),
                                onPressed: () {
                                  Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                      builder: (context) => const VerificationHistoryScreen(),
                                    ),
                                  );
                                },
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              flex: 2,
                              child: _buildModernActionButton(
                                context,
                                icon: provider.isLoading
                                    ? Icons.hourglass_empty_rounded
                                    : Icons.add_card_rounded,
                                label: provider.isLoading ? 'Issuing...' : 'Issue New Credentials',
                                color: const Color(0xFF1976D2),
                                isLoading: provider.isLoading,
                                onPressed: provider.isLoading
                                    ? null
                                    : () async {
                                        await provider.issueCredentials();
                                      },
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
                // Error Message
                if (provider.error != null)
                  SliverToBoxAdapter(
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                      child: Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: Colors.red.shade50,
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(
                            color: Colors.red.shade200,
                            width: 1,
                          ),
                        ),
                        child: Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(8),
                              decoration: BoxDecoration(
                                color: Colors.red.shade100,
                                borderRadius: BorderRadius.circular(10),
                              ),
                              child: const Icon(Icons.error_outline_rounded, color: Colors.red, size: 20),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Text(
                                provider.error!,
                                style: TextStyle(
                                  color: Colors.red.shade900,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                // Issuance Status
                if (provider.correlationId != null)
                  SliverToBoxAdapter(
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                      child: Container(
                        padding: const EdgeInsets.all(20),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(20),
                          gradient: LinearGradient(
                            colors: [
                              Colors.blue.shade50,
                              Colors.blue.shade100.withOpacity(0.5),
                            ],
                          ),
                          border: Border.all(
                            color: Colors.blue.shade200.withOpacity(0.5),
                            width: 1,
                          ),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(
                                    color: Colors.blue.shade700,
                                    borderRadius: BorderRadius.circular(10),
                                  ),
                                  child: const Icon(
                                    Icons.info_outline_rounded,
                                    color: Colors.white,
                                    size: 20,
                                  ),
                                ),
                                const SizedBox(width: 12),
                                const Text(
                                  'Issuance Status',
                                  style: TextStyle(
                                    fontSize: 18,
                                    fontWeight: FontWeight.bold,
                                    letterSpacing: 0.15,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 16),
                            _buildStatusRow('Status', provider.status ?? 'Unknown', Icons.check_circle_outline_rounded),
                            if (provider.progress != null) ...[
                              const SizedBox(height: 12),
                              _buildStatusRow('Progress', '${provider.progress}%', Icons.trending_up_rounded),
                              const SizedBox(height: 12),
                              ClipRRect(
                                borderRadius: BorderRadius.circular(8),
                                child: LinearProgressIndicator(
                                  value: provider.progress! / 100,
                                  minHeight: 8,
                                  backgroundColor: Colors.grey.shade300,
                                  valueColor: AlwaysStoppedAnimation<Color>(Colors.blue.shade700),
                                ),
                              ),
                            ],
                            if (provider.message != null) ...[
                              const SizedBox(height: 12),
                              _buildStatusRow('Message', provider.message!, Icons.message_outlined),
                            ],
                          ],
                        ),
                      ),
                    ),
                  ),
                // Credentials List or Offers or Empty State
                if (provider.walletCredentials != null && provider.walletCredentials!.isNotEmpty) ...[
                  // Apple Wallet-style stacked cards
                  SliverToBoxAdapter(
                    child: _WalletCardStack(
                      credentials: provider.walletCredentials!,
                      onCardTap: (credential, credentialData, credentialType) {
                        _showCredentialDetails(context, credential, credentialData, credentialType);
                      },
                    ),
                  ),
                ] else if (provider.credentialOfferUrls != null &&
                    provider.credentialOfferUrls!.isNotEmpty &&
                    provider.credentialTypes != null &&
                    provider.credentialTypes!.isNotEmpty) ...[
                  SliverToBoxAdapter(
                    child: Padding(
                      padding: const EdgeInsets.fromLTRB(20, 24, 20, 12),
                      child: Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.all(8),
                            decoration: BoxDecoration(
                              color: Colors.orange.shade100,
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Icon(
                              Icons.notifications_active_rounded,
                              color: Colors.orange.shade700,
                              size: 20,
                            ),
                          ),
                          const SizedBox(width: 12),
                          const Text(
                            'New Credentials Available',
                            style: TextStyle(
                              fontSize: 22,
                              fontWeight: FontWeight.bold,
                              letterSpacing: -0.5,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  SliverList(
                    delegate: SliverChildBuilderDelegate(
                      (context, index) {
                        final offerUrl = provider.credentialOfferUrls![index];
                        final credentialType = (index < provider.credentialTypes!.length) 
                            ? provider.credentialTypes![index] 
                            : 'Credential';
                        return Padding(
                          padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
                          child: _buildOfferCard(context, offerUrl, credentialType),
                        );
                      },
                      childCount: provider.credentialOfferUrls!.length,
                    ),
                  ),
                ] else if (provider.walletCredentials != null && provider.walletCredentials!.isEmpty && !provider.isLoading)
                  SliverFillRemaining(
                    hasScrollBody: false,
                    child: Center(
                      child: Padding(
                        padding: const EdgeInsets.all(32.0),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Container(
                              padding: const EdgeInsets.all(32),
                            decoration: BoxDecoration(
                                gradient: LinearGradient(
                                  begin: Alignment.topLeft,
                                  end: Alignment.bottomRight,
                                  colors: [
                                    Colors.blue.shade50,
                                    Colors.blue.shade100.withOpacity(0.5),
                                  ],
                                ),
                              shape: BoxShape.circle,
                                border: Border.all(
                                  color: Colors.blue.shade200.withOpacity(0.5),
                                  width: 2,
                                ),
                                boxShadow: [
                                  BoxShadow(
                                    color: Colors.blue.withOpacity(0.1),
                                    blurRadius: 20,
                                    offset: const Offset(0, 8),
                                  ),
                                ],
                            ),
                            child: Icon(
                              Icons.account_balance_wallet_outlined,
                                size: 72,
                                color: Colors.blue.shade400,
                            ),
                          ),
                            const SizedBox(height: 32),
                          Text(
                            'No credentials yet',
                            style: TextStyle(
                                fontSize: 24,
                              fontWeight: FontWeight.bold,
                                color: Colors.grey.shade800,
                                letterSpacing: -0.5,
                            ),
                          ),
                            const SizedBox(height: 12),
                          Text(
                            'Issue new credentials to get started',
                              textAlign: TextAlign.center,
                            style: TextStyle(
                                fontSize: 16,
                              color: Colors.grey.shade600,
                                height: 1.5,
                              ),
                            ),
                            const SizedBox(height: 32),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
                              decoration: BoxDecoration(
                                gradient: LinearGradient(
                                  colors: [
                                    Colors.blue.shade700,
                                    Colors.blue.shade800,
                                  ],
                                ),
                                borderRadius: BorderRadius.circular(16),
                                boxShadow: [
                                  BoxShadow(
                                    color: Colors.blue.withOpacity(0.3),
                                    blurRadius: 12,
                                    offset: const Offset(0, 6),
                                  ),
                                ],
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: const [
                                  Icon(
                                    Icons.add_circle_outline_rounded,
                                    color: Colors.white,
                                    size: 20,
                                  ),
                                  SizedBox(width: 8),
                                  Text(
                                    'Get Started',
                                    style: TextStyle(
                                      color: Colors.white,
                                      fontSize: 16,
                                      fontWeight: FontWeight.w600,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildActionButton(
    BuildContext context, {
    required IconData icon,
    required String label,
    required MaterialColor color,
    required VoidCallback? onPressed,
    bool isLoading = false,
  }) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        gradient: onPressed != null
            ? LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [
                  color.shade700,
                  color.shade800,
                ],
              )
            : null,
        color: onPressed == null ? color.shade300 : null,
        boxShadow: onPressed != null
            ? [
          BoxShadow(
                  color: color.withOpacity(0.3),
                  blurRadius: 12,
                  offset: const Offset(0, 6),
                  spreadRadius: 0,
                ),
                BoxShadow(
                  color: color.withOpacity(0.15),
                  blurRadius: 4,
                  offset: const Offset(0, 2),
                ),
              ]
            : null,
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onPressed,
          borderRadius: BorderRadius.circular(18),
          child: Container(
            padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 20),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                if (isLoading)
                  SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(
                      strokeWidth: 2.5,
                  valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                ),
              )
                else
                  Icon(icon, size: 22, color: Colors.white),
                const SizedBox(width: 10),
                Text(
                  label,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: Colors.white,
                    letterSpacing: 0.3,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildModernActionButton(
    BuildContext context, {
    required IconData icon,
    required String label,
    required Color color,
    required VoidCallback? onPressed,
    bool isLoading = false,
  }) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onPressed,
            borderRadius: BorderRadius.circular(16),
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 16),
          decoration: BoxDecoration(
            color: onPressed != null ? color.withOpacity(0.1) : Colors.grey.shade100,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(
              color: onPressed != null ? color.withOpacity(0.3) : Colors.grey.shade300,
              width: 1.5,
            ),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (isLoading)
                SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    valueColor: AlwaysStoppedAnimation<Color>(color),
                  ),
                )
              else
                Icon(icon, size: 20, color: onPressed != null ? color : Colors.grey),
              const SizedBox(width: 8),
              Text(
                label,
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: onPressed != null ? color : Colors.grey,
                  letterSpacing: 0.2,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCredentialCard(
    BuildContext context,
    Map<String, dynamic> credential,
    Map<String, dynamic> credentialData,
    String credentialType,
    int index,
  ) {
    final colors = _getCredentialColors(credentialType);
    final icon = _getCredentialIconData(credentialType);
    
    // Extract credential subject data
    final credentialSubject = credentialData['credentialSubject'] as Map<String, dynamic>?;
    
    // Extract student name - try multiple fields
    String studentName = '';
    if (credentialSubject != null) {
      final givenName = credentialSubject['givenName']?.toString() ?? '';
      final familyName = credentialSubject['familyName']?.toString() ?? '';
      final name = credentialSubject['name']?.toString() ?? '';
      
      if (givenName.isNotEmpty && familyName.isNotEmpty) {
        studentName = '$givenName $familyName'.trim();
      } else if (givenName.isNotEmpty) {
        studentName = givenName;
      } else if (name.isNotEmpty) {
        studentName = name;
      }
    }
    
    // Extract student ID - prefer studentId, then id, but filter out DIDs
    String? rawStudentId = credentialSubject?['studentId']?.toString() ?? 
                           credentialSubject?['id']?.toString();
    final studentId = (rawStudentId != null && 
                       !rawStudentId.startsWith('did:') && 
                       rawStudentId.length < 50) 
        ? rawStudentId 
        : '';
    
    final courseName = credentialSubject?['courseName'] ?? '';
    
    // Extract dates (try both formats)
    final issuanceDate = credentialData['issuanceDate'] as String? ?? 
                         credentialData['issued'] as String? ??
                         credential['addedOn'] as String?;
    final expirationDate = credentialData['expirationDate'] as String? ?? 
                           credentialData['expiration'] as String? ??
                           credentialData['validUntil'] as String?;
    
    // Extract issuer (check both parsedDocument and top-level)
    final issuer = credentialData['issuer'] ?? credential['issuer'];
    
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.0, end: 1.0),
      duration: Duration(milliseconds: 300 + (index * 100)),
      curve: Curves.easeOutCubic,
      builder: (context, value, child) {
        return Transform.scale(
          scale: 0.9 + (value * 0.1),
          child: Opacity(
            opacity: value,
            child: child,
          ),
        );
      },
      child: _AnimatedCredentialCard(
        credential: credential,
        credentialData: credentialData,
        credentialType: credentialType,
        index: index,
        colors: colors,
        icon: icon,
        studentName: studentName,
        studentId: studentId,
        courseName: courseName,
        issuanceDate: issuanceDate,
        expirationDate: expirationDate,
      onTap: () => _showCredentialDetails(context, credential, credentialData, credentialType),
      ),
    );
  }

  Widget _buildOfferCard(BuildContext context, String offerUrl, String credentialType) {
    final colors = _getCredentialColors(credentialType);
    final icon = _getCredentialIconData(credentialType);
    
    return Container(
      margin: const EdgeInsets.fromLTRB(20, 0, 20, 20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            Colors.white,
            colors[0].withOpacity(0.05),
          ],
        ),
        border: Border.all(
          color: colors[0].withOpacity(0.2),
          width: 2,
        ),
        boxShadow: [
          BoxShadow(
            color: colors[0].withOpacity(0.15),
            blurRadius: 16,
            offset: const Offset(0, 6),
          ),
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: colors[0].withOpacity(0.1),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Icon(
                    icon,
                    color: colors[0],
                    size: 28,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _getCredentialName(credentialType, null),
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          letterSpacing: -0.5,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Ready to accept',
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.grey.shade600,
                        ),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: Colors.orange.shade100,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Icon(
                    Icons.notifications_active_rounded,
                    color: Colors.orange.shade700,
                    size: 20,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: () => _showQRCode(context, offerUrl),
                    icon: const Icon(Icons.qr_code_rounded, size: 18),
                    label: const Text('QR Code'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: colors[0],
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      elevation: 0,
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () => _openUrl(offerUrl),
                    icon: const Icon(Icons.open_in_new_rounded, size: 18),
                    label: const Text('Open'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: colors[0],
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      side: BorderSide(color: colors[0], width: 1.5),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatusRow(String label, String value, IconData icon) {
    return Row(
      children: [
        Icon(
          icon,
          size: 18,
          color: Colors.blue.shade700,
        ),
        const SizedBox(width: 12),
        Expanded(
          child: RichText(
            text: TextSpan(
              style: TextStyle(
                fontSize: 14,
                color: Colors.grey.shade700,
              ),
              children: [
                TextSpan(
                  text: '$label: ',
                  style: const TextStyle(fontWeight: FontWeight.w500),
                ),
                TextSpan(
                  text: value,
                  style: TextStyle(
                    fontWeight: FontWeight.w600,
                    color: Colors.grey.shade900,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  IconData _getCredentialIconData(String credentialType) {
    switch (credentialType) {
      case 'EducationalID':
        return Icons.school_rounded;
      case 'IdentityCredential':
        return Icons.verified_user_rounded;
      case 'EuropeanStudentCard':
        return Icons.credit_card_rounded;
      case 'UniversityDegree':
        return Icons.workspace_premium_rounded;
      default:
        return Icons.badge_rounded;
    }
  }

  List<Color> _getCredentialColors(String credentialType) {
    switch (credentialType) {
      case 'EducationalID':
        return [
          const Color(0xFF1976D2),
          const Color(0xFF1565C0),
        ];
      case 'IdentityCredential':
        return [
          const Color(0xFF43A047),
          const Color(0xFF388E3C),
        ];
      case 'EuropeanStudentCard':
        return [
          const Color(0xFFFF9800),
          const Color(0xFFF57C00),
        ];
      case 'UniversityDegree':
        return [
          const Color(0xFF9C27B0),
          const Color(0xFF7B1FA2),
        ];
      default:
        return [
          Colors.grey.shade700,
          Colors.grey.shade900,
        ];
    }
  }

  String _formatDate(String dateString) {
    try {
      final date = DateTime.parse(dateString);
      return '${date.day}/${date.month}/${date.year}';
    } catch (e) {
      return dateString;
    }
  }

  String _getCredentialName(String credentialType, Map<String, dynamic>? credentialData) {
    // Check if it's a conference session credential - use conference name
    if (credentialType == 'ConferenceSessionCredential' && credentialData != null) {
      final credentialSubject = credentialData['credentialSubject'] as Map<String, dynamic>?;
      final conferenceName = credentialSubject?['conferenceName']?.toString();
      final sessionTitle = credentialSubject?['sessionTitle']?.toString();
      
      if (conferenceName != null && conferenceName.isNotEmpty) {
        return conferenceName;
      }
      if (sessionTitle != null && sessionTitle.isNotEmpty) {
        return sessionTitle;
      }
    }
    
    // Check for displayName in credential data
    if (credentialData != null) {
      final displayName = credentialData['displayName']?.toString() ?? 
                         credentialData['name']?.toString();
      if (displayName != null && displayName.isNotEmpty) {
        return displayName;
      }
    }
    
    switch (credentialType) {
      case 'EducationalID':
        return 'Educational ID';
      case 'IdentityCredential':
        return 'Identity Credential';
      case 'EuropeanStudentCard':
        return 'European Student Card';
      case 'UniversityDegree':
        return 'University Degree';
      case 'ConferenceSessionCredential':
        return 'Conference Session';
      default:
        // Check if it starts with ConferenceSession_ - extract conference name
        if (credentialType.startsWith('ConferenceSession_')) {
          return credentialType.replaceFirst('ConferenceSession_', '').replaceAll(RegExp(r'([A-Z])'), ' \$1').trim();
        }
        return credentialType;
    }
  }

  String _extractIssuerId(dynamic issuer) {
    if (issuer == null) return 'Unknown';
    if (issuer is Map) {
      // Try to get id first
      if (issuer['id'] != null) {
        final id = issuer['id'].toString();
        // If it's a DID, extract a readable name
        if (id.startsWith('did:')) {
          // Try to get name from issuer map
          if (issuer['name'] != null) {
            return issuer['name'].toString();
          }
          // Extract institution name if available
          if (issuer['institutionName'] != null) {
            return issuer['institutionName'].toString();
          }
          // Return shortened DID
          return id.length > 30 ? '${id.substring(0, 27)}...' : id;
        }
        return id;
      }
      // Try name field
      if (issuer['name'] != null) {
        return issuer['name'].toString();
      }
      // Try institutionName
      if (issuer['institutionName'] != null) {
        return issuer['institutionName'].toString();
      }
      return issuer.toString();
    }
    return issuer.toString();
  }

  void _showCredentialDetails(
    BuildContext context,
    Map<String, dynamic> credential,
    Map<String, dynamic> credentialData,
    String credentialType,
  ) {
    final credentialSubject = credentialData['credentialSubject'] as Map<String, dynamic>?;
    final colors = _getCredentialColors(credentialType);
    
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      enableDrag: true,
      builder: (context) => DraggableScrollableSheet(
        initialChildSize: 0.85,
        minChildSize: 0.5,
        maxChildSize: 0.95,
        builder: (context, scrollController) => Container(
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: const BorderRadius.vertical(top: Radius.circular(32)),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.2),
                blurRadius: 30,
                offset: const Offset(0, -10),
                spreadRadius: 5,
              ),
            ],
          ),
          child: Column(
            children: [
              // Drag Handle
              Container(
                margin: const EdgeInsets.only(top: 12, bottom: 8),
                width: 40,
                height: 5,
                decoration: BoxDecoration(
                  color: Colors.grey.shade300,
                  borderRadius: BorderRadius.circular(3),
                ),
              ),
              Expanded(
                child: SingleChildScrollView(
                  controller: scrollController,
                  padding: const EdgeInsets.all(24.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Header with Gradient Background
                      Container(
                        padding: const EdgeInsets.all(24),
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            begin: Alignment.topLeft,
                            end: Alignment.bottomRight,
                            colors: [
                              colors[0],
                              colors.length > 1 ? colors[1] : colors[0].withOpacity(0.8),
                            ],
                          ),
                          borderRadius: BorderRadius.circular(24),
                          boxShadow: [
                            BoxShadow(
                              color: colors[0].withOpacity(0.3),
                              blurRadius: 20,
                              offset: const Offset(0, 8),
                            ),
                          ],
                        ),
                        child: Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(14),
                              decoration: BoxDecoration(
                                color: Colors.white.withOpacity(0.25),
                                borderRadius: BorderRadius.circular(18),
                                border: Border.all(
                                  color: Colors.white.withOpacity(0.3),
                                  width: 1.5,
                                ),
                              ),
                              child: Icon(
                                _getCredentialIconData(credentialType),
                                color: Colors.white,
                                size: 32,
                              ),
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    _getCredentialName(credentialType, credentialData),
                                    style: const TextStyle(
                                      fontSize: 24,
                                      fontWeight: FontWeight.bold,
                                      color: Colors.white,
                                      letterSpacing: -0.5,
                                    ),
                                  ),
                                  const SizedBox(height: 4),
                                  Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                    decoration: BoxDecoration(
                                      color: Colors.white.withOpacity(0.25),
                                      borderRadius: BorderRadius.circular(12),
                                    ),
                                    child: const Text(
                                      'Verifiable Credential',
                                      style: TextStyle(
                                        fontSize: 12,
                                        color: Colors.white,
                                        fontWeight: FontWeight.w600,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 24),
                      // Student Information Section
                      if (credentialSubject != null) ...[
                        Container(
                          padding: const EdgeInsets.all(20),
                          decoration: BoxDecoration(
                            gradient: LinearGradient(
                              begin: Alignment.topLeft,
                              end: Alignment.bottomRight,
                              colors: [
                                colors[0].withOpacity(0.08),
                                colors.length > 1 ? colors[1].withOpacity(0.05) : colors[0].withOpacity(0.03),
                              ],
                            ),
                            borderRadius: BorderRadius.circular(20),
                            border: Border.all(
                              color: colors[0].withOpacity(0.2),
                              width: 1.5,
                            ),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Container(
                                    padding: const EdgeInsets.all(8),
                                    decoration: BoxDecoration(
                                      color: colors[0].withOpacity(0.15),
                                      borderRadius: BorderRadius.circular(12),
                                    ),
                                    child: Icon(
                                      Icons.person_rounded,
                                      color: colors[0],
                                      size: 22,
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  const Text(
                                    'Student Information',
                                    style: TextStyle(
                                      fontSize: 20,
                                      fontWeight: FontWeight.bold,
                                      letterSpacing: -0.5,
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 20),
                              // Student Name
                              if ((credentialSubject['givenName'] != null || credentialSubject['name'] != null)) ...[
                                _buildModernDetailRow(
                                  Icons.person_outline_rounded,
                                  'Name',
                                  _getStudentName(credentialSubject),
                                  colors[0],
                                ),
                                const SizedBox(height: 12),
                              ],
                              // Student ID
                              if (credentialSubject['studentId'] != null) ...[
                                _buildModernDetailRow(
                                  Icons.badge_outlined,
                                  'Student ID',
                                  credentialSubject['studentId'].toString(),
                                  colors[0],
                                ),
                                const SizedBox(height: 12),
                              ],
                              // Course Name
                              if (credentialSubject['courseName'] != null) ...[
                                _buildModernDetailRow(
                                  Icons.school_outlined,
                                  'Course',
                                  credentialSubject['courseName'].toString(),
                                  colors[0],
                                ),
                                const SizedBox(height: 12),
                              ],
                              // Conference Session specific info
                              if (credentialType == 'ConferenceSessionCredential' || credentialType.startsWith('ConferenceSession_')) ...[
                                if (credentialSubject['conferenceName'] != null) ...[
                                  _buildModernDetailRow(
                                    Icons.event_note_rounded,
                                    'Conference',
                                    credentialSubject['conferenceName'].toString(),
                                    colors[0],
                                  ),
                                  const SizedBox(height: 12),
                                ],
                                if (credentialSubject['sessionTitle'] != null) ...[
                                  _buildModernDetailRow(
                                    Icons.event_rounded,
                                    'Session',
                                    credentialSubject['sessionTitle'].toString(),
                                    colors[0],
                                  ),
                                  const SizedBox(height: 12),
                                ],
                                if (credentialSubject['location'] != null) ...[
                                  _buildModernDetailRow(
                                    Icons.location_on_outlined,
                                    'Location',
                                    credentialSubject['location'].toString(),
                                    colors[0],
                                  ),
                                  const SizedBox(height: 12),
                                ],
                                if (credentialSubject['startTime'] != null) ...[
                                  _buildModernDetailRow(
                                    Icons.access_time_rounded,
                                    'Start Time',
                                    _formatDateTime(credentialSubject['startTime'].toString()),
                                    colors[0],
                                  ),
                                  const SizedBox(height: 12),
                                ],
                                if (credentialSubject['endTime'] != null) ...[
                                  _buildModernDetailRow(
                                    Icons.access_time_filled_rounded,
                                    'End Time',
                                    _formatDateTime(credentialSubject['endTime'].toString()),
                                    colors[0],
                                  ),
                                  const SizedBox(height: 12),
                                ],
                                if (credentialSubject['registrationDate'] != null) ...[
                                  _buildModernDetailRow(
                                    Icons.how_to_reg_rounded,
                                    'Registered',
                                    _formatDateTime(credentialSubject['registrationDate'].toString()),
                                    colors[0],
                                  ),
                                ],
                              ],
                            ],
                          ),
                        ),
                        const SizedBox(height: 20),
                      ],
                      // Credential Information Section
                      if (credentialSubject != null) ...[
                        Container(
                          padding: const EdgeInsets.all(20),
                          decoration: BoxDecoration(
                            color: Colors.grey.shade50,
                            borderRadius: BorderRadius.circular(20),
                            border: Border.all(
                              color: Colors.grey.shade200,
                              width: 1.5,
                            ),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Icon(
                                    Icons.info_outline_rounded,
                                    color: colors[0],
                                    size: 22,
                                  ),
                                  const SizedBox(width: 12),
                                  const Text(
                                    'Credential Information',
                                    style: TextStyle(
                                      fontSize: 20,
                                      fontWeight: FontWeight.bold,
                                      letterSpacing: -0.5,
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 20),
                              ...credentialSubject.entries.map((entry) {
                                // Skip fields already shown in Student Information section
                                final key = entry.key;
                                if (key == 'givenName' || key == 'familyName' || key == 'name' ||
                                    key == 'studentId' || key == 'courseName' ||
                                    key == 'conferenceName' || key == 'sessionTitle' ||
                                    key == 'location' || key == 'startTime' || key == 'endTime' ||
                                    key == 'registrationDate' || key == 'id' ||
                                    entry.value == null || entry.value.toString().isEmpty) {
                                  return const SizedBox.shrink();
                                }
                                return Container(
                                  margin: const EdgeInsets.only(bottom: 16),
                                  padding: const EdgeInsets.all(16),
                                  decoration: BoxDecoration(
                                    color: Colors.white,
                                    borderRadius: BorderRadius.circular(16),
                                    border: Border.all(
                                      color: Colors.grey.shade200,
                                      width: 1,
                                    ),
                                  ),
                                  child: Row(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Container(
                                        padding: const EdgeInsets.all(8),
                                        decoration: BoxDecoration(
                                          color: colors[0].withOpacity(0.1),
                                          borderRadius: BorderRadius.circular(10),
                                        ),
                                        child: Icon(
                                          Icons.circle,
                                          size: 6,
                                          color: colors[0],
                                        ),
                                      ),
                                      const SizedBox(width: 12),
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              _formatFieldName(entry.key),
                                              style: TextStyle(
                                                fontSize: 12,
                                                color: Colors.grey.shade600,
                                                fontWeight: FontWeight.w600,
                                                letterSpacing: 0.3,
                                              ),
                                            ),
                                            const SizedBox(height: 4),
                                            Text(
                                              entry.value.toString(),
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
                              }),
                            ],
                          ),
                        ),
                        const SizedBox(height: 20),
                      ],
                      // Details Section
                      Container(
                        padding: const EdgeInsets.all(20),
                        decoration: BoxDecoration(
                          color: Colors.grey.shade50,
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(
                            color: Colors.grey.shade200,
                            width: 1.5,
                          ),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Icon(
                                  Icons.description_outlined,
                                  color: colors[0],
                                  size: 22,
                                ),
                                const SizedBox(width: 12),
                                const Text(
                                  'Details',
                                  style: TextStyle(
                                    fontSize: 20,
                                    fontWeight: FontWeight.bold,
                                    letterSpacing: -0.5,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 20),
                            _buildModernDetailRow(
                              Icons.calendar_today_rounded,
                              'Issued',
                              _formatDateForDetail(credentialData['issuanceDate']?.toString() ?? credential['addedOn']?.toString() ?? 'Unknown'),
                              colors[0],
                            ),
                            if (credentialData['expirationDate'] != null) ...[
                              const SizedBox(height: 12),
                              _buildModernDetailRow(
                                Icons.event_rounded,
                                'Expires',
                                _formatDateForDetail(credentialData['expirationDate'].toString()),
                                colors[0],
                              ),
                            ],
                            if (credentialData['issuer'] != null || credential['issuer'] != null) ...[
                              const SizedBox(height: 12),
                              _buildModernDetailRow(
                                Icons.business_rounded,
                                'Issuer',
                                _extractIssuerId(credentialData['issuer'] ?? credential['issuer']),
                                colors[0],
                              ),
                            ],
                            if (credential['id'] != null) ...[
                              const SizedBox(height: 12),
                              _buildModernDetailRow(
                                Icons.fingerprint_rounded,
                                'Credential ID',
                                credential['id'].toString(),
                                colors[0],
                              ),
                            ],
                          ],
                        ),
                      ),
                      const SizedBox(height: 24),
                      SizedBox(
                        width: double.infinity,
                        child: ElevatedButton(
                          onPressed: () => Navigator.pop(context),
                          style: ElevatedButton.styleFrom(
                            padding: const EdgeInsets.symmetric(vertical: 18),
                            backgroundColor: colors[0],
                            foregroundColor: Colors.white,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(16),
                            ),
                            elevation: 0,
                          ),
                          child: const Text(
                            'Close',
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w600,
                              letterSpacing: 0.5,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildModernDetailRow(IconData icon, String label, String value, Color color) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: Colors.grey.shade200,
          width: 1,
        ),
      ),
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
                    fontWeight: FontWeight.w600,
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

  Widget _buildDetailRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 100,
            child: Text(
              label,
              style: TextStyle(
                fontSize: 14,
                color: Colors.grey.shade600,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
        ],
      ),
    );
  }

  String _getStudentName(Map<String, dynamic> credentialSubject) {
    final givenName = credentialSubject['givenName']?.toString() ?? '';
    final familyName = credentialSubject['familyName']?.toString() ?? '';
    final name = credentialSubject['name']?.toString() ?? '';
    
    if (givenName.isNotEmpty && familyName.isNotEmpty) {
      return '$givenName $familyName';
    } else if (givenName.isNotEmpty) {
      return givenName;
    } else if (name.isNotEmpty) {
      return name;
    }
    return 'Unknown';
  }

  String _formatDateTime(String dateTimeString) {
    try {
      final dateTime = DateTime.parse(dateTimeString);
      final months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      final hours = dateTime.hour.toString().padLeft(2, '0');
      final minutes = dateTime.minute.toString().padLeft(2, '0');
      return '${dateTime.day} ${months[dateTime.month - 1]} ${dateTime.year} at $hours:$minutes';
    } catch (e) {
      return dateTimeString;
    }
  }

  String _formatDateForDetail(String dateString) {
    if (dateString == 'Unknown' || dateString.isEmpty) return dateString;
    try {
      final date = DateTime.parse(dateString);
      final months = ['January', 'February', 'March', 'April', 'May', 'June',
                      'July', 'August', 'September', 'October', 'November', 'December'];
      return '${months[date.month - 1]} ${date.day}, ${date.year}';
    } catch (e) {
      return dateString;
    }
  }

  String _formatTimeOnly(String dateTimeString) {
    try {
      final dateTime = DateTime.parse(dateTimeString);
      final hours = dateTime.hour.toString().padLeft(2, '0');
      final minutes = dateTime.minute.toString().padLeft(2, '0');
      return '$hours:$minutes';
    } catch (e) {
      return dateTimeString;
    }
  }

  String _formatFieldName(String key) {
    return key
        .replaceAllMapped(RegExp(r'([A-Z])'), (match) => ' ${match.group(1)}')
        .trim()
        .split(' ')
        .map((word) => word[0].toUpperCase() + word.substring(1))
        .join(' ');
  }

  void _showPasteUrlDialog(BuildContext context) {
    final TextEditingController urlController = TextEditingController();
    
    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Paste Verification URL'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text(
                'Paste the verification URL from the verifier app:',
                style: TextStyle(fontSize: 14),
              ),
              const SizedBox(height: 16),
              TextField(
                controller: urlController,
                decoration: const InputDecoration(
                  hintText: 'openid4vp://authorize?...',
                  border: OutlineInputBorder(),
                ),
                maxLines: 3,
                autofocus: true,
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel'),
            ),
            ElevatedButton(
              onPressed: () async {
                final url = urlController.text.trim();
                if (url.isNotEmpty) {
                  Navigator.pop(context);
                  await _handleVerificationUrl(url);
                }
              },
              child: const Text('Open'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _handleVerificationUrl(String url) async {
    if (!mounted) return;
    
    try {
      // Check if it's an openid4vp:// URL (verification request)
      if (url.startsWith('openid4vp://') || url.startsWith('openid4vci://')) {
        final uri = Uri.parse(url);

        if (!UrlGuard.isAllowed(uri)) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('Blocked: this verification link is not allowed.'),
                backgroundColor: Colors.red,
              ),
            );
          }
        } else if (await canLaunchUrl(uri)) {
          await launchUrl(
            uri,
            mode: LaunchMode.externalApplication,
          );
          
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('Opening verification request...'),
                backgroundColor: Colors.green,
                duration: Duration(seconds: 2),
              ),
            );
          }
        } else {
          throw Exception('Cannot launch URL: $url');
        }
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Invalid verification URL. Expected openid4vp:// or openid4vci://'),
              backgroundColor: Colors.orange,
              duration: Duration(seconds: 3),
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error: ${e.toString()}'),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 3),
          ),
        );
      }
    }
  }

  void _showQRCode(BuildContext context, String url) {
    showDialog(
      context: context,
      builder: (context) {
        return Dialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(28),
          ),
          child: Container(
            padding: const EdgeInsets.all(28.0),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(28),
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [
                  Colors.white,
                  Colors.grey.shade50,
                ],
              ),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.blue.shade50,
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Icon(
                    Icons.qr_code_rounded,
                    size: 32,
                    color: Colors.blue.shade700,
                  ),
                ),
                const SizedBox(height: 16),
                const Text(
                  'Credential QR Code',
                  style: TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.bold,
                    letterSpacing: -0.5,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Scan this code to access your credential',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 14,
                    color: Colors.grey.shade600,
                  ),
                ),
                const SizedBox(height: 24),
                Container(
                  padding: const EdgeInsets.all(20),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(
                      color: Colors.grey.shade200,
                      width: 2,
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.05),
                        blurRadius: 12,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: QrImageView(
                    data: url,
                    version: QrVersions.auto,
                    size: 250.0,
                    backgroundColor: Colors.white,
                    padding: const EdgeInsets.all(8),
                  ),
                ),
                const SizedBox(height: 28),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: () => Navigator.pop(context),
                    style: ElevatedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      backgroundColor: Colors.blue.shade700,
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16),
                      ),
                      elevation: 0,
                    ),
                    child: const Text(
                      'Close',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _openUrl(String url) async {
    final uri = Uri.parse(url);
    if (!UrlGuard.isAllowed(uri)) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'Blocked: "${uri.host.isNotEmpty ? uri.host : uri.scheme}" is not '
              'an allowed destination. This link was not opened.',
            ),
            backgroundColor: Colors.red,
          ),
        );
      }
      return;
    }
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    } else {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Could not open URL: $url')),
        );
      }
    }
  }
}

class _AnimatedCredentialCard extends StatefulWidget {
  final Map<String, dynamic> credential;
  final Map<String, dynamic> credentialData;
  final String credentialType;
  final int index;
  final List<Color> colors;
  final IconData icon;
  final String studentName;
  final String studentId;
  final String courseName;
  final String? issuanceDate;
  final String? expirationDate;
  final VoidCallback onTap;

  const _AnimatedCredentialCard({
    required this.credential,
    required this.credentialData,
    required this.credentialType,
    required this.index,
    required this.colors,
    required this.icon,
    required this.studentName,
    required this.studentId,
    required this.courseName,
    required this.issuanceDate,
    required this.expirationDate,
    required this.onTap,
  });

  @override
  State<_AnimatedCredentialCard> createState() => _AnimatedCredentialCardState();
}

class _AnimatedCredentialCardState extends State<_AnimatedCredentialCard>
    with SingleTickerProviderStateMixin {
  bool _isPressed = false;
  late AnimationController _controller;
  late Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 150),
    );
    _scaleAnimation = Tween<double>(begin: 1.0, end: 0.95).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTapDown: (_) {
        setState(() => _isPressed = true);
        _controller.forward();
      },
      onTapUp: (_) {
        setState(() => _isPressed = false);
        _controller.reverse();
        Future.delayed(const Duration(milliseconds: 100), widget.onTap);
      },
      onTapCancel: () {
        setState(() => _isPressed = false);
        _controller.reverse();
      },
      child: ScaleTransition(
        scale: _scaleAnimation,
        child: Container(
          margin: EdgeInsets.only(
            bottom: 20,
            left: 20,
            right: 20,
            top: widget.index == 0 ? 20 : 0,
          ),
          height: 240,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(32),
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                widget.colors[0],
                widget.colors.length > 1 ? widget.colors[1] : widget.colors[0].withOpacity(0.85),
                widget.colors[0].withOpacity(0.9),
              ],
              stops: const [0.0, 0.5, 1.0],
            ),
            boxShadow: [
              BoxShadow(
                color: widget.colors[0].withOpacity(0.6),
                blurRadius: 32,
                offset: const Offset(0, 16),
                spreadRadius: 2,
              ),
              BoxShadow(
                color: widget.colors[0].withOpacity(0.4),
                blurRadius: 16,
                offset: const Offset(0, 8),
                spreadRadius: 1,
              ),
              BoxShadow(
                color: Colors.black.withOpacity(0.15),
                blurRadius: 8,
                offset: const Offset(0, 4),
              ),
            ],
          ),
        child: Stack(
          children: [
            // Credit Card Style Background Pattern
            Positioned(
              right: -30,
              top: -30,
              child: Container(
                width: 200,
                height: 200,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: RadialGradient(
                    colors: [
                      Colors.white.withOpacity(0.2),
                      Colors.white.withOpacity(0.05),
                    ],
                  ),
                ),
              ),
            ),
            Positioned(
              right: 20,
              bottom: -40,
              child: Container(
                width: 150,
                height: 150,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: RadialGradient(
                    colors: [
                      Colors.white.withOpacity(0.15),
                      Colors.white.withOpacity(0.03),
                    ],
                  ),
                ),
              ),
            ),
            // Simple Credit Card Chip
            Positioned(
              left: 28,
              top: 28,
              child: Container(
                width: 40,
                height: 32,
                decoration: BoxDecoration(
                  color: Colors.white.withOpacity(0.3),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(
                    color: Colors.white.withOpacity(0.5),
                    width: 1.5,
                  ),
                ),
                child: Container(
                  margin: const EdgeInsets.all(5),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(5),
                  ),
                ),
              ),
            ),
            // Holographic Strip
            Positioned(
              left: 0,
              right: 0,
              top: 80,
              child: Container(
                height: 40,
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.centerLeft,
                    end: Alignment.centerRight,
                    colors: [
                      Colors.white.withOpacity(0.0),
                      Colors.white.withOpacity(0.3),
                      Colors.white.withOpacity(0.0),
                    ],
                    stops: const [0.0, 0.5, 1.0],
                  ),
                ),
              ),
            ),
            // Card Content
            Padding(
              padding: const EdgeInsets.all(28.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      // Clean, Simple Icon
                      Icon(
                        widget.icon,
                        color: Colors.white,
                        size: 32,
                        shadows: [
                          Shadow(
                            color: Colors.black.withOpacity(0.3),
                            offset: const Offset(0, 2),
                            blurRadius: 4,
                          ),
                        ],
                      ),
                      // Simple Verified Badge
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.25),
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(
                            color: Colors.white.withOpacity(0.4),
                            width: 1.5,
                          ),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(
                              Icons.verified_rounded,
                              color: Colors.white,
                              size: 16,
                            ),
                            const SizedBox(width: 6),
                            Text(
                              'VERIFIED',
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 11,
                                fontWeight: FontWeight.w700,
                                letterSpacing: 1.0,
                                shadows: [
                                  Shadow(
                                    color: Colors.black.withOpacity(0.25),
                                    offset: const Offset(0, 1),
                                    blurRadius: 2,
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const Spacer(),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                    child: Text(
                      _getCredentialNameHelper(widget.credentialType, widget.credentialData),
                      style: const TextStyle(
                        fontSize: 28,
                        fontWeight: FontWeight.w800,
                        color: Colors.white,
                        letterSpacing: -0.5,
                        height: 1.15,
                        shadows: [
                          Shadow(
                            color: Colors.black38,
                            offset: Offset(0, 2),
                            blurRadius: 4,
                          ),
                          Shadow(
                            color: Colors.black26,
                            offset: Offset(0, 1),
                            blurRadius: 2,
                          ),
                        ],
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                        ],
                      ),
                      if (widget.studentName.toString().isNotEmpty || 
                          (widget.studentId.toString().isNotEmpty && 
                           !widget.studentId.toString().startsWith('did:'))) ...[
                        const SizedBox(height: 12),
                        if (widget.studentName.toString().isNotEmpty)
                          Text(
                            widget.studentName.toString().toUpperCase(),
                            style: TextStyle(
                              fontSize: 16,
                              color: Colors.white.withOpacity(0.98),
                              fontWeight: FontWeight.w800,
                              letterSpacing: 1.8,
                              shadows: [
                                Shadow(
                                  color: Colors.black.withOpacity(0.4),
                                  offset: const Offset(0, 2),
                                  blurRadius: 3,
                                ),
                                Shadow(
                                  color: Colors.black.withOpacity(0.2),
                                  offset: const Offset(0, 1),
                                  blurRadius: 1,
                                ),
                              ],
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        if (widget.studentId.toString().isNotEmpty && 
                            !widget.studentId.toString().startsWith('did:') &&
                            widget.studentId.toString().length < 50) ...[
                          SizedBox(height: widget.studentName.toString().isNotEmpty ? 8 : 0),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                            decoration: BoxDecoration(
                              gradient: LinearGradient(
                                begin: Alignment.topLeft,
                                end: Alignment.bottomRight,
                                colors: [
                                  Colors.white.withOpacity(0.3),
                                  Colors.white.withOpacity(0.2),
                                ],
                              ),
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(
                                color: Colors.white.withOpacity(0.5),
                                width: 2,
                              ),
                              boxShadow: [
                                BoxShadow(
                                  color: Colors.white.withOpacity(0.2),
                                  blurRadius: 8,
                                  spreadRadius: 0,
                                  offset: const Offset(0, 2),
                                ),
                                BoxShadow(
                                  color: Colors.black.withOpacity(0.15),
                                  blurRadius: 4,
                                  offset: const Offset(0, 2),
                                ),
                              ],
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Icon(
                                  Icons.badge_rounded,
                                  size: 16,
                                  color: Colors.white.withOpacity(0.95),
                                ),
                                const SizedBox(width: 8),
                                Flexible(
                                  child: Text(
                                    widget.studentId.toString(),
                                    style: TextStyle(
                                      fontSize: 14,
                                      color: Colors.white.withOpacity(0.98),
                                      fontWeight: FontWeight.w800,
                                      letterSpacing: 0.8,
                                      shadows: [
                                        Shadow(
                                          color: Colors.black.withOpacity(0.3),
                                          offset: const Offset(0, 1),
                                          blurRadius: 2,
                                        ),
                                      ],
                                    ),
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ],
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      if (widget.issuanceDate != null)
                        Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                              'ISSUED',
                                style: TextStyle(
                                  fontSize: 9,
                                color: Colors.white.withOpacity(0.75),
                                letterSpacing: 1.5,
                                fontWeight: FontWeight.w700,
                                ),
                              ),
                            const SizedBox(height: 4),
                              Text(
                              _formatDateHelper(widget.issuanceDate!),
                                style: const TextStyle(
                                fontSize: 12,
                                  color: Colors.white,
                                fontWeight: FontWeight.w700,
                                letterSpacing: 0.5,
                                shadows: [
                                  Shadow(
                                    color: Colors.black26,
                                    offset: Offset(0, 1),
                                    blurRadius: 2,
                              ),
                            ],
                          ),
                        ),
                          ],
                        ),
                      if (widget.expirationDate != null)
                        Column(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              Text(
                              'EXPIRES',
                                style: TextStyle(
                                  fontSize: 9,
                                color: Colors.white.withOpacity(0.75),
                                letterSpacing: 1.5,
                                fontWeight: FontWeight.w700,
                                ),
                              ),
                            const SizedBox(height: 4),
                              Text(
                              _formatDateHelper(widget.expirationDate!),
                                style: const TextStyle(
                                fontSize: 12,
                                  color: Colors.white,
                                fontWeight: FontWeight.w700,
                                letterSpacing: 0.5,
                                shadows: [
                                  Shadow(
                                    color: Colors.black26,
                                    offset: Offset(0, 1),
                                    blurRadius: 2,
                              ),
                            ],
                          ),
                        ),
                      ],
                        ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
        ),
      ),
    );
  }
}

// Apple Wallet-style stacked card view
class _WalletCardStack extends StatefulWidget {
  final List<Map<String, dynamic>> credentials;
  final Function(Map<String, dynamic>, Map<String, dynamic>, String) onCardTap;

  const _WalletCardStack({
    required this.credentials,
    required this.onCardTap,
  });

  @override
  State<_WalletCardStack> createState() => _WalletCardStackState();
}

class _WalletCardStackState extends State<_WalletCardStack> with SingleTickerProviderStateMixin {
  int _expandedIndex = -1;
  int? _tappedIndex;

  void _toggleCard(int index) {
    setState(() {
      _tappedIndex = index;
      // Small delay for tap feedback
      Future.delayed(const Duration(milliseconds: 100), () {
        if (mounted) {
          setState(() {
            _tappedIndex = null;
            if (_expandedIndex == index) {
              // Collapse current card
              _expandedIndex = -1;
            } else {
              // Expand new card
              _expandedIndex = index;
            }
          });
        }
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    if (widget.credentials.isEmpty) {
      return const SizedBox.shrink();
    }

    // Calculate total height needed for proper stacking
    final collapsedHeight = 180.0; // Compact but shows title
    final expandedHeight = 650.0;
    final peekOffset = 40.0; // Show more of each card behind
    
    final totalHeight = _expandedIndex == -1
        ? collapsedHeight + (widget.credentials.length - 1) * peekOffset
        : expandedHeight + (widget.credentials.length - 1) * peekOffset;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const SizedBox(height: 12),
          // Stack cards using Stack widget for proper overlapping
          SizedBox(
            height: totalHeight,
            child: Stack(
              clipBehavior: Clip.none,
              children: widget.credentials.asMap().entries.map((entry) {
                final index = entry.key;
                final credential = entry.value;
                
                // WaltID Wallet API returns credentials with nested parsedDocument
                final parsedDoc = credential['parsedDocument'] as Map<String, dynamic>?;
                final credentialData = parsedDoc ?? credential;
                
                // Extract credential type
                final types = credentialData['type'] as List<dynamic>?;
                final credentialType = types?.firstWhere(
                  (t) => t != 'VerifiableCredential',
                  orElse: () => types?.isNotEmpty == true ? types!.first : 'Unknown',
                )?.toString() ?? 'Unknown';
                
                final isExpanded = _expandedIndex == index;
                
                // Calculate position in stack - reverse order (first card on top)
                final reverseIndex = widget.credentials.length - 1 - index;
                
                // Apple Wallet style - visible peek of cards behind
                // Show enough of each card to see the title clearly
                final double stackOffset = isExpanded 
                    ? 0.0 
                    : (reverseIndex * 40.0); // Increased to show more of each card
                
                // Dynamic card height - taller collapsed to show title
                final cardHeight = isExpanded ? expandedHeight : 180.0; // Reduced height but still shows title
                
                // Calculate z-index effect
                final isBehind = _expandedIndex != -1 && index > _expandedIndex;
                final isInFront = _expandedIndex != -1 && index < _expandedIndex;
                
                return Positioned(
                  bottom: stackOffset,
                  left: 0,
                  right: 0,
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 500),
                    curve: Curves.easeOutCubic,
                    height: cardHeight,
                    margin: EdgeInsets.only(
                      bottom: isExpanded ? 24 : 0,
                    ),
                    child: GestureDetector(
                      onTap: () => _toggleCard(index),
                      behavior: HitTestBehavior.opaque,
                      child: _buildStackedCard(
                        context,
                        credential,
                        credentialData,
                        credentialType,
                        index,
                        isExpanded,
                        isBehind,
                        isInFront,
                        reverseIndex,
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
          const SizedBox(height: 32),
        ],
      ),
    );
  }

  Widget _buildStackedCard(
    BuildContext context,
    Map<String, dynamic> credential,
    Map<String, dynamic> credentialData,
    String credentialType,
    int index,
    bool isExpanded,
    bool isBehind,
    bool isInFront,
    int stackPosition,
  ) {
    // Apple Wallet style - cards behind should be clearly visible
    // Only reduce opacity slightly for depth, but keep them readable
    final opacity = isExpanded 
        ? 1.0 
        : (1.0 - (stackPosition * 0.03)).clamp(0.92, 1.0); // Much more visible
    
    // Minimal scale effect - cards should look natural
    final scale = isExpanded 
        ? 1.0 
        : (1.0 - (stackPosition * 0.008)).clamp(0.98, 1.0); // Very subtle scaling

    final colors = _getCredentialColors(credentialType);
    final icon = _getCredentialIconData(credentialType);
    
    // Extract credential subject data
    final credentialSubject = credentialData['credentialSubject'] as Map<String, dynamic>?;
    
    // Extract student name
    String studentName = '';
    if (credentialSubject != null) {
      final givenName = credentialSubject['givenName']?.toString() ?? '';
      final familyName = credentialSubject['familyName']?.toString() ?? '';
      final name = credentialSubject['name']?.toString() ?? '';
      
      if (givenName.isNotEmpty && familyName.isNotEmpty) {
        studentName = '$givenName $familyName'.trim();
      } else if (givenName.isNotEmpty) {
        studentName = givenName;
      } else if (name.isNotEmpty) {
        studentName = name;
      }
    }
    
    // Extract student ID
    String? rawStudentId = credentialSubject?['studentId']?.toString() ?? 
                           credentialSubject?['id']?.toString();
    final studentId = (rawStudentId != null && 
                       !rawStudentId.startsWith('did:') && 
                       rawStudentId.length < 50) 
        ? rawStudentId 
        : '';
    
    final courseName = credentialSubject?['courseName'] ?? '';
    
    // Extract dates
    final issuanceDate = credentialData['issuanceDate'] as String? ?? 
                         credentialData['issued'] as String? ??
                         credential['addedOn'] as String?;
    final expirationDate = credentialData['expirationDate'] as String? ?? 
                           credentialData['expiration'] as String? ??
                           credentialData['validUntil'] as String?;
    
    final credentialName = _getCredentialName(credentialType, credentialData);
    
    // Add subtle tap animation
    final isTapped = _tappedIndex == index;
    final tapScale = isTapped ? 0.98 : 1.0;
    
    return Transform.scale(
      scale: scale * tapScale,
      child: Opacity(
        opacity: opacity,
        child: Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(isExpanded ? 24 : 16), // Slightly larger radius
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            colors[0],
            colors.length > 1 ? colors[1] : colors[0].withOpacity(0.95),
            colors.length > 1 ? colors[0].withOpacity(0.9) : colors[0].withOpacity(0.85),
          ],
          stops: const [0.0, 0.6, 1.0],
        ),
        boxShadow: [
          // Enhanced shadow for better depth - Apple Wallet style
          BoxShadow(
            color: colors[0].withOpacity(isExpanded ? 0.6 : 0.4),
            blurRadius: isExpanded ? 60 : (30 + (stackPosition * 5)),
            offset: Offset(0, isExpanded ? 25 : (10 + (stackPosition * 2))),
            spreadRadius: isExpanded ? -12 : -6,
          ),
          // Deep shadow for realistic depth
          BoxShadow(
            color: Colors.black.withOpacity(isExpanded ? 0.5 : 0.3),
            blurRadius: isExpanded ? 50 : (25 + (stackPosition * 3)),
            offset: Offset(0, isExpanded ? 20 : (8 + (stackPosition * 1.5))),
            spreadRadius: 0,
          ),
          // Subtle highlight glow
          BoxShadow(
            color: Colors.white.withOpacity(0.2),
            blurRadius: 20,
            offset: const Offset(-4, -4),
            spreadRadius: -4,
          ),
          // Additional depth shadow for stacked effect
          if (!isExpanded && stackPosition > 0)
            BoxShadow(
              color: Colors.black.withOpacity(0.15),
              blurRadius: 15,
              offset: Offset(0, 5 + (stackPosition * 2)),
              spreadRadius: -2,
            ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(isExpanded ? 24 : 16),
        child: Stack(
          children: [
            // Beautiful gradient overlay effects
            Positioned(
              right: -80,
              top: -80,
              child: Container(
                width: 280,
                height: 280,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: RadialGradient(
                    colors: [
                      Colors.white.withOpacity(0.2),
                      Colors.white.withOpacity(0.1),
                      Colors.white.withOpacity(0.0),
                    ],
                    stops: const [0.0, 0.4, 1.0],
                  ),
                ),
              ),
            ),
            // Accent glow for depth
            Positioned(
              left: -50,
              bottom: -50,
              child: Container(
                width: 200,
                height: 200,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: RadialGradient(
                    colors: [
                      Colors.white.withOpacity(0.15),
                      Colors.white.withOpacity(0.05),
                      Colors.white.withOpacity(0.0),
                    ],
                    stops: const [0.0, 0.5, 1.0],
                  ),
                ),
              ),
            ),
            // Enhanced shimmer effect overlay - Apple Wallet style
            Container(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(isExpanded ? 24 : 16),
                gradient: LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [
                    Colors.white.withOpacity(0.2),
                    Colors.white.withOpacity(0.08),
                    Colors.white.withOpacity(0.0),
                    Colors.black.withOpacity(0.1),
                  ],
                  stops: const [0.0, 0.25, 0.5, 1.0],
                ),
              ),
            ),
            // Additional subtle gradient for depth
            Positioned.fill(
              child: Container(
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(isExpanded ? 24 : 16),
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Colors.transparent,
                      Colors.black.withOpacity(0.05),
                    ],
                  ),
                ),
              ),
            ),
            // Card content - clean and minimal
            Padding(
              padding: EdgeInsets.symmetric(
                horizontal: isExpanded ? 28.0 : 20.0,
                vertical: isExpanded ? 28.0 : 14.0,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  // Top section - premium design
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        padding: EdgeInsets.all(isExpanded ? 10 : 5),
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            begin: Alignment.topLeft,
                            end: Alignment.bottomRight,
                            colors: [
                              Colors.white.withOpacity(0.25),
                              Colors.white.withOpacity(0.15),
                            ],
                          ),
                          borderRadius: BorderRadius.circular(14),
                          border: Border.all(
                            color: Colors.white.withOpacity(0.3),
                            width: 1.5,
                          ),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withOpacity(0.3),
                              blurRadius: 12,
                              offset: const Offset(0, 3),
                            ),
                            BoxShadow(
                              color: Colors.white.withOpacity(0.1),
                              blurRadius: 6,
                              offset: const Offset(-2, -2),
                            ),
                          ],
                        ),
                        child: Icon(
                          icon,
                          color: Colors.white,
                          size: isExpanded ? 30 : 18,
                        ),
                      ),
                      if (!isExpanded)
                        Container(
                          padding: const EdgeInsets.all(6),
                          decoration: BoxDecoration(
                            color: Colors.white.withOpacity(0.1),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Icon(
                            Icons.expand_more_rounded,
                            color: Colors.white.withOpacity(0.8),
                            size: 18,
                          ),
                        ),
                    ],
                  ),
                  
                  // Middle section - title prominently displayed at top
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.start,
                      children: [
                        SizedBox(height: isExpanded ? 24 : 4), // Less top spacing when collapsed
                        Text(
                          credentialName,
                          style: TextStyle(
                            fontSize: isExpanded ? 30 : 24, // Larger font when collapsed
                            fontWeight: FontWeight.w700,
                            color: Colors.white,
                            letterSpacing: -0.8,
                            height: 1.1,
                            shadows: [
                              Shadow(
                                color: Colors.black.withOpacity(0.8),
                                offset: const Offset(0, 2),
                                blurRadius: 8,
                              ),
                              Shadow(
                                color: Colors.black.withOpacity(0.6),
                                offset: const Offset(0, 1),
                                blurRadius: 4,
                              ),
                            ],
                          ),
                          maxLines: isExpanded ? 2 : 2, // Allow 2 lines even when collapsed
                          overflow: TextOverflow.ellipsis,
                        ),
                        // Show student name in collapsed view if available
                        if (!isExpanded && studentName.isNotEmpty) ...[
                          const SizedBox(height: 4),
                          Text(
                            studentName,
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                              color: Colors.white.withOpacity(0.95),
                              letterSpacing: 0.1,
                              shadows: [
                                Shadow(
                                  color: Colors.black.withOpacity(0.6),
                                  offset: const Offset(0, 1.5),
                                  blurRadius: 5,
                                ),
                              ],
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ],
                      if (isExpanded && studentName.isNotEmpty) ...[
                        const SizedBox(height: 24),
                        Text(
                          studentName,
                          style: TextStyle(
                            fontSize: 24,
                            fontWeight: FontWeight.w600,
                            color: Colors.white,
                            letterSpacing: 0.2,
                            shadows: [
                              Shadow(
                                color: Colors.black.withOpacity(0.25),
                                offset: const Offset(0, 1),
                                blurRadius: 2,
                              ),
                            ],
                          ),
                        ),
                      ],
                      if (isExpanded && studentId.isNotEmpty) ...[
                        const SizedBox(height: 12),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                          decoration: BoxDecoration(
                            color: Colors.white.withOpacity(0.15),
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(
                              color: Colors.white.withOpacity(0.2),
                              width: 1,
                            ),
                          ),
                          child: Text(
                            studentId,
                            style: TextStyle(
                              fontSize: 15,
                              fontWeight: FontWeight.w500,
                              color: Colors.white.withOpacity(0.95),
                              letterSpacing: 0.5,
                              fontFeatures: const [
                                FontFeature.tabularFigures(),
                              ],
                            ),
                          ),
                        ),
                      ],
                      ],
                    ),
                  ),
                  
                  // Bottom section - minimal
                  if (isExpanded) ...[
                    const SizedBox(height: 12),
                    // Premium info section with more details
                    if (courseName.toString().isNotEmpty || issuanceDate != null || 
                        (credentialType == 'ConferenceSessionCredential' || credentialType.startsWith('ConferenceSession_')) && 
                        credentialSubject != null) ...[
                      Padding(
                        padding: const EdgeInsets.only(top: 12),
                        child: Container(
                          padding: const EdgeInsets.all(20),
                          decoration: BoxDecoration(
                            color: Colors.white.withOpacity(0.12),
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(
                              color: Colors.white.withOpacity(0.15),
                              width: 1,
                            ),
                            boxShadow: [
                              BoxShadow(
                                color: Colors.black.withOpacity(0.2),
                                blurRadius: 10,
                                offset: const Offset(0, 2),
                              ),
                            ],
                          ),
                          child: Column(
                            children: [
                              // First row: Course and Issued date
                              Row(
                                children: [
                                  if (courseName.toString().isNotEmpty) ...[
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            'COURSE',
                                            style: TextStyle(
                                              fontSize: 10,
                                              color: Colors.white.withOpacity(0.6),
                                              fontWeight: FontWeight.w600,
                                              letterSpacing: 1.2,
                                            ),
                                          ),
                                          const SizedBox(height: 6),
                                          Text(
                                            courseName.toString(),
                                            style: const TextStyle(
                                              fontSize: 17,
                                              color: Colors.white,
                                              fontWeight: FontWeight.w600,
                                              letterSpacing: -0.3,
                                            ),
                                            maxLines: 2,
                                            overflow: TextOverflow.ellipsis,
                                          ),
                                        ],
                                      ),
                                    ),
                                  ],
                                  if (issuanceDate != null) ...[
                                    if (courseName.toString().isNotEmpty)
                                      Container(
                                        width: 1,
                                        height: 44,
                                        margin: const EdgeInsets.symmetric(horizontal: 20),
                                        decoration: BoxDecoration(
                                          gradient: LinearGradient(
                                            begin: Alignment.topCenter,
                                            end: Alignment.bottomCenter,
                                            colors: [
                                              Colors.white.withOpacity(0.0),
                                              Colors.white.withOpacity(0.3),
                                              Colors.white.withOpacity(0.0),
                                            ],
                                          ),
                                        ),
                                      ),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            'ISSUED',
                                            style: TextStyle(
                                              fontSize: 10,
                                              color: Colors.white.withOpacity(0.6),
                                              fontWeight: FontWeight.w600,
                                              letterSpacing: 1.2,
                                            ),
                                          ),
                                          const SizedBox(height: 6),
                                          Text(
                                            _formatDate(issuanceDate),
                                            style: const TextStyle(
                                              fontSize: 17,
                                              color: Colors.white,
                                              fontWeight: FontWeight.w600,
                                              letterSpacing: -0.3,
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ],
                                ],
                              ),
                              // Conference session details - multiple rows
                              if ((credentialType == 'ConferenceSessionCredential' || credentialType.startsWith('ConferenceSession_')) && 
                                  credentialSubject != null) ...[
                                if (courseName.toString().isNotEmpty || issuanceDate != null)
                                  const SizedBox(height: 16),
                                // Location row
                                if (credentialSubject['location'] != null) ...[
                                  Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        'LOCATION',
                                        style: TextStyle(
                                          fontSize: 10,
                                          color: Colors.white.withOpacity(0.6),
                                          fontWeight: FontWeight.w600,
                                          letterSpacing: 1.2,
                                        ),
                                      ),
                                      const SizedBox(height: 6),
                                      Text(
                                        credentialSubject['location'].toString(),
                                        style: const TextStyle(
                                          fontSize: 15,
                                          color: Colors.white,
                                          fontWeight: FontWeight.w600,
                                          letterSpacing: -0.3,
                                        ),
                                        maxLines: 2,
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 12),
                                ],
                                // Time row
                                if (credentialSubject['startTime'] != null || credentialSubject['endTime'] != null) ...[
                                  Row(
                                    children: [
                                      if (credentialSubject['startTime'] != null) ...[
                                        Expanded(
                                          child: Column(
                                            crossAxisAlignment: CrossAxisAlignment.start,
                                            children: [
                                              Text(
                                                'START',
                                                style: TextStyle(
                                                  fontSize: 10,
                                                  color: Colors.white.withOpacity(0.6),
                                                  fontWeight: FontWeight.w600,
                                                  letterSpacing: 1.2,
                                                ),
                                              ),
                                              const SizedBox(height: 6),
                                              Text(
                                                _formatTimeOnly(credentialSubject['startTime'].toString()),
                                                style: const TextStyle(
                                                  fontSize: 15,
                                                  color: Colors.white,
                                                  fontWeight: FontWeight.w600,
                                                  letterSpacing: -0.3,
                                                ),
                                              ),
                                            ],
                                          ),
                                        ),
                                      ],
                                      if (credentialSubject['endTime'] != null && credentialSubject['startTime'] != null)
                                        Container(
                                          width: 1,
                                          height: 40,
                                          margin: const EdgeInsets.symmetric(horizontal: 16),
                                          decoration: BoxDecoration(
                                            gradient: LinearGradient(
                                              begin: Alignment.topCenter,
                                              end: Alignment.bottomCenter,
                                              colors: [
                                                Colors.white.withOpacity(0.0),
                                                Colors.white.withOpacity(0.3),
                                                Colors.white.withOpacity(0.0),
                                              ],
                                            ),
                                          ),
                                        ),
                                      if (credentialSubject['endTime'] != null) ...[
                                        Expanded(
                                          child: Column(
                                            crossAxisAlignment: CrossAxisAlignment.start,
                                            children: [
                                              Text(
                                                'END',
                                                style: TextStyle(
                                                  fontSize: 10,
                                                  color: Colors.white.withOpacity(0.6),
                                                  fontWeight: FontWeight.w600,
                                                  letterSpacing: 1.2,
                                                ),
                                              ),
                                              const SizedBox(height: 6),
                                              Text(
                                                _formatTimeOnly(credentialSubject['endTime'].toString()),
                                                style: const TextStyle(
                                                  fontSize: 15,
                                                  color: Colors.white,
                                                  fontWeight: FontWeight.w600,
                                                  letterSpacing: -0.3,
                                                ),
                                              ),
                                            ],
                                          ),
                                        ),
                                      ],
                                    ],
                                  ),
                                ],
                                // Description row
                                if (credentialSubject['description'] != null) ...[
                                  const SizedBox(height: 12),
                                  Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        'DESCRIPTION',
                                        style: TextStyle(
                                          fontSize: 10,
                                          color: Colors.white.withOpacity(0.6),
                                          fontWeight: FontWeight.w600,
                                          letterSpacing: 1.2,
                                        ),
                                      ),
                                      const SizedBox(height: 6),
                                      Text(
                                        credentialSubject['description'].toString(),
                                        style: const TextStyle(
                                          fontSize: 14,
                                          color: Colors.white,
                                          fontWeight: FontWeight.w500,
                                          letterSpacing: 0,
                                          height: 1.4,
                                        ),
                                        maxLines: 3,
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                    ],
                                  ),
                                ],
                              ],
                              // Additional info for other credential types
                              if (credentialType != 'ConferenceSessionCredential' && !credentialType.startsWith('ConferenceSession_')) ...[
                                if (expirationDate != null) ...[
                                  if (courseName.toString().isNotEmpty || issuanceDate != null)
                                    const SizedBox(height: 16),
                                  Row(
                                    children: [
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              'EXPIRES',
                                              style: TextStyle(
                                                fontSize: 10,
                                                color: Colors.white.withOpacity(0.6),
                                                fontWeight: FontWeight.w600,
                                                letterSpacing: 1.2,
                                              ),
                                            ),
                                            const SizedBox(height: 6),
                                            Text(
                                              _formatDate(expirationDate),
                                              style: const TextStyle(
                                                fontSize: 15,
                                                color: Colors.white,
                                                fontWeight: FontWeight.w600,
                                                letterSpacing: -0.3,
                                              ),
                                            ),
                                          ],
                                        ),
                                      ),
                                    ],
                                  ),
                                ],
                              ],
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),
                    ],
                    // Premium button
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                        onPressed: () {
                          widget.onCardTap(credential, credentialData, credentialType);
                        },
                        style: ElevatedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          backgroundColor: Colors.white,
                          foregroundColor: colors[0],
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(14),
                          ),
                          elevation: 4,
                          shadowColor: Colors.black.withOpacity(0.3),
                        ),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text(
                              'View Details',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.w600,
                                letterSpacing: 0.3,
                                color: colors[0],
                              ),
                            ),
                            const SizedBox(width: 8),
                            Icon(
                              Icons.arrow_forward_rounded,
                              size: 18,
                              color: colors[0],
                            ),
                          ],
                        ),
                      ),
                    ),
                  ] else ...[
                    // Premium collapsed state - very compact
                    const Spacer(),
                    if (studentId.isNotEmpty)
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.12),
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(
                            color: Colors.white.withOpacity(0.2),
                            width: 1,
                          ),
                        ),
                        child: Text(
                          studentId,
                          style: TextStyle(
                            fontSize: 11,
                            color: Colors.white.withOpacity(0.95),
                            fontWeight: FontWeight.w500,
                            letterSpacing: 0.5,
                            fontFeatures: const [
                              FontFeature.tabularFigures(),
                            ],
                          ),
                        ),
                      ),
                  ],
                ],
              ),
            ),
          ],
        ),
        ),
      ),
    ),
    );
  }

  String _formatDate(String dateString) {
    try {
      final date = DateTime.parse(dateString);
      return '${date.day}/${date.month}/${date.year}';
    } catch (e) {
      return dateString;
    }
  }

  String _formatTimeOnly(String dateTimeString) {
    try {
      final dateTime = DateTime.parse(dateTimeString);
      final hours = dateTime.hour.toString().padLeft(2, '0');
      final minutes = dateTime.minute.toString().padLeft(2, '0');
      return '$hours:$minutes';
    } catch (e) {
      return dateTimeString;
    }
  }

  List<Color> _getCredentialColors(String credentialType) {
    switch (credentialType) {
      case 'EducationalID':
        // Vibrant electric blue to cyan gradient
        return [const Color(0xFF0066FF), const Color(0xFF00D4FF)];
      case 'IdentityCredential':
        // Rich magenta to pink gradient
        return [const Color(0xFF8B00FF), const Color(0xFFFF00D4)];
      case 'EuropeanStudentCard':
        // Fresh mint to emerald gradient
        return [const Color(0xFF00FFB3), const Color(0xFF00D4AA)];
      case 'UniversityDegree':
        // Royal purple to violet gradient
        return [const Color(0xFF7B2FF7), const Color(0xFFB84DFF)];
      case 'ConferenceSessionCredential':
        // Vibrant coral to orange gradient
        return [const Color(0xFFFF4D6D), const Color(0xFFFF8C42)];
      default:
        if (credentialType.startsWith('ConferenceSession_')) {
          // Vibrant coral to orange gradient for conference sessions
          return [const Color(0xFFFF4D6D), const Color(0xFFFF8C42)];
        }
        // Default vibrant indigo to purple gradient
        return [const Color(0xFF6366F1), const Color(0xFF8B5CF6)];
    }
  }

  IconData _getCredentialIconData(String credentialType) {
    switch (credentialType) {
      case 'EducationalID':
        return Icons.school_rounded;
      case 'IdentityCredential':
        return Icons.badge_rounded;
      case 'EuropeanStudentCard':
        return Icons.credit_card_rounded;
      case 'UniversityDegree':
        return Icons.workspace_premium_rounded;
      case 'ConferenceSessionCredential':
        return Icons.event_rounded;
      default:
        if (credentialType.startsWith('ConferenceSession_')) {
          return Icons.event_rounded;
        }
        return Icons.verified_user_rounded;
    }
  }

  String _getCredentialName(String credentialType, Map<String, dynamic>? credentialData) {
    // Check if it's a conference session credential - use conference name
    if ((credentialType == 'ConferenceSessionCredential' || credentialType.startsWith('ConferenceSession_')) && credentialData != null) {
      final credentialSubject = credentialData['credentialSubject'] as Map<String, dynamic>?;
      final conferenceName = credentialSubject?['conferenceName']?.toString();
      final sessionTitle = credentialSubject?['sessionTitle']?.toString();
      
      if (conferenceName != null && conferenceName.isNotEmpty) {
        return conferenceName;
      }
      if (sessionTitle != null && sessionTitle.isNotEmpty) {
        return sessionTitle;
      }
    }
    
    // Check for displayName in credential data
    if (credentialData != null) {
      final displayName = credentialData['displayName']?.toString() ?? 
                         credentialData['name']?.toString();
      if (displayName != null && displayName.isNotEmpty) {
        return displayName;
      }
    }
    
    switch (credentialType) {
      case 'EducationalID':
        return 'Educational ID';
      case 'IdentityCredential':
        return 'Identity Credential';
      case 'EuropeanStudentCard':
        return 'European Student Card';
      case 'UniversityDegree':
        return 'University Degree';
      case 'ConferenceSessionCredential':
        return 'Conference Session';
      default:
        if (credentialType.startsWith('ConferenceSession_')) {
          return credentialType.replaceFirst('ConferenceSession_', '').replaceAll(RegExp(r'([A-Z])'), ' \$1').trim();
        }
        return credentialType;
    }
  }
}


