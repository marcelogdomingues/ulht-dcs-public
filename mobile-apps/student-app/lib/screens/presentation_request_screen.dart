import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import '../services/api_service.dart';
import '../services/verification_history_service.dart';

class PresentationRequestScreen extends StatefulWidget {
  final String presentationRequestUrl;
  final Map<String, dynamic>? presentationDefinition;
  final String? walletId;

  const PresentationRequestScreen({
    super.key,
    required this.presentationRequestUrl,
    this.presentationDefinition,
    this.walletId,
  });

  @override
  State<PresentationRequestScreen> createState() => _PresentationRequestScreenState();
}

class _PresentationRequestScreenState extends State<PresentationRequestScreen> {
  final ApiService _apiService = ApiService();
  final VerificationHistoryService _historyService = VerificationHistoryService();
  bool _isLoading = true;
  bool _isSubmitting = false;
  String? _error;
  List<Map<String, dynamic>>? _matchedCredentials;
  Map<String, bool> _selectedAttributes = {};
  Map<String, String> _attributeLabels = {};
  String? _credentialType;
  String? _issuerName;
  String? _fullPresentationRequestUrl;
  String? _walletId;
  Map<String, dynamic>? _studentData;

  @override
  void initState() {
    super.initState();
    _loadMatchedCredentials();
  }

  Future<void> _loadMatchedCredentials() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      // Call matchCredentialsForPresentationDefinition through credential service
      // This will handle authentication and return matched credentials with disclosures
      final result = await _apiService.matchCredentialsForPresentationDefinition(
        presentationRequestUrl: widget.presentationRequestUrl,
      );

      if (result['success'] != true) {
        throw Exception(result['message']?.toString() ?? 'Failed to match credentials');
      }

      final matchedCredentialsList = result['matchedCredentials'] as List<dynamic>?;
      
      if (matchedCredentialsList == null || matchedCredentialsList.isEmpty) {
        throw Exception('No matching credentials found for this presentation request.');
      }

      // Extract wallet ID and full presentation request URL from result
      _walletId = result['walletId']?.toString();
      _fullPresentationRequestUrl = result['fullPresentationRequestUrl']?.toString();
      
      // Extract matched credentials
      final matchedCredentials = matchedCredentialsList.map((c) => c as Map<String, dynamic>).toList();

      if (matchedCredentials.isEmpty) {
        throw Exception('No matching credentials found for this presentation request.');
      }

      // Extract attributes from the first matched credential
      final credential = matchedCredentials[0] as Map<String, dynamic>;
      final parsedDocument = credential['parsedDocument'] as Map<String, dynamic>?;
      final credentialSubject = parsedDocument?['credentialSubject'] as Map<String, dynamic>?;
      
      // Extract credential type
      final types = parsedDocument?['type'] as List<dynamic>?;
      _credentialType = types?.firstWhere(
        (type) => type != 'VerifiableCredential',
        orElse: () => 'Unknown',
      )?.toString() ?? 'Unknown';

      // Extract issuer
      final issuer = parsedDocument?['issuer'] as Map<String, dynamic>?;
      _issuerName = issuer?['id']?.toString() ?? 'Unknown';

      // Extract student data for verification history
      if (credentialSubject != null) {
        _studentData = {
          'givenName': credentialSubject['givenName'],
          'familyName': credentialSubject['familyName'],
          'name': credentialSubject['name'],
          'studentId': credentialSubject['studentId'],
          'email': credentialSubject['email'],
          'courseName': credentialSubject['courseName'],
          'courseCode': credentialSubject['courseCode'],
          'institutionName': credentialSubject['institutionName'],
        };
      }

      // Parse disclosures from the credential
      final disclosures = credential['disclosures']?.toString();
      if (disclosures != null && disclosures.isNotEmpty) {
        _parseDisclosures(disclosures);
      } else if (credentialSubject != null) {
        // If no disclosures, extract all attributes from credentialSubject
        _extractAttributesFromCredentialSubject(credentialSubject);
      }

      setState(() {
        _matchedCredentials = matchedCredentials;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  Map<String, dynamic> _buildPresentationDefinitionFromUrl(String url) {
    // Try to extract presentation definition from URL
    // For now, create a default one for EducationalID
    return {
      'id': 'presentation-definition-${DateTime.now().millisecondsSinceEpoch}',
      'input_descriptors': [
        {
          'id': 'EducationalID',
          'format': {
            'jwt_vc_json': {
              'alg': ['EdDSA']
            }
          },
          'constraints': {
            'fields': [
              {
                'path': ['\$.vc.type'],
                'filter': {
                  'type': 'string',
                  'pattern': 'EducationalID'
                }
              }
            ]
          }
        }
      ]
    };
  }

  void _parseDisclosures(String disclosuresString) {
    // Disclosures are in format: "attr1~attr2~attr3"
    // Each disclosure is base64url encoded and separated by ~
    final parts = disclosuresString.split('~');
    
    for (var part in parts) {
      try {
        // Decode base64url
        final decoded = _decodeBase64Url(part);
        final json = jsonDecode(decoded);
        
        if (json is List && json.length >= 2) {
          final attributeName = json[1]?.toString();
          if (attributeName != null) {
            _selectedAttributes[attributeName] = false; // Default to not selected
            _attributeLabels[attributeName] = _formatAttributeName(attributeName);
          }
        }
      } catch (e) {
        // Skip invalid disclosures
        debugPrint('Error parsing disclosure: $e');
      }
    }
  }

  String _decodeBase64Url(String input) {
    // Base64URL decoding
    String base64 = input.replaceAll('-', '+').replaceAll('_', '/');
    switch (base64.length % 4) {
      case 1:
        base64 += '===';
        break;
      case 2:
        base64 += '==';
        break;
      case 3:
        base64 += '=';
        break;
    }
    return utf8.decode(base64Decode(base64));
  }

  void _extractAttributesFromCredentialSubject(Map<String, dynamic> credentialSubject) {
    // Extract all attributes except internal fields
    credentialSubject.forEach((key, value) {
      if (key != 'id' && key != '_sd' && !key.startsWith('_')) {
        _selectedAttributes[key] = false;
        _attributeLabels[key] = _formatAttributeName(key);
      }
    });
  }

  String _formatAttributeName(String name) {
    // Convert camelCase to Title Case
    return name
        .replaceAllMapped(RegExp(r'([A-Z])'), (match) => ' ${match.group(0)}')
        .trim()
        .split(' ')
        .map((word) => word.isEmpty 
            ? '' 
            : word[0].toUpperCase() + word.substring(1).toLowerCase())
        .join(' ');
  }

  void _toggleAllAttributes(bool selectAll) {
    setState(() {
      _selectedAttributes.forEach((key, value) {
        _selectedAttributes[key] = selectAll;
      });
    });
  }

  Future<void> _submitPresentation() async {
    if (_matchedCredentials == null || _matchedCredentials!.isEmpty) {
      return;
    }

    final selectedAttributesList = _selectedAttributes.entries
        .where((entry) => entry.value)
        .map((entry) => entry.key)
        .toList();

    if (selectedAttributesList.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Please select at least one attribute to disclose.'),
          backgroundColor: Colors.orange,
        ),
      );
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    try {
      final credential = _matchedCredentials![0];
      final walletId = _walletId ?? credential['wallet']?.toString() ?? widget.walletId;
      
      if (walletId == null) {
        throw Exception('Wallet ID not found');
      }

      // Use the full presentation request URL if available, otherwise use the original
      final presentationRequestUrl = _fullPresentationRequestUrl ?? widget.presentationRequestUrl;

      await _apiService.submitPresentationWithDisclosures(
        walletId: walletId,
        presentationRequestUrl: presentationRequestUrl,
        selectedDisclosures: selectedAttributesList,
        credentialData: credential,
      );

      // Save to verification history
      final credentialId = credential['id']?.toString() ?? '';
      final studentName = _studentData != null
          ? '${_studentData!['givenName'] ?? ''} ${_studentData!['familyName'] ?? ''}'.trim()
          : _studentData?['name']?.toString();
      final studentId = _studentData?['studentId']?.toString();

      await _historyService.saveVerification(
        credentialType: _credentialType ?? 'Unknown',
        credentialId: credentialId,
        disclosedAttributes: selectedAttributesList,
        timestamp: DateTime.now(),
        studentName: studentName,
        studentId: studentId,
        studentData: _studentData,
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('✅ Credentials presented successfully!'),
            backgroundColor: Colors.green,
            duration: Duration(seconds: 2),
          ),
        );
        
        Navigator.pop(context, {'success': true});
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to present credentials: ${e.toString()}'),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 4),
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Presentation Request'),
        backgroundColor: Colors.blue.shade700,
        foregroundColor: Colors.white,
      ),
      body: _isLoading
          ? const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(),
                  SizedBox(height: 16),
                  Text('Loading presentation request...'),
                ],
              ),
            )
          : _error != null
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24.0),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.error_outline,
                          size: 64,
                          color: Colors.red.shade300,
                        ),
                        const SizedBox(height: 16),
                        Text(
                          'Error',
                          style: TextStyle(
                            fontSize: 24,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          _error!,
                          textAlign: TextAlign.center,
                          style: TextStyle(color: Colors.grey.shade700),
                        ),
                        const SizedBox(height: 24),
                        ElevatedButton(
                          onPressed: () {
                            Navigator.pop(context);
                          },
                          child: const Text('Close'),
                        ),
                      ],
                    ),
                  ),
                )
              : _buildContent(),
    );
  }

  Widget _buildContent() {
    final allSelected = _selectedAttributes.values.every((value) => value);
    final someSelected = _selectedAttributes.values.any((value) => value);

    return Column(
      children: [
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Presentation Request Header
                Card(
                  elevation: 2,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.all(20.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Icon(
                              Icons.verified_user,
                              color: Colors.blue.shade700,
                              size: 28,
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    _credentialType ?? 'Credential',
                                    style: const TextStyle(
                                      fontSize: 20,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                  const SizedBox(height: 4),
                                  Text(
                                    'Issuer: ${_issuerName ?? "Unknown"}',
                                    style: TextStyle(
                                      fontSize: 14,
                                      color: Colors.grey.shade600,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 16),
                        const Divider(),
                        const SizedBox(height: 12),
                        const Text(
                          '1 of 1',
                          style: TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w500,
                            color: Colors.grey,
                          ),
                        ),
                        const SizedBox(height: 4),
                        const Text(
                          'Credential to present',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 24),
                
                // Disclose All Button
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: () => _toggleAllAttributes(!allSelected),
                        icon: Icon(allSelected ? Icons.check_box : Icons.check_box_outline_blank),
                        label: Text(allSelected ? 'Deselect All' : 'Disclose All'),
                        style: OutlinedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(vertical: 16),
                          side: BorderSide(color: Colors.blue.shade700),
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 24),
                
                // Attributes List
                const Text(
                  'Attributes to disclose',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 16),
                
                ..._selectedAttributes.entries.map((entry) {
                  final attributeName = entry.key;
                  final isSelected = entry.value;
                  final label = _attributeLabels[attributeName] ?? attributeName;
                  
                  return Card(
                    margin: const EdgeInsets.only(bottom: 12),
                    elevation: 1,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                      side: BorderSide(
                        color: isSelected 
                            ? Colors.blue.shade300 
                            : Colors.grey.shade300,
                        width: isSelected ? 2 : 1,
                      ),
                    ),
                    child: CheckboxListTile(
                      title: Text(
                        label,
                        style: TextStyle(
                          fontWeight: isSelected 
                              ? FontWeight.w600 
                              : FontWeight.normal,
                        ),
                      ),
                      subtitle: Text(
                        attributeName,
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.grey.shade600,
                        ),
                      ),
                      value: isSelected,
                      onChanged: (value) {
                        setState(() {
                          _selectedAttributes[attributeName] = value ?? false;
                        });
                      },
                      activeColor: Colors.blue.shade700,
                      controlAffinity: ListTileControlAffinity.leading,
                    ),
                  );
                }).toList(),
              ],
            ),
          ),
        ),
        
        // Bottom Action Buttons
        Container(
          padding: const EdgeInsets.all(24.0),
          decoration: BoxDecoration(
            color: Colors.white,
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.1),
                blurRadius: 10,
                offset: const Offset(0, -2),
              ),
            ],
          ),
          child: SafeArea(
            child: Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: _isSubmitting ? null : () => Navigator.pop(context),
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      side: BorderSide(color: Colors.grey.shade400),
                    ),
                    child: const Text(
                      'Decline',
                      style: TextStyle(fontSize: 16),
                    ),
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  flex: 2,
                  child: ElevatedButton(
                    onPressed: _isSubmitting ? null : _submitPresentation,
                    style: ElevatedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      backgroundColor: Colors.blue.shade700,
                      foregroundColor: Colors.white,
                    ),
                    child: _isSubmitting
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                            ),
                          )
                        : const Text(
                            'Disclose',
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
        ),
      ],
    );
  }
}

