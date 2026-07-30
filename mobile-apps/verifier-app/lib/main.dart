import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'screens/verification_screen.dart';
import 'screens/history_screen.dart';
import 'screens/profile_screen.dart';
import 'screens/settings_screen.dart';
import 'screens/statistics_screen.dart';
import 'services/api_service.dart';
import 'services/notification_service.dart';
import 'providers/verification_provider.dart';
import 'providers/profile_provider.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // Initialize notification service (gracefully handles missing platform support)
  try {
    await NotificationService().initialize();
  } catch (e) {
    // Notifications may not be available on all platforms (e.g., web, desktop)
    // The app will continue to work without notifications
    debugPrint('Notification initialization failed: $e');
  }
  runApp(const VerifierApp());
}

class VerifierApp extends StatelessWidget {
  const VerifierApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => ProfileProvider()),
        ChangeNotifierProvider(create: (_) => VerificationProvider()),
        Provider(create: (_) => ApiService()),
      ],
      child: MaterialApp(
        title: 'ULHT Verifier App',
        theme: ThemeData(
          primarySwatch: Colors.green,
          useMaterial3: true,
          colorScheme: ColorScheme.fromSeed(
            seedColor: Colors.green,
            brightness: Brightness.light,
          ),
          cardTheme: CardThemeData(
            elevation: 0,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
          inputDecorationTheme: InputDecorationTheme(
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
            ),
            filled: true,
          ),
        ),
        darkTheme: ThemeData(
          primarySwatch: Colors.green,
          useMaterial3: true,
          colorScheme: ColorScheme.fromSeed(
            seedColor: Colors.green,
            brightness: Brightness.dark,
          ),
          cardTheme: CardThemeData(
            elevation: 0,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
          inputDecorationTheme: InputDecorationTheme(
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
            ),
            filled: true,
          ),
        ),
        themeMode: ThemeMode.system, // Follow system theme
        home: const MainScreen(),
        debugShowCheckedModeBanner: false,
      ),
    );
  }
}

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _currentIndex = 0;

  final List<Widget> _screens = [
    const VerificationScreen(),
    const HistoryScreen(),
    const StatisticsScreen(),
    const ProfileScreen(),
  ];

  final List<String> _titles = [
    'Verify Credentials',
    'Verification History',
    'Statistics',
    'Profiles',
  ];

  @override
  Widget build(BuildContext context) {
    return Consumer<ProfileProvider>(
      builder: (context, profileProvider, child) {
        final activeProfile = profileProvider.activeProfile;
        final appBarColors = activeProfile != null
            ? [_darkenColor(activeProfile.color, 0.2), _darkenColor(activeProfile.color, 0.1)]
            : [Colors.green.shade700, Colors.green.shade600];
        
        return Scaffold(
          appBar: AppBar(
            title: Text(
              _titles[_currentIndex],
              style: const TextStyle(
                fontWeight: FontWeight.bold,
                letterSpacing: -0.5,
              ),
            ),
            backgroundColor: appBarColors[0],
            foregroundColor: Colors.white,
            elevation: 0,
            centerTitle: false,
            flexibleSpace: Container(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: appBarColors,
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
              ),
            ),
            actions: [
              // Profile button (always visible)
              IconButton(
                icon: Stack(
                  children: [
                    Icon(
                      activeProfile?.icon ?? Icons.tune_rounded,
                      size: 24,
                    ),
                    if (activeProfile != null)
                      Positioned(
                        right: 0,
                        top: 0,
                        child: Container(
                          width: 8,
                          height: 8,
                          decoration: BoxDecoration(
                            color: activeProfile.color,
                            shape: BoxShape.circle,
                            border: Border.all(
                              color: Colors.white,
                              width: 1.5,
                            ),
                          ),
                        ),
                      ),
                  ],
                ),
                tooltip: 'Verification Profile',
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const ProfileScreen(),
                    ),
                  );
                },
              ),
          // Settings button
          IconButton(
            icon: const Icon(Icons.settings_rounded),
            tooltip: 'Settings',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => const SettingsScreen(),
                ),
              );
            },
          ),
          // Clear history button (only on history screen)
          if (_currentIndex == 1)
                Consumer<VerificationProvider>(
                  builder: (context, provider, child) {
                    if (provider.history.isNotEmpty) {
                      return IconButton(
                        icon: const Icon(Icons.delete_outline_rounded),
                        tooltip: 'Clear History',
                        onPressed: () {
                          showDialog(
                            context: context,
                            builder: (context) => AlertDialog(
                              title: const Text('Clear History'),
                              content: const Text(
                                  'Are you sure you want to clear all verification history?'),
                              actions: [
                                TextButton(
                                  onPressed: () => Navigator.pop(context),
                                  child: const Text('Cancel'),
                                ),
                                TextButton(
                                  onPressed: () {
                                    provider.clearHistory();
                                    Navigator.pop(context);
                                  },
                                  child: const Text('Clear'),
                                ),
                              ],
                            ),
                          );
                        },
                      );
                    }
                    return const SizedBox.shrink();
                  },
                ),
        ],
      ),
            body: _screens[_currentIndex],
            bottomNavigationBar: Container(
              decoration: BoxDecoration(
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.05),
                    blurRadius: 10,
                    offset: const Offset(0, -5),
                  ),
                ],
              ),
              child: BottomNavigationBar(
                currentIndex: _currentIndex,
                onTap: (index) => setState(() => _currentIndex = index),
                type: BottomNavigationBarType.fixed,
                backgroundColor: Colors.white,
                selectedItemColor: activeProfile?.color ?? Colors.green.shade700,
                unselectedItemColor: Colors.grey.shade600,
          selectedLabelStyle: const TextStyle(
            fontWeight: FontWeight.w600,
            fontSize: 12,
          ),
          unselectedLabelStyle: const TextStyle(
            fontWeight: FontWeight.w500,
            fontSize: 12,
          ),
              items: [
                BottomNavigationBarItem(
                  icon: Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(12),
                      color: _currentIndex == 0
                          ? (activeProfile?.color.withOpacity(0.1) ?? Colors.green.shade50)
                          : Colors.transparent,
                    ),
                    child: Icon(
                      _currentIndex == 0
                          ? Icons.qr_code_scanner_rounded
                          : Icons.qr_code_scanner_outlined,
                      size: 24,
                    ),
                  ),
                  label: 'Verify',
                ),
                BottomNavigationBarItem(
                  icon: Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(12),
                      color: _currentIndex == 1
                          ? (activeProfile?.color.withOpacity(0.1) ?? Colors.green.shade50)
                          : Colors.transparent,
                    ),
                    child: Icon(
                      _currentIndex == 1
                          ? Icons.history_rounded
                          : Icons.history_outlined,
                      size: 24,
                    ),
                  ),
                  label: 'History',
                ),
                BottomNavigationBarItem(
                  icon: Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(12),
                      color: _currentIndex == 2
                          ? (activeProfile?.color.withOpacity(0.1) ?? Colors.green.shade50)
                          : Colors.transparent,
                    ),
                    child: Icon(
                      _currentIndex == 2
                          ? Icons.analytics_rounded
                          : Icons.analytics_outlined,
                      size: 24,
                    ),
                  ),
                  label: 'Stats',
                ),
                BottomNavigationBarItem(
                  icon: Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(12),
                      color: _currentIndex == 3
                          ? (activeProfile?.color.withOpacity(0.1) ?? Colors.green.shade50)
                          : Colors.transparent,
                    ),
                    child: Icon(
                      _currentIndex == 3
                          ? Icons.tune_rounded
                          : Icons.tune_outlined,
                      size: 24,
                    ),
                  ),
                  label: 'Profiles',
                ),
              ],
            ),
          ),
        );
      },
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
