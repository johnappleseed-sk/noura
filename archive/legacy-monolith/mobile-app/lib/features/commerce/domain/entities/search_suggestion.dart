class SearchSuggestion {
  const SearchSuggestion({required this.value, required this.scope});

  factory SearchSuggestion.fromJson(Map<String, dynamic> json) {
    return SearchSuggestion(
      value: (json['value'] as String?) ?? '',
      scope: (json['scope'] as String?) ?? '',
    );
  }

  final String value;
  final String scope;
}
