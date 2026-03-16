import 'package:flutter/material.dart';

/// A reusable empty-state widget that displays guidance when no data is available.
class AppEmptyView extends StatelessWidget {
  /// Creates an [AppEmptyView] with optional subtitle and action button.
  const AppEmptyView({
    super.key,
    required this.title,
    this.subtitle,
    this.actionLabel,
    this.onActionPressed,
    this.icon = Icons.inbox_outlined,
  });

  /// Headline text shown in the empty state.
  final String title;

  /// Supporting text that gives additional context.
  final String? subtitle;

  /// Label for the optional CTA button.
  final String? actionLabel;

  /// Callback for the optional CTA button.
  final VoidCallback? onActionPressed;

  /// Icon used to visually represent the empty state.
  final IconData icon;

  /// Builds the empty-state layout.
  @override
  Widget build(BuildContext context) {
    final TextTheme textTheme = Theme.of(context).textTheme;
    final ColorScheme colorScheme = Theme.of(context).colorScheme;

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 420),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 56, color: colorScheme.outline),
              const SizedBox(height: 12),
              Text(
                title,
                style: textTheme.titleMedium,
                textAlign: TextAlign.center,
              ),
              if (subtitle != null) ...[
                const SizedBox(height: 8),
                Text(
                  subtitle!,
                  textAlign: TextAlign.center,
                  style: textTheme.bodyMedium,
                ),
              ],
              if (actionLabel != null && onActionPressed != null) ...[
                const SizedBox(height: 16),
                ElevatedButton(
                  onPressed: onActionPressed,
                  child: Text(actionLabel!),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
