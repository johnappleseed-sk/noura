class CreateProductReviewRequestDto {
  const CreateProductReviewRequestDto({
    required this.rating,
    required this.comment,
  });

  final int rating;
  final String comment;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{'rating': rating, 'comment': comment};
  }
}
