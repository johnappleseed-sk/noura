import 'package:dio/dio.dart';

import '../config/app_environment.dart';
import '../storage/token_storage.dart';
import 'api_endpoints.dart';
import 'interceptors/auth_interceptor.dart';
import 'interceptors/retry_interceptor.dart';

abstract final class DioFactory {
  static Dio create({
    required AppEnvironment environment,
    required TokenStorage tokenStorage,
    required void Function() onSessionExpired,
  }) {
    final dio = Dio(
      BaseOptions(
        baseUrl: environment.versionedBaseUrl,
        connectTimeout: Duration(milliseconds: environment.connectTimeoutMs),
        sendTimeout: Duration(milliseconds: environment.sendTimeoutMs),
        receiveTimeout: Duration(milliseconds: environment.receiveTimeoutMs),
        headers: const <String, String>{
          'Accept': 'application/json',
          'Content-Type': 'application/json',
        },
        responseType: ResponseType.json,
      ),
    );

    dio.interceptors.add(RetryInterceptor(dio: dio));

    dio.interceptors.add(
      AuthInterceptor(
        dio: dio,
        tokenStorage: tokenStorage,
        refreshPath: ApiEndpoints.authRefresh,
        onSessionExpired: onSessionExpired,
      ),
    );

    if (environment.enableNetworkLogs) {
      dio.interceptors.add(
        LogInterceptor(
          requestBody: true,
          responseBody: true,
          requestHeader: false,
          responseHeader: false,
        ),
      );
    }

    return dio;
  }
}
