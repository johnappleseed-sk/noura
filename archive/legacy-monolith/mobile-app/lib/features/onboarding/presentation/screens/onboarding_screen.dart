import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';

class OnboardingScreen extends StatelessWidget {
  const OnboardingScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Spacer(),
              Text(
                'Noura Commerce',
                style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 12),
              Text(
                'Shop premium products with personalized recommendations,'
                ' fast checkout, and real-time order updates.',
                style: Theme.of(context).textTheme.bodyLarge,
              ),
              const Spacer(),
              ElevatedButton(
                onPressed: () => context.go(AppRoutes.loginPath),
                child: const Text('Login'),
              ),
              const SizedBox(height: 12),
              OutlinedButton(
                onPressed: () => context.go(AppRoutes.registerPath),
                child: const Text('Create account'),
              ),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                children: [
                  TextButton(
                    onPressed: () => context.go(
                      AppRoutes.legalDocumentPath.replaceFirst(
                        ':topicId',
                        'terms',
                      ),
                    ),
                    child: const Text('Terms'),
                  ),
                  TextButton(
                    onPressed: () => context.go(
                      AppRoutes.legalDocumentPath.replaceFirst(
                        ':topicId',
                        'privacy',
                      ),
                    ),
                    child: const Text('Privacy'),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
