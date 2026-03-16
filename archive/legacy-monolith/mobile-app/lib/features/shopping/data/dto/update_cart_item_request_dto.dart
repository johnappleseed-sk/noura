class UpdateCartItemRequestDto {
  const UpdateCartItemRequestDto({required this.quantity});

  final int quantity;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{'quantity': quantity};
  }
}
