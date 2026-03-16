class Order {
  const Order({
    required this.id,
    required this.userId,
    required this.storeId,
    required this.subtotal,
    required this.discountAmount,
    required this.shippingAmount,
    required this.totalAmount,
    required this.fulfillmentMethod,
    required this.status,
    required this.refundStatus,
    required this.couponCode,
    required this.createdAt,
    required this.items,
  });

  factory Order.fromJson(Map<String, dynamic> json) {
    return Order(
      id: (json['id'] ?? '').toString(),
      userId: (json['userId'] ?? '').toString(),
      storeId: (json['storeId'] ?? '').toString(),
      subtotal: _toDouble(json['subtotal']),
      discountAmount: _toDouble(json['discountAmount']),
      shippingAmount: _toDouble(json['shippingAmount']),
      totalAmount: _toDouble(json['totalAmount']),
      fulfillmentMethod: (json['fulfillmentMethod'] as String?) ?? '',
      status: (json['status'] as String?) ?? '',
      refundStatus: (json['refundStatus'] as String?) ?? '',
      couponCode: (json['couponCode'] as String?) ?? '',
      createdAt: _toDateTime(json['createdAt']),
      items: (json['items'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(OrderItem.fromJson)
          .toList(growable: false),
    );
  }

  final String id;
  final String userId;
  final String storeId;
  final double subtotal;
  final double discountAmount;
  final double shippingAmount;
  final double totalAmount;
  final String fulfillmentMethod;
  final String status;
  final String refundStatus;
  final String couponCode;
  final DateTime? createdAt;
  final List<OrderItem> items;

  static double _toDouble(Object? value) {
    if (value is num) {
      return value.toDouble();
    }
    if (value is String) {
      return double.tryParse(value) ?? 0;
    }
    return 0;
  }

  static DateTime? _toDateTime(Object? value) {
    if (value is String && value.trim().isNotEmpty) {
      return DateTime.tryParse(value);
    }
    return null;
  }
}

class OrderItem {
  const OrderItem({
    required this.productId,
    required this.productName,
    required this.quantity,
    required this.unitPrice,
    required this.lineTotal,
  });

  factory OrderItem.fromJson(Map<String, dynamic> json) {
    return OrderItem(
      productId: (json['productId'] ?? '').toString(),
      productName: (json['productName'] as String?) ?? '',
      quantity: _toInt(json['quantity']),
      unitPrice: _toDouble(json['unitPrice']),
      lineTotal: _toDouble(json['lineTotal']),
    );
  }

  final String productId;
  final String productName;
  final int quantity;
  final double unitPrice;
  final double lineTotal;

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

class OrderTimelineEvent {
  const OrderTimelineEvent({
    required this.id,
    required this.orderId,
    required this.status,
    required this.refundStatus,
    required this.actor,
    required this.note,
    required this.createdAt,
  });

  factory OrderTimelineEvent.fromJson(Map<String, dynamic> json) {
    return OrderTimelineEvent(
      id: (json['id'] ?? '').toString(),
      orderId: (json['orderId'] ?? '').toString(),
      status: (json['status'] as String?) ?? '',
      refundStatus: (json['refundStatus'] as String?) ?? '',
      actor: (json['actor'] as String?) ?? '',
      note: (json['note'] as String?) ?? '',
      createdAt: _toDateTime(json['createdAt']),
    );
  }

  final String id;
  final String orderId;
  final String status;
  final String refundStatus;
  final String actor;
  final String note;
  final DateTime? createdAt;

  static DateTime? _toDateTime(Object? value) {
    if (value is String && value.trim().isNotEmpty) {
      return DateTime.tryParse(value);
    }
    return null;
  }
}
