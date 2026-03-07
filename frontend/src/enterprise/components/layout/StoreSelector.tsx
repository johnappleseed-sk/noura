import { useEffect, useMemo, useRef, useState } from 'react'
import { FiChevronDown, FiMapPin } from 'react-icons/fi'
import { Link, useLocation } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { pushNotification } from '@/features/notifications/notificationsSlice'
import { refreshRecommendations } from '@/features/recommendations/recommendationsSlice'
import { selectStoreById } from '@/features/stores/storesSlice'
import { geoContextToCoordinates, getStoresSortedByDistance, matchesStoreQuery } from '@/lib/storeLocator'
import { StoreCoordinates } from '@/types'

const formatDistance = (value: number): string => `${value.toFixed(1)} km`

/**
 * Renders the StoreSelector component.
 *
 * @returns The rendered component tree.
 */
export const StoreSelector = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const location = useLocation()
  const storesState = useAppSelector((state) => state.stores)
  const geoContext = useAppSelector((state) => state.geo.context)
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [coordinates, setCoordinates] = useState<StoreCoordinates | null>(null)
  const [geoLoading, setGeoLoading] = useState(false)
  const containerRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    /**
     * Executes on window click.
     *
     * @param event The event value.
     * @returns No value.
     */
    const onWindowClick = (event: MouseEvent): void => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setOpen(false)
      }
    }
    /**
     * Executes on escape.
     *
     * @param event The event value.
     * @returns No value.
     */
    const onEscape = (event: KeyboardEvent): void => {
      if (event.key === 'Escape') {
        setOpen(false)
      }
    }
    if (open) {
      window.addEventListener('click', onWindowClick)
      window.addEventListener('keydown', onEscape)
    }
    return () => {
      window.removeEventListener('click', onWindowClick)
      window.removeEventListener('keydown', onEscape)
    }
  }, [open])

  const activeCoordinates = coordinates ?? geoContextToCoordinates(geoContext)

  const nearbyStores = useMemo(() => {
    const matches = storesState.availableStores.filter((store) => matchesStoreQuery(store, query))
    return getStoresSortedByDistance(matches, activeCoordinates).slice(0, 8)
  }, [activeCoordinates, query, storesState.availableStores])

  const selectedStoreLabel = storesState.selectedStoreDetails?.name ?? 'Choose store'

  /**
   * Executes choose store.
   *
   * @param storeId The store id used to locate the target record.
   * @returns No value.
   */
  const chooseStore = (storeId: string): void => {
    const createdAt = new Date().toISOString()
    dispatch(selectStoreById(storeId))
    void dispatch(refreshRecommendations())
    dispatch(
      pushNotification({
        id: `store-dropdown-${storeId}-${createdAt}`,
        title: 'My store updated',
        description: 'Product availability and promotions have been refreshed.',
        category: 'store',
        createdAt,
        read: false,
      }),
    )
    setOpen(false)
  }

  /**
   * Executes use current location.
   *
   * @returns No value.
   */
  const useCurrentLocation = (): void => {
    if (!navigator.geolocation) {
      const createdAt = new Date().toISOString()
      dispatch(
        pushNotification({
          id: `store-geo-unavailable-${createdAt}`,
          title: 'Location unavailable',
          description: 'Geolocation is not available in this browser.',
          category: 'store',
          createdAt,
          read: false,
        }),
      )
      return
    }

    setGeoLoading(true)
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setCoordinates({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
        })
        setGeoLoading(false)
      },
      () => {
        const createdAt = new Date().toISOString()
        setGeoLoading(false)
        dispatch(
          pushNotification({
            id: `store-geo-error-${createdAt}`,
            title: 'Could not get location',
            description: 'Use city or ZIP search to choose your nearest store.',
            category: 'store',
            createdAt,
            read: false,
          }),
        )
      },
      { enableHighAccuracy: false, timeout: 5000 },
    )
  }

  return (
    <div className="relative" ref={containerRef}>
      <button
        aria-expanded={open}
        aria-haspopup="menu"
        aria-label="Select my store"
        className="m3-btn m3-btn-outlined !h-10 !gap-2 !rounded-full !px-4"
        onClick={() => setOpen((current) => !current)}
        type="button"
      >
        <FiMapPin size={14} />
        <span className="max-w-[170px] truncate text-xs sm:text-sm">{selectedStoreLabel}</span>
        <FiChevronDown size={13} />
      </button>

      {open ? (
        <section aria-label="Store selector" className="ent-dropdown absolute right-0 z-50 mt-2 w-[360px] p-3" role="menu">
          <h3 className="text-sm font-semibold">Select store</h3>
          <p className="m3-subtitle mt-1 text-xs">Prices and availability are based on your selected store.</p>

          <input
            className="m3-input mt-3 !h-10"
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search by city or ZIP"
            type="search"
            value={query}
          />

          <div className="mt-2 flex items-center justify-between gap-2">
            <button
              className="m3-btn m3-btn-outlined !h-9 !rounded-xl !px-3 !text-xs"
              onClick={useCurrentLocation}
              type="button"
            >
              {geoLoading ? 'Locating...' : 'Use my current location'}
            </button>
            <Link className="m3-link text-xs" state={{ from: location.pathname }} to="/stores">
              View all stores
            </Link>
          </div>

          <ul className="mt-3 max-h-72 space-y-2 overflow-y-auto">
            {nearbyStores.length === 0 ? (
              <li className="m3-subtitle rounded-2xl p-3 text-xs">No stores found for this search.</li>
            ) : (
              nearbyStores.map((store) => {
                const isSelected = storesState.selectedStoreId === store.id
                return (
                  <li
                    className="rounded-2xl border p-3"
                    key={store.id}
                    style={{
                      borderColor: isSelected ? 'var(--m3-primary)' : 'var(--m3-outline-variant)',
                      background: isSelected ? 'var(--m3-primary-container)' : 'var(--m3-surface-container-low)',
                    }}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <p className="truncate text-sm font-semibold">{store.name}</p>
                        <p className="m3-subtitle mt-1 text-xs">
                          {store.city}, {store.state} {store.zipCode}
                        </p>
                        <p className="m3-subtitle text-xs">
                          {store.hoursSummary} • {formatDistance(store.distanceKm)}
                        </p>
                      </div>
                      <button
                        className="m3-btn m3-btn-filled !h-8 !rounded-xl !px-3 !py-1 text-xs"
                        onClick={() => chooseStore(store.id)}
                        type="button"
                      >
                        {isSelected ? 'Selected' : 'Set'}
                      </button>
                    </div>
                  </li>
                )
              })
            )}
          </ul>
        </section>
      ) : null}
    </div>
  )
}
