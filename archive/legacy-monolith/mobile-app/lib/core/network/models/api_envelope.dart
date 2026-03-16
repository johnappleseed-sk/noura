class ApiEnvelope {
  const ApiEnvelope({
    required this.success,
    this.message,
    this.data,
    this.error,
    this.timestamp,
    this.path,
  });

  factory ApiEnvelope.fromJson(Map<String, dynamic> json) {
    return ApiEnvelope(
      success: json['success'] == true,
      message: json['message'] as String?,
      data: json['data'],
      error: json['error'] is Map<String, dynamic>
          ? ApiErrorBody.fromJson(json['error'] as Map<String, dynamic>)
          : null,
      timestamp: json['timestamp'] as String?,
      path: json['path'] as String?,
    );
  }

  final bool success;
  final String? message;
  final Object? data;
  final ApiErrorBody? error;
  final String? timestamp;
  final String? path;
}

class ApiErrorBody {
  const ApiErrorBody({this.code, this.detail});

  factory ApiErrorBody.fromJson(Map<String, dynamic> json) {
    return ApiErrorBody(
      code: json['code'] as String?,
      detail: json['detail'] as String?,
    );
  }

  final String? code;
  final String? detail;
}
