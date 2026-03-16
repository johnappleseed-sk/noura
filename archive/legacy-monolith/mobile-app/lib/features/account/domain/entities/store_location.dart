class StoreLocation {
  const StoreLocation({
    required this.id,
    required this.name,
    required this.addressLine1,
    required this.city,
    required this.state,
    required this.zipCode,
    required this.country,
    required this.region,
    required this.latitude,
    required this.longitude,
    required this.serviceRadiusMeters,
    required this.active,
    required this.services,
    required this.shippingFee,
    required this.freeShippingThreshold,
    required this.distanceKm,
    required this.openNow,
  });

  factory StoreLocation.fromJson(Map<String, dynamic> json) {
    return StoreLocation(
      id: (json['id'] ?? '').toString(),
      name: (json['name'] as String?) ?? '',
      addressLine1: (json['addressLine1'] as String?) ?? '',
      city: (json['city'] as String?) ?? '',
      state: (json['state'] as String?) ?? '',
      zipCode: (json['zipCode'] as String?) ?? '',
      country: (json['country'] as String?) ?? '',
      region: (json['region'] as String?) ?? '',
      latitude: _toDouble(json['latitude']),
      longitude: _toDouble(json['longitude']),
      serviceRadiusMeters: _toInt(json['serviceRadiusMeters']),
      active: json['active'] == true,
      services: (json['services'] as List<dynamic>? ?? const [])
          .map((dynamic value) => value.toString())
          .toList(growable: false),
      shippingFee: _toDouble(json['shippingFee']),
      freeShippingThreshold: _toDouble(json['freeShippingThreshold']),
      distanceKm: _toDouble(json['distanceKm']),
      openNow: json['openNow'] == true,
    );
  }

  final String id;
  final String name;
  final String addressLine1;
  final String city;
  final String state;
  final String zipCode;
  final String country;
  final String region;
  final double latitude;
  final double longitude;
  final int serviceRadiusMeters;
  final bool active;
  final List<String> services;
  final double shippingFee;
  final double freeShippingThreshold;
  final double distanceKm;
  final bool openNow;

  String get compactAddress {
    final values = <String>[
      addressLine1,
      city,
      state,
      zipCode,
      country,
    ].where((value) => value.trim().isNotEmpty);
    return values.join(', ');
  }

  static int _toInt(Object? value) {
    if (value is num) {
      return value.toInt();
    }
    if (value is String) {
      return int.tryParse(value) ?? 0;
    }
    return 0;
  }

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }
    if (value is String) {
      return double.tryParse(value) ?? 0;
    }
    return 0;
  }
}
