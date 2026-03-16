import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/error/app_exception.dart';
import '../../../core/error/error_presenter.dart';
import '../data/repositories/shopping_repository_impl.dart';
import 'checkout_state.dart';

class CheckoutController extends AsyncNotifier<CheckoutState> {
  @override
  Future<CheckoutState> build() async {
    return _load();
  }

  Future<void> refresh() async {
    state = const AsyncLoading<CheckoutState>();
    state = await AsyncValue.guard<CheckoutState>(_load);
  }

  void setFulfillmentMethod(String value) {
    final current = state.valueOrNull;
    if (current == null) {
      return;
    }
    state = AsyncData<CheckoutState>(
      current.copyWith(
        fulfillmentMethod: value,
        errorMessage: null,
        statusMessage: null,
      ),
    );
  }

  void setSelectedAddress(String? addressId) {
    final current = state.valueOrNull;
    if (current == null) {
      return;
    }
    state = AsyncData<CheckoutState>(
      current.copyWith(
        selectedAddressId: addressId,
        errorMessage: null,
        statusMessage: null,
      ),
    );
  }

  void setPaymentReference(String value) {
    final current = state.valueOrNull;
    if (current == null) {
      return;
    }
    state = AsyncData<CheckoutState>(
      current.copyWith(
        paymentReference: value,
        errorMessage: null,
        statusMessage: null,
      ),
    );
  }

  void setCouponCode(String value) {
    final current = state.valueOrNull;
    if (current == null) {
      return;
    }
    state = AsyncData<CheckoutState>(
      current.copyWith(
        couponCode: value,
        errorMessage: null,
        statusMessage: null,
      ),
    );
  }

  void setB2bInvoice(bool value) {
    final current = state.valueOrNull;
    if (current == null) {
      return;
    }
    state = AsyncData<CheckoutState>(
      current.copyWith(b2bInvoice: value, errorMessage: null),
    );
  }

  Future<bool> submitShippingStep() async {
    final current = state.valueOrNull;
    if (current == null) {
      return false;
    }
    if (current.fulfillmentMethod == 'DELIVERY' &&
        (current.selectedAddressId == null ||
            current.selectedAddressId!.trim().isEmpty)) {
      state = AsyncData<CheckoutState>(
        current.copyWith(
          isSubmitting: false,
          statusMessage: null,
          errorMessage: 'Please select a delivery address.',
        ),
      );
      return false;
    }

    return _runMutation(() async {
      final preview = await ref
          .read(shoppingRepositoryProvider)
          .submitShippingStep(
            fulfillmentMethod: current.fulfillmentMethod,
            storeId: current.preview.cart.storeId,
            addressId: current.fulfillmentMethod == 'DELIVERY'
                ? current.selectedAddressId
                : null,
          );
      return current.copyWith(
        preview: preview,
        statusMessage: preview.message.isNotEmpty
            ? preview.message
            : 'Shipping step completed.',
        errorMessage: null,
      );
    });
  }

  Future<bool> submitPaymentStep() async {
    final current = state.valueOrNull;
    if (current == null) {
      return false;
    }
    if (current.paymentReference.trim().isEmpty) {
      state = AsyncData<CheckoutState>(
        current.copyWith(
          isSubmitting: false,
          statusMessage: null,
          errorMessage: 'Please provide or select a payment reference.',
        ),
      );
      return false;
    }

    return _runMutation(() async {
      final preview = await ref
          .read(shoppingRepositoryProvider)
          .submitPaymentStep(
            paymentReference: current.paymentReference.trim().isEmpty
                ? null
                : current.paymentReference.trim(),
            couponCode: current.couponCode.trim().isEmpty
                ? null
                : current.couponCode.trim(),
            b2bInvoice: current.b2bInvoice,
            idempotencyKey: _createIdempotencyKey('payment'),
          );
      return current.copyWith(
        preview: preview,
        statusMessage: preview.message.isNotEmpty
            ? preview.message
            : 'Payment step completed.',
        errorMessage: null,
      );
    });
  }

  Future<bool> confirmOrder() async {
    final current = state.valueOrNull;
    if (current == null) {
      return false;
    }
    if (current.fulfillmentMethod == 'DELIVERY' &&
        (current.selectedAddressId == null ||
            current.selectedAddressId!.trim().isEmpty)) {
      state = AsyncData<CheckoutState>(
        current.copyWith(
          isSubmitting: false,
          statusMessage: null,
          errorMessage:
              'Please select a delivery address before placing order.',
        ),
      );
      return false;
    }
    if (current.paymentReference.trim().isEmpty) {
      state = AsyncData<CheckoutState>(
        current.copyWith(
          isSubmitting: false,
          statusMessage: null,
          errorMessage: 'Payment reference is required before placing order.',
        ),
      );
      return false;
    }

    return _runMutation(() async {
      final order = await ref
          .read(shoppingRepositoryProvider)
          .confirmCheckout(
            fulfillmentMethod: current.fulfillmentMethod,
            storeId: current.preview.cart.storeId,
            addressId: current.fulfillmentMethod == 'DELIVERY'
                ? current.selectedAddressId
                : null,
            paymentReference: current.paymentReference.trim().isEmpty
                ? null
                : current.paymentReference.trim(),
            couponCode: current.couponCode.trim().isEmpty
                ? null
                : current.couponCode.trim(),
            b2bInvoice: current.b2bInvoice,
            idempotencyKey: _createIdempotencyKey('confirm'),
          );
      return current.copyWith(
        placedOrder: order,
        statusMessage: 'Order placed successfully.',
        errorMessage: null,
      );
    });
  }

  void clearMessages() {
    final current = state.valueOrNull;
    if (current == null) {
      return;
    }
    state = AsyncData<CheckoutState>(
      current.copyWith(statusMessage: null, errorMessage: null),
    );
  }

  Future<CheckoutState> _load() async {
    final preview = await ref
        .read(shoppingRepositoryProvider)
        .reviewCheckoutStep();
    final defaultAddressId = preview.cart.addressId.isNotEmpty
        ? preview.cart.addressId
        : null;
    return CheckoutState(
      preview: preview,
      fulfillmentMethod: defaultAddressId == null ? 'PICKUP' : 'DELIVERY',
      selectedAddressId: defaultAddressId,
      paymentReference: '',
      couponCode: preview.cart.totals.couponCode,
      b2bInvoice: false,
      isSubmitting: false,
    );
  }

  Future<bool> _runMutation(Future<CheckoutState> Function() operation) async {
    final current = state.valueOrNull;
    if (current == null) {
      return false;
    }

    state = AsyncData<CheckoutState>(
      current.copyWith(
        isSubmitting: true,
        errorMessage: null,
        statusMessage: null,
      ),
    );

    try {
      final nextState = await operation();
      state = AsyncData<CheckoutState>(nextState.copyWith(isSubmitting: false));
      return true;
    } on AppException catch (error) {
      state = AsyncData<CheckoutState>(
        current.copyWith(
          isSubmitting: false,
          errorMessage: error.message,
          statusMessage: null,
        ),
      );
      return false;
    } on Object catch (error) {
      state = AsyncData<CheckoutState>(
        current.copyWith(
          isSubmitting: false,
          errorMessage: ErrorPresenter.message(error),
          statusMessage: null,
        ),
      );
      return false;
    }
  }

  String _createIdempotencyKey(String prefix) {
    return 'mobile-$prefix-${DateTime.now().microsecondsSinceEpoch}';
  }
}

final checkoutControllerProvider =
    AsyncNotifierProvider<CheckoutController, CheckoutState>(
      CheckoutController.new,
    );
