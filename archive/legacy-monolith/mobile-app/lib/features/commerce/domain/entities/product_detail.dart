class ProductDetail {
  const ProductDetail({
    required this.id,
    required this.name,
    required this.category,
    required this.brand,
    required this.price,
    required this.flashSale,
    required this.trending,
    required this.bestSeller,
    required this.averageRating,
    required this.reviewCount,
    required this.popularityScore,
    required this.shortDescription,
    required this.longDescription,
    required this.active,
    required this.allowBackorder,
    required this.attributes,
    required this.variants,
    required this.media,
    required this.storeInventory,
  });

  factory ProductDetail.fromJson(Map<String, dynamic> json) {
    return ProductDetail(
      id: (json['id'] ?? '').toString(),
      name: (json['name'] as String?) ?? '',
      category: (json['category'] as String?) ?? '',
      brand: (json['brand'] as String?) ?? '',
      price: _toDouble(json['price']),
      flashSale: json['flashSale'] == true,
      trending: json['trending'] == true,
      bestSeller: json['bestSeller'] == true,
      averageRating: _toDouble(json['averageRating']),
      reviewCount: _toInt(json['reviewCount']),
      popularityScore: _toInt(json['popularityScore']),
      shortDescription: (json['shortDescription'] as String?) ?? '',
      longDescription: (json['longDescription'] as String?) ?? '',
      active: json['active'] == true,
      allowBackorder: json['allowBackorder'] == true,
      attributes: json['attributes'] is Map
          ? Map<String, dynamic>.from(json['attributes'] as Map)
          : const <String, dynamic>{},
      variants: (json['variants'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(ProductVariantDetail.fromJson)
          .toList(growable: false),
      media: (json['media'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(ProductMediaDetail.fromJson)
          .toList(growable: false),
      storeInventory: (json['storeInventory'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(ProductStoreInventoryDetail.fromJson)
          .toList(growable: false),
    );
  }

  final String id;
  final String name;
  final String category;
  final String brand;
  final double price;
  final bool flashSale;
  final bool trending;
  final bool bestSeller;
  final double averageRating;
  final int reviewCount;
  final int popularityScore;
  final String shortDescription;
  final String longDescription;
  final bool active;
  final bool allowBackorder;
  final Map<String, dynamic> attributes;
  final List<ProductVariantDetail> variants;
  final List<ProductMediaDetail> media;
  final List<ProductStoreInventoryDetail> storeInventory;

  ProductMediaDetail? get primaryMedia {
    if (media.isEmpty) {
      return null;
    }
    return media.firstWhere(
      (ProductMediaDetail item) => item.primary,
      orElse: () => media.first,
    );
  }

  int get totalStock {
    return storeInventory.fold<int>(
      0,
      (int total, ProductStoreInventoryDetail item) => total + item.stock,
    );
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

class ProductVariantDetail {
  const ProductVariantDetail({
    required this.id,
    required this.color,
    required this.size,
    required this.sku,
    required this.stock,
    required this.active,
  });

  factory ProductVariantDetail.fromJson(Map<String, dynamic> json) {
    return ProductVariantDetail(
      id: (json['id'] ?? '').toString(),
      color: (json['color'] as String?) ?? '',
      size: (json['size'] as String?) ?? '',
      sku: (json['sku'] as String?) ?? '',
      stock: json['stock'] is num ? (json['stock'] as num).toInt() : 0,
      active: json['active'] == true,
    );
  }

  final String id;
  final String color;
  final String size;
  final String sku;
  final int stock;
  final bool active;
}

class ProductMediaDetail {
  const ProductMediaDetail({
    required this.id,
    required this.mediaType,
    required this.url,
    required this.sortOrder,
    required this.primary,
  });

  factory ProductMediaDetail.fromJson(Map<String, dynamic> json) {
    return ProductMediaDetail(
      id: (json['id'] ?? '').toString(),
      mediaType: (json['mediaType'] as String?) ?? '',
      url: (json['url'] as String?) ?? '',
      sortOrder: json['sortOrder'] is num
          ? (json['sortOrder'] as num).toInt()
          : 0,
      primary: json['primary'] == true,
    );
  }

  final String id;
  final String mediaType;
  final String url;
  final int sortOrder;
  final bool primary;
}

class ProductStoreInventoryDetail {
  const ProductStoreInventoryDetail({
    required this.storeId,
    required this.storeName,
    required this.stock,
    required this.storePrice,
  });

  factory ProductStoreInventoryDetail.fromJson(Map<String, dynamic> json) {
    return ProductStoreInventoryDetail(
      storeId: (json['storeId'] ?? '').toString(),
      storeName: (json['storeName'] as String?) ?? '',
      stock: json['stock'] is num ? (json['stock'] as num).toInt() : 0,
      storePrice: json['storePrice'] is num
          ? (json['storePrice'] as num).toDouble()
          : 0,
    );
  }

  final String storeId;
  final String storeName;
  final int stock;
  final double storePrice;
}
