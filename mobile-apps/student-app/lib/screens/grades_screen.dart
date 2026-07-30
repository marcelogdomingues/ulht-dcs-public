import 'package:flutter/material.dart';
import '../services/api_service.dart';

class GradesScreen extends StatefulWidget {
  const GradesScreen({super.key});

  @override
  State<GradesScreen> createState() => _GradesScreenState();
}

class _GradesScreenState extends State<GradesScreen> {
  final ApiService _apiService = ApiService();
  late Future<Map<String, dynamic>> _gradesFuture;

  @override
  void initState() {
    super.initState();
    _gradesFuture = _apiService.getStudentGrades();
  }

  Color _getGradeColor(int? grade) {
    if (grade == null) return Colors.grey;
    if (grade >= 18) return const Color(0xFF43A047);
    if (grade >= 14) return const Color(0xFF1976D2);
    if (grade >= 10) return const Color(0xFFFF9800);
    return const Color(0xFFE53935);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              Colors.amber.shade50,
              Colors.orange.shade50.withOpacity(0.3),
              Colors.grey.shade50,
            ],
            stops: const [0.0, 0.5, 1.0],
          ),
        ),
        child: FutureBuilder<Map<String, dynamic>>(
          future: _gradesFuture,
          builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) {
              return const Center(
                child: CircularProgressIndicator(),
              );
            }

            if (snapshot.hasError) {
              return Center(
                child: Padding(
                  padding: const EdgeInsets.all(32.0),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Container(
                        padding: const EdgeInsets.all(24),
                        decoration: BoxDecoration(
                          color: Colors.red.shade50,
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          Icons.error_outline_rounded,
                          size: 64,
                          color: Colors.red.shade700,
                        ),
                      ),
                      const SizedBox(height: 24),
                      const Text(
                        'Error loading grades',
                        style: TextStyle(
                          fontSize: 22,
                          fontWeight: FontWeight.bold,
                          letterSpacing: -0.5,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        snapshot.error.toString(),
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.grey.shade600,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 24),
                      ElevatedButton.icon(
                        onPressed: () {
                          setState(() {
                            _gradesFuture = _apiService.getStudentGrades();
                          });
                        },
                        icon: const Icon(Icons.refresh_rounded),
                        label: const Text('Retry'),
                        style: ElevatedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(16),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }

            final data = snapshot.data ?? {};
            final grades = data['grades'] as List<dynamic>? ?? [];
            final errorCode = data['errorCode'] as String? ?? '';

            if (grades.isEmpty) {
              return Center(
                child: Padding(
                  padding: const EdgeInsets.all(32.0),
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
                          Icons.grade_outlined,
                          size: 64,
                          color: Colors.grey.shade400,
                        ),
                      ),
                      const SizedBox(height: 24),
                      Text(
                        'No grades available',
                        style: TextStyle(
                          fontSize: 22,
                          fontWeight: FontWeight.bold,
                          color: Colors.grey.shade700,
                          letterSpacing: -0.5,
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }

            return RefreshIndicator(
              onRefresh: () async {
                setState(() {
                  _gradesFuture = _apiService.getStudentGrades();
                });
                await _gradesFuture;
              },
              child: CustomScrollView(
                slivers: [
                  SliverToBoxAdapter(
                    child: Container(
                      margin: const EdgeInsets.all(20),
                      padding: const EdgeInsets.all(24),
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(24),
                        gradient: LinearGradient(
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                          colors: [
                            Colors.amber.shade700,
                            Colors.orange.shade700,
                          ],
                        ),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.amber.withOpacity(0.4),
                            blurRadius: 20,
                            offset: const Offset(0, 10),
                          ),
                        ],
                      ),
                      child: Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: Colors.white.withOpacity(0.25),
                              borderRadius: BorderRadius.circular(16),
                              border: Border.all(
                                color: Colors.white.withOpacity(0.3),
                                width: 1.5,
                              ),
                            ),
                            child: const Icon(
                              Icons.grade_rounded,
                              size: 32,
                              color: Colors.white,
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text(
                                  'My Grades',
                                  style: TextStyle(
                                    fontSize: 24,
                                    fontWeight: FontWeight.bold,
                                    color: Colors.white,
                                    letterSpacing: -0.5,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  '${grades.length} course${grades.length != 1 ? 's' : ''}',
                                  style: TextStyle(
                                    fontSize: 14,
                                    color: Colors.white.withOpacity(0.9),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  SliverPadding(
                    padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
                    sliver: SliverList(
                      delegate: SliverChildBuilderDelegate(
                        (context, index) {
                          final grade = grades[index] as Map<String, dynamic>;
                          final courseName = grade['courseName'] as String? ?? 'Unknown Course';
                          final courseCode = grade['courseCode'] as String? ?? '';
                          final gradeValue = grade['grade'] as int?;
                          final credits = grade['credits'] as int? ?? 0;
                          final semester = grade['semester'] as int? ?? 0;
                          final academicYear = grade['academicYear'] as String? ?? '';

                          final gradeColor = _getGradeColor(gradeValue);

                          return Container(
                            margin: const EdgeInsets.only(bottom: 16),
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(20),
                              boxShadow: [
                                BoxShadow(
                                  color: gradeColor.withOpacity(0.15),
                                  blurRadius: 12,
                                  offset: const Offset(0, 4),
                                ),
                                BoxShadow(
                                  color: Colors.black.withOpacity(0.05),
                                  blurRadius: 4,
                                  offset: const Offset(0, 2),
                                ),
                              ],
                            ),
                            child: Padding(
                              padding: const EdgeInsets.all(20.0),
                              child: Row(
                                children: [
                                  Container(
                                    width: 70,
                                    height: 70,
                                    decoration: BoxDecoration(
                                      gradient: LinearGradient(
                                        begin: Alignment.topLeft,
                                        end: Alignment.bottomRight,
                                        colors: [
                                          gradeColor,
                                          gradeColor.withOpacity(0.8),
                                        ],
                                      ),
                                      shape: BoxShape.circle,
                                      boxShadow: [
                                        BoxShadow(
                                          color: gradeColor.withOpacity(0.3),
                                          blurRadius: 12,
                                          offset: const Offset(0, 4),
                                        ),
                                      ],
                                    ),
                                    child: Center(
                                      child: Text(
                                        gradeValue?.toString() ?? 'N/A',
                                        style: const TextStyle(
                                          fontSize: 24,
                                          fontWeight: FontWeight.bold,
                                          color: Colors.white,
                                        ),
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 20),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          courseName,
                                          style: const TextStyle(
                                            fontSize: 17,
                                            fontWeight: FontWeight.bold,
                                            letterSpacing: -0.3,
                                          ),
                                          maxLines: 2,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                        const SizedBox(height: 6),
                                        if (courseCode.isNotEmpty)
                                          Container(
                                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                            decoration: BoxDecoration(
                                              color: Colors.grey.shade100,
                                              borderRadius: BorderRadius.circular(8),
                                            ),
                                            child: Text(
                                              courseCode,
                                              style: TextStyle(
                                                fontSize: 11,
                                                color: Colors.grey.shade700,
                                                fontWeight: FontWeight.w600,
                                                letterSpacing: 0.5,
                                              ),
                                            ),
                                          ),
                                        const SizedBox(height: 8),
                                        Wrap(
                                          spacing: 8,
                                          runSpacing: 6,
                                          children: [
                                            if (academicYear.isNotEmpty)
                                              Container(
                                                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                                decoration: BoxDecoration(
                                                  color: Colors.blue.shade50,
                                                  borderRadius: BorderRadius.circular(8),
                                                ),
                                                child: Text(
                                                  academicYear,
                                                  style: TextStyle(
                                                    fontSize: 11,
                                                    color: Colors.blue.shade700,
                                                    fontWeight: FontWeight.w600,
                                                  ),
                                                ),
                                              ),
                                            if (semester > 0)
                                              Container(
                                                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                                decoration: BoxDecoration(
                                                  color: Colors.purple.shade50,
                                                  borderRadius: BorderRadius.circular(8),
                                                ),
                                                child: Text(
                                                  'Sem $semester',
                                                  style: TextStyle(
                                                    fontSize: 11,
                                                    color: Colors.purple.shade700,
                                                    fontWeight: FontWeight.w600,
                                                  ),
                                                ),
                                              ),
                                            if (credits > 0)
                                              Container(
                                                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                                decoration: BoxDecoration(
                                                  color: Colors.green.shade50,
                                                  borderRadius: BorderRadius.circular(8),
                                                ),
                                                child: Text(
                                                  '$credits ECTS',
                                                  style: TextStyle(
                                                    fontSize: 11,
                                                    color: Colors.green.shade700,
                                                    fontWeight: FontWeight.w600,
                                                  ),
                                                ),
                                              ),
                                          ],
                                        ),
                                      ],
                                    ),
                                  ),
                                  Icon(
                                    Icons.chevron_right_rounded,
                                    color: Colors.grey.shade400,
                                    size: 28,
                                  ),
                                ],
                              ),
                            ),
                          );
                        },
                        childCount: grades.length,
                      ),
                    ),
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }
}
