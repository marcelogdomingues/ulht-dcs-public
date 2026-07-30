import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/session_provider.dart';
import '../models/session.dart';
import 'session_detail_screen.dart';
import 'create_session_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  @override
  void initState() {
    super.initState();
    // Load sessions when screen is initialized
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<SessionProvider>().loadSessions();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      body: SafeArea(
        child: Consumer<SessionProvider>(
          builder: (context, provider, child) {
            if (provider.isLoading && provider.sessions.isEmpty) {
              return const Center(
                child: CircularProgressIndicator(),
              );
            }

            if (provider.errorMessage != null && provider.sessions.isEmpty) {
              return Padding(
                padding: const EdgeInsets.all(24.0),
                child: Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Container(
                        padding: const EdgeInsets.all(20),
                        decoration: BoxDecoration(
                          color: Colors.red.shade50,
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          Icons.error_outline_rounded,
                          size: 48,
                          color: Colors.red.shade400,
                        ),
                      ),
                      const SizedBox(height: 24),
                      Text(
                        'Error loading sessions',
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.bold,
                              color: Colors.grey.shade800,
                            ),
                      ),
                      const SizedBox(height: 12),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 32.0),
                        child: Text(
                          provider.errorMessage ?? 'Unknown error',
                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                color: Colors.grey.shade600,
                              ),
                          textAlign: TextAlign.center,
                        ),
                      ),
                      const SizedBox(height: 32),
                      ElevatedButton.icon(
                        onPressed: () {
                          provider.clearError();
                          provider.loadSessions();
                        },
                        icon: const Icon(Icons.refresh_rounded),
                        label: const Text('Retry'),
                        style: ElevatedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 32,
                            vertical: 16,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }

            if (provider.sessions.isEmpty) {
              return Padding(
                padding: const EdgeInsets.all(24.0),
                child: Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Container(
                        padding: const EdgeInsets.all(24),
                        decoration: BoxDecoration(
                          color: Colors.orange.shade50,
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          Icons.event_available_rounded,
                          size: 64,
                          color: Colors.orange.shade400,
                        ),
                      ),
                      const SizedBox(height: 32),
                      Text(
                        'No sessions yet',
                        style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                              fontWeight: FontWeight.bold,
                              color: Colors.grey.shade800,
                            ),
                      ),
                      const SizedBox(height: 12),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 32.0),
                        child: Text(
                          'Create your first conference session to get started',
                          style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                                color: Colors.grey.shade600,
                              ),
                          textAlign: TextAlign.center,
                        ),
                      ),
                      const SizedBox(height: 40),
                      ElevatedButton.icon(
                        onPressed: () => _navigateToCreateSession(context),
                        icon: const Icon(Icons.add_rounded),
                        label: const Text('Create First Session'),
                        style: ElevatedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 32,
                            vertical: 16,
                          ),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }

            return CustomScrollView(
              slivers: [
                // Header
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(20, 20, 20, 12),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Conference Sessions',
                          style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                                fontWeight: FontWeight.bold,
                                color: Colors.grey.shade900,
                                letterSpacing: -0.5,
                              ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '${provider.sessions.length} ${provider.sessions.length == 1 ? 'session' : 'sessions'}',
                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                color: Colors.grey.shade600,
                              ),
                        ),
                      ],
                    ),
                  ),
                ),
                // Sessions List
                SliverPadding(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  sliver: SliverList(
                    delegate: SliverChildBuilderDelegate(
                      (context, index) {
                        final session = provider.sessions[index];
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 16),
                          child: _buildSessionCard(context, session, provider),
                        );
                      },
                      childCount: provider.sessions.length,
                    ),
                  ),
                ),
                // Bottom padding
                const SliverToBoxAdapter(
                  child: SizedBox(height: 100),
                ),
              ],
            );
          },
        ),
      ),
      floatingActionButton: Container(
        margin: const EdgeInsets.only(bottom: 16, right: 4),
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
        child: FloatingActionButton.extended(
          onPressed: () => _navigateToCreateSession(context),
          backgroundColor: Colors.orange.shade700,
          foregroundColor: Colors.white,
          icon: const Icon(Icons.add_rounded),
          label: const Text(
            'New Session',
            style: TextStyle(
              fontWeight: FontWeight.w600,
              fontSize: 16,
            ),
          ),
          elevation: 0,
        ),
      ),
    );
  }

  Widget _buildSessionCard(BuildContext context, Session session, SessionProvider provider) {
    final dateFormat = DateFormat('MMM dd, yyyy');
    final timeFormat = DateFormat('HH:mm');

    return Container(
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
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: () => _navigateToSessionDetail(context, session),
          borderRadius: BorderRadius.circular(20),
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header Row
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            session.title,
                            style: Theme.of(context).textTheme.titleLarge?.copyWith(
                                  fontWeight: FontWeight.bold,
                                  color: Colors.grey.shade900,
                                  letterSpacing: -0.3,
                                ),
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                          const SizedBox(height: 6),
                          Row(
                            children: [
                              Icon(
                                Icons.event_rounded,
                                size: 14,
                                color: Colors.orange.shade600,
                              ),
                              const SizedBox(width: 4),
                              Flexible(
                                child: Text(
                                  session.conferenceName,
                                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                        color: Colors.grey.shade600,
                                        fontWeight: FontWeight.w500,
                                      ),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 12),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                      decoration: BoxDecoration(
                        color: session.isActive
                            ? Colors.green.shade50
                            : Colors.grey.shade100,
                        borderRadius: BorderRadius.circular(20),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Container(
                            width: 8,
                            height: 8,
                            decoration: BoxDecoration(
                              color: session.isActive
                                  ? Colors.green.shade600
                                  : Colors.grey.shade400,
                              shape: BoxShape.circle,
                            ),
                          ),
                          const SizedBox(width: 6),
                          Text(
                            session.isActive ? 'Active' : 'Inactive',
                            style: TextStyle(
                              color: session.isActive
                                  ? Colors.green.shade700
                                  : Colors.grey.shade700,
                              fontWeight: FontWeight.w600,
                              fontSize: 12,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                // Description
                if (session.description != null && session.description!.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  Text(
                    session.description!,
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: Colors.grey.shade700,
                          height: 1.4,
                        ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
                // Info Row
                if (session.startTime != null || session.location != null) ...[
                  const SizedBox(height: 16),
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.grey.shade50,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Column(
                      children: [
                        if (session.startTime != null)
                          Row(
                            children: [
                              Icon(
                                Icons.access_time_rounded,
                                size: 18,
                                color: Colors.grey.shade700,
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: Text(
                                  '${dateFormat.format(session.startTime!)} at ${timeFormat.format(session.startTime!)}',
                                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                        color: Colors.grey.shade800,
                                        fontWeight: FontWeight.w500,
                                      ),
                                ),
                              ),
                            ],
                          ),
                        if (session.startTime != null && session.location != null)
                          const SizedBox(height: 8),
                        if (session.location != null)
                          Row(
                            children: [
                              Icon(
                                Icons.location_on_rounded,
                                size: 18,
                                color: Colors.grey.shade700,
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: Text(
                                  session.location!,
                                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                        color: Colors.grey.shade800,
                                        fontWeight: FontWeight.w500,
                                      ),
                                ),
                              ),
                            ],
                          ),
                      ],
                    ),
                  ),
                ],
                // Footer
                const SizedBox(height: 16),
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                      decoration: BoxDecoration(
                        color: Colors.blue.shade50,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            Icons.people_rounded,
                            size: 16,
                            color: Colors.blue.shade700,
                          ),
                          const SizedBox(width: 6),
                          Text(
                            '${session.registeredCount} registered',
                            style: TextStyle(
                              color: Colors.blue.shade700,
                              fontWeight: FontWeight.w600,
                              fontSize: 13,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const Spacer(),
                    Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: Colors.orange.shade50,
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Icon(
                        Icons.arrow_forward_rounded,
                        size: 18,
                        color: Colors.orange.shade700,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  void _navigateToCreateSession(BuildContext context) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => const CreateSessionScreen(),
      ),
    ).then((created) {
      if (created == true) {
        context.read<SessionProvider>().loadSessions();
      }
    });
  }

  void _navigateToSessionDetail(BuildContext context, Session session) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => SessionDetailScreen(session: session),
      ),
    );
  }
}
