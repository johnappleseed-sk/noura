import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../auth/session_event_bus.dart';
import '../config/app_environment.dart';
import '../network/api_client.dart';
import '../network/dio_factory.dart';
import '../storage/token_storage.dart';

final appEnvironmentProvider = Provider<AppEnvironment>((ref) {
  return AppEnvironment.fromDefines();
});

final tokenStorageProvider = Provider<TokenStorage>((ref) {
  return SecureTokenStorage();
});

final sessionEventBusProvider = Provider<SessionEventBus>((ref) {
  final bus = SessionEventBus();
  ref.onDispose(bus.dispose);
  return bus;
});

final dioProvider = Provider<Dio>((ref) {
  final environment = ref.watch(appEnvironmentProvider);
  final tokenStorage = ref.watch(tokenStorageProvider);
  final sessionEventBus = ref.watch(sessionEventBusProvider);
  return DioFactory.create(
    environment: environment,
    tokenStorage: tokenStorage,
    onSessionExpired: () {
      sessionEventBus.emit(SessionEvent.expired);
    },
  );
});

final apiClientProvider = Provider<ApiClient>((ref) {
  final dio = ref.watch(dioProvider);
  return ApiClient(dio);
});
