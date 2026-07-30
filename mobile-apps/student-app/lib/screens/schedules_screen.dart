import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../services/api_service.dart';

class SchedulesScreen extends StatefulWidget {
  const SchedulesScreen({super.key});

  @override
  State<SchedulesScreen> createState() => _SchedulesScreenState();
}

class _SchedulesScreenState extends State<SchedulesScreen> {
  final ApiService _apiService = ApiService();
  late Future<List<dynamic>> _scheduleFuture;

  @override
  void initState() {
    super.initState();
    _scheduleFuture = _apiService.getStudentSchedule();
  }

  Map<String, List<dynamic>> _groupSchedulesByDay(List<dynamic> scheduleItems) {
    final Map<String, List<dynamic>> grouped = {};
    
    for (var item in scheduleItems) {
      final day = item['day'] as String? ?? 'Unknown';
      if (!grouped.containsKey(day)) {
        grouped[day] = [];
      }
      grouped[day]!.add(item);
    }
    
    // Sort days in order
    final dayOrder = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
    final sortedGrouped = <String, List<dynamic>>{};
    for (var day in dayOrder) {
      if (grouped.containsKey(day)) {
        sortedGrouped[day] = grouped[day]!;
      }
    }
    // Add any remaining days not in the standard order
    for (var entry in grouped.entries) {
      if (!sortedGrouped.containsKey(entry.key)) {
        sortedGrouped[entry.key] = entry.value;
      }
    }
    
    return sortedGrouped;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: FutureBuilder<List<dynamic>>(
        future: _scheduleFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(
              child: CircularProgressIndicator(),
            );
          }

          if (snapshot.hasError) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(
                    Icons.error_outline,
                    size: 64,
                    color: Colors.red,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Error loading schedule',
                    style: TextStyle(
                      fontSize: 18,
                      color: Colors.grey.shade700,
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
                  const SizedBox(height: 16),
                  ElevatedButton.icon(
                    onPressed: () {
                      setState(() {
                        _scheduleFuture = _apiService.getStudentSchedule();
                      });
                    },
                    icon: const Icon(Icons.refresh),
                    label: const Text('Retry'),
                  ),
                ],
              ),
            );
          }

          final scheduleItems = snapshot.data ?? [];
          
          if (scheduleItems.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(
                    Icons.calendar_today_outlined,
                    size: 64,
                    color: Colors.grey,
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'No schedule available',
                    style: TextStyle(
                      fontSize: 18,
                      color: Colors.grey.shade700,
                    ),
                  ),
                ],
              ),
            );
          }

          final groupedSchedules = _groupSchedulesByDay(scheduleItems);

          return RefreshIndicator(
            onRefresh: () async {
              setState(() {
                _scheduleFuture = _apiService.getStudentSchedule();
              });
              await _scheduleFuture;
            },
            child: ListView.builder(
              padding: const EdgeInsets.all(16.0),
              itemCount: groupedSchedules.length,
              itemBuilder: (context, index) {
                final day = groupedSchedules.keys.elementAt(index);
                final courses = groupedSchedules[day]!;
                
                return Card(
                  margin: const EdgeInsets.only(bottom: 16),
                  elevation: 2,
                  child: ExpansionTile(
                    leading: CircleAvatar(
                      backgroundColor: Colors.blue.shade700,
                      child: Text(
                        day.substring(0, 1),
                        style: const TextStyle(color: Colors.white),
                      ),
                    ),
                    title: Text(
                      day,
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    subtitle: Text(
                      '${courses.length} course(s)',
                      style: TextStyle(color: Colors.grey.shade600),
                    ),
                    children: courses.map<Widget>((course) {
                      final courseName = course['course'] as String? ?? 'Unknown Course';
                      final time = course['time'] as String? ?? 'TBA';
                      final room = course['room'] as String? ?? 'TBA';
                      final professor = course['professor'] as String? ?? '';
                      final type = course['type'] as String? ?? '';
                      
                      return ListTile(
                        leading: const Icon(Icons.book, color: Colors.blue),
                        title: Text(courseName),
                        subtitle: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('$time - Room: $room'),
                            if (professor.isNotEmpty)
                              Text(
                                'Professor: $professor',
                                style: TextStyle(
                                  fontSize: 12,
                                  color: Colors.grey.shade600,
                                ),
                              ),
                            if (type.isNotEmpty)
                              Text(
                                'Type: $type',
                                style: TextStyle(
                                  fontSize: 12,
                                  color: Colors.grey.shade600,
                                ),
                              ),
                          ],
                        ),
                        trailing: const Icon(Icons.chevron_right),
                      );
                    }).toList(),
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }
}

