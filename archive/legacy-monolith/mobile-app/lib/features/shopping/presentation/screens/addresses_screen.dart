import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/utils/form_validators.dart';
import '../../../../core/widgets/app_empty_view.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_inline_banner.dart';
import '../../../../core/widgets/app_loading_view.dart';
import '../../application/addresses_controller.dart';
import '../../application/addresses_state.dart';
import '../../domain/entities/address.dart';

class AddressesScreen extends ConsumerWidget {
  const AddressesScreen({super.key, this.selectionMode = false});

  final bool selectionMode;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final addressesAsync = ref.watch(addressesControllerProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(selectionMode ? 'Select Address' : 'Addresses'),
        actions: [
          IconButton(
            onPressed: () => _showAddressForm(context, ref),
            icon: const Icon(Icons.add_location_alt_outlined),
          ),
        ],
      ),
      body: addressesAsync.when(
        loading: () => const AppLoadingView(message: 'Loading addresses...'),
        error: (Object error, StackTrace stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () =>
              ref.read(addressesControllerProvider.notifier).refresh(),
        ),
        data: (AddressesState state) {
          if (state.items.isEmpty) {
            return AppEmptyView(
              title: 'No addresses saved.',
              subtitle: 'Add a delivery address to continue checkout.',
              actionLabel: 'Add address',
              onActionPressed: () => _showAddressForm(context, ref),
            );
          }

          return RefreshIndicator(
            onRefresh: () =>
                ref.read(addressesControllerProvider.notifier).refresh(),
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
              children: [
                if (state.actionError != null) ...[
                  AppInlineBanner(
                    message: state.actionError!,
                    isError: true,
                    onClose: () => ref
                        .read(addressesControllerProvider.notifier)
                        .clearMessages(),
                  ),
                  const SizedBox(height: 10),
                ],
                if (state.actionMessage != null) ...[
                  AppInlineBanner(
                    message: state.actionMessage!,
                    isError: false,
                    onClose: () => ref
                        .read(addressesControllerProvider.notifier)
                        .clearMessages(),
                  ),
                  const SizedBox(height: 10),
                ],
                ...state.items.map(
                  (address) => Card(
                    margin: const EdgeInsets.only(bottom: 10),
                    child: ListTile(
                      onTap: selectionMode
                          ? () => Navigator.of(context).pop(address.id)
                          : null,
                      title: Row(
                        children: [
                          Expanded(child: Text(address.displayLabel)),
                          if (address.defaultAddress)
                            const Chip(
                              label: Text('Default'),
                              visualDensity: VisualDensity.compact,
                            ),
                        ],
                      ),
                      subtitle: Text(
                        '${address.fullName}\n${address.compactAddress}',
                      ),
                      isThreeLine: true,
                      trailing: PopupMenuButton<String>(
                        onSelected: (action) {
                          switch (action) {
                            case 'set-default':
                              ref
                                  .read(addressesControllerProvider.notifier)
                                  .setDefaultAddress(address.id);
                              break;
                            case 'edit':
                              _showAddressForm(context, ref, existing: address);
                              break;
                            case 'delete':
                              ref
                                  .read(addressesControllerProvider.notifier)
                                  .deleteAddress(address.id);
                              break;
                          }
                        },
                        itemBuilder: (context) => [
                          if (!address.defaultAddress)
                            const PopupMenuItem<String>(
                              value: 'set-default',
                              child: Text('Set as default'),
                            ),
                          const PopupMenuItem<String>(
                            value: 'edit',
                            child: Text('Edit'),
                          ),
                          const PopupMenuItem<String>(
                            value: 'delete',
                            child: Text('Delete'),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Future<void> _showAddressForm(
    BuildContext context,
    WidgetRef ref, {
    Address? existing,
  }) async {
    final result = await showModalBottomSheet<_AddressFormValue>(
      context: context,
      isScrollControlled: true,
      builder: (context) => _AddressFormSheet(existing: existing),
    );
    if (result == null) {
      return;
    }

    if (existing == null) {
      await ref
          .read(addressesControllerProvider.notifier)
          .addAddress(
            fullName: result.fullName,
            line1: result.line1,
            city: result.city,
            stateName: result.stateName,
            zipCode: result.zipCode,
            country: result.country,
            label: result.label,
            phone: result.phone,
            line2: result.line2,
            district: result.district,
            deliveryInstructions: result.deliveryInstructions,
            defaultAddress: result.defaultAddress,
          );
      return;
    }

    await ref
        .read(addressesControllerProvider.notifier)
        .updateAddress(
          addressId: existing.id,
          fullName: result.fullName,
          line1: result.line1,
          city: result.city,
          stateName: result.stateName,
          zipCode: result.zipCode,
          country: result.country,
          label: result.label,
          phone: result.phone,
          line2: result.line2,
          district: result.district,
          deliveryInstructions: result.deliveryInstructions,
          defaultAddress: result.defaultAddress,
        );
  }
}

class _AddressFormSheet extends StatefulWidget {
  const _AddressFormSheet({this.existing});

  final Address? existing;

  @override
  State<_AddressFormSheet> createState() => _AddressFormSheetState();
}

class _AddressFormSheetState extends State<_AddressFormSheet> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _labelController;
  late final TextEditingController _fullNameController;
  late final TextEditingController _phoneController;
  late final TextEditingController _line1Controller;
  late final TextEditingController _line2Controller;
  late final TextEditingController _districtController;
  late final TextEditingController _cityController;
  late final TextEditingController _stateController;
  late final TextEditingController _zipController;
  late final TextEditingController _countryController;
  late final TextEditingController _instructionsController;
  late bool _defaultAddress;

  @override
  void initState() {
    super.initState();
    final existing = widget.existing;
    _labelController = TextEditingController(text: existing?.label ?? '');
    _fullNameController = TextEditingController(text: existing?.fullName ?? '');
    _phoneController = TextEditingController(text: existing?.phone ?? '');
    _line1Controller = TextEditingController(text: existing?.line1 ?? '');
    _line2Controller = TextEditingController(text: existing?.line2 ?? '');
    _districtController = TextEditingController(text: existing?.district ?? '');
    _cityController = TextEditingController(text: existing?.city ?? '');
    _stateController = TextEditingController(text: existing?.state ?? '');
    _zipController = TextEditingController(text: existing?.zipCode ?? '');
    _countryController = TextEditingController(text: existing?.country ?? '');
    _instructionsController = TextEditingController(
      text: existing?.deliveryInstructions ?? '',
    );
    _defaultAddress = existing?.defaultAddress ?? false;
  }

  @override
  void dispose() {
    _labelController.dispose();
    _fullNameController.dispose();
    _phoneController.dispose();
    _line1Controller.dispose();
    _line2Controller.dispose();
    _districtController.dispose();
    _cityController.dispose();
    _stateController.dispose();
    _zipController.dispose();
    _countryController.dispose();
    _instructionsController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.only(
          left: 16,
          right: 16,
          top: 16,
          bottom: 16 + MediaQuery.of(context).viewInsets.bottom,
        ),
        child: Form(
          key: _formKey,
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  widget.existing == null ? 'Add address' : 'Edit address',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _labelController,
                  decoration: const InputDecoration(labelText: 'Label'),
                ),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _fullNameController,
                  decoration: const InputDecoration(labelText: 'Full name'),
                  validator: (value) => FormValidators.requiredField(
                    value,
                    fieldName: 'Full name',
                  ),
                ),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _phoneController,
                  decoration: const InputDecoration(labelText: 'Phone'),
                  validator: FormValidators.phone,
                ),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _line1Controller,
                  decoration: const InputDecoration(
                    labelText: 'Address line 1',
                  ),
                  validator: (value) => FormValidators.requiredField(
                    value,
                    fieldName: 'Address line 1',
                  ),
                ),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _line2Controller,
                  decoration: const InputDecoration(
                    labelText: 'Address line 2',
                  ),
                ),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _districtController,
                  decoration: const InputDecoration(labelText: 'District'),
                ),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _cityController,
                  decoration: const InputDecoration(labelText: 'City'),
                  validator: (value) =>
                      FormValidators.requiredField(value, fieldName: 'City'),
                ),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _stateController,
                  decoration: const InputDecoration(labelText: 'State'),
                  validator: (value) =>
                      FormValidators.requiredField(value, fieldName: 'State'),
                ),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _zipController,
                  decoration: const InputDecoration(labelText: 'Zip code'),
                  validator: FormValidators.zipCode,
                ),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _countryController,
                  decoration: const InputDecoration(labelText: 'Country'),
                  validator: (value) =>
                      FormValidators.requiredField(value, fieldName: 'Country'),
                ),
                const SizedBox(height: 8),
                TextFormField(
                  controller: _instructionsController,
                  decoration: const InputDecoration(
                    labelText: 'Delivery instructions',
                  ),
                ),
                const SizedBox(height: 8),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Set as default'),
                  value: _defaultAddress,
                  onChanged: (value) => setState(() => _defaultAddress = value),
                ),
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton(
                    onPressed: _submit,
                    child: Text(widget.existing == null ? 'Add' : 'Save'),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  void _submit() {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    Navigator.of(context).pop(
      _AddressFormValue(
        label: _labelController.text.trim(),
        fullName: _fullNameController.text.trim(),
        phone: _phoneController.text.trim(),
        line1: _line1Controller.text.trim(),
        line2: _line2Controller.text.trim(),
        district: _districtController.text.trim(),
        city: _cityController.text.trim(),
        stateName: _stateController.text.trim(),
        zipCode: _zipController.text.trim(),
        country: _countryController.text.trim(),
        deliveryInstructions: _instructionsController.text.trim(),
        defaultAddress: _defaultAddress,
      ),
    );
  }
}

class _AddressFormValue {
  const _AddressFormValue({
    required this.label,
    required this.fullName,
    required this.phone,
    required this.line1,
    required this.line2,
    required this.district,
    required this.city,
    required this.stateName,
    required this.zipCode,
    required this.country,
    required this.deliveryInstructions,
    required this.defaultAddress,
  });

  final String label;
  final String fullName;
  final String phone;
  final String line1;
  final String line2;
  final String district;
  final String city;
  final String stateName;
  final String zipCode;
  final String country;
  final String deliveryInstructions;
  final bool defaultAddress;
}
