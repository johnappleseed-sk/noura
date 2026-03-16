class Address {
  const Address({
    required this.id,
    required this.label,
    required this.fullName,
    required this.phone,
    required this.line1,
    required this.line2,
    required this.district,
    required this.city,
    required this.state,
    required this.zipCode,
    required this.country,
    required this.latitude,
    required this.longitude,
    required this.accuracyMeters,
    required this.placeId,
    required this.formattedAddress,
    required this.deliveryInstructions,
    required this.validationStatus,
    required this.defaultAddress,
  });

  factory Address.fromJson(Map<String, dynamic> json) {
    return Address(
      id: (json['id'] ?? '').toString(),
      label: (json['label'] as String?) ?? '',
      fullName: (json['fullName'] as String?) ?? '',
      phone: (json['phone'] as String?) ?? '',
      line1: (json['line1'] as String?) ?? '',
      line2: (json['line2'] as String?) ?? '',
      district: (json['district'] as String?) ?? '',
      city: (json['city'] as String?) ?? '',
      state: (json['state'] as String?) ?? '',
      zipCode: (json['zipCode'] as String?) ?? '',
      country: (json['country'] as String?) ?? '',
      latitude: _toDouble(json['latitude']),
      longitude: _toDouble(json['longitude']),
      accuracyMeters: _toInt(json['accuracyMeters']),
      placeId: (json['placeId'] as String?) ?? '',
      formattedAddress: (json['formattedAddress'] as String?) ?? '',
      deliveryInstructions: (json['deliveryInstructions'] as String?) ?? '',
      validationStatus: (json['validationStatus'] as String?) ?? 'UNVERIFIED',
      defaultAddress: json['defaultAddress'] == true,
    );
  }

  final String id;
  final String label;
  final String fullName;
  final String phone;
  final String line1;
  final String line2;
  final String district;
  final String city;
  final String state;
  final String zipCode;
  final String country;
  final double latitude;
  final double longitude;
  final int accuracyMeters;
  final String placeId;
  final String formattedAddress;
  final String deliveryInstructions;
  final String validationStatus;
  final bool defaultAddress;

  String get displayLabel {
    if (label.trim().isNotEmpty) {
      return label.trim();
    }
    if (formattedAddress.trim().isNotEmpty) {
      return formattedAddress.trim();
    }
    return '$line1, $city';
  }

  String get compactAddress {
    final values = <String>[
      line1,
      if (line2.trim().isNotEmpty) line2,
      if (district.trim().isNotEmpty) district,
      city,
      state,
      zipCode,
      country,
    ].where((value) => value.trim().isNotEmpty);
    return values.join(', ');
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

  static int _toInt(Object? value) {
    if (value is num) {
      return value.toInt();
    }
    if (value is String) {
      return int.tryParse(value) ?? 0;
    }
    return 0;
  }
}
