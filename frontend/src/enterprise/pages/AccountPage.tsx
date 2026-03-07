import { useEffect, useMemo, useState } from 'react'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import {
  accountApi,
  AddressDto,
  AddressRequest,
  ApprovalDto,
  CompanyProfileRequest,
  PaymentMethodDto,
} from '@/api/accountApi'
import { Seo } from '@/components/common/Seo'
import { updateProfile } from '@/features/auth/authSlice'
import { addBulkItems } from '@/features/cart/cartSlice'
import { fetchAdminOrders, fetchMyOrders } from '@/features/orders/ordersSlice'
import { getProductStockAtStore } from '@/lib/productAvailability'
import { formatCurrency } from '@/utils/currency'

type AccountTab = 'profile' | 'orders' | 'addresses' | 'payments' | 'company' | 'approvals'

interface AddressEntry {
  id: string
  label: string
  fullName: string
  line1: string
  city: string
  state: string
  zipCode: string
  country: string
  isDefault?: boolean
  preferredStoreId?: string | null
}

interface PaymentMethodEntry {
  id: string
  brand: string
  last4: string
  expiry: string
  isDefault?: boolean
}

interface ApprovalEntry {
  id: string
  requesterId: string
  orderId?: string | null
  total: number
  status: 'pending' | 'approved' | 'rejected'
  reviewerNotes?: string
}

const toAddressEntry = (address: AddressDto): AddressEntry => ({
  id: address.id,
  label: address.label ?? '',
  fullName: address.fullName,
  line1: address.line1,
  city: address.city,
  state: address.state,
  zipCode: address.zipCode,
  country: address.country,
  isDefault: address.defaultAddress,
  preferredStoreId: null,
})

const parsePaymentReference = (reference: string): { last4: string; expiry: string } => {
  const [base, expiry] = reference.split('|')
  const last4 = (base.match(/(\d{4})$/)?.[1] ?? base.slice(-4) ?? '0000').padStart(4, '0')
  return { last4, expiry: expiry ?? 'N/A' }
}

const toPaymentMethodEntry = (paymentMethod: PaymentMethodDto): PaymentMethodEntry => {
  const parsed = parsePaymentReference(paymentMethod.tokenizedReference)
  return {
    id: paymentMethod.id,
    brand: paymentMethod.provider,
    last4: parsed.last4,
    expiry: parsed.expiry,
    isDefault: paymentMethod.defaultMethod,
  }
}

const toApprovalEntry = (approval: ApprovalDto): ApprovalEntry => ({
  id: approval.id,
  requesterId: approval.requesterId,
  orderId: approval.orderId,
  total: Number(approval.amount),
  status: approval.status.toLowerCase() as ApprovalEntry['status'],
  reviewerNotes: approval.reviewerNotes,
})

/**
 * Renders the AccountPage component.
 *
 * @returns The rendered component tree.
 */
export const AccountPage = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const user = useAppSelector((state) => state.auth.user)
  const userId = user?.id ?? null
  const userRole = user?.role ?? null
  const products = useAppSelector((state) => state.products.items)
  const stores = useAppSelector((state) => state.stores.availableStores)
  const selectedStore = useAppSelector((state) => state.stores.selectedStoreDetails)
  const ordersState = useAppSelector((state) => state.orders)
  const [activeTab, setActiveTab] = useState<AccountTab>('profile')
  const [fullName, setFullName] = useState(user?.fullName ?? '')
  const [phone, setPhone] = useState(user?.phone ?? '')
  const [quickSku, setQuickSku] = useState('')
  const [feedback, setFeedback] = useState<string | null>(null)
  const [loadingAccountData, setLoadingAccountData] = useState(false)
  const [orderSearch, setOrderSearch] = useState('')
  const [orderStatusFilter, setOrderStatusFilter] = useState<string>('all')

  const [addresses, setAddresses] = useState<AddressEntry[]>([])
  const [newAddress, setNewAddress] = useState<Omit<AddressEntry, 'id'>>({
    label: '',
    fullName: user?.fullName ?? '',
    line1: '',
    city: '',
    state: '',
    zipCode: '',
    country: 'United States',
    isDefault: false,
    preferredStoreId: selectedStore?.id ?? null,
  })

  const [paymentMethods, setPaymentMethods] = useState<PaymentMethodEntry[]>([])
  const [newPayment, setNewPayment] = useState({
    brand: 'Visa',
    cardNumber: '',
    expiry: '',
  })

  const [companySettings, setCompanySettings] = useState({
    companyName: '',
    taxId: '',
    costCenter: '',
    approvalEmail: user?.email ?? '',
    approvalRequired: true,
    approvalThreshold: '1000',
  })
  const [approvals, setApprovals] = useState<ApprovalEntry[]>([])

  const tabs = useMemo(
    () =>
      [
        { id: 'profile' as const, label: 'Profile' },
        { id: 'orders' as const, label: 'Orders' },
        { id: 'addresses' as const, label: 'Addresses' },
        { id: 'payments' as const, label: 'Payments' },
        ...(user?.role === 'b2b'
          ? [
              { id: 'company' as const, label: 'Company' },
              { id: 'approvals' as const, label: 'Approvals' },
            ]
          : []),
      ] satisfies Array<{ id: AccountTab; label: string }>,
    [user?.role],
  )

  const orders = useMemo(() => {
    const query = orderSearch.trim().toLowerCase()
    return ordersState.items.filter((order) => {
      const matchesQuery =
        query.length === 0 ||
        order.id.toLowerCase().includes(query) ||
        order.items.some((item) => item.name.toLowerCase().includes(query))
      const matchesStatus = orderStatusFilter === 'all' || order.status === orderStatusFilter
      return matchesQuery && matchesStatus
    })
  }, [orderSearch, orderStatusFilter, ordersState.items])

  const availableOrderStatuses = useMemo(
    () => Array.from(new Set(ordersState.items.map((order) => order.status))),
    [ordersState.items],
  )

  useEffect(() => {
    setFullName(user?.fullName ?? '')
    setPhone(user?.phone ?? '')
  }, [user?.fullName, user?.phone])

  useEffect(() => {
    if (!userId) {
      return
    }

    if (userRole === 'admin') {
      void dispatch(fetchAdminOrders())
    } else {
      void dispatch(fetchMyOrders())
    }
  }, [dispatch, userId, userRole])

  useEffect(() => {
    if (!userId) {
      return
    }

    let cancelled = false
    const load = async (): Promise<void> => {
      setLoadingAccountData(true)
      try {
        const profilePromise = accountApi.getProfile()
        const addressesPromise = accountApi.getAddresses()
        const paymentMethodsPromise = accountApi.getPaymentMethods()
        const approvalsPromise = userRole === 'b2b' ? accountApi.getApprovals() : Promise.resolve([])
        const [profile, addressesResult, paymentMethodsResult, approvalsResult] = await Promise.all([
          profilePromise,
          addressesPromise,
          paymentMethodsPromise,
          approvalsPromise,
        ])

        if (cancelled) {
          return
        }

        setFullName(profile.fullName)
        setPhone(profile.phone ?? '')
        dispatch(updateProfile({ fullName: profile.fullName, phone: profile.phone ?? '' }))
        setAddresses(addressesResult.map(toAddressEntry))
        setPaymentMethods(paymentMethodsResult.map(toPaymentMethodEntry))
        setApprovals(approvalsResult.map(toApprovalEntry))
      } catch (error) {
        if (!cancelled) {
          setFeedback(error instanceof Error ? error.message : 'Unable to load account data.')
        }
      } finally {
        if (!cancelled) {
          setLoadingAccountData(false)
        }
      }
    }

    void load()
    return () => {
      cancelled = true
    }
  }, [dispatch, userId, userRole])

  /**
   * Handles quick reorder.
   */
  const quickReorder = (orderId: string): void => {
    const order = ordersState.items.find((entry) => entry.id === orderId)
    if (!order) {
      return
    }

    const lines = order.items.map((item) => {
      const product = products.find((productEntry) => productEntry.id === item.productId)
      const stockAtStore = selectedStore && product ? getProductStockAtStore(product, selectedStore.id) : undefined
      return {
        productId: item.productId,
        name: item.name,
        image: product?.images[0] ?? '/vite.svg',
        price: item.price,
        quantity: item.quantity,
        storeId: selectedStore?.id ?? null,
        storeName: selectedStore?.name,
        unavailableAtStore: selectedStore ? (stockAtStore ?? 0) <= 0 : false,
      }
    })
    const unavailable = lines.filter((line) => line.unavailableAtStore)
    dispatch(
      addBulkItems(
        lines
          .filter((line) => !line.unavailableAtStore)
          .map((line) => ({
            productId: line.productId,
            name: line.name,
            image: line.image,
            price: line.price,
            quantity: line.quantity,
            storeId: line.storeId,
            storeName: line.storeName,
          })),
      ),
    )
    setFeedback(
      unavailable.length > 0
        ? `Added ${lines.length - unavailable.length} lines. ${unavailable.length} item(s) unavailable at ${selectedStore?.name ?? 'current store'}.`
        : `Added ${lines.length} lines from ${order.id} to cart.`,
    )
  }

  /**
   * Executes quick order by sku.
   *
   * @returns No value.
   */
  const quickOrderBySku = (): void => {
    const normalized = quickSku.trim().toLowerCase()
    if (!normalized) {
      setFeedback('Enter a SKU/product id first.')
      return
    }

    const product = products.find((item) => item.id.toLowerCase() === normalized)
    if (!product) {
      setFeedback(`SKU "${quickSku}" was not found.`)
      return
    }

    if (selectedStore && getProductStockAtStore(product, selectedStore.id) <= 0) {
      setFeedback(`${product.name} is not available at ${selectedStore.name}.`)
      return
    }

    dispatch(
      addBulkItems([
        {
          productId: product.id,
          name: product.name,
          image: product.images[0],
          price: product.price,
          quantity: 1,
          storeId: selectedStore?.id ?? null,
          storeName: selectedStore?.name,
        },
      ]),
    )
    setQuickSku('')
    setFeedback(`Added ${product.name} to cart from quick order.`)
  }

  /**
   * Executes save profile.
   *
   * @returns No value.
   */
  const saveProfile = async (): Promise<void> => {
    try {
      const updated = await accountApi.updateProfile({ fullName, phone })
      dispatch(updateProfile({ fullName: updated.fullName, phone: updated.phone ?? '' }))
      setFeedback('Profile saved.')
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : 'Unable to save profile.')
    }
  }

  /**
   * Executes add address.
   *
   * @returns No value.
   */
  const addAddress = async (): Promise<void> => {
    if (!newAddress.fullName || !newAddress.line1 || !newAddress.city || !newAddress.state || !newAddress.zipCode) {
      setFeedback('Please complete all required address fields.')
      return
    }

    const payload: AddressRequest = {
      label: newAddress.label,
      fullName: newAddress.fullName,
      line1: newAddress.line1,
      city: newAddress.city,
      state: newAddress.state,
      zipCode: newAddress.zipCode,
      country: newAddress.country,
      defaultAddress: Boolean(newAddress.isDefault),
    }

    try {
      const created = await accountApi.addAddress(payload)
      setAddresses((current) => [
        ...current.map((address) =>
          created.defaultAddress ? { ...address, isDefault: false } : address,
        ),
        toAddressEntry(created),
      ])
      setNewAddress({
        label: '',
        fullName: user?.fullName ?? '',
        line1: '',
        city: '',
        state: '',
        zipCode: '',
        country: 'United States',
        isDefault: false,
        preferredStoreId: selectedStore?.id ?? null,
      })
      setFeedback('Address saved.')
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : 'Unable to save address.')
    }
  }

  /**
   * Executes add payment method.
   *
   * @returns No value.
   */
  const addPaymentMethod = async (): Promise<void> => {
    const normalized = newPayment.cardNumber.replace(/\s+/g, '')
    if (!/^\d{12,19}$/.test(normalized)) {
      setFeedback('Enter a valid card number.')
      return
    }
    if (!/^(0[1-9]|1[0-2])\/\d{2}$/.test(newPayment.expiry)) {
      setFeedback('Use MM/YY for expiry.')
      return
    }

    try {
      const created = await accountApi.addPaymentMethod({
        methodType: 'CARD',
        provider: newPayment.brand,
        tokenizedReference: `${normalized.slice(-4)}|${newPayment.expiry}`,
        defaultMethod: paymentMethods.length === 0,
      })
      setPaymentMethods((current) => [...current, toPaymentMethodEntry(created)])
      setNewPayment({ brand: 'Visa', cardNumber: '', expiry: '' })
      setFeedback('Payment method added.')
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : 'Unable to add payment method.')
    }
  }

  /**
   * Executes save company settings.
   *
   * @returns No value.
   */
  const saveCompanySettings = async (): Promise<void> => {
    const payload: CompanyProfileRequest = {
      companyName: companySettings.companyName,
      taxId: companySettings.taxId,
      costCenter: companySettings.costCenter,
      approvalEmail: companySettings.approvalEmail,
      approvalRequired: companySettings.approvalRequired,
      approvalThreshold: Number(companySettings.approvalThreshold),
    }

    try {
      const saved = await accountApi.upsertCompanyProfile(payload)
      setCompanySettings({
        companyName: saved.companyName ?? '',
        taxId: saved.taxId ?? '',
        costCenter: saved.costCenter ?? '',
        approvalEmail: saved.approvalEmail ?? '',
        approvalRequired: saved.approvalRequired,
        approvalThreshold: String(saved.approvalThreshold ?? '0'),
      })
      setFeedback('Company settings saved.')
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : 'Unable to save company settings.')
    }
  }

  return (
    <div className="space-y-6">
      <Seo description="Manage your profile and review order history." title="My Account" />

      <header className="panel p-6">
        <h1 className="m3-title">My Account</h1>
        <p className="m3-subtitle mt-2">Manage profile, addresses, payment methods, and order activity.</p>

        <div className="m3-segment mt-4 flex w-full gap-1 overflow-x-auto pb-1">
          {tabs.map((tab) => (
            <button
              className={`m3-segment-btn whitespace-nowrap ${activeTab === tab.id ? 'm3-segment-btn-active' : ''}`}
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              type="button"
            >
              {tab.label}
            </button>
          ))}
        </div>

        {feedback ? (
          <p className="mt-3 rounded-2xl p-2 text-xs" style={{ background: 'var(--m3-surface-container-high)' }}>
            {feedback}
          </p>
        ) : null}
      </header>

      {activeTab === 'profile' ? (
        <section className="panel max-w-2xl space-y-4 p-6">
          <h2 className="text-lg font-semibold">Profile management</h2>
          {loadingAccountData ? <p className="m3-subtitle text-sm">Loading profile...</p> : null}
          <div className="space-y-3">
            <label className="block text-sm">
              Full name
              <input
                className="m3-input mt-1"
                onChange={(event) => setFullName(event.target.value)}
                type="text"
                value={fullName}
              />
            </label>

            <label className="block text-sm">
              Email (read-only)
              <input className="m3-input mt-1 opacity-80" readOnly type="email" value={user?.email ?? ''} />
            </label>

            <label className="block text-sm">
              Phone
              <input
                className="m3-input mt-1"
                onChange={(event) => setPhone(event.target.value)}
                type="tel"
                value={phone}
              />
            </label>
          </div>

          <button className="m3-btn m3-btn-filled" onClick={() => void saveProfile()} type="button">
            Save profile
          </button>
        </section>
      ) : null}

      {activeTab === 'orders' ? (
        <section className="space-y-4">
          {user?.role === 'b2b' ? (
            <article className="panel space-y-3 p-5">
              <h2 className="text-base font-semibold">Quick order / reorder (B2B)</h2>
              <div className="flex flex-col gap-2 sm:flex-row">
                <input
                  className="m3-input"
                  onChange={(event) => setQuickSku(event.target.value)}
                  placeholder="Enter SKU / product id"
                  type="text"
                  value={quickSku}
                />
                <button className="m3-btn m3-btn-filled" onClick={quickOrderBySku} type="button">
                  Add SKU
                </button>
              </div>
            </article>
          ) : null}

          <article className="panel grid gap-3 p-4 sm:grid-cols-[1fr_auto]">
            <input
              className="m3-input"
              onChange={(event) => setOrderSearch(event.target.value)}
              placeholder="Search by order id or product name"
              type="search"
              value={orderSearch}
            />
            <select
              className="m3-select !h-11 !rounded-2xl"
              onChange={(event) => setOrderStatusFilter(event.target.value)}
              value={orderStatusFilter}
            >
              <option value="all">All statuses</option>
              {availableOrderStatuses.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </article>

          {ordersState.status === 'loading' ? <p className="m3-subtitle text-sm">Loading orders...</p> : null}
          {ordersState.error ? <p className="text-sm text-rose-600">{ordersState.error}</p> : null}
          {ordersState.status !== 'loading' && orders.length === 0 ? (
            <article className="panel p-5 text-sm">No orders found for the selected filters.</article>
          ) : null}

          {orders.map((order) => (
            <article className="panel p-5" key={order.id}>
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h2 className="text-base font-semibold">{order.id}</h2>
                <span className="m3-chip text-xs font-semibold uppercase">{order.status}</span>
              </div>
              <p className="m3-subtitle mt-1 text-xs">{new Date(order.createdAt).toLocaleString()}</p>
              <p className="m3-subtitle text-xs">Store: {order.storeName ?? order.storeId ?? 'Not available'}</p>

              <ul className="mt-3 space-y-1 text-sm">
                {order.items.map((item) => (
                  <li className="flex justify-between gap-2" key={`${order.id}-${item.productId}`}>
                    <span>
                      {item.name} x{item.quantity}
                    </span>
                    <span>{formatCurrency(item.price * item.quantity)}</span>
                  </li>
                ))}
              </ul>

              <p className="mt-3 text-right text-sm font-bold">Total: {formatCurrency(order.total)}</p>
              {user?.role === 'b2b' ? (
                <div className="mt-3 flex justify-end">
                  <button
                    aria-label={`Quick reorder ${order.id}`}
                    className="m3-btn m3-btn-tonal !px-4 !py-2 !text-xs uppercase tracking-wide"
                    onClick={() => quickReorder(order.id)}
                    type="button"
                  >
                    Quick Reorder
                  </button>
                </div>
              ) : null}
            </article>
          ))}
        </section>
      ) : null}

      {activeTab === 'addresses' ? (
        <section className="grid gap-4 xl:grid-cols-[1.2fr_1fr]">
          <article className="panel space-y-3 p-5">
            <h2 className="text-base font-semibold">Saved addresses</h2>
            {loadingAccountData && addresses.length === 0 ? <p className="m3-subtitle text-sm">Loading addresses...</p> : null}
            {addresses.length === 0 && !loadingAccountData ? (
              <p className="m3-subtitle text-sm">No addresses saved yet.</p>
            ) : null}
            {addresses.map((address) => (
              <div
                className="rounded-2xl border p-3 text-sm"
                key={address.id}
                style={{ borderColor: 'var(--m3-outline-variant)' }}
              >
                <div className="flex items-center justify-between gap-2">
                  <p className="font-semibold">
                    {address.label || 'Address'} {address.isDefault ? '(Default)' : ''}
                  </p>
                </div>
                <p>{address.fullName}</p>
                <p>{address.line1}</p>
                <p>
                  {address.city}, {address.state} {address.zipCode}
                </p>
                <p>{address.country}</p>
                {address.preferredStoreId ? (
                  <p className="m3-subtitle text-xs">
                    Preferred store: {stores.find((store) => store.id === address.preferredStoreId)?.name ?? address.preferredStoreId}
                  </p>
                ) : null}
              </div>
            ))}
          </article>

          <article className="panel space-y-3 p-5">
            <h2 className="text-base font-semibold">Add address</h2>
            <input
              className="m3-input"
              onChange={(event) => setNewAddress((state) => ({ ...state, label: event.target.value }))}
              placeholder="Label (Office, Warehouse...)"
              type="text"
              value={newAddress.label}
            />
            <input
              className="m3-input"
              onChange={(event) => setNewAddress((state) => ({ ...state, fullName: event.target.value }))}
              placeholder="Full name"
              type="text"
              value={newAddress.fullName}
            />
            <input
              className="m3-input"
              onChange={(event) => setNewAddress((state) => ({ ...state, line1: event.target.value }))}
              placeholder="Address line"
              type="text"
              value={newAddress.line1}
            />
            <div className="grid grid-cols-2 gap-2">
              <input
                className="m3-input"
                onChange={(event) => setNewAddress((state) => ({ ...state, city: event.target.value }))}
                placeholder="City"
                type="text"
                value={newAddress.city}
              />
              <input
                className="m3-input"
                onChange={(event) => setNewAddress((state) => ({ ...state, state: event.target.value }))}
                placeholder="State"
                type="text"
                value={newAddress.state}
              />
            </div>
            <div className="grid grid-cols-2 gap-2">
              <input
                className="m3-input"
                onChange={(event) => setNewAddress((state) => ({ ...state, zipCode: event.target.value }))}
                placeholder="ZIP"
                type="text"
                value={newAddress.zipCode}
              />
              <input
                className="m3-input"
                onChange={(event) => setNewAddress((state) => ({ ...state, country: event.target.value }))}
                placeholder="Country"
                type="text"
                value={newAddress.country}
              />
            </div>
            <label className="flex items-center gap-2 text-sm">
              <input
                checked={Boolean(newAddress.isDefault)}
                onChange={(event) => setNewAddress((state) => ({ ...state, isDefault: event.target.checked }))}
                type="checkbox"
              />
              Set as default address
            </label>
            <button className="m3-btn m3-btn-filled" onClick={() => void addAddress()} type="button">
              Save address
            </button>
          </article>
        </section>
      ) : null}

      {activeTab === 'payments' ? (
        <section className="grid gap-4 xl:grid-cols-[1.2fr_1fr]">
          <article className="panel space-y-3 p-5">
            <h2 className="text-base font-semibold">Payment methods</h2>
            {loadingAccountData && paymentMethods.length === 0 ? <p className="m3-subtitle text-sm">Loading payment methods...</p> : null}
            {paymentMethods.length === 0 && !loadingAccountData ? (
              <p className="m3-subtitle text-sm">No payment methods saved yet.</p>
            ) : null}
            {paymentMethods.map((method) => (
              <div
                className="rounded-2xl border p-3 text-sm"
                key={method.id}
                style={{ borderColor: 'var(--m3-outline-variant)' }}
              >
                <p className="font-semibold">
                  {method.brand} •••• {method.last4} {method.isDefault ? '(Default)' : ''}
                </p>
                <p className="m3-subtitle">Expires {method.expiry}</p>
              </div>
            ))}
          </article>

          <article className="panel space-y-3 p-5">
            <h2 className="text-base font-semibold">Add payment method</h2>
            <select
              className="m3-select"
              onChange={(event) => setNewPayment((state) => ({ ...state, brand: event.target.value }))}
              value={newPayment.brand}
            >
              <option value="Visa">Visa</option>
              <option value="Mastercard">Mastercard</option>
              <option value="Amex">Amex</option>
            </select>
            <input
              className="m3-input"
              onChange={(event) => setNewPayment((state) => ({ ...state, cardNumber: event.target.value }))}
              placeholder="Card number"
              type="text"
              value={newPayment.cardNumber}
            />
            <input
              className="m3-input"
              onChange={(event) => setNewPayment((state) => ({ ...state, expiry: event.target.value }))}
              placeholder="MM/YY"
              type="text"
              value={newPayment.expiry}
            />
            <button className="m3-btn m3-btn-filled" onClick={() => void addPaymentMethod()} type="button">
              Save payment method
            </button>
          </article>
        </section>
      ) : null}

      {activeTab === 'company' && user?.role === 'b2b' ? (
        <section className="panel max-w-2xl space-y-3 p-5">
          <h2 className="text-base font-semibold">Company settings</h2>
          <input
            className="m3-input"
            onChange={(event) => setCompanySettings((state) => ({ ...state, companyName: event.target.value }))}
            placeholder="Company name"
            type="text"
            value={companySettings.companyName}
          />
          <div className="grid grid-cols-2 gap-2">
            <input
              className="m3-input"
              onChange={(event) => setCompanySettings((state) => ({ ...state, taxId: event.target.value }))}
              placeholder="Tax ID"
              type="text"
              value={companySettings.taxId}
            />
            <input
              className="m3-input"
              onChange={(event) => setCompanySettings((state) => ({ ...state, costCenter: event.target.value }))}
              placeholder="Cost center"
              type="text"
              value={companySettings.costCenter}
            />
          </div>
          <input
            className="m3-input"
            onChange={(event) => setCompanySettings((state) => ({ ...state, approvalEmail: event.target.value }))}
            placeholder="Approval email"
            type="email"
            value={companySettings.approvalEmail}
          />
          <input
            className="m3-input"
            min={0}
            onChange={(event) => setCompanySettings((state) => ({ ...state, approvalThreshold: event.target.value }))}
            placeholder="Approval threshold"
            type="number"
            value={companySettings.approvalThreshold}
          />
          <label className="flex items-center gap-2 text-sm">
            <input
              checked={companySettings.approvalRequired}
              onChange={(event) =>
                setCompanySettings((state) => ({ ...state, approvalRequired: event.target.checked }))
              }
              type="checkbox"
            />
            Require approval for orders above threshold
          </label>
          <button className="m3-btn m3-btn-filled" onClick={() => void saveCompanySettings()} type="button">
            Save company settings
          </button>
        </section>
      ) : null}

      {activeTab === 'approvals' && user?.role === 'b2b' ? (
        <section className="panel space-y-3 p-5">
          <h2 className="text-base font-semibold">Approval queue</h2>
          {loadingAccountData && approvals.length === 0 ? <p className="m3-subtitle text-sm">Loading approvals...</p> : null}
          {approvals.length === 0 && !loadingAccountData ? (
            <p className="m3-subtitle text-sm">No approvals found.</p>
          ) : null}
          {approvals.map((entry) => (
            <article
              className="grid gap-3 rounded-2xl border p-3 sm:grid-cols-[1fr_auto]"
              key={entry.id}
              style={{ borderColor: 'var(--m3-outline-variant)' }}
            >
              <div>
                <p className="font-semibold">{entry.id}</p>
                <p className="m3-subtitle text-sm">
                  Requester {entry.requesterId} · {formatCurrency(entry.total)}
                </p>
                {entry.orderId ? <p className="m3-subtitle text-xs">Order {entry.orderId}</p> : null}
                {entry.reviewerNotes ? <p className="m3-subtitle text-xs">Note: {entry.reviewerNotes}</p> : null}
                <p className="mt-1 text-xs uppercase">{entry.status}</p>
              </div>
            </article>
          ))}
        </section>
      ) : null}
    </div>
  )
}
