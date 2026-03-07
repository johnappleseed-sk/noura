import { FormEvent, useEffect, useMemo, useState } from 'react'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { Seo } from '@/components/common/Seo'
import { fetchAdminOrders, fetchMyOrders } from '@/features/orders/ordersSlice'
import { deleteProduct, upsertProduct } from '@/features/products/productsSlice'
import { deleteStore, upsertStore } from '@/features/stores/storesSlice'
import { Product, StoreLocation, StoreService } from '@/types'
import { formatCurrency } from '@/utils/currency'

type AdminTab = 'overview' | 'products' | 'orders' | 'stores'

interface ProductForm {
  id?: string
  name: string
  category: string
  brand: string
  price: string
  stock: string
  inventoryStoreId: string
  inventoryStock: string
}

interface StoreForm {
  id?: string
  name: string
  addressLine1: string
  city: string
  state: string
  zipCode: string
  country: string
  phone: string
  latitude: string
  longitude: string
  region: StoreLocation['region']
  status: StoreLocation['status']
  services: StoreService[]
  hoursSummary: string
  freeShippingThreshold: string
  shippingFee: string
}

const serviceOptions: StoreService[] = ['pickup', 'delivery', 'curbside', 'b2b-desk']

const emptyForm: ProductForm = {
  name: '',
  category: '',
  brand: '',
  price: '',
  stock: '',
  inventoryStoreId: '',
  inventoryStock: '',
}

const emptyStoreForm: StoreForm = {
  name: '',
  addressLine1: '',
  city: '',
  state: '',
  zipCode: '',
  country: 'United States',
  phone: '',
  latitude: '',
  longitude: '',
  region: 'global',
  status: 'active',
  services: ['pickup', 'delivery'],
  hoursSummary: 'Daily 10:00-20:00',
  freeShippingThreshold: '300',
  shippingFee: '14',
}

/**
 * Transforms data for map to form.
 *
 * @param product The product value.
 * @returns The result of map to form.
 */
const mapToForm = (product: Product): ProductForm => ({
  id: product.id,
  name: product.name,
  category: product.category,
  brand: product.brand,
  price: String(product.price),
  stock: String(product.stock),
  inventoryStoreId: Object.keys(product.storeInventory ?? {})[0] ?? '',
  inventoryStock: '',
})

/**
 * Transforms data for map to store form.
 *
 * @param store The store value.
 * @returns The result of map to store form.
 */
const mapToStoreForm = (store: StoreLocation): StoreForm => ({
  id: store.id,
  name: store.name,
  addressLine1: store.addressLine1,
  city: store.city,
  state: store.state,
  zipCode: store.zipCode,
  country: store.country,
  phone: store.phone,
  latitude: String(store.latitude),
  longitude: String(store.longitude),
  region: store.region,
  status: store.status,
  services: store.services,
  hoursSummary: store.hoursSummary,
  freeShippingThreshold: String(store.freeShippingThreshold),
  shippingFee: String(store.shippingFee),
})

const defaultHours: StoreLocation['hours'] = [
  { day: 'Mon', open: '10:00', close: '20:00' },
  { day: 'Tue', open: '10:00', close: '20:00' },
  { day: 'Wed', open: '10:00', close: '20:00' },
  { day: 'Thu', open: '10:00', close: '20:00' },
  { day: 'Fri', open: '10:00', close: '20:00' },
  { day: 'Sat', open: '10:00', close: '20:00' },
  { day: 'Sun', open: '10:00', close: '20:00' },
]

/**
 * Executes toggle service.
 *
 * @param services The services value.
 * @param service The service value.
 * @returns A list of matching items.
 */
const toggleService = (services: StoreService[], service: StoreService): StoreService[] =>
  services.includes(service)
    ? services.filter((item) => item !== service)
    : [...services, service]

/**
 * Renders the AdminPanelPage component.
 *
 * @returns The rendered component tree.
 */
export const AdminPanelPage = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const user = useAppSelector((state) => state.auth.user)
  const userId = user?.id ?? null
  const userRole = user?.role ?? null
  const products = useAppSelector((state) => state.products.items)
  const ordersState = useAppSelector((state) => state.orders)
  const stores = useAppSelector((state) => state.stores.availableStores)
  const [activeTab, setActiveTab] = useState<AdminTab>('overview')
  const [form, setForm] = useState<ProductForm>(emptyForm)
  const [storeForm, setStoreForm] = useState<StoreForm>(emptyStoreForm)

  useEffect(() => {
    if (!userId) {
      return
    }
    if (userRole === 'admin') {
      void dispatch(fetchAdminOrders())
      return
    }
    void dispatch(fetchMyOrders())
  }, [dispatch, userId, userRole])

  const dashboardMetrics = useMemo(
    () => ({
      totalProducts: products.length,
      inventoryUnits: products.reduce((acc, product) => acc + product.stock, 0),
      averageRating:
        products.length > 0
          ? products.reduce((acc, product) => acc + product.rating, 0) / products.length
          : 0,
      ordersThisMonth: ordersState.items.filter((order) => {
        const created = new Date(order.createdAt)
        const now = new Date()
        return created.getMonth() === now.getMonth() && created.getFullYear() === now.getFullYear()
      }).length,
      totalStores: stores.length,
      activeStores: stores.filter((store) => store.status === 'active').length,
    }),
    [ordersState.items, products, stores],
  )

  /**
   * Executes submit product.
   *
   * @param event The event value.
   * @returns No value.
   */
  const submitProduct = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()

    const productId = form.id ?? `p-${Date.now()}`
    const existing = products.find((product) => product.id === productId)
    const priceValue = Number(form.price)
    const stockValue = Number(form.stock)
    const inventoryStoreId = form.inventoryStoreId || stores[0]?.id
    const inventoryStockValue = Number(form.inventoryStock || form.stock)
    const nextStoreInventory = { ...(existing?.storeInventory ?? {}) }
    if (inventoryStoreId) {
      nextStoreInventory[inventoryStoreId] = Number.isFinite(inventoryStockValue) ? Math.max(0, inventoryStockValue) : stockValue
    }

    const nextProduct: Product = {
      id: productId,
      name: form.name,
      category: form.category,
      brand: form.brand,
      price: priceValue,
      originalPrice: existing?.originalPrice,
      rating: existing?.rating ?? 4.1,
      reviewCount: existing?.reviewCount ?? 0,
      stock: stockValue,
      popularity: existing?.popularity ?? 60,
      createdAt: existing?.createdAt ?? new Date().toISOString(),
      images:
        existing?.images ?? [
          'https://images.unsplash.com/photo-1484704849700-f032a568e944?auto=format&fit=crop&w=1200&q=80',
        ],
      tags: existing?.tags ?? ['new product'],
      features: existing?.features ?? ['Admin-created product'],
      description:
        existing?.description ?? 'Product description can be expanded once backend product management is connected.',
      reviews: existing?.reviews ?? [],
      storeInventory: nextStoreInventory,
    }

    dispatch(upsertProduct(nextProduct))
    setForm(emptyForm)
  }

  /**
   * Executes submit store.
   *
   * @param event The event value.
   * @returns No value.
   */
  const submitStore = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()

    const storeId = storeForm.id ?? `store-${Date.now()}`
    const existing = stores.find((store) => store.id === storeId)
    const nextStore: StoreLocation = {
      id: storeId,
      name: storeForm.name,
      addressLine1: storeForm.addressLine1,
      city: storeForm.city,
      state: storeForm.state,
      zipCode: storeForm.zipCode,
      country: storeForm.country,
      phone: storeForm.phone,
      latitude: Number(storeForm.latitude),
      longitude: Number(storeForm.longitude),
      region: storeForm.region,
      hoursSummary: storeForm.hoursSummary,
      hours: existing?.hours ?? defaultHours,
      services: storeForm.services.length > 0 ? storeForm.services : ['pickup'],
      freeShippingThreshold: Number(storeForm.freeShippingThreshold),
      shippingFee: Number(storeForm.shippingFee),
      status: storeForm.status,
    }

    dispatch(upsertStore(nextStore))
    setStoreForm(emptyStoreForm)
  }

  return (
    <div className="space-y-6">
      <Seo description="Admin dashboard, product management, and order management frontend UI." title="Admin Panel" />

      <header className="panel p-6">
        <h1 className="m3-title">Admin Panel</h1>
        <p className="m3-subtitle mt-2">
          Frontend-only dashboard for operational visibility and management workflows.
        </p>
        <div className="m3-segment mt-4 flex w-fit flex-wrap gap-1">
          {(['overview', 'products', 'orders', 'stores'] as AdminTab[]).map((tab) => (
            <button
              className={`m3-segment-btn ${tab === activeTab ? 'm3-segment-btn-active' : ''}`}
              key={tab}
              onClick={() => setActiveTab(tab)}
              type="button"
            >
              {tab.charAt(0).toUpperCase() + tab.slice(1)}
            </button>
          ))}
        </div>
      </header>

      {activeTab === 'overview' ? (
        <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          <article className="panel p-5">
            <p className="m3-subtitle text-sm">Total products</p>
            <p className="mt-1 text-2xl font-bold">{dashboardMetrics.totalProducts}</p>
          </article>
          <article className="panel p-5">
            <p className="m3-subtitle text-sm">Inventory units</p>
            <p className="mt-1 text-2xl font-bold">{dashboardMetrics.inventoryUnits}</p>
          </article>
          <article className="panel p-5">
            <p className="m3-subtitle text-sm">Average rating</p>
            <p className="mt-1 text-2xl font-bold">{dashboardMetrics.averageRating.toFixed(2)}</p>
          </article>
          <article className="panel p-5">
            <p className="m3-subtitle text-sm">Orders this month</p>
            <p className="mt-1 text-2xl font-bold">{dashboardMetrics.ordersThisMonth}</p>
          </article>
          <article className="panel p-5">
            <p className="m3-subtitle text-sm">Stores</p>
            <p className="mt-1 text-2xl font-bold">{dashboardMetrics.totalStores}</p>
          </article>
          <article className="panel p-5">
            <p className="m3-subtitle text-sm">Active stores</p>
            <p className="mt-1 text-2xl font-bold">{dashboardMetrics.activeStores}</p>
          </article>
        </section>
      ) : null}

      {activeTab === 'products' ? (
        <div className="grid gap-6 lg:grid-cols-[360px_1fr]">
          <form className="panel space-y-3 p-5" onSubmit={submitProduct}>
            <h2 className="text-lg font-semibold">{form.id ? 'Edit Product' : 'Create Product'}</h2>
            <input
              className="m3-input"
              onChange={(event) => setForm((state) => ({ ...state, name: event.target.value }))}
              placeholder="Product name"
              required
              type="text"
              value={form.name}
            />
            <input
              className="m3-input"
              onChange={(event) => setForm((state) => ({ ...state, category: event.target.value }))}
              placeholder="Category"
              required
              type="text"
              value={form.category}
            />
            <input
              className="m3-input"
              onChange={(event) => setForm((state) => ({ ...state, brand: event.target.value }))}
              placeholder="Brand"
              required
              type="text"
              value={form.brand}
            />
            <div className="grid grid-cols-2 gap-3">
              <input
                className="m3-input"
                min={0}
                onChange={(event) => setForm((state) => ({ ...state, price: event.target.value }))}
                placeholder="Price"
                required
                type="number"
                value={form.price}
              />
              <input
                className="m3-input"
                min={0}
                onChange={(event) => setForm((state) => ({ ...state, stock: event.target.value }))}
                placeholder="Stock"
                required
                type="number"
                value={form.stock}
              />
            </div>
            <select
              className="m3-select"
              onChange={(event) => setForm((state) => ({ ...state, inventoryStoreId: event.target.value }))}
              value={form.inventoryStoreId}
            >
              <option value="">Inventory store (optional)</option>
              {stores.map((store) => (
                <option key={store.id} value={store.id}>
                  {store.name}
                </option>
              ))}
            </select>
            <input
              className="m3-input"
              min={0}
              onChange={(event) => setForm((state) => ({ ...state, inventoryStock: event.target.value }))}
              placeholder="Store stock for selected store"
              type="number"
              value={form.inventoryStock}
            />
            <button className="m3-btn m3-btn-filled w-full" type="submit">
              {form.id ? 'Update Product' : 'Create Product'}
            </button>
          </form>

          <section className="panel overflow-hidden p-5">
            <h2 className="mb-3 text-lg font-semibold">Product CRUD UI</h2>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[760px] text-left text-sm">
                <thead>
                  <tr className="border-b" style={{ borderColor: 'var(--m3-outline-variant)' }}>
                    <th className="py-2">Name</th>
                    <th className="py-2">Category</th>
                    <th className="py-2">Price</th>
                    <th className="py-2">Stock</th>
                    <th className="py-2">Store inventory</th>
                    <th className="py-2 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {products.map((product) => (
                    <tr className="border-b" key={product.id} style={{ borderColor: 'var(--m3-outline-variant)' }}>
                      <td className="py-2">{product.name}</td>
                      <td className="py-2">{product.category}</td>
                      <td className="py-2">{formatCurrency(product.price)}</td>
                      <td className="py-2">{product.stock}</td>
                      <td className="py-2">{Object.keys(product.storeInventory ?? {}).length}</td>
                      <td className="py-2 text-right">
                        <div className="flex justify-end gap-2">
                          <button
                            className="m3-btn m3-btn-outlined !h-8 !px-3 !py-1 text-xs"
                            onClick={() => setForm(mapToForm(product))}
                            type="button"
                          >
                            Edit
                          </button>
                          <button
                            className="m3-btn m3-btn-outlined !h-8 !px-3 !py-1 text-xs text-rose-700"
                            onClick={() => dispatch(deleteProduct(product.id))}
                            type="button"
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      ) : null}

      {activeTab === 'orders' ? (
        <section className="panel p-5">
          <h2 className="mb-3 text-lg font-semibold">Order Management UI</h2>
          {ordersState.status === 'loading' ? <p className="m3-subtitle mb-3 text-sm">Loading orders...</p> : null}
          {ordersState.error ? <p className="mb-3 text-sm text-rose-600">{ordersState.error}</p> : null}
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead>
                <tr className="border-b" style={{ borderColor: 'var(--m3-outline-variant)' }}>
                  <th className="py-2">Order ID</th>
                  <th className="py-2">Date</th>
                  <th className="py-2">Status</th>
                  <th className="py-2">Store</th>
                  <th className="py-2">Total</th>
                  <th className="py-2 text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {ordersState.items.map((order) => (
                  <tr className="border-b" key={order.id} style={{ borderColor: 'var(--m3-outline-variant)' }}>
                    <td className="py-2">{order.id}</td>
                    <td className="py-2">{new Date(order.createdAt).toLocaleDateString()}</td>
                    <td className="py-2">
                      <span className="m3-chip text-xs font-semibold uppercase">
                        {order.status}
                      </span>
                    </td>
                    <td className="py-2">{order.storeName ?? order.storeId ?? 'N/A'}</td>
                    <td className="py-2">{formatCurrency(order.total)}</td>
                    <td className="py-2 text-right">
                      <button
                        className="m3-btn m3-btn-outlined !h-8 !px-3 !py-1 text-xs"
                        type="button"
                      >
                        View
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}

      {activeTab === 'stores' ? (
        <div className="grid gap-6 lg:grid-cols-[400px_1fr]">
          <form className="panel space-y-3 p-5" onSubmit={submitStore}>
            <h2 className="text-lg font-semibold">{storeForm.id ? 'Edit Store' : 'Create Store'}</h2>
            <input
              className="m3-input"
              onChange={(event) => setStoreForm((state) => ({ ...state, name: event.target.value }))}
              placeholder="Store name"
              required
              type="text"
              value={storeForm.name}
            />
            <input
              className="m3-input"
              onChange={(event) => setStoreForm((state) => ({ ...state, addressLine1: event.target.value }))}
              placeholder="Address"
              required
              type="text"
              value={storeForm.addressLine1}
            />
            <div className="grid grid-cols-2 gap-2">
              <input
                className="m3-input"
                onChange={(event) => setStoreForm((state) => ({ ...state, city: event.target.value }))}
                placeholder="City"
                required
                type="text"
                value={storeForm.city}
              />
              <input
                className="m3-input"
                onChange={(event) => setStoreForm((state) => ({ ...state, state: event.target.value }))}
                placeholder="State"
                required
                type="text"
                value={storeForm.state}
              />
            </div>
            <div className="grid grid-cols-2 gap-2">
              <input
                className="m3-input"
                onChange={(event) => setStoreForm((state) => ({ ...state, zipCode: event.target.value }))}
                placeholder="ZIP"
                required
                type="text"
                value={storeForm.zipCode}
              />
              <input
                className="m3-input"
                onChange={(event) => setStoreForm((state) => ({ ...state, country: event.target.value }))}
                placeholder="Country"
                required
                type="text"
                value={storeForm.country}
              />
            </div>
            <input
              className="m3-input"
              onChange={(event) => setStoreForm((state) => ({ ...state, phone: event.target.value }))}
              placeholder="Phone"
              required
              type="text"
              value={storeForm.phone}
            />
            <div className="grid grid-cols-2 gap-2">
              <input
                className="m3-input"
                onChange={(event) => setStoreForm((state) => ({ ...state, latitude: event.target.value }))}
                placeholder="Latitude"
                required
                type="number"
                value={storeForm.latitude}
              />
              <input
                className="m3-input"
                onChange={(event) => setStoreForm((state) => ({ ...state, longitude: event.target.value }))}
                placeholder="Longitude"
                required
                type="number"
                value={storeForm.longitude}
              />
            </div>
            <div className="grid grid-cols-2 gap-2">
              <select
                className="m3-select"
                onChange={(event) =>
                  setStoreForm((state) => ({ ...state, region: event.target.value as StoreLocation['region'] }))
                }
                value={storeForm.region}
              >
                <option value="global">Global</option>
                <option value="us">US</option>
                <option value="eu">EU</option>
                <option value="apac">APAC</option>
              </select>
              <select
                className="m3-select"
                onChange={(event) =>
                  setStoreForm((state) => ({ ...state, status: event.target.value as StoreLocation['status'] }))
                }
                value={storeForm.status}
              >
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
              </select>
            </div>
            <input
              className="m3-input"
              onChange={(event) => setStoreForm((state) => ({ ...state, hoursSummary: event.target.value }))}
              placeholder="Hours summary"
              required
              type="text"
              value={storeForm.hoursSummary}
            />
            <div className="grid grid-cols-2 gap-2">
              <input
                className="m3-input"
                min={0}
                onChange={(event) =>
                  setStoreForm((state) => ({ ...state, freeShippingThreshold: event.target.value }))
                }
                placeholder="Free shipping threshold"
                required
                type="number"
                value={storeForm.freeShippingThreshold}
              />
              <input
                className="m3-input"
                min={0}
                onChange={(event) => setStoreForm((state) => ({ ...state, shippingFee: event.target.value }))}
                placeholder="Shipping fee"
                required
                type="number"
                value={storeForm.shippingFee}
              />
            </div>
            <fieldset className="rounded-2xl border p-3" style={{ borderColor: 'var(--m3-outline-variant)' }}>
              <legend className="px-1 text-xs font-semibold uppercase">Services</legend>
              <div className="mt-2 grid gap-2 sm:grid-cols-2">
                {serviceOptions.map((service) => (
                  <label className="flex items-center gap-2 text-sm" key={service}>
                    <input
                      checked={storeForm.services.includes(service)}
                      onChange={() =>
                        setStoreForm((state) => ({
                          ...state,
                          services: toggleService(state.services, service),
                        }))
                      }
                      type="checkbox"
                    />
                    {service}
                  </label>
                ))}
              </div>
            </fieldset>
            <div className="grid gap-2 sm:grid-cols-2">
              <button className="m3-btn m3-btn-filled w-full" type="submit">
                {storeForm.id ? 'Update Store' : 'Create Store'}
              </button>
              <button
                className="m3-btn m3-btn-outlined w-full"
                onClick={() => setStoreForm(emptyStoreForm)}
                type="button"
              >
                Reset
              </button>
            </div>
          </form>

          <section className="panel overflow-hidden p-5">
            <h2 className="mb-3 text-lg font-semibold">Stores Management</h2>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[860px] text-left text-sm">
                <thead>
                  <tr className="border-b" style={{ borderColor: 'var(--m3-outline-variant)' }}>
                    <th className="py-2">Name</th>
                    <th className="py-2">City</th>
                    <th className="py-2">Region</th>
                    <th className="py-2">Status</th>
                    <th className="py-2">Services</th>
                    <th className="py-2">Shipping</th>
                    <th className="py-2 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {stores.map((store) => (
                    <tr className="border-b" key={store.id} style={{ borderColor: 'var(--m3-outline-variant)' }}>
                      <td className="py-2">
                        <p className="font-semibold">{store.name}</p>
                        <p className="m3-subtitle text-xs">{store.addressLine1}</p>
                      </td>
                      <td className="py-2">
                        {store.city}, {store.state}
                      </td>
                      <td className="py-2 uppercase">{store.region}</td>
                      <td className="py-2">
                        <span className="m3-chip text-[10px] uppercase">{store.status}</span>
                      </td>
                      <td className="py-2">{store.services.join(', ')}</td>
                      <td className="py-2">
                        {formatCurrency(store.shippingFee)} / {formatCurrency(store.freeShippingThreshold)}
                      </td>
                      <td className="py-2 text-right">
                        <div className="flex justify-end gap-2">
                          <button
                            className="m3-btn m3-btn-outlined !h-8 !px-3 !py-1 text-xs"
                            onClick={() => setStoreForm(mapToStoreForm(store))}
                            type="button"
                          >
                            Edit
                          </button>
                          <button
                            className="m3-btn m3-btn-outlined !h-8 !px-3 !py-1 text-xs text-rose-700"
                            onClick={() => dispatch(deleteStore(store.id))}
                            type="button"
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      ) : null}
    </div>
  )
}
