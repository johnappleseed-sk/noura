import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:noura/features/account/application/support_provider.dart';
import 'package:noura/features/account/domain/entities/runtime_features.dart';
import 'package:noura/features/account/domain/entities/store_location.dart';
import 'package:noura/features/account/domain/entities/support_topic.dart';
import 'package:noura/features/account/presentation/screens/legal_document_screen.dart';
import 'package:noura/features/account/presentation/screens/support_screen.dart';

const _runtimeFeatures = RuntimeFeatures(
  contractVersion: '1.0',
  features: <String, bool>{},
  messages: <String, String>{},
);

final _store = StoreLocation(
  id: 'store-1',
  name: 'Noura Downtown',
  addressLine1: '123 Main St',
  city: 'Phnom Penh',
  state: 'PP',
  zipCode: '12000',
  country: 'Cambodia',
  region: 'Central',
  latitude: 11.5564,
  longitude: 104.9282,
  serviceRadiusMeters: 5000,
  active: true,
  services: const <String>['DELIVERY', 'PICKUP'],
  shippingFee: 2.5,
  freeShippingThreshold: 20,
  distanceKm: 1.5,
  openNow: true,
);

const _termsTopic = SupportTopic(
  id: 'terms',
  title: 'Terms of Service',
  summary: 'Usage rules and customer obligations.',
  content: 'These are the terms.',
);

void main() {
  group('Support and legal screens', () {
    testWidgets('SupportScreen renders support topics and store list', (
      WidgetTester tester,
    ) async {
      final data = SupportViewData(
        topics: const <SupportTopic>[_termsTopic],
        stores: <StoreLocation>[_store],
        runtimeFeatures: _runtimeFeatures,
        contactMessage: 'Contact support via in-app chat.',
      );

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            supportViewDataProvider.overrideWith((ref) async => data),
          ],
          child: const MaterialApp(home: SupportScreen()),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Need help?'), findsOneWidget);
      expect(find.text('Contact support via in-app chat.'), findsOneWidget);
      expect(find.text('Terms of Service'), findsOneWidget);
      expect(find.text('Noura Downtown'), findsOneWidget);
    });

    testWidgets('LegalDocumentScreen renders selected topic', (
      WidgetTester tester,
    ) async {
      final data = SupportViewData(
        topics: const <SupportTopic>[_termsTopic],
        stores: <StoreLocation>[],
        runtimeFeatures: _runtimeFeatures,
        contactMessage: 'Contact support via in-app chat.',
      );

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            supportViewDataProvider.overrideWith((ref) async => data),
          ],
          child: const MaterialApp(home: LegalDocumentScreen(topicId: 'terms')),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Terms of Service'), findsOneWidget);
      expect(find.text('These are the terms.'), findsOneWidget);
    });
  });
}
