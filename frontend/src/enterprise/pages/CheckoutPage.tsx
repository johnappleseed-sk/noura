import { FormEvent, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { CartItem } from '@/components/cart/CartItem'
import { Seo } from '@/components/common/Seo'
import {
  clearCart,
  removeFromCart,
  selectCartTotals,
  updateQuantity,
} from '@/features/cart/cartSlice'
import { formatCurrency } from '@/utils/currency'

type CheckoutStep = 1 | 2 | 3 | 4
type FulfillmentMethod = 'pickup' | 'delivery'

interface ShippingForm {
  fullName: string
  email: string
  company: string
  address: string
  city: string
  state: string
  country: string
  zipCode: string
}

interface PaymentForm {
  method: 'card' | 'invoice'
  cardHolder: string
  cardNumber: string
  expiry: string
  cvc: string
}

type ShippingErrors = Partial<Record<keyof ShippingForm, string>>
type PaymentErrors = Partial<Record<keyof PaymentForm, string>>

/**
 * Renders the CheckoutPage component.
 *
 * @returns The rendered component tree.
 */
export const CheckoutPage = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const user = useAppSelector((state) => state.auth.user)
  const selectedStore = useAppSelector((state) => state.stores.selectedStoreDetails)
  const items = useAppSelector((state) => state.cart.items)
  const totals = useAppSelector(selectCartTotals)
  const [step, setStep] = useState<CheckoutStep>(1)
  const [orderPlaced, setOrderPlaced] = useState(false)
  const [orderId, setOrderId] = useState<string | null>(null)
  const [shippingForm, setShippingForm] = useState<ShippingForm>({
    fullName: user?.fullName ?? '',
    email: user?.email ?? '',
    company: user?.role === 'b2b' ? 'Noura Procurement' : '',
    address: '',
    city: '',
    state: '',
    country: 'United States',
    zipCode: '',
  })
  const [paymentForm, setPaymentForm] = useState<PaymentForm>({
    method: user?.role === 'b2b' ? 'invoice' : 'card',
    cardHolder: '',
    cardNumber: '',
    expiry: '',
    cvc: '',
  })
  const [shippingErrors, setShippingErrors] = useState<ShippingErrors>({})
  const [paymentErrors, setPaymentErrors] = useState<PaymentErrors>({})
  const [fulfillmentMethod, setFulfillmentMethod] = useState<FulfillmentMethod>('pickup')
  const [pickupInstructions, setPickupInstructions] = useState('Bring a photo ID and order confirmation email.')
  const [checkoutMessage, setCheckoutMessage] = useState('')

  const steps = useMemo(
    () => [
      { id: 1, label: 'Cart Review' },
      { id: 2, label: 'Shipping' },
      { id: 3, label: 'Payment' },
      { id: 4, label: 'Confirm' },
    ],
    [],
  )

  /**
   * Handles validate shipping.
   */
  const validateShipping = (): boolean => {
    const errors: ShippingErrors = {}
    if (fulfillmentMethod === 'pickup' && !selectedStore) {
      setCheckoutMessage('Select a store before continuing with pickup.')
      setShippingErrors(errors)
      return false
    }
    if (shippingForm.fullName.trim().length < 2) {
      errors.fullName = 'Enter full name.'
    }
    if (!/^\S+@\S+\.\S+$/.test(shippingForm.email.trim())) {
      errors.email = 'Enter a valid email.'
    }
    if (shippingForm.address.trim().length < 6) {
      errors.address = 'Enter a valid street address.'
    }
    if (!shippingForm.city.trim()) {
      errors.city = 'City is required.'
    }
    if (!shippingForm.state.trim()) {
      errors.state = 'State/Province is required.'
    }
    if (!shippingForm.country.trim()) {
      errors.country = 'Country is required.'
    }
    if (!/^[A-Za-z0-9 -]{4,10}$/.test(shippingForm.zipCode.trim())) {
      errors.zipCode = 'Enter a valid ZIP/postal code.'
    }
    setShippingErrors(errors)
    if (Object.keys(errors).length === 0) {
      setCheckoutMessage('')
    }
    return Object.keys(errors).length === 0
  }

  /**
   * Validates validate payment.
   *
   * @returns The result of validate payment.
   */
  const validatePayment = (): boolean => {
    const errors: PaymentErrors = {}
    if (paymentForm.method === 'invoice') {
      setPaymentErrors(errors)
      return true
    }

    const normalizedCard = paymentForm.cardNumber.replace(/\s+/g, '')
    if (paymentForm.cardHolder.trim().length < 2) {
      errors.cardHolder = 'Card holder is required.'
    }
    if (!/^\d{13,19}$/.test(normalizedCard)) {
      errors.cardNumber = 'Card number must be 13 to 19 digits.'
    }
    if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(paymentForm.expiry.trim())) {
      errors.expiry = 'Use MM/YY format.'
    }
    if (!/^\d{3,4}$/.test(paymentForm.cvc.trim())) {
      errors.cvc = 'Enter a valid CVC.'
    }

    setPaymentErrors(errors)
    return Object.keys(errors).length === 0
  }

  /**
   * Executes go to next step.
   *
   * @returns No value.
   */
  const goToNextStep = (): void => {
    if (step === 1) {
      if (items.length === 0) {
        return
      }
      setStep(2)
      return
    }
    if (step === 2) {
      if (!validateShipping()) {
        return
      }
      setStep(3)
      return
    }
    if (step === 3) {
      if (!validatePayment()) {
        return
      }
      setStep(4)
    }
  }

  /**
   * Executes submit order.
   *
   * @param event The event value.
   * @returns No value.
   */
  const submitOrder = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()

    if (!validateShipping() || !validatePayment()) {
      return
    }

    const generatedOrderId = `ord-${new Date().getFullYear()}-${Math.floor(100000 + Math.random() * 900000)}`
    setOrderId(generatedOrderId)
    dispatch(clearCart())
    setCheckoutMessage(
      `Order payload prepared with storeId: ${selectedStore?.id ?? 'none'} and fulfillment: ${fulfillmentMethod}.`,
    )
    setOrderPlaced(true)
  }

  if (items.length === 0 && !orderPlaced) {
    return (
      <section className="panel p-8 text-center">
        <Seo description="Secure checkout with shipping, payment, and order review steps." title="Checkout" />
        <h1 className="text-2xl font-bold">Your cart is empty</h1>
        <p className="m3-subtitle mt-2 text-sm">Add products before entering checkout.</p>
        <Link className="m3-btn m3-btn-filled mt-4" to="/products">
          Go to products
        </Link>
      </section>
    )
  }

  if (orderPlaced) {
    return (
      <section className="panel p-10 text-center">
        <Seo description="Order submitted successfully." title="Checkout Complete" />
        <h1 className="text-2xl font-bold text-emerald-700 dark:text-emerald-300">Order placed successfully</h1>
        <p className="m3-subtitle mt-2 text-sm">
          Reference: <span className="font-semibold">{orderId}</span>
        </p>
        <p className="m3-subtitle mt-2 text-sm">
          Fulfillment: {fulfillmentMethod === 'pickup' ? `Pickup at ${selectedStore?.name ?? 'store not selected'}` : 'Delivery'}
        </p>
        <p className="m3-subtitle mt-2 text-sm">
          Stripe payment flow is integrated as UI only. Connect Stripe Elements for live transactions.
        </p>
        <div className="mt-4 flex flex-wrap justify-center gap-2">
          <Link className="m3-btn m3-btn-outlined" to="/products">
            Continue shopping
          </Link>
          <Link className="m3-btn m3-btn-filled" to={user ? '/account' : '/login'}>
            {user ? 'View account' : 'Sign in to track order'}
          </Link>
        </div>
      </section>
    )
  }

  return (
    <div className="space-y-6">
      <Seo description="Multi-step checkout flow with Stripe payment form UI." title="Checkout" />

      <header className="panel p-6">
        <h1 className="m3-title">Checkout</h1>
        <p className="m3-subtitle mt-2 text-sm">
          {user ? 'Signed in checkout with saved profile support.' : 'Guest checkout enabled. Sign in is optional.'}
        </p>
        <div className="mt-4 flex flex-wrap items-center gap-2">
          {steps.map((item) => (
            <div
              className={`m3-chip text-xs font-semibold uppercase tracking-wide ${
                step === item.id ? 'm3-chip-active' : ''
              }`}
              key={item.id}
            >
              {item.label}
            </div>
          ))}
        </div>
        {checkoutMessage ? (
          <p className="mt-3 rounded-2xl p-2 text-xs" style={{ background: 'var(--m3-surface-container-high)' }}>
            {checkoutMessage}
          </p>
        ) : null}
      </header>

      <form className="grid gap-6 lg:grid-cols-[1fr_330px]" onSubmit={submitOrder}>
        <section className="panel space-y-6 p-6">
          {step === 1 ? (
            <div className="space-y-3">
              <h2 className="text-lg font-semibold">Review items</h2>
              <p className="m3-subtitle text-sm">
                Update quantity or remove items before shipping and payment.
              </p>
              {items.map((item) => (
                <CartItem
                  item={item}
                  key={item.productId}
                  onRemove={() => dispatch(removeFromCart(item.productId))}
                  onUpdateQuantity={(quantity) => dispatch(updateQuantity({ productId: item.productId, quantity }))}
                />
              ))}
              <p className="rounded-2xl p-3 text-xs" style={{ background: 'var(--m3-surface-container-high)' }}>
                {totals.shipping === 0
                  ? 'Free shipping unlocked.'
                  : `Add ${formatCurrency(Math.max(0, (selectedStore?.freeShippingThreshold ?? 300) - totals.subtotal))} more for free shipping.`}
              </p>
            </div>
          ) : null}

          {step === 2 ? (
            <div className="space-y-3">
              <h2 className="text-lg font-semibold">Shipping details</h2>
              <div className="rounded-2xl border p-3" style={{ borderColor: 'var(--m3-outline-variant)' }}>
                <p className="text-sm font-semibold">Fulfillment method</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  <button
                    className={`m3-btn !h-10 !rounded-xl !px-3 !py-1 text-xs ${fulfillmentMethod === 'pickup' ? 'm3-btn-filled' : 'm3-btn-outlined'}`}
                    onClick={() => setFulfillmentMethod('pickup')}
                    type="button"
                  >
                    Pickup from store
                  </button>
                  <button
                    className={`m3-btn !h-10 !rounded-xl !px-3 !py-1 text-xs ${fulfillmentMethod === 'delivery' ? 'm3-btn-filled' : 'm3-btn-outlined'}`}
                    onClick={() => setFulfillmentMethod('delivery')}
                    type="button"
                  >
                    Delivery
                  </button>
                </div>
                {fulfillmentMethod === 'pickup' ? (
                  <div className="mt-2 text-xs">
                    <p className="font-semibold">
                      {selectedStore ? `Pickup store: ${selectedStore.name}` : 'No store selected'}
                    </p>
                    {selectedStore ? (
                      <>
                        <p className="m3-subtitle">{selectedStore.addressLine1}, {selectedStore.city}</p>
                        <p className="m3-subtitle">Pickup hours: {selectedStore.hoursSummary}</p>
                      </>
                    ) : null}
                    <label className="mt-2 block text-xs">
                      Pickup instructions
                      <textarea
                        className="m3-input mt-1 !h-20 !rounded-2xl !py-2"
                        onChange={(event) => setPickupInstructions(event.target.value)}
                        value={pickupInstructions}
                      />
                    </label>
                    <Link className="m3-link mt-1 inline-flex" state={{ from: '/checkout' }} to="/stores">
                      Change store
                    </Link>
                  </div>
                ) : (
                  <p className="m3-subtitle mt-2 text-xs">
                    Estimated delivery: {selectedStore ? '2-4 business days from your selected store region.' : '3-5 business days.'}
                  </p>
                )}
              </div>
              <div className="grid gap-3 sm:grid-cols-2">
                <label className="block text-sm sm:col-span-2">
                  Full name
                  <input
                    className="m3-input mt-1"
                    onChange={(event) => setShippingForm((state) => ({ ...state, fullName: event.target.value }))}
                    placeholder="Full name"
                    type="text"
                    value={shippingForm.fullName}
                  />
                  {shippingErrors.fullName ? <p className="mt-1 text-xs text-rose-600">{shippingErrors.fullName}</p> : null}
                </label>

                <label className="block text-sm sm:col-span-2">
                  Email
                  <input
                    className="m3-input mt-1"
                    onChange={(event) => setShippingForm((state) => ({ ...state, email: event.target.value }))}
                    placeholder="Email"
                    type="email"
                    value={shippingForm.email}
                  />
                  {shippingErrors.email ? <p className="mt-1 text-xs text-rose-600">{shippingErrors.email}</p> : null}
                </label>

                <label className="block text-sm sm:col-span-2">
                  Company (optional)
                  <input
                    className="m3-input mt-1"
                    onChange={(event) => setShippingForm((state) => ({ ...state, company: event.target.value }))}
                    placeholder="Company"
                    type="text"
                    value={shippingForm.company}
                  />
                </label>

                <label className="block text-sm sm:col-span-2">
                  Address
                  <input
                    className="m3-input mt-1"
                    onChange={(event) => setShippingForm((state) => ({ ...state, address: event.target.value }))}
                    placeholder="Street address"
                    type="text"
                    value={shippingForm.address}
                  />
                  {shippingErrors.address ? <p className="mt-1 text-xs text-rose-600">{shippingErrors.address}</p> : null}
                </label>

                <label className="block text-sm">
                  City
                  <input
                    className="m3-input mt-1"
                    onChange={(event) => setShippingForm((state) => ({ ...state, city: event.target.value }))}
                    placeholder="City"
                    type="text"
                    value={shippingForm.city}
                  />
                  {shippingErrors.city ? <p className="mt-1 text-xs text-rose-600">{shippingErrors.city}</p> : null}
                </label>

                <label className="block text-sm">
                  State / Province
                  <input
                    className="m3-input mt-1"
                    onChange={(event) => setShippingForm((state) => ({ ...state, state: event.target.value }))}
                    placeholder="State"
                    type="text"
                    value={shippingForm.state}
                  />
                  {shippingErrors.state ? <p className="mt-1 text-xs text-rose-600">{shippingErrors.state}</p> : null}
                </label>

                <label className="block text-sm">
                  Country
                  <input
                    className="m3-input mt-1"
                    onChange={(event) => setShippingForm((state) => ({ ...state, country: event.target.value }))}
                    placeholder="Country"
                    type="text"
                    value={shippingForm.country}
                  />
                  {shippingErrors.country ? <p className="mt-1 text-xs text-rose-600">{shippingErrors.country}</p> : null}
                </label>

                <label className="block text-sm">
                  ZIP / Postal code
                  <input
                    className="m3-input mt-1"
                    onChange={(event) => setShippingForm((state) => ({ ...state, zipCode: event.target.value }))}
                    placeholder="ZIP code"
                    type="text"
                    value={shippingForm.zipCode}
                  />
                  {shippingErrors.zipCode ? <p className="mt-1 text-xs text-rose-600">{shippingErrors.zipCode}</p> : null}
                </label>
              </div>
            </div>
          ) : null}

          {step === 3 ? (
            <div className="space-y-3">
              <h2 className="text-lg font-semibold">Payment</h2>
              <p className="rounded-2xl p-3 text-xs" style={{ background: 'var(--m3-primary-container)', color: 'var(--m3-on-primary-container)' }}>
                Stripe integration UI only. Replace with Stripe Elements + PaymentIntent on the backend.
              </p>

              {user?.role === 'b2b' ? (
                <div className="grid gap-2 sm:grid-cols-2">
                  <button
                    aria-pressed={paymentForm.method === 'invoice'}
                    className={`m3-btn !h-11 !justify-start !rounded-2xl ${
                      paymentForm.method === 'invoice' ? 'm3-btn-filled' : 'm3-btn-outlined'
                    }`}
                    onClick={() => setPaymentForm((state) => ({ ...state, method: 'invoice' }))}
                    type="button"
                  >
                    Invoice terms
                  </button>
                  <button
                    aria-pressed={paymentForm.method === 'card'}
                    className={`m3-btn !h-11 !justify-start !rounded-2xl ${
                      paymentForm.method === 'card' ? 'm3-btn-filled' : 'm3-btn-outlined'
                    }`}
                    onClick={() => setPaymentForm((state) => ({ ...state, method: 'card' }))}
                    type="button"
                  >
                    Card payment
                  </button>
                </div>
              ) : null}

              {paymentForm.method === 'invoice' ? (
                <p className="rounded-2xl p-3 text-sm" style={{ background: 'var(--m3-surface-container-high)' }}>
                  Invoice workflow selected. PO validation and approval routing continue after order submission.
                </p>
              ) : (
                <div className="grid gap-3 sm:grid-cols-2">
                  <label className="block text-sm sm:col-span-2">
                    Card holder
                    <input
                      className="m3-input mt-1"
                      onChange={(event) => setPaymentForm((state) => ({ ...state, cardHolder: event.target.value }))}
                      placeholder="Card holder name"
                      type="text"
                      value={paymentForm.cardHolder}
                    />
                    {paymentErrors.cardHolder ? <p className="mt-1 text-xs text-rose-600">{paymentErrors.cardHolder}</p> : null}
                  </label>

                  <label className="block text-sm sm:col-span-2">
                    Card number
                    <input
                      className="m3-input mt-1"
                      maxLength={19}
                      onChange={(event) => setPaymentForm((state) => ({ ...state, cardNumber: event.target.value }))}
                      placeholder="4242424242424242"
                      type="text"
                      value={paymentForm.cardNumber}
                    />
                    {paymentErrors.cardNumber ? <p className="mt-1 text-xs text-rose-600">{paymentErrors.cardNumber}</p> : null}
                  </label>

                  <label className="block text-sm">
                    Expiry
                    <input
                      className="m3-input mt-1"
                      onChange={(event) => setPaymentForm((state) => ({ ...state, expiry: event.target.value }))}
                      placeholder="MM/YY"
                      type="text"
                      value={paymentForm.expiry}
                    />
                    {paymentErrors.expiry ? <p className="mt-1 text-xs text-rose-600">{paymentErrors.expiry}</p> : null}
                  </label>

                  <label className="block text-sm">
                    CVC
                    <input
                      className="m3-input mt-1"
                      onChange={(event) => setPaymentForm((state) => ({ ...state, cvc: event.target.value }))}
                      placeholder="CVC"
                      type="password"
                      value={paymentForm.cvc}
                    />
                    {paymentErrors.cvc ? <p className="mt-1 text-xs text-rose-600">{paymentErrors.cvc}</p> : null}
                  </label>
                </div>
              )}
            </div>
          ) : null}

          {step === 4 ? (
            <div className="space-y-4 text-sm">
              <h2 className="text-lg font-semibold">Confirm order</h2>
              <div className="rounded-3xl border p-4" style={{ borderColor: 'var(--m3-outline-variant)' }}>
                <p className="font-medium">{shippingForm.fullName}</p>
                <p>{shippingForm.email}</p>
                {shippingForm.company ? <p>{shippingForm.company}</p> : null}
                <p>{shippingForm.address}</p>
                <p>
                  {shippingForm.city}, {shippingForm.state} {shippingForm.zipCode}
                </p>
                <p>{shippingForm.country}</p>
              </div>

              <div className="rounded-3xl border p-4" style={{ borderColor: 'var(--m3-outline-variant)' }}>
                <p className="font-medium">Payment</p>
                {paymentForm.method === 'invoice' ? (
                  <p className="m3-subtitle">Invoice terms (B2B approval flow)</p>
                ) : (
                  <p className="m3-subtitle">Card ending in {paymentForm.cardNumber.slice(-4) || '....'}</p>
                )}
              </div>

              <div className="rounded-3xl border p-4" style={{ borderColor: 'var(--m3-outline-variant)' }}>
                <p className="font-medium">Fulfillment</p>
                {fulfillmentMethod === 'pickup' ? (
                  <>
                    <p className="m3-subtitle">Pickup at {selectedStore?.name ?? 'No store selected'}.</p>
                    <p className="m3-subtitle">Instructions: {pickupInstructions}</p>
                  </>
                ) : (
                  <p className="m3-subtitle">Delivery to shipping address.</p>
                )}
              </div>

              <button className="m3-btn m3-btn-filled" type="submit">
                Place order
              </button>
            </div>
          ) : null}

          <div className="flex items-center justify-between border-t pt-4" style={{ borderColor: 'var(--m3-outline-variant)' }}>
            <button
              className="m3-btn m3-btn-outlined"
              disabled={step === 1}
              onClick={() => setStep((current) => (Math.max(1, current - 1) as CheckoutStep))}
              type="button"
            >
              Back
            </button>

            {step < 4 ? (
              <button className="m3-btn m3-btn-filled" onClick={goToNextStep} type="button">
                Next
              </button>
            ) : null}
          </div>
        </section>

        <aside className="panel h-fit space-y-3 p-5 lg:sticky lg:top-24">
          <h2 className="text-lg font-semibold">Summary</h2>
          <p className="m3-subtitle text-xs">{items.length} items</p>
          {items.map((item) => (
            <div className="flex items-center justify-between text-sm" key={item.productId}>
              <span>
                {item.name} x{item.quantity}
              </span>
              <span>{formatCurrency(item.price * item.quantity)}</span>
            </div>
          ))}
          <div className="space-y-1 border-t pt-3 text-sm" style={{ borderColor: 'var(--m3-outline-variant)' }}>
            <p className="m3-subtitle text-xs">
              Prices and availability based on {selectedStore?.name ?? 'selected store'}.
            </p>
            <div className="flex justify-between">
              <span>Subtotal</span>
              <span>{formatCurrency(totals.subtotal)}</span>
            </div>
            <div className="flex justify-between">
              <span>Discount</span>
              <span>-{formatCurrency(totals.discount)}</span>
            </div>
            <div className="flex justify-between">
              <span>Shipping</span>
              <span>{totals.shipping === 0 ? 'Free' : formatCurrency(totals.shipping)}</span>
            </div>
            <div className="flex justify-between pt-1 text-base font-bold">
              <span>Total</span>
              <span>{formatCurrency(totals.total)}</span>
            </div>
          </div>
        </aside>
      </form>
    </div>
  )
}
