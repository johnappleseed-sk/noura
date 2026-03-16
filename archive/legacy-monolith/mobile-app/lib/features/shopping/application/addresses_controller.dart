import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/error/app_exception.dart';
import '../../../core/error/error_presenter.dart';
import '../data/repositories/shopping_repository_impl.dart';
import '../domain/entities/address.dart';
import 'addresses_state.dart';

class AddressesController extends AsyncNotifier<AddressesState> {
  @override
  Future<AddressesState> build() async {
    return _load();
  }

  Future<void> refresh() async {
    state = const AsyncLoading<AddressesState>();
    state = await AsyncValue.guard<AddressesState>(_load);
  }

  Future<bool> addAddress({
    required String fullName,
    required String line1,
    required String city,
    required String stateName,
    required String zipCode,
    required String country,
    String? label,
    String? phone,
    String? line2,
    String? district,
    String? deliveryInstructions,
    bool defaultAddress = false,
  }) {
    return _mutate(() async {
      await ref
          .read(shoppingRepositoryProvider)
          .addAddress(
            fullName: fullName,
            line1: line1,
            city: city,
            state: stateName,
            zipCode: zipCode,
            country: country,
            label: label,
            phone: phone,
            line2: line2,
            district: district,
            deliveryInstructions: deliveryInstructions,
            defaultAddress: defaultAddress,
          );
      return _fetchAddresses();
    }, successMessage: 'Address added.');
  }

  Future<bool> updateAddress({
    required String addressId,
    required String fullName,
    required String line1,
    required String city,
    required String stateName,
    required String zipCode,
    required String country,
    String? label,
    String? phone,
    String? line2,
    String? district,
    String? deliveryInstructions,
    bool defaultAddress = false,
  }) {
    return _mutate(() async {
      await ref
          .read(shoppingRepositoryProvider)
          .updateAddress(
            addressId: addressId,
            fullName: fullName,
            line1: line1,
            city: city,
            state: stateName,
            zipCode: zipCode,
            country: country,
            label: label,
            phone: phone,
            line2: line2,
            district: district,
            deliveryInstructions: deliveryInstructions,
            defaultAddress: defaultAddress,
          );
      return _fetchAddresses();
    }, successMessage: 'Address updated.');
  }

  Future<bool> deleteAddress(String addressId) {
    return _mutate(() async {
      await ref
          .read(shoppingRepositoryProvider)
          .deleteAddress(addressId: addressId);
      return _fetchAddresses();
    }, successMessage: 'Address deleted.');
  }

  Future<bool> setDefaultAddress(String addressId) {
    return _mutate(() async {
      await ref
          .read(shoppingRepositoryProvider)
          .setDefaultAddress(addressId: addressId);
      return _fetchAddresses();
    }, successMessage: 'Default address updated.');
  }

  void clearMessages() {
    final current = state.valueOrNull;
    if (current == null) {
      return;
    }
    state = AsyncData<AddressesState>(
      current.copyWith(actionMessage: null, actionError: null),
    );
  }

  Future<AddressesState> _load() async {
    final addresses = await _fetchAddresses();
    return AddressesState(items: addresses, isMutating: false);
  }

  Future<bool> _mutate(
    Future<List<Address>> Function() operation, {
    String? successMessage,
  }) async {
    final current = state.valueOrNull;
    if (current != null) {
      state = AsyncData<AddressesState>(
        current.copyWith(
          isMutating: true,
          actionError: null,
          actionMessage: null,
        ),
      );
    }
    try {
      final items = await operation();
      state = AsyncData<AddressesState>(
        AddressesState(
          items: items,
          isMutating: false,
          actionMessage: successMessage,
        ),
      );
      return true;
    } on AppException catch (error) {
      final previous = current ?? const AddressesState.initial();
      state = AsyncData<AddressesState>(
        previous.copyWith(
          isMutating: false,
          actionError: error.message,
          actionMessage: null,
        ),
      );
      return false;
    } on Object catch (error) {
      final previous = current ?? const AddressesState.initial();
      state = AsyncData<AddressesState>(
        previous.copyWith(
          isMutating: false,
          actionError: ErrorPresenter.message(error),
          actionMessage: null,
        ),
      );
      return false;
    }
  }

  Future<List<Address>> _fetchAddresses() async {
    final addresses = await ref.read(shoppingRepositoryProvider).getAddresses();
    final sorted = addresses.toList(growable: false)
      ..sort((a, b) {
        if (a.defaultAddress == b.defaultAddress) {
          return a.displayLabel.compareTo(b.displayLabel);
        }
        return a.defaultAddress ? -1 : 1;
      });
    return sorted;
  }
}

final addressesControllerProvider =
    AsyncNotifierProvider<AddressesController, AddressesState>(
      AddressesController.new,
    );
