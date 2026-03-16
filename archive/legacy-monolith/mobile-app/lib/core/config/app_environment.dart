import 'package:flutter/foundation.dart';

class AppEnvironment {
  const AppEnvironment({
    required this.name,
    required this.baseUrl,
    required this.apiVersionPath,
    required this.connectTimeoutMs,
    required this.sendTimeoutMs,
    required this.receiveTimeoutMs,
    required this.enableNetworkLogs,
  });

  factory AppEnvironment.fromDefines() {
    const envName = String.fromEnvironment('APP_ENV', defaultValue: 'dev');
    const rawBaseUrl = String.fromEnvironment(
      'API_BASE_URL',
      defaultValue: 'http://localhost:8080',
    );
    const rawVersionPath = String.fromEnvironment(
      'API_VERSION_PATH',
      defaultValue: '/api/v1',
    );
    const rawConnectTimeout = int.fromEnvironment(
      'API_CONNECT_TIMEOUT_MS',
      defaultValue: 15000,
    );
    const rawSendTimeout = int.fromEnvironment(
      'API_SEND_TIMEOUT_MS',
      defaultValue: 15000,
    );
    const rawReceiveTimeout = int.fromEnvironment(
      'API_RECEIVE_TIMEOUT_MS',
      defaultValue: 20000,
    );
    const rawEnableNetworkLogs = bool.fromEnvironment(
      'API_ENABLE_LOGS',
      defaultValue: true,
    );

    return AppEnvironment(
      name: envName,
      baseUrl: _normalizeBaseUrl(rawBaseUrl),
      apiVersionPath: _normalizePath(rawVersionPath),
      connectTimeoutMs: rawConnectTimeout,
      sendTimeoutMs: rawSendTimeout,
      receiveTimeoutMs: rawReceiveTimeout,
      enableNetworkLogs: !kReleaseMode && rawEnableNetworkLogs,
    );
  }

  final String name;
  final String baseUrl;
  final String apiVersionPath;
  final int connectTimeoutMs;
  final int sendTimeoutMs;
  final int receiveTimeoutMs;
  final bool enableNetworkLogs;

  String get versionedBaseUrl => '$baseUrl$apiVersionPath';

  static String _normalizeBaseUrl(String value) {
    final trimmed = value.trim();
    if (trimmed.isEmpty) {
      return 'http://localhost:8080';
    }
    if (trimmed.endsWith('/')) {
      return trimmed.substring(0, trimmed.length - 1);
    }
    return trimmed;
  }

  static String _normalizePath(String value) {
    final trimmed = value.trim();
    if (trimmed.isEmpty) {
      return '/api/v1';
    }
    final withLeadingSlash = trimmed.startsWith('/') ? trimmed : '/$trimmed';
    if (withLeadingSlash.endsWith('/')) {
      return withLeadingSlash.substring(0, withLeadingSlash.length - 1);
    }
    return withLeadingSlash;
  }
}
