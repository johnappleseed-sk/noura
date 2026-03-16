class ApplyCouponRequestDto {
  const ApplyCouponRequestDto({required this.couponCode});

  final String couponCode;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{'couponCode': couponCode};
  }
}
