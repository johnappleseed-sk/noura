import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/config/app_environment.dart';
import '../../../../core/providers/core_providers.dart';
import '../../../../core/widgets/app_empty_view.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_inline_banner.dart';
import '../../../../core/widgets/app_loading_view.dart';
import '../../../auth/application/auth_controller.dart';
import '../../application/settings_controller.dart';
import '../../application/settings_state.dart';
import '../../domain/entities/store_location.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settingsAsync = ref.watch(settingsControllerProvider);
    final environment = ref.watch(appEnvironmentProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: settingsAsync.when(
        loading: () => const AppLoadingView(message: 'Loading settings...'),
        error: (Object error, StackTrace stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () =>
              ref.read(settingsControllerProvider.notifier).refresh(),
        ),
        data: (SettingsState state) {
          return ListView(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
            children: [
              if (state.error != null) ...[
                AppInlineBanner(
                  message: state.error!,
                  isError: true,
                  onClose: () => ref
                      .read(settingsControllerProvider.notifier)
                      .clearMessages(),
                ),
                const SizedBox(height: 10),
              ],
              if (state.message != null) ...[
                AppInlineBanner(
                  message: state.message!,
                  isError: false,
                  onClose: () => ref
                      .read(settingsControllerProvider.notifier)
                      .clearMessages(),
                ),
                const SizedBox(height: 10),
              ],
              _EnvironmentCard(environment: environment, state: state),
              const SizedBox(height: 10),
              _PreferredStoreCard(state: state),
              const SizedBox(height: 10),
              _RuntimeFeaturesCard(state: state),
              const SizedBox(height: 10),
              _RuntimeMessagesCard(state: state),
              const SizedBox(height: 10),
              Card(
                child: Column(
                  children: [
                    ListTile(
                      leading: const Icon(Icons.person_outline),
                      title: const Text('Profile'),
                      onTap: () => context.go(AppRoutes.profilePath),
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: const Icon(Icons.receipt_long_outlined),
                      title: const Text('Orders'),
                      onTap: () => context.go(AppRoutes.ordersPath),
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: const Icon(Icons.notifications_outlined),
                      title: const Text('Notifications'),
                      onTap: () => context.go(AppRoutes.notificationsPath),
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: const Icon(Icons.help_outline),
                      title: const Text('Support'),
                      onTap: () => context.go(AppRoutes.supportPath),
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: const Icon(Icons.description_outlined),
                      title: const Text('Terms of Service'),
                      onTap: () => context.go(
                        AppRoutes.legalDocumentPath.replaceFirst(
                          ':topicId',
                          'terms',
                        ),
                      ),
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: const Icon(Icons.privacy_tip_outlined),
                      title: const Text('Privacy Policy'),
                      onTap: () => context.go(
                        AppRoutes.legalDocumentPath.replaceFirst(
                          ':topicId',
                          'privacy',
                        ),
                      ),
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: const Icon(Icons.info_outline),
                      title: const Text('About Noura'),
                      onTap: () => context.go(
                        AppRoutes.legalDocumentPath.replaceFirst(
                          ':topicId',
                          'about',
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              OutlinedButton.icon(
                onPressed: () async {
                  await ref.read(authControllerProvider.notifier).logout();
                  if (context.mounted) {
                    context.go(AppRoutes.loginPath);
                  }
                },
                icon: const Icon(Icons.logout),
                label: const Text('Sign out'),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _EnvironmentCard extends StatelessWidget {
  const _EnvironmentCard({required this.environment, required this.state});

  final AppEnvironment environment;
  final SettingsState state;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Environment', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            Text('App env: ${environment.name}'),
            Text('API: ${environment.versionedBaseUrl}'),
            Text(
              'Contract: '
              '${state.runtimeFeatures.contractVersion.isEmpty ? '-' : state.runtimeFeatures.contractVersion}',
            ),
          ],
        ),
      ),
    );
  }
}

class _PreferredStoreCard extends ConsumerWidget {
  const _PreferredStoreCard({required this.state});

  final SettingsState state;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (state.stores.isEmpty) {
      return const AppEmptyView(
        title: 'No stores available.',
        subtitle: 'Store settings will appear when stores are configured.',
      );
    }

    final selectedStore = _resolveSelectedStore(
      state.stores,
      state.preferredStoreId,
    );

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Preferred Store',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            DropdownButtonFormField<String>(
              initialValue: selectedStore?.id,
              items: state.stores
                  .map(
                    (store) => DropdownMenuItem<String>(
                      value: store.id,
                      child: Text(store.name),
                    ),
                  )
                  .toList(growable: false),
              onChanged: state.isMutating
                  ? null
                  : (value) {
                      if (value == null) {
                        return;
                      }
                      ref
                          .read(settingsControllerProvider.notifier)
                          .setPreferredStore(value);
                    },
            ),
            if (selectedStore != null) ...[
              const SizedBox(height: 8),
              Text(selectedStore.compactAddress),
            ],
          ],
        ),
      ),
    );
  }

  StoreLocation? _resolveSelectedStore(
    List<StoreLocation> stores,
    String preferredStoreId,
  ) {
    if (preferredStoreId.trim().isEmpty) {
      return stores.first;
    }
    return stores.cast<StoreLocation?>().firstWhere(
      (store) => store?.id == preferredStoreId,
      orElse: () => stores.first,
    );
  }
}

class _RuntimeFeaturesCard extends StatelessWidget {
  const _RuntimeFeaturesCard({required this.state});

  final SettingsState state;

  @override
  Widget build(BuildContext context) {
    final entries = state.runtimeFeatures.features.entries.toList(
      growable: false,
    )..sort((a, b) => a.key.compareTo(b.key));

    if (entries.isEmpty) {
      return const AppEmptyView(
        title: 'No runtime features available.',
        subtitle: 'Feature toggles are not configured.',
      );
    }

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Feature Toggles',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            ...entries.map(
              (entry) => SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(entry.key),
                value: entry.value,
                onChanged: null,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _RuntimeMessagesCard extends StatelessWidget {
  const _RuntimeMessagesCard({required this.state});

  final SettingsState state;

  @override
  Widget build(BuildContext context) {
    final entries = state.runtimeFeatures.messages.entries.toList(
      growable: false,
    )..sort((a, b) => a.key.compareTo(b.key));

    if (entries.isEmpty) {
      return const SizedBox.shrink();
    }

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Runtime Messages',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            ...entries.map(
              (entry) => ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(entry.key),
                subtitle: Text(entry.value),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
