import { useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { Seo } from '@/components/common/Seo'
import { pushNotification } from '@/features/notifications/notificationsSlice'
import { refreshRecommendations } from '@/features/recommendations/recommendationsSlice'
import { selectStoreById } from '@/features/stores/storesSlice'
import { formatStoreAddress, isStoreOpenNow, matchesStoreQuery } from '@/lib/storeLocator'
import { StoreLocation, StoreService } from '@/types'

const serviceOptions: Array<{ id: StoreService; label: string }> = [
  { id: 'pickup', label: 'Pickup' },
  { id: 'delivery', label: 'Delivery' },
  { id: 'curbside', label: 'Curbside' },
  { id: 'b2b-desk', label: 'B2B Desk' },
]

/**
 * Executes normalize.
 *
 * @param value The value value.
 * @param min The min value.
 * @param max The max value.
 * @returns The result of normalize.
 */
const normalize = (value: number, min: number, max: number): number => {
  if (max === min) {
    return 0.5
  }
  return (value - min) / (max - min)
}

/**
 * Executes to map point.
 *
 * @param store The store value.
 * @param bounds The bounds value.
 * @returns The result of to map point.
 */
const toMapPoint = (
  store: StoreLocation,
  bounds: { minLat: number; maxLat: number; minLng: number; maxLng: number },
): { left: number; top: number } => {
  const x = normalize(store.longitude, bounds.minLng, bounds.maxLng)
  const y = 1 - normalize(store.latitude, bounds.minLat, bounds.maxLat)
  return {
    left: 12 + x * 76,
    top: 8 + y * 82,
  }
}

/**
 * Renders the StoreLocatorPage component.
 *
 * @returns The rendered component tree.
 */
export const StoreLocatorPage = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const storesState = useAppSelector((state) => state.stores)
  const [query, setQuery] = useState('')
  const [serviceFilter, setServiceFilter] = useState<StoreService | 'all'>('all')
  const [openNowOnly, setOpenNowOnly] = useState(false)
  const [activeStoreId, setActiveStoreId] = useState(storesState.selectedStoreId)

  const filteredStores = useMemo(
    () =>
      storesState.availableStores.filter((store) => {
        if (!matchesStoreQuery(store, query)) {
          return false
        }
        if (serviceFilter !== 'all' && !store.services.includes(serviceFilter)) {
          return false
        }
        if (openNowOnly && !isStoreOpenNow(store)) {
          return false
        }
        return true
      }),
    [openNowOnly, query, serviceFilter, storesState.availableStores],
  )

  const activeStore = useMemo(
    () => filteredStores.find((store) => store.id === activeStoreId) ?? filteredStores[0] ?? null,
    [activeStoreId, filteredStores],
  )

  const mapBounds = useMemo(() => {
    const source = filteredStores.length > 0 ? filteredStores : storesState.availableStores
    const latitudes = source.map((store) => store.latitude)
    const longitudes = source.map((store) => store.longitude)
    return {
      minLat: Math.min(...latitudes, 0),
      maxLat: Math.max(...latitudes, 1),
      minLng: Math.min(...longitudes, 0),
      maxLng: Math.max(...longitudes, 1),
    }
  }, [filteredStores, storesState.availableStores])

  /**
   * Updates set my store.
   *
   * @param storeId The store id used to locate the target record.
   * @returns No value.
   */
  const setMyStore = (storeId: string): void => {
    const store = storesState.availableStores.find((entry) => entry.id === storeId)
    if (!store) {
      return
    }
    const createdAt = new Date().toISOString()

    dispatch(selectStoreById(storeId))
    void dispatch(refreshRecommendations())
    dispatch(
      pushNotification({
        id: `store-locator-set-${storeId}-${createdAt}`,
        title: 'Store selected',
        description: `${store.name} is now your selected store.`,
        category: 'store',
        createdAt,
        read: false,
      }),
    )

    const fallbackPath = '/'
    const from = (location.state as { from?: string } | null)?.from ?? fallbackPath
    navigate(from, { replace: false })
  }

  return (
    <div className="space-y-6">
      <Seo description="Find and set your preferred store for pickup, stock checks, and local promotions." title="Store Locator" />

      <header className="panel p-6">
        <h1 className="m3-title">Store Locator</h1>
        <p className="m3-subtitle mt-2 text-sm">
          Choose a store for accurate pickup availability, delivery estimates, and local promotions.
        </p>
      </header>

      <section className="grid gap-4 xl:grid-cols-[1.2fr_1fr]">
        <article className="panel p-4">
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <input
              className="m3-input flex-1"
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Filter by city, ZIP, or store name"
              type="search"
              value={query}
            />
            <select
              className="m3-select !h-11 !w-auto !rounded-2xl"
              onChange={(event) => setServiceFilter(event.target.value as StoreService | 'all')}
              value={serviceFilter}
            >
              <option value="all">All services</option>
              {serviceOptions.map((service) => (
                <option key={service.id} value={service.id}>
                  {service.label}
                </option>
              ))}
            </select>
            <label className="m3-chip cursor-pointer">
              <input
                checked={openNowOnly}
                className="mr-2"
                onChange={(event) => setOpenNowOnly(event.target.checked)}
                type="checkbox"
              />
              Open now
            </label>
          </div>

          <div className="panel relative h-[520px] overflow-hidden p-0">
            <div className="absolute inset-0 bg-gradient-to-br from-sky-100 via-blue-50 to-indigo-100" />
            <div className="absolute inset-0 opacity-40" style={{ backgroundImage: 'radial-gradient(circle at 1px 1px, #4f7ab8 1px, transparent 0)', backgroundSize: '22px 22px' }} />

            {filteredStores.map((store) => {
              const point = toMapPoint(store, mapBounds)
              const active = activeStore?.id === store.id
              return (
                <button
                  aria-label={`View ${store.name}`}
                  className={`absolute -translate-x-1/2 -translate-y-1/2 rounded-full border px-2 py-1 text-[11px] font-semibold ${
                    active ? 'bg-brand-600 text-white' : 'bg-white text-slate-800'
                  }`}
                  key={store.id}
                  onClick={() => setActiveStoreId(store.id)}
                  style={{ left: `${point.left}%`, top: `${point.top}%` }}
                  type="button"
                >
                  {store.city}
                </button>
              )
            })}

            {activeStore ? (
              <div className="absolute bottom-3 left-3 right-3 rounded-2xl border bg-white/95 p-3 text-sm shadow-md" style={{ borderColor: 'var(--m3-outline-variant)' }}>
                <p className="font-semibold">{activeStore.name}</p>
                <p className="m3-subtitle text-xs">{formatStoreAddress(activeStore)}</p>
                <p className="m3-subtitle text-xs">{activeStore.hoursSummary}</p>
              </div>
            ) : null}
          </div>
        </article>

        <aside className="panel space-y-3 p-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">Stores</h2>
            <span className="m3-chip">{filteredStores.length}</span>
          </div>

          {storesState.loading ? <p className="m3-subtitle text-sm">Loading stores...</p> : null}
          {storesState.error ? <p className="text-sm text-rose-600">{storesState.error}</p> : null}

          <ul className="max-h-[520px] space-y-2 overflow-y-auto pr-1">
            {filteredStores.map((store) => {
              const selected = storesState.selectedStoreId === store.id
              const active = activeStore?.id === store.id
              return (
                <li
                  className="rounded-2xl border p-3"
                  key={store.id}
                  style={{
                    borderColor: active ? 'var(--m3-primary)' : 'var(--m3-outline-variant)',
                    background: active ? 'var(--m3-primary-container)' : 'var(--m3-surface-container-low)',
                  }}
                >
                  <button
                    className="w-full text-left"
                    onClick={() => setActiveStoreId(store.id)}
                    type="button"
                  >
                    <p className="text-sm font-semibold">{store.name}</p>
                    <p className="m3-subtitle mt-1 text-xs">{formatStoreAddress(store)}</p>
                    <p className="m3-subtitle text-xs">{store.phone} • {store.hoursSummary}</p>
                  </button>
                  <div className="mt-2 flex items-center justify-between gap-2">
                    <span className="m3-chip text-[10px] uppercase">{isStoreOpenNow(store) ? 'Open now' : 'Closed now'}</span>
                    <button
                      className="m3-btn m3-btn-filled !h-8 !rounded-xl !px-3 !py-1 text-xs"
                      onClick={() => setMyStore(store.id)}
                      type="button"
                    >
                      {selected ? 'My store' : 'Set as my store'}
                    </button>
                  </div>
                </li>
              )
            })}
          </ul>

          <div className="pt-2">
            <Link className="m3-link text-xs" to="/">
              Back to home
            </Link>
          </div>
        </aside>
      </section>
    </div>
  )
}
