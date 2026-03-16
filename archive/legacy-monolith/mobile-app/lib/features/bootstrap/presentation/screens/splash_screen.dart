import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../auth/application/auth_session_controller.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_loading_view.dart';

class SplashScreen extends ConsumerWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authSession = ref.watch(authSessionControllerProvider);

    return Scaffold(
      body: authSession.when(
        loading: () => const AppLoadingView(message: 'Initializing app...'),
        error: (Object error, StackTrace stackTrace) => AppErrorView(
          message: 'Unable to initialize app session.',
          onRetry: () => ref.invalidate(authSessionControllerProvider),
        ),
        data: (_) => const AppLoadingView(message: 'Initializing app...'),
      ),
    );
  }
}
