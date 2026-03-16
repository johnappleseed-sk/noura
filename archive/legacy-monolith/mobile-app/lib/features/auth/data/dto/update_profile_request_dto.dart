class UpdateProfileRequestDto {
  const UpdateProfileRequestDto({required this.fullName, required this.phone});

  final String fullName;
  final String phone;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'fullName': fullName.trim(),
      'phone': phone.trim(),
    };
  }
}
