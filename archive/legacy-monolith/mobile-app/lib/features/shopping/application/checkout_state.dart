import '../domain/entities/checkout_preview.dart';
import '../domain/entities/order.dart';

class CheckoutState {
  const CheckoutState({
    required this.preview,
    required this.fulfillmentMethod,
    required this.selectedAddressId,
    required this.paymentReference,
    required this.couponCode,
    required this.b2bInvoice,
    required this.isSubmitting,
    this.statusMessage,
    this.errorMessage,
    this.placedOrder,
  });

  final CheckoutPreview preview;
  final String fulfillmentMethod;
  final String? selectedAddressId;
  final String paymentReference;
  final String couponCode;
  final bool b2bInvoice;
  final bool isSubmitting;
  final String? statusMessage;
  final String? errorMessage;
  final Order? placedOrder;

  CheckoutState copyWith({
    CheckoutPreview? preview,
    String? fulfillmentMethod,
    Object? selectedAddressId = _unset,
    String? paymentReference,
    String? couponCode,
    bool? b2bInvoice,
    bool? isSubmitting,
    Object? statusMessage = _unset,
    Object? errorMessage = _unset,
    Object? placedOrder = _unset,
  }) {
    return CheckoutState(
      preview: preview ?? this.preview,
      fulfillmentMethod: fulfillmentMethod ?? this.fulfillmentMethod,
      selectedAddressId: identical(selectedAddressId, _unset)
          ? this.selectedAddressId
          : selectedAddressId as String?,
      paymentReference: paymentReference ?? this.paymentReference,
      couponCode: couponCode ?? this.couponCode,
      b2bInvoice: b2bInvoice ?? this.b2bInvoice,
      isSubmitting: isSubmitting ?? this.isSubmitting,
      statusMessage: identical(statusMessage, _unset)
          ? this.statusMessage
          : statusMessage as String?,
      errorMessage: identical(errorMessage, _unset)
          ? this.errorMessage
          : errorMessage as String?,
      placedOrder: identical(placedOrder, _unset)
          ? this.placedOrder
          : placedOrder as Order?,
    );
  }

  static const Object _unset = Object();
}
