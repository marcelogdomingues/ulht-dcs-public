import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:provider/provider.dart';
import '../services/api_service.dart';
import '../providers/credential_provider.dart';
import '../utils/url_guard.dart';
import 'presentation_request_screen.dart';

class QRScannerScreen extends StatefulWidget {
  const QRScannerScreen({super.key});

  @override
  State<QRScannerScreen> createState() => _QRScannerScreenState();
}

class _QRScannerScreenState extends State<QRScannerScreen> {
  late final MobileScannerController _controller;
  bool _isProcessing = false;
  bool _isInitialized = false;
  String? _lastProcessedCode; // Track last processed QR code to prevent duplicates

  @override
  void initState() {
    super.initState();
    // For web, use front camera by default (better for desktop/laptop users)
    // For mobile, use back camera
    _controller = MobileScannerController(
      detectionSpeed: DetectionSpeed.noDuplicates,
      facing: kIsWeb ? CameraFacing.front : CameraFacing.back,
    );
    _initializeScanner();
  }

  Future<void> _initializeScanner() async {
    try {
      await _controller.start();
      if (mounted) {
        setState(() {
          _isInitialized = true;
        });
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Camera not available. Please check permissions or try again later.\nError: ${e.toString()}'),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 5),
            action: SnackBarAction(
              label: 'Close',
              textColor: Colors.white,
              onPressed: () {
                Navigator.pop(context);
              },
            ),
          ),
        );
        // Close the screen if camera fails
        Future.delayed(const Duration(seconds: 2), () {
          if (mounted) {
            Navigator.pop(context);
          }
        });
      }
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _handleQRCode(String code) async {
    if (_isProcessing) return;

    // Trim whitespace
    final trimmedCode = code.trim();
    
    // Prevent processing the same code multiple times
    if (_lastProcessedCode == trimmedCode) {
      return;
    }

    // Stop scanner immediately to prevent multiple detections
    await _controller.stop();

    setState(() {
      _isProcessing = true;
      _lastProcessedCode = trimmedCode;
    });

    try {
      
      // Check if it's an openid4vp:// URL (verification request)
      if (trimmedCode.startsWith('openid4vp://') || trimmedCode.startsWith('openid4vci://')) {
        final uri = Uri.parse(trimmedCode);
        
        if (kIsWeb) {
          // On web, use wallet API to handle presentation request
          if (mounted) {
            await _handleWebPresentationRequest(trimmedCode);
          }
        } else {
          // On mobile, navigate to presentation request screen for selective disclosure
          if (mounted) {
            final result = await Navigator.push(
              context,
              MaterialPageRoute(
                builder: (context) => PresentationRequestScreen(
                  presentationRequestUrl: trimmedCode,
                ),
              ),
            );
            
            // Close scanner after handling presentation request
            if (mounted) {
              Navigator.pop(context, result);
            }
          }
        }
      } else if (trimmedCode.startsWith('http://') || trimmedCode.startsWith('https://')) {
        // Handle HTTP/HTTPS URLs - might contain verification requests or session registration
        try {
          final uri = Uri.parse(trimmedCode);
          
          // Check if it's a session registration URL
          if (uri.path.contains('/issuer/sessions/') && (uri.path.contains('/register') || uri.pathSegments.contains('sessions'))) {
            // Handle session registration
            if (mounted) {
              await _handleSessionRegistration(trimmedCode);
            }
            // Don't reset processing flag - let the handler manage it
            return;
          }
          
          // Check if it contains verification-related parameters
          if (uri.queryParameters.containsKey('response_type') || 
              uri.queryParameters.containsKey('client_id') ||
              uri.scheme == 'https' && uri.host.contains('verifier')) {
            // Try to handle as verification request
            if (kIsWeb) {
              if (mounted) {
                await _handleWebPresentationRequest(trimmedCode);
              }
            } else {
              if (!UrlGuard.isAllowed(uri)) {
                if (mounted) _showBlockedUrlMessage(uri);
              } else if (await canLaunchUrl(uri)) {
                await launchUrl(uri, mode: LaunchMode.externalApplication);
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Opening verification URL...'),
                      backgroundColor: Colors.green,
                      duration: Duration(seconds: 2),
                    ),
                  );
                  Future.delayed(const Duration(seconds: 1), () {
                    if (mounted) Navigator.pop(context);
                  });
                }
              }
            }
          } else {
            // Regular HTTP/HTTPS URL - show info message
            if (mounted) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text('This appears to be a regular web link.\nExpected: openid4vp:// or openid4vci:// URL'),
                  backgroundColor: Colors.orange,
                  duration: const Duration(seconds: 4),
                  action: SnackBarAction(
                    label: 'Open Link',
                    textColor: Colors.white,
                    onPressed: () async {
                      if (!UrlGuard.isAllowed(uri)) {
                        if (mounted) _showBlockedUrlMessage(uri);
                        return;
                      }
                      if (await canLaunchUrl(uri)) {
                        await launchUrl(uri, mode: LaunchMode.externalApplication);
                      }
                    },
                  ),
                ),
              );
            }
          }
        } catch (e) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text('Invalid URL format: ${e.toString()}'),
                backgroundColor: Colors.red,
                duration: const Duration(seconds: 3),
              ),
            );
          }
        }
      } else {
        // Not a recognized format - provide helpful error message
        if (mounted) {
          final isNumeric = RegExp(r'^\d+$').hasMatch(trimmedCode);
          final message = isNumeric
              ? 'This appears to be a numeric code (barcode), not a verification QR code.\n\nPlease scan a QR code from the Verifier app that starts with:\n• openid4vp://\n• openid4vci://'
              : 'Unrecognized QR code format.\n\nExpected formats:\n• openid4vp://... (verification request)\n• openid4vci://... (credential offer)\n\nScanned: ${trimmedCode.length > 50 ? "${trimmedCode.substring(0, 50)}..." : trimmedCode}';
          
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(message),
              backgroundColor: Colors.orange,
              duration: const Duration(seconds: 5),
              action: SnackBarAction(
                label: 'OK',
                textColor: Colors.white,
                onPressed: () {},
              ),
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error processing QR code: ${e.toString()}'),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 4),
          ),
        );
        // Reset processing flag on error so user can try again
        setState(() {
          _isProcessing = false;
          _lastProcessedCode = null; // Reset so they can retry
        });
        // Restart scanner on error
        try {
          await _controller.start();
        } catch (_) {
          // Ignore restart errors
        }
      }
    } finally {
      // Only reset if we're not closing the scanner
      // (session registration will close the scanner itself)
      if (mounted && _isProcessing && _lastProcessedCode != null) {
        // Check if we processed a session registration - if so, don't reset
        final lastCode = _lastProcessedCode;
        if (lastCode != null && !lastCode.contains('/issuer/sessions/')) {
          setState(() {
            _isProcessing = false;
          });
          // Restart scanner for other types of QR codes
          try {
            await _controller.start();
          } catch (_) {
            // Ignore restart errors
          }
        }
      }
    }
  }

  Future<void> _handleSessionRegistration(String sessionUrl) async {
    // Ensure scanner is stopped and won't process more codes
    try {
      await _controller.stop();
    } catch (_) {
      // Ignore errors stopping scanner
    }
    
    try {
      final apiService = ApiService();
      
      // Show loading dialog
      showDialog(
        context: context,
        barrierDismissible: false,
        builder: (context) => const Center(
          child: Card(
            child: Padding(
              padding: EdgeInsets.all(20),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  CircularProgressIndicator(),
                  SizedBox(height: 16),
                  Text('Registering for session...'),
                ],
              ),
            ),
          ),
        ),
      );
      
      // Get session info first
      Map<String, dynamic>? sessionInfo;
      try {
        sessionInfo = await apiService.getSessionInfo(sessionUrl);
      } catch (e) {
        // Session info not critical, continue with registration
      }
      
      // Register for session
      final result = await apiService.registerForSession(sessionUrl);
      
      if (mounted) {
        // Close loading dialog
        Navigator.pop(context);
        
        // Show success snackbar
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                const Icon(Icons.check_circle, color: Colors.white),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    'Successfully registered! Session credential added to wallet.',
                    style: const TextStyle(fontSize: 14),
                  ),
                ),
              ],
            ),
            backgroundColor: Colors.green.shade600,
            duration: const Duration(seconds: 2),
          ),
        );
        
        // Refresh wallet credentials
        final credentialProvider = Provider.of<CredentialProvider>(context, listen: false);
        await credentialProvider.loadWalletCredentials();
        
        // Close scanner immediately and return success
        if (mounted) {
          Navigator.pop(context, {'success': true, 'goToWallet': true});
        }
      }
    } catch (e) {
      if (mounted) {
        // Close loading dialog if still open
        Navigator.pop(context);
        
        // Reset processing state on error
        setState(() {
          _isProcessing = false;
          _lastProcessedCode = null;
        });
        
        // Restart scanner on error so user can try again
        try {
          await _controller.start();
        } catch (_) {
          // Ignore restart errors
        }
        
        final errorMessage = e.toString();
        final isConnectionError = errorMessage.contains('Connection') || 
                                  errorMessage.contains('timeout') ||
                                  errorMessage.contains('unavailable');
        
        showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: Row(
              children: [
                Icon(
                  isConnectionError ? Icons.info_outline : Icons.error_outline,
                  color: isConnectionError ? Colors.orange.shade600 : Colors.red.shade600,
                ),
                const SizedBox(width: 8),
                Text(isConnectionError ? 'Backend Not Available' : 'Registration Failed'),
              ],
            ),
            content: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (isConnectionError) ...[
                    const Text(
                      'The session registration feature requires backend API endpoints to be implemented.',
                      style: TextStyle(fontWeight: FontWeight.w500),
                    ),
                    const SizedBox(height: 12),
                    const Text(
                      'Currently, the backend endpoints for session registration are not available. '
                      'This is expected during development.',
                    ),
                    const SizedBox(height: 12),
                    const Text(
                      'To enable this feature:',
                      style: TextStyle(fontWeight: FontWeight.w600),
                    ),
                    const SizedBox(height: 8),
                    const Text('1. Implement issuer endpoints in credential service'),
                    const Text('2. Ensure credential service is running on port 8086'),
                    const Text('3. See IMPLEMENTATION_GUIDE.md for details'),
                  ] else ...[
                    Text(
                      'Failed to register for session.',
                      style: const TextStyle(fontWeight: FontWeight.w500),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      errorMessage,
                      style: const TextStyle(fontSize: 12),
                    ),
                  ],
                ],
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('OK'),
              ),
              if (isConnectionError)
                TextButton(
                  onPressed: () {
                    Navigator.pop(context);
                    Navigator.pop(context); // Close scanner too
                  },
                  child: const Text('Close Scanner'),
                ),
            ],
          ),
        );
      }
    }
  }

  void _showBlockedUrlMessage(Uri uri) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          'Blocked: "${uri.host.isNotEmpty ? uri.host : uri.scheme}" is not an '
          'allowed destination. For your safety this link was not opened.',
        ),
        backgroundColor: Colors.red,
        duration: const Duration(seconds: 5),
      ),
    );
  }

  Future<void> _handleWebPresentationRequest(String url) async {
    try {
      // Navigate to presentation request screen for selective disclosure
      if (mounted) {
        final result = await Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => PresentationRequestScreen(
              presentationRequestUrl: url,
            ),
          ),
        );
        
        // Close scanner after handling presentation request
        if (mounted) {
          Navigator.pop(context, result);
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to process verification request: ${e.toString()}'),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 5),
          ),
        );
      }
    }
  }

  Widget _buildScannerOverlay(BuildContext context) {
    final size = MediaQuery.of(context).size;
    final scanAreaSize = size.width * 0.75; // 75% of screen width
    final scanAreaTop = (size.height - scanAreaSize) / 2;

    return Stack(
      children: [
        // Dark overlay
        ColorFiltered(
          colorFilter: ColorFilter.mode(
            Colors.black.withOpacity(0.5),
            BlendMode.srcOut,
          ),
          child: Stack(
            children: [
              Container(
                decoration: const BoxDecoration(
                  color: Colors.black,
                  backgroundBlendMode: BlendMode.dstOut,
                ),
                child: CustomPaint(
                  painter: _ScannerOverlayPainter(
                    scanAreaSize: scanAreaSize,
                    scanAreaTop: scanAreaTop,
                  ),
                ),
              ),
            ],
          ),
        ),
        // Scanning frame with corner indicators
        Positioned(
          top: scanAreaTop,
          left: (size.width - scanAreaSize) / 2,
          child: Container(
            width: scanAreaSize,
            height: scanAreaSize,
            decoration: BoxDecoration(
              border: Border.all(
                color: Colors.white,
                width: 2,
              ),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Stack(
              children: [
                // Top-left corner
                Positioned(
                  top: 0,
                  left: 0,
                  child: _buildCornerIndicator(true, true),
                ),
                // Top-right corner
                Positioned(
                  top: 0,
                  right: 0,
                  child: _buildCornerIndicator(true, false),
                ),
                // Bottom-left corner
                Positioned(
                  bottom: 0,
                  left: 0,
                  child: _buildCornerIndicator(false, true),
                ),
                // Bottom-right corner
                Positioned(
                  bottom: 0,
                  right: 0,
                  child: _buildCornerIndicator(false, false),
                ),
              ],
            ),
          ),
        ),
        // Scanning line animation
        if (!_isProcessing)
          Positioned(
            top: scanAreaTop,
            left: (size.width - scanAreaSize) / 2,
            child: SizedBox(
              width: scanAreaSize,
              height: scanAreaSize,
              child: Stack(
                children: [
                  TweenAnimationBuilder<double>(
                    tween: Tween(begin: 0.0, end: 1.0),
                    duration: const Duration(seconds: 2),
                    curve: Curves.easeInOut,
                    onEnd: () {
                      if (mounted) {
                        setState(() {});
                      }
                    },
                    builder: (context, value, child) {
                      return Positioned(
                        top: scanAreaSize * value,
                        left: 0,
                        right: 0,
                        child: Container(
                          height: 2,
                          decoration: BoxDecoration(
                            gradient: LinearGradient(
                              colors: [
                                Colors.transparent,
                                Colors.blue.shade400,
                                Colors.transparent,
                              ],
                            ),
                            boxShadow: [
                              BoxShadow(
                                color: Colors.blue.shade400.withOpacity(0.8),
                                blurRadius: 4,
                                spreadRadius: 1,
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }

  Widget _buildCornerIndicator(bool isTop, bool isLeft) {
    final cornerLength = 30.0;
    final cornerWidth = 4.0;

    return Container(
      width: cornerLength,
      height: cornerLength,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.only(
          topLeft: isTop && isLeft ? const Radius.circular(20) : Radius.zero,
          topRight: isTop && !isLeft ? const Radius.circular(20) : Radius.zero,
          bottomLeft: !isTop && isLeft ? const Radius.circular(20) : Radius.zero,
          bottomRight: !isTop && !isLeft ? const Radius.circular(20) : Radius.zero,
        ),
      ),
      child: Stack(
        children: [
          // Horizontal line
          Positioned(
            left: isLeft ? 0 : null,
            right: isLeft ? null : 0,
            top: isTop ? 0 : null,
            bottom: isTop ? null : 0,
            child: Container(
              width: cornerLength,
              height: cornerWidth,
              decoration: BoxDecoration(
                color: Colors.blue.shade400,
                borderRadius: BorderRadius.only(
                  topLeft: isTop && isLeft ? const Radius.circular(2) : Radius.zero,
                  topRight: isTop && !isLeft ? const Radius.circular(2) : Radius.zero,
                  bottomLeft: !isTop && isLeft ? const Radius.circular(2) : Radius.zero,
                  bottomRight: !isTop && !isLeft ? const Radius.circular(2) : Radius.zero,
                ),
                boxShadow: [
                  BoxShadow(
                    color: Colors.blue.shade400.withOpacity(0.8),
                    blurRadius: 4,
                    spreadRadius: 1,
                  ),
                ],
              ),
            ),
          ),
          // Vertical line
          Positioned(
            left: isLeft ? 0 : null,
            right: isLeft ? null : 0,
            top: isTop ? 0 : null,
            bottom: isTop ? null : 0,
            child: Container(
              width: cornerWidth,
              height: cornerLength,
              decoration: BoxDecoration(
                color: Colors.blue.shade400,
                borderRadius: BorderRadius.only(
                  topLeft: isTop && isLeft ? const Radius.circular(2) : Radius.zero,
                  topRight: isTop && !isLeft ? const Radius.circular(2) : Radius.zero,
                  bottomLeft: !isTop && isLeft ? const Radius.circular(2) : Radius.zero,
                  bottomRight: !isTop && !isLeft ? const Radius.circular(2) : Radius.zero,
                ),
                boxShadow: [
                  BoxShadow(
                    color: Colors.blue.shade400.withOpacity(0.8),
                    blurRadius: 4,
                    spreadRadius: 1,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Scan QR Code'),
        backgroundColor: Colors.blue.shade700,
        foregroundColor: Colors.white,
      ),
      body: _isInitialized
          ? Stack(
              children: [
                MobileScanner(
                  controller: _controller,
                  onDetect: (capture) {
                    final List<Barcode> barcodes = capture.barcodes;
                    for (final barcode in barcodes) {
                      if (barcode.rawValue != null) {
                        _handleQRCode(barcode.rawValue!);
                        break; // Process only the first barcode
                      }
                    }
                  },
                ),
                // Dark overlay with transparent scanning area
                _buildScannerOverlay(context),
                // Camera switch button (top right)
                Positioned(
                  top: 16,
                  right: 16,
                  child: FloatingActionButton(
                    mini: true,
                    backgroundColor: Colors.black.withOpacity(0.6),
                    onPressed: () async {
                      await _controller.switchCamera();
                    },
                    child: const Icon(Icons.cameraswitch, color: Colors.white),
                  ),
                ),
                // Overlay with instructions
                Positioned(
                  bottom: 0,
                  left: 0,
                  right: 0,
                  child: Container(
                    padding: const EdgeInsets.all(24),
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [
                          Colors.transparent,
                          Colors.black.withOpacity(0.8),
                        ],
                      ),
                    ),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Text(
                          'Point camera at verification QR code',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                          ),
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 8),
                        Text(
                          kIsWeb 
                              ? 'Allow camera access when prompted. Use the switch button to change cameras.'
                              : 'Scan the QR code from the verifier app to present your credentials',
                          style: TextStyle(
                            color: Colors.white.withOpacity(0.8),
                            fontSize: 14,
                          ),
                          textAlign: TextAlign.center,
                        ),
                        if (_isProcessing) ...[
                          const SizedBox(height: 16),
                          const CircularProgressIndicator(
                            valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
              ],
            )
          : const Center(
              child: CircularProgressIndicator(),
            ),
    );
  }
}

class _ScannerOverlayPainter extends CustomPainter {
  final double scanAreaSize;
  final double scanAreaTop;

  _ScannerOverlayPainter({
    required this.scanAreaSize,
    required this.scanAreaTop,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.black
      ..blendMode = BlendMode.dstOut;

    final scanAreaLeft = (size.width - scanAreaSize) / 2;
    final scanAreaRect = RRect.fromRectAndRadius(
      Rect.fromLTWH(scanAreaLeft, scanAreaTop, scanAreaSize, scanAreaSize),
      const Radius.circular(20),
    );

    canvas.drawRRect(scanAreaRect, paint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

