import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/session_provider.dart';

class CreateSessionScreen extends StatefulWidget {
  const CreateSessionScreen({super.key});

  @override
  State<CreateSessionScreen> createState() => _CreateSessionScreenState();
}

class _CreateSessionScreenState extends State<CreateSessionScreen> {
  final _formKey = GlobalKey<FormState>();
  final _titleController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _conferenceNameController = TextEditingController();
  final _locationController = TextEditingController();
  
  DateTime? _startTime;
  DateTime? _endTime;
  bool _isSubmitting = false;

  @override
  void dispose() {
    _titleController.dispose();
    _descriptionController.dispose();
    _conferenceNameController.dispose();
    _locationController.dispose();
    super.dispose();
  }

  Future<void> _selectStartTime() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: DateTime.now(),
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    
    if (picked != null) {
      final time = await showTimePicker(
        context: context,
        initialTime: TimeOfDay.now(),
      );
      
      if (time != null) {
        setState(() {
          _startTime = DateTime(
            picked.year,
            picked.month,
            picked.day,
            time.hour,
            time.minute,
          );
        });
      }
    }
  }

  Future<void> _selectEndTime() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _startTime ?? DateTime.now(),
      firstDate: _startTime ?? DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    
    if (picked != null) {
      final time = await showTimePicker(
        context: context,
        initialTime: TimeOfDay.now(),
      );
      
      if (time != null) {
        setState(() {
          _endTime = DateTime(
            picked.year,
            picked.month,
            picked.day,
            time.hour,
            time.minute,
          );
        });
      }
    }
  }

  Future<void> _submitForm() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    final provider = context.read<SessionProvider>();
    final session = await provider.createSession(
      title: _titleController.text.trim(),
      description: _descriptionController.text.trim().isEmpty
          ? null
          : _descriptionController.text.trim(),
      conferenceName: _conferenceNameController.text.trim(),
      startTime: _startTime,
      endTime: _endTime,
      location: _locationController.text.trim().isEmpty
          ? null
          : _locationController.text.trim(),
    );

    setState(() {
      _isSubmitting = false;
    });

    if (!mounted) return;

    if (session != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Session created successfully!'),
          backgroundColor: Colors.green,
        ),
      );
      Navigator.pop(context, true);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            provider.errorMessage ?? 'Failed to create session',
          ),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('MMM dd, yyyy - HH:mm');

    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      appBar: AppBar(
        title: const Text(
          'Create Session',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            letterSpacing: -0.3,
          ),
        ),
      ),
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Container(
                  padding: const EdgeInsets.all(20),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(20),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.04),
                        blurRadius: 10,
                        offset: const Offset(0, 2),
                      ),
                    ],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Session Information',
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.bold,
                              color: Colors.grey.shade900,
                            ),
                      ),
                      const SizedBox(height: 20),
                      TextFormField(
                        controller: _conferenceNameController,
                        decoration: InputDecoration(
                          labelText: 'Conference Name *',
                          hintText: 'e.g., International Tech Conference 2024',
                          prefixIcon: Icon(
                            Icons.event_rounded,
                            color: Colors.orange.shade600,
                          ),
                          filled: true,
                          fillColor: Colors.grey.shade50,
                        ),
                        validator: (value) {
                          if (value == null || value.trim().isEmpty) {
                            return 'Please enter conference name';
                          }
                          return null;
                        },
                      ),
                      const SizedBox(height: 16),
                      TextFormField(
                        controller: _titleController,
                        decoration: InputDecoration(
                          labelText: 'Session Title *',
                          hintText: 'e.g., Keynote: Future of Technology',
                          prefixIcon: Icon(
                            Icons.title_rounded,
                            color: Colors.orange.shade600,
                          ),
                          filled: true,
                          fillColor: Colors.grey.shade50,
                        ),
                        validator: (value) {
                          if (value == null || value.trim().isEmpty) {
                            return 'Please enter session title';
                          }
                          return null;
                        },
                      ),
                      const SizedBox(height: 16),
                      TextFormField(
                        controller: _descriptionController,
                        decoration: InputDecoration(
                          labelText: 'Description (optional)',
                          hintText: 'Brief description of the session',
                          prefixIcon: Icon(
                            Icons.description_rounded,
                            color: Colors.orange.shade600,
                          ),
                          filled: true,
                          fillColor: Colors.grey.shade50,
                        ),
                        maxLines: 3,
                      ),
                      const SizedBox(height: 16),
                      TextFormField(
                        controller: _locationController,
                        decoration: InputDecoration(
                          labelText: 'Location (optional)',
                          hintText: 'e.g., Main Hall, Room 101',
                          prefixIcon: Icon(
                            Icons.location_on_rounded,
                            color: Colors.orange.shade600,
                          ),
                          filled: true,
                          fillColor: Colors.grey.shade50,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),
                Container(
                  padding: const EdgeInsets.all(20),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(20),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.04),
                        blurRadius: 10,
                        offset: const Offset(0, 2),
                      ),
                    ],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Session Time (optional)',
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.bold,
                              color: Colors.grey.shade900,
                            ),
                      ),
                      const SizedBox(height: 20),
                      InkWell(
                        onTap: _selectStartTime,
                        borderRadius: BorderRadius.circular(16),
                        child: Container(
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: Colors.grey.shade50,
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(
                              color: Colors.grey.shade200,
                            ),
                          ),
                          child: Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.all(10),
                                decoration: BoxDecoration(
                                  color: Colors.orange.shade50,
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                child: Icon(
                                  Icons.access_time_rounded,
                                  color: Colors.orange.shade700,
                                  size: 24,
                                ),
                              ),
                              const SizedBox(width: 16),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      'Start Time',
                                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                            color: Colors.grey.shade600,
                                          ),
                                    ),
                                    const SizedBox(height: 4),
                                    Text(
                                      _startTime != null
                                          ? dateFormat.format(_startTime!)
                                          : 'Not set',
                                      style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                                            color: _startTime != null
                                                ? Colors.grey.shade900
                                                : Colors.grey.shade400,
                                            fontWeight: FontWeight.w600,
                                          ),
                                    ),
                                  ],
                                ),
                              ),
                              Icon(
                                Icons.chevron_right_rounded,
                                color: Colors.grey.shade400,
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 12),
                      InkWell(
                        onTap: _selectEndTime,
                        borderRadius: BorderRadius.circular(16),
                        child: Container(
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: Colors.grey.shade50,
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(
                              color: Colors.grey.shade200,
                            ),
                          ),
                          child: Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.all(10),
                                decoration: BoxDecoration(
                                  color: Colors.orange.shade50,
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                child: Icon(
                                  Icons.access_time_filled_rounded,
                                  color: Colors.orange.shade700,
                                  size: 24,
                                ),
                              ),
                              const SizedBox(width: 16),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      'End Time',
                                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                            color: Colors.grey.shade600,
                                          ),
                                    ),
                                    const SizedBox(height: 4),
                                    Text(
                                      _endTime != null
                                          ? dateFormat.format(_endTime!)
                                          : 'Not set',
                                      style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                                            color: _endTime != null
                                                ? Colors.grey.shade900
                                                : Colors.grey.shade400,
                                            fontWeight: FontWeight.w600,
                                          ),
                                    ),
                                  ],
                                ),
                              ),
                              Icon(
                                Icons.chevron_right_rounded,
                                color: Colors.grey.shade400,
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 32),
                Container(
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(16),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.orange.shade300.withOpacity(0.4),
                        blurRadius: 12,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: ElevatedButton(
                    onPressed: _isSubmitting ? null : _submitForm,
                    style: ElevatedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 18),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16),
                      ),
                      elevation: 0,
                    ),
                    child: _isSubmitting
                        ? const SizedBox(
                            height: 24,
                            width: 24,
                            child: CircularProgressIndicator(
                              strokeWidth: 2.5,
                              valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                            ),
                          )
                        : const Text(
                            'Create Session',
                            style: TextStyle(
                              fontSize: 17,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                  ),
                ),
                const SizedBox(height: 20),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

