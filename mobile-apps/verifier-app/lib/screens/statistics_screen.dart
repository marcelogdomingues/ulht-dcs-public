import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/verification_provider.dart';
import '../providers/profile_provider.dart';

/// Helper function to darken a color
Color _darkenColor(Color color, double amount) {
  assert(amount >= 0 && amount <= 1);
  final hsl = HSLColor.fromColor(color);
  final lightness = (hsl.lightness - amount).clamp(0.0, 1.0);
  return hsl.withLightness(lightness).toColor();
}

class StatisticsScreen extends StatelessWidget {
  const StatisticsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'Statistics',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            letterSpacing: -0.5,
          ),
        ),
        backgroundColor: Colors.green.shade700,
        foregroundColor: Colors.white,
        elevation: 0,
        flexibleSpace: Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [
                Colors.green.shade700,
                Colors.green.shade600,
              ],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
          ),
        ),
      ),
      body: Consumer2<VerificationProvider, ProfileProvider>(
        builder: (context, verificationProvider, profileProvider, child) {
          final history = verificationProvider.history;
          
          if (history.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.bar_chart_rounded, size: 64, color: Colors.grey.shade400),
                  const SizedBox(height: 16),
                  Text(
                    'No Statistics Available',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: Colors.grey.shade800,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Statistics will appear after verifications',
                    style: TextStyle(
                      color: Colors.grey.shade600,
                    ),
                  ),
                ],
              ),
            );
          }

          // Calculate statistics
          final total = history.length;
          final verified = history.where((r) => r.status == 'VERIFIED').length;
          final failed = history.where((r) => r.status == 'FAILED').length;
          final pending = history.where((r) => r.status == 'PENDING').length;
          final successRate = total > 0 ? (verified / total * 100) : 0.0;

          // Today's statistics
          final today = DateTime.now();
          final todayHistory = history.where((r) {
            return r.timestamp.year == today.year &&
                   r.timestamp.month == today.month &&
                   r.timestamp.day == today.day;
          }).toList();
          final todayTotal = todayHistory.length;
          final todayVerified = todayHistory.where((r) => r.status == 'VERIFIED').length;

          // Profile statistics
          final activeProfile = profileProvider.activeProfile;
          final profileHistory = activeProfile != null
              ? history.where((r) => r.credentialType == activeProfile.defaultCredentialTypes.first).toList()
              : <dynamic>[];

          return SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _buildOverviewCard(total, verified, failed, pending, successRate),
                const SizedBox(height: 16),
                _buildTodayCard(todayTotal, todayVerified),
                const SizedBox(height: 16),
                _buildCredentialTypeChart(history),
                const SizedBox(height: 16),
                _buildRecentActivity(history.take(5).toList()),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildOverviewCard(int total, int verified, int failed, int pending, double successRate) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.grey.shade200),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20),
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
                  child: Icon(Icons.analytics_rounded, color: Colors.green.shade700, size: 24),
                ),
                const SizedBox(width: 12),
                const Text(
                  'Overview',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),
            Row(
              children: [
                Expanded(
                  child: _buildStatItem('Total', total.toString(), Colors.blue),
                ),
                Expanded(
                  child: _buildStatItem('Verified', verified.toString(), Colors.green),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: _buildStatItem('Failed', failed.toString(), Colors.red),
                ),
                Expanded(
                  child: _buildStatItem('Success Rate', '${successRate.toStringAsFixed(1)}%', Colors.purple),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatItem(String label, String value, Color color) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Column(
        children: [
            Text(
              value,
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: _darkenColor(color, 0.2),
              ),
            ),
          const SizedBox(height: 4),
          Text(
            label,
            style: TextStyle(
              fontSize: 12,
              color: Colors.grey.shade600,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTodayCard(int todayTotal, int todayVerified) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.grey.shade200),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.orange.shade100,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(Icons.today_rounded, color: Colors.orange.shade700, size: 32),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Today',
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.grey.shade600,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '$todayTotal verifications',
                    style: const TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  Text(
                    '$todayVerified successful',
                    style: TextStyle(
                      fontSize: 14,
                      color: Colors.green.shade700,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCredentialTypeChart(List history) {
    final typeCounts = <String, int>{};
    for (final record in history) {
      typeCounts[record.credentialType] = (typeCounts[record.credentialType] ?? 0) + 1;
    }

    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.grey.shade200),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'By Credential Type',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            ...typeCounts.entries.map((entry) {
              final percentage = history.isNotEmpty
                  ? (entry.value / history.length * 100)
                  : 0.0;
              return Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          _getCredentialDisplayName(entry.key),
                          style: const TextStyle(
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        Text(
                          '${entry.value} (${percentage.toStringAsFixed(1)}%)',
                          style: TextStyle(
                            color: Colors.grey.shade600,
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    LinearProgressIndicator(
                      value: percentage / 100,
                      backgroundColor: Colors.grey.shade200,
                      valueColor: AlwaysStoppedAnimation<Color>(Colors.blue.shade700),
                      minHeight: 8,
                      borderRadius: BorderRadius.circular(4),
                    ),
                  ],
                ),
              );
            }),
          ],
        ),
      ),
    );
  }

  Widget _buildRecentActivity(List recentHistory) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: Colors.grey.shade200),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Recent Activity',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            ...recentHistory.map((record) {
              final statusColor = record.status == 'VERIFIED'
                  ? Colors.green
                  : record.status == 'FAILED'
                      ? Colors.red
                      : Colors.orange;
              return Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Row(
                  children: [
                    Container(
                      width: 8,
                      height: 8,
                      decoration: BoxDecoration(
                        color: statusColor,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            _getCredentialDisplayName(record.credentialType),
                            style: const TextStyle(
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          Text(
                            DateFormat('MMM dd, HH:mm').format(record.timestamp),
                            style: TextStyle(
                              fontSize: 12,
                              color: Colors.grey.shade600,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                      decoration: BoxDecoration(
                        color: statusColor.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Text(
                        record.status,
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w600,
                          color: _darkenColor(statusColor, 0.2),
                        ),
                      ),
                    ),
                  ],
                ),
              );
            }),
          ],
        ),
      ),
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
}

