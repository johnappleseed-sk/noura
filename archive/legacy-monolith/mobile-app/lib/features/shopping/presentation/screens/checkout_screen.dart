import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../../app/router/app_routes.dart';
import '../../../../core/error/error_presenter.dart';
import '../../../../core/utils/form_validators.dart';
import '../../../../core/widgets/app_error_view.dart';
import '../../../../core/widgets/app_inline_banner.dart';
import '../../../../core/widgets/app_loading_view.dart';
import '../../application/addresses_controller.dart';
import '../../application/cart_controller.dart';
import '../../application/checkout_controller.dart';
import '../../application/checkout_state.dart';
import '../../application/payment_methods_controller.dart';
import '../../application/payment_methods_state.dart';
import '../../domain/entities/address.dart';
import '../../domain/entities/payment_method.dart';

class CheckoutScreen extends ConsumerStatefulWidget {
  const CheckoutScreen({super.key});

  @override
  ConsumerState<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends ConsumerState<CheckoutScreen> {
  final TextEditingController _paymentReferenceController =
      TextEditingController();
  final TextEditingController _couponController = TextEditingController();

  @override
  void dispose() {
    _paymentReferenceController.dispose();
    _couponController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final checkoutAsync = ref.watch(checkoutControllerProvider);
    final addressesAsync = ref.watch(addressesControllerProvider);
    final paymentMethodsAsync = ref.watch(paymentMethodsControllerProvider);
    final formatter = NumberFormat.currency(symbol: '\$');

    return Scaffold(
      appBar: AppBar(title: const Text('Checkout')),
      body: checkoutAsync.when(
        loading: () => const AppLoadingView(message: 'Preparing checkout...'),
        error: (Object error, StackTrace stackTrace) => AppErrorView(
          message: error.toString(),
          onRetry: () =>
              ref.read(checkoutControllerProvider.notifier).refresh(),
        ),
        data: (CheckoutState checkout) {
          if (_paymentReferenceController.text != checkout.paymentReference) {
            _paymentReferenceController.text = checkout.paymentReference;
          }
          if (_couponController.text != checkout.couponCode) {
            _couponController.text = checkout.couponCode;
          }

          final selectedAddress = _resolveSelectedAddress(
            addressesAsync.valueOrNull?.items ?? const <Address>[],
            checkout.selectedAddressId,
          );
          final selectedPaymentMethod = _resolveSelectedPaymentMethod(
            paymentMethodsAsync.valueOrNull?.items ?? const <PaymentMethod>[],
            checkout.paymentReference,
          );

          return ListView(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
            children: [
              if (checkout.errorMessage != null) ...[
                AppInlineBanner(
                  message: checkout.errorMessage!,
                  isError: true,
                  onClose: () => ref
                      .read(checkoutControllerProvider.notifier)
                      .clearMessages(),
                ),
                const SizedBox(height: 10),
              ],
              if (checkout.statusMessage != null) ...[
                AppInlineBanner(
                  message: checkout.statusMessage!,
                  isError: false,
                  onClose: () => ref
                      .read(checkoutControllerProvider.notifier)
                      .clearMessages(),
                ),
                const SizedBox(height: 10),
              ],
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Step: ${checkout.preview.step.isEmpty ? '-' : checkout.preview.step}',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 6),
                      Text(
                        'Next: ${checkout.preview.nextStep.isEmpty ? '-' : checkout.preview.nextStep}',
                      ),
                      const SizedBox(height: 6),
                      Text(
                        checkout.preview.message.isEmpty
                            ? 'Continue to complete shipping, payment, and confirmation.'
                            : checkout.preview.message,
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 10),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Fulfillment',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      DropdownButtonFormField<String>(
                        initialValue: checkout.fulfillmentMethod,
                        items: const [
                          DropdownMenuItem(
                            value: 'DELIVERY',
                            child: Text('Delivery'),
                          ),
                          DropdownMenuItem(
                            value: 'PICKUP',
                            child: Text('Pickup'),
                          ),
                        ],
                        onChanged: checkout.isSubmitting
                            ? null
                            : (value) {
                                if (value == null) {
                                  return;
                                }
                                ref
                                    .read(checkoutControllerProvider.notifier)
                                    .setFulfillmentMethod(value);
                              },
                      ),
                      if (checkout.fulfillmentMethod == 'DELIVERY') ...[
                        const SizedBox(height: 12),
                        Text(
                          selectedAddress == null
                              ? 'No address selected.'
                              : selectedAddress.compactAddress,
                          style: Theme.of(context).textTheme.bodyMedium,
                        ),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 8,
                          runSpacing: 8,
                          children: [
                            FilledButton.tonal(
                              onPressed: checkout.isSubmitting
                                  ? null
                                  : () => _selectAddress(context),
                              child: const Text('Select address'),
                            ),
                            TextButton(
                              onPressed: checkout.isSubmitting
                                  ? null
                                  : () async {
                                      await context.push(
                                        AppRoutes.addressesPath,
                                      );
                                      if (context.mounted) {
                                        await ref
                                            .read(
                                              addressesControllerProvider
                                                  .notifier,
                                            )
                                            .refresh();
                                      }
                                    },
                              child: const Text('Manage addresses'),
                            ),
                          ],
                        ),
                      ],
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 10),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              'Payment',
                              style: Theme.of(context).textTheme.titleMedium,
                            ),
                          ),
                          TextButton(
                            onPressed: checkout.isSubmitting
                                ? null
                                : () => _showPaymentMethodForm(context),
                            child: const Text('Add method'),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      _PaymentMethodsSection(
                        paymentMethodsAsync: paymentMethodsAsync,
                        selectedPaymentReference: checkout.paymentReference,
                        isSubmitting: checkout.isSubmitting,
                        onSelect: (method) {
                          ref
                              .read(checkoutControllerProvider.notifier)
                              .setPaymentReference(method.tokenizedReference);
                          _paymentReferenceController.text =
                              method.tokenizedReference;
                        },
                        onDelete: (methodId) => ref
                            .read(paymentMethodsControllerProvider.notifier)
                            .deletePaymentMethod(methodId),
                      ),
                      const SizedBox(height: 8),
                      TextFormField(
                        controller: _paymentReferenceController,
                        readOnly: selectedPaymentMethod != null,
                        decoration: InputDecoration(
                          labelText: selectedPaymentMethod == null
                              ? 'Payment reference'
                              : 'Payment reference (selected)',
                        ),
                        validator: FormValidators.requiredField,
                        onChanged: (value) => ref
                            .read(checkoutControllerProvider.notifier)
                            .setPaymentReference(value.trim()),
                      ),
                      const SizedBox(height: 8),
                      TextField(
                        controller: _couponController,
                        decoration: const InputDecoration(
                          labelText: 'Coupon code (optional)',
                        ),
                        onChanged: (value) => ref
                            .read(checkoutControllerProvider.notifier)
                            .setCouponCode(value.trim()),
                      ),
                      const SizedBox(height: 8),
                      SwitchListTile(
                        contentPadding: EdgeInsets.zero,
                        title: const Text('B2B Invoice'),
                        value: checkout.b2bInvoice,
                        onChanged: checkout.isSubmitting
                            ? null
                            : (value) => ref
                                  .read(checkoutControllerProvider.notifier)
                                  .setB2bInvoice(value),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 10),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Order Summary',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      ...checkout.preview.cart.items.map(
                        (item) => Padding(
                          padding: const EdgeInsets.only(bottom: 6),
                          child: Row(
                            children: [
                              Expanded(
                                child: Text(
                                  '${item.productName} x${item.quantity}',
                                ),
                              ),
                              Text(formatter.format(item.lineTotal)),
                            ],
                          ),
                        ),
                      ),
                      const Divider(),
                      _SummaryRow(
                        label: 'Subtotal',
                        value: formatter.format(
                          checkout.preview.cart.totals.subtotal,
                        ),
                      ),
                      _SummaryRow(
                        label: 'Discount',
                        value: formatter.format(
                          checkout.preview.cart.totals.discountAmount,
                        ),
                      ),
                      _SummaryRow(
                        label: 'Shipping',
                        value: formatter.format(
                          checkout.preview.cart.totals.shippingAmount,
                        ),
                      ),
                      _SummaryRow(
                        label: 'Total',
                        value: formatter.format(
                          checkout.preview.cart.totals.totalAmount,
                        ),
                        emphasize: true,
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 12),
              FilledButton.tonal(
                onPressed: checkout.isSubmitting
                    ? null
                    : () => ref
                          .read(checkoutControllerProvider.notifier)
                          .submitShippingStep(),
                child: const Text('1. Validate shipping step'),
              ),
              const SizedBox(height: 8),
              FilledButton.tonal(
                onPressed: checkout.isSubmitting
                    ? null
                    : () => ref
                          .read(checkoutControllerProvider.notifier)
                          .submitPaymentStep(),
                child: const Text('2. Validate payment step'),
              ),
              const SizedBox(height: 8),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: checkout.isSubmitting ? null : _placeOrder,
                  child: checkout.isSubmitting
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Text('3. Place order'),
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  Future<void> _selectAddress(BuildContext context) async {
    final selectedAddressId = await context.push<String>(
      '${AppRoutes.addressesPath}?select=1',
    );
    if (selectedAddressId == null || !mounted) {
      return;
    }
    ref
        .read(checkoutControllerProvider.notifier)
        .setSelectedAddress(selectedAddressId);
  }

  Future<void> _showPaymentMethodForm(BuildContext context) async {
    final result = await showDialog<_PaymentMethodFormValue>(
      context: context,
      builder: (context) => const _PaymentMethodFormDialog(),
    );
    if (result == null) {
      return;
    }
    await ref
        .read(paymentMethodsControllerProvider.notifier)
        .addPaymentMethod(
          methodType: result.methodType,
          provider: result.provider,
          tokenizedReference: result.tokenizedReference,
          defaultMethod: result.defaultMethod,
        );
  }

  Future<void> _placeOrder() async {
    final success = await ref
        .read(checkoutControllerProvider.notifier)
        .confirmOrder();
    if (!success || !mounted) {
      return;
    }
    final orderId = ref
        .read(checkoutControllerProvider)
        .valueOrNull
        ?.placedOrder
        ?.id;
    if (orderId == null || orderId.isEmpty) {
      return;
    }
    ref.invalidate(cartControllerProvider);
    context.go(AppRoutes.orderDetailPath.replaceFirst(':orderId', orderId));
  }

  Address? _resolveSelectedAddress(List<Address> addresses, String? addressId) {
    if (addressId == null || addressId.isEmpty) {
      return addresses.cast<Address?>().firstWhere(
        (address) => address?.defaultAddress == true,
        orElse: () => null,
      );
    }
    return addresses.cast<Address?>().firstWhere(
      (address) => address?.id == addressId,
      orElse: () => null,
    );
  }

  PaymentMethod? _resolveSelectedPaymentMethod(
    List<PaymentMethod> paymentMethods,
    String paymentReference,
  ) {
    if (paymentReference.trim().isEmpty) {
      return null;
    }
    return paymentMethods.cast<PaymentMethod?>().firstWhere(
      (method) => method?.tokenizedReference == paymentReference,
      orElse: () => null,
    );
  }
}

class _PaymentMethodsSection extends StatelessWidget {
  const _PaymentMethodsSection({
    required this.paymentMethodsAsync,
    required this.selectedPaymentReference,
    required this.isSubmitting,
    required this.onSelect,
    required this.onDelete,
  });

  final AsyncValue<PaymentMethodsState> paymentMethodsAsync;
  final String selectedPaymentReference;
  final bool isSubmitting;
  final ValueChanged<PaymentMethod> onSelect;
  final ValueChanged<String> onDelete;

  @override
  Widget build(BuildContext context) {
    return paymentMethodsAsync.when(
      loading: () => const LinearProgressIndicator(),
      error: (Object error, StackTrace stackTrace) => Text(
        ErrorPresenter.message(error),
        style: TextStyle(color: Theme.of(context).colorScheme.error),
      ),
      data: (state) {
        if (state.items.isEmpty) {
          return const Text('No saved payment methods. Add one to continue.');
        }
        return Column(
          children: state.items
              .map(
                (method) => ListTile(
                  onTap: isSubmitting ? null : () => onSelect(method),
                  leading: Icon(
                    method.tokenizedReference == selectedPaymentReference
                        ? Icons.radio_button_checked
                        : Icons.radio_button_off,
                  ),
                  title: Text(method.displayName),
                  subtitle: Text(method.maskedReference),
                  trailing: IconButton(
                    onPressed: isSubmitting ? null : () => onDelete(method.id),
                    icon: const Icon(Icons.delete_outline),
                  ),
                ),
              )
              .toList(growable: false),
        );
      },
    );
  }
}

class _PaymentMethodFormDialog extends StatefulWidget {
  const _PaymentMethodFormDialog();

  @override
  State<_PaymentMethodFormDialog> createState() =>
      _PaymentMethodFormDialogState();
}

class _PaymentMethodFormDialogState extends State<_PaymentMethodFormDialog> {
  final _formKey = GlobalKey<FormState>();
  final TextEditingController _providerController = TextEditingController();
  final TextEditingController _tokenController = TextEditingController();
  String _methodType = 'CARD';
  bool _defaultMethod = false;

  @override
  void dispose() {
    _providerController.dispose();
    _tokenController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Add Payment Method'),
      content: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            DropdownButtonFormField<String>(
              initialValue: _methodType,
              items: const [
                DropdownMenuItem(value: 'CARD', child: Text('Card')),
                DropdownMenuItem(value: 'INVOICE', child: Text('Invoice')),
                DropdownMenuItem(value: 'WALLET', child: Text('Wallet')),
              ],
              onChanged: (value) {
                if (value == null) {
                  return;
                }
                setState(() => _methodType = value);
              },
              decoration: const InputDecoration(labelText: 'Method type'),
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _providerController,
              decoration: const InputDecoration(labelText: 'Provider'),
              validator: (value) =>
                  FormValidators.requiredField(value, fieldName: 'Provider'),
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _tokenController,
              decoration: const InputDecoration(labelText: 'Reference token'),
              validator: (value) => FormValidators.requiredField(
                value,
                fieldName: 'Reference token',
              ),
            ),
            const SizedBox(height: 8),
            SwitchListTile(
              contentPadding: EdgeInsets.zero,
              title: const Text('Set as default'),
              value: _defaultMethod,
              onChanged: (value) => setState(() => _defaultMethod = value),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancel'),
        ),
        FilledButton(onPressed: _submit, child: const Text('Save')),
      ],
    );
  }

  void _submit() {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    Navigator.of(context).pop(
      _PaymentMethodFormValue(
        methodType: _methodType,
        provider: _providerController.text.trim(),
        tokenizedReference: _tokenController.text.trim(),
        defaultMethod: _defaultMethod,
      ),
    );
  }
}

class _PaymentMethodFormValue {
  const _PaymentMethodFormValue({
    required this.methodType,
    required this.provider,
    required this.tokenizedReference,
    required this.defaultMethod,
  });

  final String methodType;
  final String provider;
  final String tokenizedReference;
  final bool defaultMethod;
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({
    required this.label,
    required this.value,
    this.emphasize = false,
  });

  final String label;
  final String value;
  final bool emphasize;

  @override
  Widget build(BuildContext context) {
    final style = emphasize
        ? Theme.of(
            context,
          ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700)
        : Theme.of(context).textTheme.bodyMedium;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        children: [
          Text(label, style: style),
          const Spacer(),
          Text(value, style: style),
        ],
      ),
    );
  }
}
