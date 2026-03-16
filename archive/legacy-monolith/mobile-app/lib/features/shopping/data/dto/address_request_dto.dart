class AddressRequestDto {
  const AddressRequestDto({
    required this.fullName,
    required this.line1,
    required this.city,
    required this.state,
    required this.zipCode,
    required this.country,
    this.label,
    this.phone,
    this.line2,
    this.district,
    this.latitude,
    this.longitude,
    this.accuracyMeters,
    this.placeId,
    this.formattedAddress,
    this.deliveryInstructions,
    this.defaultAddress = false,
  });

  final String? label;
  final String? phone;
  final String fullName;
  final String line1;
  final String? line2;
  final String? district;
  final String city;
  final String state;
  final String zipCode;
  final String country;
  final double? latitude;
  final double? longitude;
  final int? accuracyMeters;
  final String? placeId;
  final String? formattedAddress;
  final String? deliveryInstructions;
  final bool defaultAddress;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      if (label != null && label!.trim().isNotEmpty) 'label': label,
      if (phone != null && phone!.trim().isNotEmpty) 'phone': phone,
      'fullName': fullName,
      'line1': line1,
      if (line2 != null && line2!.trim().isNotEmpty) 'line2': line2,
      if (district != null && district!.trim().isNotEmpty) 'district': district,
      'city': city,
      'state': state,
      'zipCode': zipCode,
      'country': country,
      if (latitude != null) 'latitude': latitude,
      if (longitude != null) 'longitude': longitude,
      if (accuracyMeters != null) 'accuracyMeters': accuracyMeters,
      if (placeId != null && placeId!.trim().isNotEmpty) 'placeId': placeId,
      if (formattedAddress != null && formattedAddress!.trim().isNotEmpty)
        'formattedAddress': formattedAddress,
      if (deliveryInstructions != null &&
          deliveryInstructions!.trim().isNotEmpty)
        'deliveryInstructions': deliveryInstructions,
      'defaultAddress': defaultAddress,
    };
  }
}
