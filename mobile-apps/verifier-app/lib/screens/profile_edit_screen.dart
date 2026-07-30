import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/profile_provider.dart';
import '../models/verification_profile.dart';
import '../services/haptic_service.dart';

class ProfileEditScreen extends StatefulWidget {
  final VerificationProfile? profile;

  const ProfileEditScreen({super.key, this.profile});

  @override
  State<ProfileEditScreen> createState() => _ProfileEditScreenState();
}

class _ProfileEditScreenState extends State<ProfileEditScreen> {
  late TextEditingController _nameController;
  late TextEditingController _descriptionController;
  late Color _selectedColor;
  late IconData _selectedIcon;
  late List<String> _selectedCredentialTypes;

  final List<Color> _availableColors = [
    Colors.orange,
    Colors.blue,
    Colors.purple,
    Colors.teal,
    Colors.red,
    Colors.pink,
    Colors.indigo,
    Colors.cyan,
    Colors.amber,
    Colors.green,
    Colors.brown,
    Colors.grey,
  ];

  final List<IconData> _availableIcons = [
    Icons.local_bar_rounded,
    Icons.business_rounded,
    Icons.event_rounded,
    Icons.library_books_rounded,
    Icons.school_rounded,
    Icons.restaurant_rounded,
    Icons.hotel_rounded,
    Icons.sports_soccer_rounded,
    Icons.movie_rounded,
    Icons.music_note_rounded,
    Icons.shopping_cart_rounded,
    Icons.medical_services_rounded,
  ];

  final List<String> _allCredentialTypes = [
    'EducationalID',
    'IdentityCredential',
    'EuropeanStudentCard',
    'UniversityDegree',
  ];

  @override
  void initState() {
    super.initState();
    if (widget.profile != null) {
      _nameController = TextEditingController(text: widget.profile!.name);
      _descriptionController = TextEditingController(text: widget.profile!.description);
      _selectedColor = widget.profile!.color;
      _selectedIcon = widget.profile!.icon;
      _selectedCredentialTypes = List<String>.from(widget.profile!.defaultCredentialTypes);
    } else {
      _nameController = TextEditingController();
      _descriptionController = TextEditingController();
      _selectedColor = Colors.grey;
      _selectedIcon = Icons.tune_rounded;
      _selectedCredentialTypes = ['EducationalID'];
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    try {
      return Scaffold(
        appBar: AppBar(
          title: Text(
            widget.profile == null ? 'Create Profile' : 'Edit Profile',
            style: const TextStyle(
              fontWeight: FontWeight.bold,
              letterSpacing: -0.5,
            ),
          ),
          backgroundColor: _selectedColor,
          foregroundColor: Colors.white,
          elevation: 0,
          flexibleSpace: Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [
                  _darkenColor(_selectedColor, 0.2),
                  _darkenColor(_selectedColor, 0.1),
                ],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: _saveProfile,
              child: const Text(
                'Save',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ],
        ),
        body: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _buildNameField(),
              const SizedBox(height: 20),
              _buildDescriptionField(),
              const SizedBox(height: 20),
              _buildColorSelector(),
              const SizedBox(height: 20),
              _buildIconSelector(),
              const SizedBox(height: 20),
              _buildCredentialTypesSelector(),
              const SizedBox(height: 40),
            ],
          ),
        ),
      );
    } catch (e) {
      // Fallback UI if there's an error
      return Scaffold(
        appBar: AppBar(
          title: const Text('Error'),
          backgroundColor: Colors.red,
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.error_outline, size: 64, color: Colors.red),
              const SizedBox(height: 16),
              Text('Error loading profile editor: $e'),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('Go Back'),
              ),
            ],
          ),
        ),
      );
    }
  }

  Widget _buildNameField() {
    return TextField(
      controller: _nameController,
      decoration: InputDecoration(
        labelText: 'Profile Name',
        hintText: 'e.g., Bar, Office, Conference',
        prefixIcon: const Icon(Icons.label_rounded),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        filled: true,
        fillColor: Colors.grey.shade50,
      ),
    );
  }

  Widget _buildDescriptionField() {
    return TextField(
      controller: _descriptionController,
      maxLines: 3,
      decoration: InputDecoration(
        labelText: 'Description',
        hintText: 'Describe what this profile is used for',
        prefixIcon: const Icon(Icons.description_rounded),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
        ),
        filled: true,
        fillColor: Colors.grey.shade50,
      ),
    );
  }

  Widget _buildColorSelector() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Color',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 12),
        Wrap(
          spacing: 12,
          runSpacing: 12,
          children: _availableColors.map((color) {
            final isSelected = color.value == _selectedColor.value;
            return GestureDetector(
              onTap: () {
                HapticService().lightImpact();
                setState(() {
                  _selectedColor = color;
                });
              },
              child: Container(
                width: 50,
                height: 50,
                decoration: BoxDecoration(
                  color: color,
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: isSelected ? Colors.black : Colors.grey.shade300,
                    width: isSelected ? 3 : 1,
                  ),
                  boxShadow: isSelected
                      ? [
                          BoxShadow(
                            color: color.withOpacity(0.5),
                            blurRadius: 8,
                            offset: const Offset(0, 4),
                          ),
                        ]
                      : null,
                ),
                child: isSelected
                    ? const Icon(Icons.check, color: Colors.white, size: 24)
                    : null,
              ),
            );
          }).toList(),
        ),
      ],
    );
  }

  Widget _buildIconSelector() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Icon',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 12),
        Wrap(
          spacing: 12,
          runSpacing: 12,
          children: _availableIcons.map((icon) {
            final isSelected = icon.codePoint == _selectedIcon.codePoint;
            return GestureDetector(
              onTap: () {
                HapticService().lightImpact();
                setState(() {
                  _selectedIcon = icon;
                });
              },
              child: Container(
                width: 50,
                height: 50,
                decoration: BoxDecoration(
                  color: isSelected
                      ? _selectedColor.withOpacity(0.2)
                      : Colors.grey.shade100,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: isSelected
                        ? _selectedColor
                        : Colors.grey.shade300,
                    width: isSelected ? 2 : 1,
                  ),
                ),
                child: Icon(
                  icon,
                  color: isSelected ? _selectedColor : Colors.grey.shade700,
                  size: 24,
                ),
              ),
            );
          }).toList(),
        ),
      ],
    );
  }

  Widget _buildCredentialTypesSelector() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Credential Types',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 12),
        ..._allCredentialTypes.map((type) {
          final isSelected = _selectedCredentialTypes.contains(type);
          return CheckboxListTile(
            title: Text(_getCredentialDisplayName(type)),
            value: isSelected,
            onChanged: (value) {
              HapticService().lightImpact();
              setState(() {
                if (value == true) {
                  _selectedCredentialTypes.add(type);
                } else {
                  _selectedCredentialTypes.remove(type);
                }
              });
            },
            activeColor: _selectedColor,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(8),
            ),
          );
        }),
        if (_selectedCredentialTypes.isEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(
              'Please select at least one credential type',
              style: TextStyle(
                color: Colors.red.shade700,
                fontSize: 12,
              ),
            ),
          ),
      ],
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

  Future<void> _saveProfile() async {
    if (_nameController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Please enter a profile name'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    if (_selectedCredentialTypes.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Please select at least one credential type'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    HapticService().mediumImpact();

    final provider = Provider.of<ProfileProvider>(context, listen: false);
    final profile = VerificationProfile(
      id: widget.profile?.id ?? DateTime.now().millisecondsSinceEpoch.toString(),
      name: _nameController.text.trim(),
      description: _descriptionController.text.trim(),
      icon: _selectedIcon,
      color: _selectedColor,
      defaultCredentialTypes: _selectedCredentialTypes,
      settings: widget.profile?.settings ?? {},
    );

    try {
      if (widget.profile == null) {
        await provider.addProfile(profile);
      } else {
        await provider.updateProfile(profile);
      }

      if (mounted) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              widget.profile == null
                  ? 'Profile created successfully'
                  : 'Profile updated successfully',
            ),
            backgroundColor: _selectedColor,
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(10),
            ),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error saving profile: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }
}

/// Helper function to darken a color
Color _darkenColor(Color color, double amount) {
  assert(amount >= 0 && amount <= 1);
  final hsl = HSLColor.fromColor(color);
  final lightness = (hsl.lightness - amount).clamp(0.0, 1.0);
  return hsl.withLightness(lightness).toColor();
}

