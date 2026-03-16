import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/utils/form_validators.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_inline_banner.dart';
import '../../../../core/widgets/app_loading_view.dart';
import '../../../auth/application/auth_controller.dart';
import '../../application/profile_controller.dart';
import '../../application/profile_state.dart';

class ProfileScreen extends ConsumerStatefulWidget {
  const ProfileScreen({super.key});

  @override
  ConsumerState<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends ConsumerState<ProfileScreen> {
  final _formKey = GlobalKey<FormState>();
  final TextEditingController _fullNameController = TextEditingController();
  final TextEditingController _phoneController = TextEditingController();

  @override
  void dispose() {
    _fullNameController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final profileAsync = ref.watch(profileControllerProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Profile')),
      body: profileAsync.when(
        loading: () => const AppLoadingView(message: 'Loading profile...'),
        error: (Object error, StackTrace stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () => ref.read(profileControllerProvider.notifier).refresh(),
        ),
        data: (ProfileState state) {
          if (_fullNameController.text != state.profile.fullName) {
            _fullNameController.text = state.profile.fullName;
          }
          if (_phoneController.text != state.profile.phone) {
            _phoneController.text = state.profile.phone;
          }

          return ListView(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
            children: [
              if (state.error != null) ...[
                AppInlineBanner(
                  message: state.error!,
                  isError: true,
                  onClose: () => ref
                      .read(profileControllerProvider.notifier)
                      .clearMessages(),
                ),
                const SizedBox(height: 10),
              ],
              if (state.message != null) ...[
                AppInlineBanner(
                  message: state.message!,
                  isError: false,
                  onClose: () => ref
                      .read(profileControllerProvider.notifier)
                      .clearMessages(),
                ),
                const SizedBox(height: 10),
              ],
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        state.profile.email,
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Roles: ${state.profile.roles.join(', ')}',
                        style: Theme.of(context).textTheme.bodyMedium,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        state.profile.enabled
                            ? 'Account Active'
                            : 'Account Disabled',
                      ),
                      if (state.profile.preferredStoreId.trim().isNotEmpty)
                        Text(
                          'Preferred store: ${state.profile.preferredStoreId}',
                        ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 10),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      children: [
                        TextFormField(
                          controller: _fullNameController,
                          decoration: const InputDecoration(
                            labelText: 'Full name',
                          ),
                          validator: (value) => FormValidators.requiredField(
                            value,
                            fieldName: 'Full name',
                          ),
                        ),
                        const SizedBox(height: 8),
                        TextFormField(
                          controller: _phoneController,
                          decoration: const InputDecoration(labelText: 'Phone'),
                          validator: FormValidators.phone,
                        ),
                        const SizedBox(height: 12),
                        SizedBox(
                          width: double.infinity,
                          child: FilledButton(
                            onPressed: state.isSaving
                                ? null
                                : _submitProfileUpdate,
                            child: state.isSaving
                                ? const SizedBox(
                                    width: 18,
                                    height: 18,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2,
                                    ),
                                  )
                                : const Text('Save profile'),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 10),
              Card(
                child: Column(
                  children: [
                    ListTile(
                      leading: const Icon(Icons.receipt_long_outlined),
                      title: const Text('My Orders'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => context.go(AppRoutes.ordersPath),
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: const Icon(Icons.notifications_outlined),
                      title: const Text('Notifications'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => context.go(AppRoutes.notificationsPath),
                    ),
                    const Divider(height: 1),
                    ListTile(
                      leading: const Icon(Icons.settings_outlined),
                      title: const Text('Settings'),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => context.go(AppRoutes.settingsPath),
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

  Future<void> _submitProfileUpdate() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    await ref
        .read(profileControllerProvider.notifier)
        .updateProfile(
          fullName: _fullNameController.text.trim(),
          phone: _phoneController.text.trim(),
        );
  }
}
