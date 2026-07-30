import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/verification_provider.dart';
import '../services/export_service.dart';

class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  final TextEditingController _searchController = TextEditingController();
  String _filterStatus = 'All';

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Consumer<VerificationProvider>(
        builder: (context, provider, child) {
          // Filter history
          var filteredHistory = provider.history;
          
          // Apply search filter
          if (_searchController.text.isNotEmpty) {
            final searchTerm = _searchController.text.toLowerCase();
            filteredHistory = filteredHistory.where((record) {
              return record.credentialType.toLowerCase().contains(searchTerm) ||
                     record.status.toLowerCase().contains(searchTerm) ||
                     record.correlationId.toLowerCase().contains(searchTerm);
            }).toList();
          }
          
          // Apply status filter
          if (_filterStatus != 'All') {
            filteredHistory = filteredHistory.where((record) {
              return record.status == _filterStatus;
            }).toList();
          }
          
          if (provider.history.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Container(
                    padding: const EdgeInsets.all(24),
                    decoration: BoxDecoration(
                      color: Colors.grey.shade100,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      Icons.history_rounded,
                      size: 80,
                      color: Colors.grey.shade400,
                    ),
                  ),
                  const SizedBox(height: 24),
                  Text(
                    'No Verification History',
                    style: TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.bold,
                      color: Colors.grey.shade800,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Verification records will appear here\nonce you verify credentials',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 15,
                      color: Colors.grey.shade600,
                      height: 1.5,
                    ),
                  ),
                ],
              ),
            );
          }

          return Column(
            children: [
              // Search and filter bar
              Container(
                padding: const EdgeInsets.all(16),
                color: Colors.white,
                child: Column(
                  children: [
                    TextField(
                      controller: _searchController,
                      decoration: InputDecoration(
                        hintText: 'Search history...',
                        prefixIcon: const Icon(Icons.search_rounded),
                        suffixIcon: _searchController.text.isNotEmpty
                            ? IconButton(
                                icon: const Icon(Icons.clear_rounded),
                                onPressed: () {
                                  setState(() {
                                    _searchController.clear();
                                  });
                                },
                              )
                            : null,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        filled: true,
                        fillColor: Colors.grey.shade50,
                      ),
                      onChanged: (value) => setState(() {}),
                    ),
                    const SizedBox(height: 12),
                    SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      child: Row(
                        children: ['All', 'VERIFIED', 'FAILED', 'PENDING'].map((status) {
                          final isSelected = _filterStatus == status;
                          return Padding(
                            padding: const EdgeInsets.only(right: 8),
                            child: FilterChip(
                              label: Text(status),
                              selected: isSelected,
                              onSelected: (selected) {
                                setState(() {
                                  _filterStatus = status;
                                });
                              },
                              selectedColor: Colors.green.shade100,
                              checkmarkColor: Colors.green.shade700,
                            ),
                          );
                        }).toList(),
                      ),
                    ),
                  ],
                ),
              ),
              // Export button
              if (provider.history.isNotEmpty)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  color: Colors.white,
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      TextButton.icon(
                        onPressed: () async {
                          try {
                            await ExportService.exportHistoryToCSV(provider.history);
                            if (mounted) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(
                                  content: Text('History exported successfully'),
                                  backgroundColor: Colors.green,
                                ),
                              );
                            }
                          } catch (e) {
                            if (mounted) {
                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                  content: Text('Export failed: $e'),
                                  backgroundColor: Colors.red,
                                ),
                              );
                            }
                          }
                        },
                        icon: const Icon(Icons.download_rounded),
                        label: const Text('Export CSV'),
                        style: TextButton.styleFrom(
                          foregroundColor: Colors.green.shade700,
                        ),
                      ),
                    ],
                  ),
                ),
              // History list
              Expanded(
                child: filteredHistory.isEmpty
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.search_off_rounded, size: 64, color: Colors.grey.shade400),
                            const SizedBox(height: 16),
                            Text(
                              'No results found',
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                                color: Colors.grey.shade800,
                              ),
                            ),
                          ],
                        ),
                      )
                    : RefreshIndicator(
                        onRefresh: () async {
                          // Refresh could trigger a re-check of statuses
                        },
                        child: ListView.builder(
                          padding: const EdgeInsets.all(16.0),
                          itemCount: filteredHistory.length,
                          itemBuilder: (context, index) {
                            final record = filteredHistory[index];
                            return Dismissible(
                              key: Key(record.correlationId),
                              direction: DismissDirection.endToStart,
                              background: Container(
                                alignment: Alignment.centerRight,
                                padding: const EdgeInsets.only(right: 20),
                                decoration: BoxDecoration(
                                  color: Colors.red,
                                  borderRadius: BorderRadius.circular(16),
                                ),
                                child: const Icon(
                                  Icons.delete_rounded,
                                  color: Colors.white,
                                  size: 32,
                                ),
                              ),
                              confirmDismiss: (direction) async {
                                return await showDialog<bool>(
                                  context: context,
                                  builder: (context) => AlertDialog(
                                    title: const Text('Delete Record'),
                                    content: const Text(
                                      'Are you sure you want to delete this verification record? This action cannot be undone.',
                                    ),
                                    actions: [
                                      TextButton(
                                        onPressed: () => Navigator.pop(context, false),
                                        child: const Text('Cancel'),
                                      ),
                                      TextButton(
                                        onPressed: () => Navigator.pop(context, true),
                                        style: TextButton.styleFrom(
                                          foregroundColor: Colors.red,
                                        ),
                                        child: const Text('Delete'),
                                      ),
                                    ],
                                  ),
                                ) ?? false;
                              },
                              onDismissed: (direction) {
                                provider.removeFromHistory(record.correlationId);
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(
                                    content: const Text('Record deleted'),
                                    action: SnackBarAction(
                                      label: 'Undo',
                                      onPressed: () {
                                        // Could implement undo functionality
                                      },
                                    ),
                                  ),
                                );
                              },
                              child: _buildHistoryCard(context, record, index),
                            );
                          },
                        ),
                      ),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _buildHistoryCard(BuildContext context, VerificationRecord record, int index) {
    final statusColor = _getStatusColor(record.status);
    final statusIcon = _getStatusIcon(record.status);
    
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0.0, end: 1.0),
      duration: Duration(milliseconds: 300 + (index * 50)),
      curve: Curves.easeOut,
      builder: (context, value, child) {
        return Transform.translate(
          offset: Offset(0, 20 * (1 - value)),
          child: Opacity(
            opacity: value,
            child: child,
          ),
        );
      },
      child: Card(
        margin: const EdgeInsets.only(bottom: 16),
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(
            color: statusColor.withOpacity(0.2),
            width: 1,
          ),
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(16),
          child: ExpansionTile(
            tilePadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
            childrenPadding: EdgeInsets.zero,
            leading: Container(
              width: 56,
              height: 56,
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    statusColor,
                    statusColor.withOpacity(0.7),
                  ],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(12),
                boxShadow: [
                  BoxShadow(
                    color: statusColor.withOpacity(0.3),
                    blurRadius: 8,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: Icon(
                statusIcon,
                color: Colors.white,
                size: 28,
              ),
            ),
            title: Text(
              _getCredentialDisplayName(record.credentialType),
              style: const TextStyle(
                fontSize: 17,
                fontWeight: FontWeight.bold,
                letterSpacing: -0.5,
              ),
            ),
            subtitle: Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Row(
                children: [
                  Icon(
                    Icons.access_time_rounded,
                    size: 14,
                    color: Colors.grey.shade600,
                  ),
                  const SizedBox(width: 4),
                  Text(
                    DateFormat('MMM dd, yyyy • HH:mm').format(record.timestamp),
                    style: TextStyle(
                      color: Colors.grey.shade600,
                      fontSize: 13,
                    ),
                  ),
                ],
              ),
            ),
            trailing: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              decoration: BoxDecoration(
                color: statusColor.withOpacity(0.15),
                borderRadius: BorderRadius.circular(20),
                border: Border.all(
                  color: statusColor.withOpacity(0.3),
                  width: 1,
                ),
              ),
              child: Text(
                record.status,
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: statusColor,
                  letterSpacing: 0.5,
                ),
              ),
            ),
            children: [
              Container(
                color: Colors.grey.shade50,
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _buildDetailRow(
                      'Correlation ID',
                      record.correlationId,
                      Icons.fingerprint_rounded,
                    ),
                    const SizedBox(height: 12),
                    _buildDetailRow(
                      'Status',
                      record.status,
                      Icons.info_outline_rounded,
                    ),
                    const SizedBox(height: 12),
                    _buildDetailRow(
                      'Timestamp',
                      DateFormat('yyyy-MM-dd HH:mm:ss').format(record.timestamp),
                      Icons.calendar_today_rounded,
                    ),
                    // Disclosed Attributes Section
                    if (record.disclosedAttributes != null && record.disclosedAttributes!.isNotEmpty) ...[
                      const SizedBox(height: 16),
                      Divider(color: Colors.grey.shade300),
                      const SizedBox(height: 16),
                      Row(
                        children: [
                          Icon(
                            Icons.lock_open_rounded,
                            size: 18,
                            color: Colors.blue.shade700,
                          ),
                          const SizedBox(width: 8),
                          const Text(
                            'Disclosed Attributes',
                            style: TextStyle(
                              fontSize: 15,
                              fontWeight: FontWeight.bold,
                              letterSpacing: -0.3,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: record.disclosedAttributes!.map((attr) {
                          return Chip(
                            label: Text(
                              _formatAttributeName(attr),
                              style: const TextStyle(fontSize: 12),
                            ),
                            backgroundColor: Colors.blue.shade50,
                            labelStyle: TextStyle(
                              color: Colors.blue.shade700,
                              fontWeight: FontWeight.w500,
                            ),
                          );
                        }).toList(),
                      ),
                    ],
                    if (record.result != null) ...[
                      const SizedBox(height: 16),
                      Divider(color: Colors.grey.shade300),
                      const SizedBox(height: 16),
                      Row(
                        children: [
                          Icon(
                            Icons.verified_user_rounded,
                            size: 18,
                            color: Colors.green.shade700,
                          ),
                          const SizedBox(width: 8),
                          const Text(
                            'Credential Details',
                            style: TextStyle(
                              fontSize: 15,
                              fontWeight: FontWeight.bold,
                              letterSpacing: -0.3,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      ..._extractCredentialDetails(record.result!),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDetailRow(String label, String value, IconData icon) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(
          icon,
          size: 18,
          color: Colors.grey.shade600,
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
                  letterSpacing: 0.2,
                ),
              ),
              const SizedBox(height: 4),
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.grey.shade200),
                ),
                child: SelectableText(
                  value,
                  style: const TextStyle(
                    fontSize: 14,
                    color: Colors.black87,
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Color _getStatusColor(String status) {
    switch (status.toUpperCase()) {
      case 'VERIFIED':
      case 'COMPLETED':
        return Colors.green;
      case 'FAILED':
        return Colors.red;
      case 'PENDING':
      case 'PROCESSING':
        return Colors.orange;
      default:
        return Colors.grey;
    }
  }

  IconData _getStatusIcon(String status) {
    switch (status.toUpperCase()) {
      case 'VERIFIED':
      case 'COMPLETED':
        return Icons.check_circle_rounded;
      case 'FAILED':
        return Icons.error_rounded;
      case 'PENDING':
      case 'PROCESSING':
        return Icons.hourglass_empty_rounded;
      default:
        return Icons.info_rounded;
    }
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

  String _formatAttributeName(String name) {
    return name
        .replaceAllMapped(RegExp(r'([A-Z])'), (match) => ' ${match.group(0)}')
        .trim()
        .split(' ')
        .map((word) => word.isEmpty 
            ? '' 
            : word[0].toUpperCase() + word.substring(1).toLowerCase())
        .join(' ');
  }

  List<Widget> _extractCredentialDetails(Map<String, dynamic> result) {
    List<Widget> details = [];
    
    try {
      // Navigate through the response structure to find credential data
      if (result['credentialsByFormat'] != null) {
        final byFormat = result['credentialsByFormat'] as Map<String, dynamic>;
        if (byFormat['jwt_vc_json'] != null) {
          final jwtCreds = byFormat['jwt_vc_json'] as List;
          if (jwtCreds.isNotEmpty) {
            final firstCred = jwtCreds[0] as Map<String, dynamic>;
            if (firstCred['verifiableCredentials'] != null) {
              final vcs = firstCred['verifiableCredentials'] as List;
              if (vcs.isNotEmpty) {
                final vc = vcs[0] as Map<String, dynamic>;
                if (vc['payload'] != null) {
                  final payload = vc['payload'] as Map<String, dynamic>;
                  if (payload['vc'] != null) {
                    final vcData = payload['vc'] as Map<String, dynamic>;
                    
                    // Extract issuance date
                    if (vcData['issuanceDate'] != null) {
                      details.add(const SizedBox(height: 8));
                      details.add(_buildDetailRow('Issuance Date', vcData['issuanceDate'].toString(), Icons.calendar_today_rounded));
                    }
                    
                    if (vcData['credentialSubject'] != null) {
                      final subject = vcData['credentialSubject'] as Map<String, dynamic>;
                      
                      // Student Information Section
                      bool hasStudentInfo = false;
                      if (subject['studentId'] != null) {
                        details.add(const SizedBox(height: 8));
                        details.add(_buildDetailRow('Student ID', subject['studentId'].toString(), Icons.badge_rounded));
                        hasStudentInfo = true;
                      }
                      if (subject['givenName'] != null) {
                        details.add(const SizedBox(height: 8));
                        details.add(_buildDetailRow('Given Name', subject['givenName'].toString(), Icons.person_rounded));
                        hasStudentInfo = true;
                      }
                      if (subject['familyName'] != null) {
                        details.add(const SizedBox(height: 8));
                        details.add(_buildDetailRow('Family Name', subject['familyName'].toString(), Icons.person_outline_rounded));
                        hasStudentInfo = true;
                      }
                      if (subject['email'] != null) {
                        details.add(const SizedBox(height: 8));
                        details.add(_buildDetailRow('Email', subject['email'].toString(), Icons.email_rounded));
                        hasStudentInfo = true;
                      }
                      if (subject['courseName'] != null) {
                        details.add(const SizedBox(height: 8));
                        details.add(_buildDetailRow('Course', subject['courseName'].toString(), Icons.school_rounded));
                        hasStudentInfo = true;
                      }
                      if (subject['courseCode'] != null) {
                        details.add(const SizedBox(height: 8));
                        details.add(_buildDetailRow('Course Code', subject['courseCode'].toString(), Icons.code_rounded));
                        hasStudentInfo = true;
                      }
                      if (subject['institutionName'] != null) {
                        details.add(const SizedBox(height: 8));
                        details.add(_buildDetailRow('Institution', subject['institutionName'].toString(), Icons.business_rounded));
                        hasStudentInfo = true;
                      }
                      if (subject['country'] != null) {
                        details.add(const SizedBox(height: 8));
                        details.add(_buildDetailRow('Country', subject['country'].toString(), Icons.public_rounded));
                        hasStudentInfo = true;
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    } catch (e) {
      // If parsing fails, show raw result
      details.add(const SizedBox(height: 8));
      details.add(_buildDetailRow('Raw Data', result.toString(), Icons.code_rounded));
    }
    
    return details.isEmpty ? [const SizedBox.shrink()] : details;
  }
}
