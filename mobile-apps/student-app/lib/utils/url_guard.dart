/// URL allowlist helper.
///
/// Before launching any scanned / arbitrary URL externally we validate it
/// against an allowlist so a malicious QR code cannot drive the wallet to an
/// attacker-controlled site. The credential-exchange schemes used by
/// OpenID4VP / OpenID4VCI are allowed since they are handled in-app.
///
/// NOTE for production: replace the localhost / gateway hosts below with the
/// real production gateway host(s) over https only, and drop localhost.
class UrlGuard {
  /// Schemes that are handled in-app (openid4vp / openid4vci) and are safe.
  static const Set<String> _allowedSchemes = {
    'openid4vp',
    'openid4vci',
    'haip',
  };

  /// Hosts (and suffixes) that http(s) URLs may point to.
  static const Set<String> _allowedHosts = {
    'localhost',
    '127.0.0.1',
    '10.0.2.2', // Android emulator loopback to host
  };

  /// Domain suffixes that are allowed (matches host or any sub-domain).
  /// NOTE: these are placeholders for the public repository. In production
  /// replace them with the real university gateway / SIS domain suffix(es).
  static const List<String> _allowedHostSuffixes = [
    'university-sis.example.edu',
    'ulusofona.pt',
  ];

  static bool isAllowed(Uri uri) {
    final scheme = uri.scheme.toLowerCase();

    // Non http(s) schemes: only allow the known credential-exchange schemes.
    if (scheme != 'http' && scheme != 'https') {
      return _allowedSchemes.contains(scheme);
    }

    final host = uri.host.toLowerCase();
    if (host.isEmpty) return false;
    if (_allowedHosts.contains(host)) return true;
    for (final suffix in _allowedHostSuffixes) {
      if (host == suffix || host.endsWith('.$suffix')) return true;
    }
    return false;
  }

  /// Convenience for parsing a raw string and validating it.
  static bool isAllowedString(String url) {
    final uri = Uri.tryParse(url.trim());
    if (uri == null) return false;
    return isAllowed(uri);
  }
}
