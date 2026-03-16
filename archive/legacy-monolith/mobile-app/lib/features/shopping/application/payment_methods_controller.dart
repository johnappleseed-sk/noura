import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/error/app_exception.dart';
import '../../../core/error/error_presenter.dart';
import '../data/repositories/shopping_repository_impl.dart';
import '../domain/entities/payment_method.dart';
import 'payment_methods_state.dart';

class PaymentMethodsController extends AsyncNotifier<PaymentMethodsState> {
  @override
  Future<PaymentMethodsState> build() async {
    return _load();
  }

  Future<void> refresh() async {
    state = const AsyncLoading<PaymentMethodsState>();
    state = await AsyncValue.guard<PaymentMethodsState>(_load);
  }

  Future<bool> addPaymentMethod({
    required String methodType,
    required String provider,
    required String tokenizedReference,
    bool defaultMethod = false,
  }) {
    return _mutate(() async {
      await ref
          .read(shoppingRepositoryProvider)
          .addPaymentMethod(
            methodType: methodType,
            provider: provider,
            tokenizedReference: tokenizedReference,
            defaultMethod: defaultMethod,
          );
      return _fetchPaymentMethods();
    }, successMessage: 'Payment method added.');
  }

  Future<bool> updatePaymentMethod({
    required String paymentMethodId,
    required String methodType,
    required String provider,
    required String tokenizedReference,
    bool defaultMethod = false,
  }) {
    return _mutate(() async {
      await ref
          .read(shoppingRepositoryProvider)
          .updatePaymentMethod(
            paymentMethodId: paymentMethodId,
            methodType: methodType,
            provider: provider,
            tokenizedReference: tokenizedReference,
            defaultMethod: defaultMethod,
          );
      return _fetchPaymentMethods();
    }, successMessage: 'Payment method updated.');
  }

  Future<bool> deletePaymentMethod(String paymentMethodId) {
    return _mutate(() async {
      await ref
          .read(shoppingRepositoryProvider)
          .deletePaymentMethod(paymentMethodId: paymentMethodId);
      return _fetchPaymentMethods();
    }, successMessage: 'Payment method removed.');
  }

  void clearMessages() {
    final current = state.valueOrNull;
    if (current == null) {
      return;
    }
    state = AsyncData<PaymentMethodsState>(
      current.copyWith(actionError: null, actionMessage: null),
    );
  }

  Future<PaymentMethodsState> _load() async {
    final paymentMethods = await _fetchPaymentMethods();
    return PaymentMethodsState(items: paymentMethods, isMutating: false);
  }

  Future<List<PaymentMethod>> _fetchPaymentMethods() async {
    final paymentMethods = await ref
        .read(shoppingRepositoryProvider)
        .getPaymentMethods();
    final sorted = paymentMethods.toList(growable: false)
      ..sort((a, b) {
        if (a.defaultMethod == b.defaultMethod) {
          return a.displayName.compareTo(b.displayName);
        }
        return a.defaultMethod ? -1 : 1;
      });
    return sorted;
  }

  Future<bool> _mutate(
    Future<List<PaymentMethod>> Function() operation, {
    String? successMessage,
  }) async {
    final current = state.valueOrNull;
    if (current != null) {
      state = AsyncData<PaymentMethodsState>(
        current.copyWith(
          isMutating: true,
          actionError: null,
          actionMessage: null,
        ),
      );
    }
    try {
      final items = await operation();
      state = AsyncData<PaymentMethodsState>(
        PaymentMethodsState(
          items: items,
          isMutating: false,
          actionMessage: successMessage,
        ),
      );
      return true;
    } on AppException catch (error) {
      final previous = current ?? const PaymentMethodsState.initial();
      state = AsyncData<PaymentMethodsState>(
        previous.copyWith(
          isMutating: false,
          actionError: error.message,
          actionMessage: null,
        ),
      );
      return false;
    } on Object catch (error) {
      final previous = current ?? const PaymentMethodsState.initial();
      state = AsyncData<PaymentMethodsState>(
        previous.copyWith(
          isMutating: false,
          actionError: ErrorPresenter.message(error),
          actionMessage: null,
        ),
      );
      return false;
    }
  }
}

final paymentMethodsControllerProvider =
    AsyncNotifierProvider<PaymentMethodsController, PaymentMethodsState>(
      PaymentMethodsController.new,
    );
