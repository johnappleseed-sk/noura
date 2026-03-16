import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class WishlistLocalStore {
  WishlistLocalStore({FlutterSecureStorage? storage})
    : _storage =
          storage ??
          const FlutterSecureStorage(
            aOptions: AndroidOptions(encryptedSharedPreferences: true),
            iOptions: IOSOptions(
              accessibility: KeychainAccessibility.first_unlock_this_device,
            ),
          );

  static const _wishlistIdsKey = 'noura_wishlist_product_ids';

  final FlutterSecureStorage _storage;

  Future<Set<String>> readIds() async {
    final raw = await _storage.read(key: _wishlistIdsKey);
    if (raw == null || raw.trim().isEmpty) {
      return <String>{};
    }
    try {
      final decoded = jsonDecode(raw);
      if (decoded is List<dynamic>) {
        return decoded.map((dynamic value) => value.toString()).toSet();
      }
    } on FormatException {
      // If storage contains corrupted JSON, gracefully reset to empty set.
    }
    return <String>{};
  }

  Future<Set<String>> writeIds(Set<String> ids) async {
    await _storage.write(
      key: _wishlistIdsKey,
      value: jsonEncode(ids.toList(growable: false)),
    );
    return ids;
  }
}
