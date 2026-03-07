import { FormEvent, useEffect, useMemo, useRef, useState } from 'react'
import { FiChevronDown, FiGift, FiGlobe, FiHeart, FiMapPin, FiMenu, FiMic, FiPackage, FiSearch, FiShoppingCart, FiUser, FiX } from 'react-icons/fi'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { searchApi } from '@/api/searchApi'
import { ThemeToggle } from '@/components/common/ThemeToggle'
import { NotificationCenter } from '@/components/layout/NotificationCenter'
import { StoreSelector } from '@/components/layout/StoreSelector'
import { StoreSuggestionBanner } from '@/components/layout/StoreSuggestionBanner'
import { logout } from '@/features/auth/authSlice'
import { addBulkItems, selectCartCount, selectCartItems, selectCartTotals } from '@/features/cart/cartSlice'
import { selectCmsMenu } from '@/features/cms/cmsSlice'
import { setCurrency, setLanguage, setRegion } from '@/features/geo/geoSlice'
import { pushNotification } from '@/features/notifications/notificationsSlice'
import { fetchAdminOrders, fetchMyOrders } from '@/features/orders/ordersSlice'
import { setAvailableAtMyStore, setCategoryFilters, setPriceRange, setSearchQuery } from '@/features/products/productsSlice'
import { selectRecommendedProducts } from '@/features/recommendations/recommendationsSlice'
import { selectCurrentStore } from '@/features/stores/storesSlice'
import { setChatbotOpen } from '@/features/ui/uiSlice'
import { selectWishlistCount } from '@/features/wishlist/wishlistSlice'
import { useDebounce } from '@/hooks/useDebounce'
import { CurrencyCode, GeoRegion, LanguageCode, Product } from '@/types'
import { formatCurrency } from '@/utils/currency'
import { getPredictiveSuggestions } from '@/utils/predictiveSearch'

const regions: GeoRegion[] = ['us', 'eu', 'apac', 'global']
const currencies: CurrencyCode[] = ['USD', 'EUR', 'GBP', 'THB', 'JPY']
const languages: LanguageCode[] = ['en-US', 'en-GB', 'de-DE', 'fr-FR', 'th-TH', 'ja-JP']
const promoKey = 'enterprise_header_promo_dismissed'
const recentKey = 'enterprise_recent_searches'
const allScope = 'All Departments'
type Panel = 'mega' | 'account' | 'cart' | 'wishlist' | 'order' | null

/**
 * Executes read recent.
 *
 * @returns A list of matching items.
 */
const readRecent = (): string[] => {
  try {
    const raw = localStorage.getItem(recentKey)
    const parsed = raw ? (JSON.parse(raw) as unknown) : []
    return Array.isArray(parsed) ? parsed.filter((x): x is string => typeof x === 'string').slice(0, 6) : []
  } catch {
    return []
  }
}

/**
 * Renders the Navbar component.
 *
 * @returns The rendered component tree.
 */
export const Navbar = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const rootRef = useRef<HTMLElement | null>(null)
  const products = useAppSelector((s) => s.products.items)
  const activeSearch = useAppSelector((s) => s.products.filters.searchQuery)
  const availableAtMyStore = useAppSelector((s) => s.products.filters.availableAtMyStore)
  const history = useAppSelector((s) => s.products.browsingHistory)
  const user = useAppSelector((s) => s.auth.user)
  const userId = user?.id ?? null
  const userRole = user?.role ?? null
  const geo = useAppSelector((s) => s.geo.context)
  const menu = useAppSelector(selectCmsMenu)
  const rec = useAppSelector(selectRecommendedProducts)
  const selectedStore = useAppSelector(selectCurrentStore)
  const cartItems = useAppSelector(selectCartItems)
  const cartTotals = useAppSelector(selectCartTotals)
  const cartCount = useAppSelector(selectCartCount)
  const wishCount = useAppSelector(selectWishlistCount)
  const wishIds = useAppSelector((s) => s.wishlist.productIds)
  const orders = useAppSelector((s) => s.orders.items)
  const personal = useAppSelector((s) => s.personalization)
  const [query, setQuery] = useState(activeSearch)
  const [scope, setScope] = useState(allScope)
  const [open, setOpen] = useState<Panel>(null)
  const [mobile, setMobile] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const [shrink, setShrink] = useState(false)
  const [promo, setPromo] = useState(() => localStorage.getItem(promoKey) !== '1')
  const [recent, setRecent] = useState<string[]>(() => readRecent())
  const [orderId, setOrderId] = useState('')
  const [orderResult, setOrderResult] = useState('')
  const [remoteSuggestions, setRemoteSuggestions] = useState<string[]>([])
  const [remoteSuggestionsQuery, setRemoteSuggestionsQuery] = useState('')
  const [remoteTrends, setRemoteTrends] = useState<string[]>([])
  const debounced = useDebounce(query, 150)

  const categories = useMemo(() => Array.from(new Set(products.map((p) => p.category))).slice(0, 8), [products])
  const nav = menu.length > 0 ? menu.map((m) => m.label).slice(0, 4) : categories.slice(0, 4)
  const scopes = useMemo(() => [allScope, ...categories.slice(0, 5)], [categories])
  const activeScope = scopes.includes(scope) ? scope : allScope
  const localSuggestions = useMemo(() => getPredictiveSuggestions(products, debounced, 6), [products, debounced])
  const suggestions = useMemo(() => {
    const trimmed = debounced.trim()
    if (!trimmed) {
      return []
    }
    if (remoteSuggestionsQuery === trimmed && remoteSuggestions.length > 0) {
      return remoteSuggestions.slice(0, 6)
    }
    return localSuggestions
  }, [debounced, localSuggestions, remoteSuggestions, remoteSuggestionsQuery])
  const localTrends = useMemo(() => Array.from(new Set(products.flatMap((p) => p.tags))).slice(0, 6), [products])
  const trends = remoteTrends.length > 0 ? remoteTrends : localTrends
  const suggestionProducts = useMemo(
    () =>
      (debounced.trim()
        ? products.filter((product) => product.name.toLowerCase().includes(debounced.trim().toLowerCase()))
        : products
      ).slice(0, 4),
    [products, debounced],
  )
  const productMap = useMemo(() => new Map<string, Product>(products.map((p) => [p.id, p])), [products])
  const wishPreview = useMemo(() => wishIds.map((id) => productMap.get(id)).filter((p): p is Product => Boolean(p)).slice(0, 3), [wishIds, productMap])
  const recNames = rec.slice(0, 3).map((p) => p.name).join(', ')
  const viewedNames = history.map((id) => productMap.get(id)?.name).filter((x): x is string => Boolean(x)).slice(0, 3).join(', ')

  useEffect(() => {
    try {
      localStorage.setItem(recentKey, JSON.stringify(recent))
    } catch (error) {
      void error
    }
  }, [recent])

  useEffect(() => {
    const trimmed = debounced.trim()
    if (!trimmed) {
      return
    }

    let cancelled = false
    const scopeValue = activeScope === allScope ? 'all' : activeScope

    void searchApi
      .predictive(trimmed, scopeValue)
      .then((items) => {
        if (cancelled) {
          return
        }
        setRemoteSuggestionsQuery(trimmed)
        setRemoteSuggestions(items.slice(0, 6))
      })
      .catch(() => {})

    return () => {
      cancelled = true
    }
  }, [activeScope, debounced])

  useEffect(() => {
    if (products.length === 0 || remoteTrends.length > 0) {
      return
    }

    let cancelled = false

    void searchApi
      .trendTags()
      .then((items) => {
        if (cancelled) {
          return
        }
        setRemoteTrends(items.slice(0, 6))
      })
      .catch(() => {})

    return () => {
      cancelled = true
    }
  }, [products, remoteTrends.length])

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

  useEffect(() => {
    const onScroll = (): void => setShrink(window.scrollY > 10)
    /**
     * Handles on click.
     */
    const onClick = (e: MouseEvent): void => { if (!rootRef.current?.contains(e.target as Node)) { setOpen(null); setSearchOpen(false) } }
    /**
     * Handles on esc.
     */
    const onEsc = (e: KeyboardEvent): void => { if (e.key === 'Escape') { setOpen(null); setSearchOpen(false); setMobile(false) } }
    window.addEventListener('scroll', onScroll, { passive: true }); window.addEventListener('mousedown', onClick); window.addEventListener('keydown', onEsc)
    return () => { window.removeEventListener('scroll', onScroll); window.removeEventListener('mousedown', onClick); window.removeEventListener('keydown', onEsc) }
  }, [])

  /**
   * Executes run search.
   *
   * @param value The value value.
   * @param opts Additional options that customize this operation.
   * @returns No value.
   */
  const runSearch = (value: string, opts?: { category?: string; min?: number; max?: number; inStore?: boolean }): void => {
    const q = value.trim(); dispatch(setSearchQuery(q)); if (opts?.category) dispatch(setCategoryFilters([opts.category])); else if (activeScope !== allScope) dispatch(setCategoryFilters([activeScope]))
    if (opts?.min !== undefined && opts?.max !== undefined) dispatch(setPriceRange({ minPrice: opts.min, maxPrice: opts.max }))
    if (opts?.inStore !== undefined) dispatch(setAvailableAtMyStore(opts.inStore))
    if (q) setRecent((cur) => [q, ...cur.filter((x) => x !== q)].slice(0, 6))
    navigate('/products'); setSearchOpen(false); setMobile(false)
  }

  /**
   * Executes on voice.
   *
   * @returns No value.
   */
  const onVoice = (): void => {
    dispatch(pushNotification({ id: `voice-${Date.now()}`, title: 'Voice search', description: 'Voice capture is enabled when browser support is available.', category: 'system', createdAt: new Date().toISOString(), read: false }))
  }
  /**
   * Executes on store.
   *
   * @returns No value.
   */
  const onStore = (): void => {
    dispatch(pushNotification({ id: `store-${Date.now()}`, title: 'Store locator', description: 'Nearest outlet lookup triggered via geolocation.', category: 'system', createdAt: new Date().toISOString(), read: false }))
  }
  /**
   * Handles on order.
   */
  const onOrder = (e: FormEvent<HTMLFormElement>): void => { e.preventDefault(); const m = orders.find((o) => o.id.toLowerCase() === orderId.trim().toLowerCase()); setOrderResult(m ? `Status: ${m.status.toUpperCase()} - ${new Date(m.createdAt).toLocaleDateString()}` : 'Order not found') }
  /**
   * Handles quick reorder.
   */
  const quickReorder = (): void => { const o = orders[0]; if (!o) { setOrderResult('No order history available yet.'); return }; dispatch(addBulkItems(o.items.map((i) => ({ productId: i.productId, name: i.name, image: productMap.get(i.productId)?.images[0] ?? '', price: i.price, quantity: i.quantity, storeId: selectedStore?.id ?? null, storeName: selectedStore?.name })))); navigate('/cart'); setOpen(null) }
  /**
   * Handles dismiss promo.
   */
  const dismissPromo = (): void => { setPromo(false); localStorage.setItem(promoKey, '1') }

  return (
    <header className={`ent-header sticky top-0 z-50 ${shrink ? 'ent-header-shrink' : ''}`} ref={rootRef}>
      {promo ? <div className="ent-promo-bar"><div className="mx-auto flex max-w-none items-center justify-between px-4 py-2 text-xs sm:px-6 lg:px-8"><p className="truncate">{geo.region === 'eu' ? '20% off ends Friday.' : 'Free shipping above $300.'}</p><div className="flex items-center gap-2"><Link className="ent-inline-link" to="/products">Shop now</Link><button className="ent-touch-btn" onClick={dismissPromo} type="button"><FiX size={15} /></button></div></div></div> : null}
      <StoreSuggestionBanner />
      <div className={`ent-utility-bar ${shrink ? 'max-h-0 overflow-hidden opacity-0' : 'max-h-40 opacity-100'} transition-all`}>
        <div className="relative mx-auto flex max-w-none flex-wrap items-center justify-between gap-2 px-4 py-2 text-xs sm:px-6 lg:px-8">
          <div className="flex flex-wrap items-center gap-2">
            <span className="inline-flex items-center gap-1 rounded-full border px-2 py-1" style={{ borderColor: 'var(--m3-outline-variant)' }}><FiGlobe size={12} /><select aria-label="Region" className="bg-transparent text-[11px] uppercase outline-none" onChange={(e) => dispatch(setRegion(e.target.value as GeoRegion))} value={geo.region}>{regions.map((r) => <option key={r} value={r}>{r.toUpperCase()}</option>)}</select></span>
            <select aria-label="Currency" className="rounded-full border bg-transparent px-2 py-1 text-[11px] uppercase outline-none" onChange={(e) => dispatch(setCurrency(e.target.value as CurrencyCode))} style={{ borderColor: 'var(--m3-outline-variant)' }} value={geo.currency}>{currencies.map((c) => <option key={c}>{c}</option>)}</select>
            <select aria-label="Language" className="rounded-full border bg-transparent px-2 py-1 text-[11px] outline-none" onChange={(e) => dispatch(setLanguage(e.target.value as LanguageCode))} style={{ borderColor: 'var(--m3-outline-variant)' }} value={geo.language}>{languages.map((l) => <option key={l}>{l}</option>)}</select>
            <StoreSelector />
            <button className="ent-inline-link" onClick={onStore} type="button"><FiMapPin size={12} />Find a Store</button>
            <button className="ent-inline-link" onClick={() => dispatch(setChatbotOpen(true))} type="button"><FiPackage size={12} />Live Chat</button>
            <button className="ent-inline-link" onClick={() => setOpen(open === 'order' ? null : 'order')} type="button">Order Status</button>
          </div>
          <div className="hidden items-center gap-3 lg:flex"><button className="ent-inline-link" onClick={quickReorder} type="button">Quick Reorder</button><Link className="ent-inline-link" to="/?section=gift-cards"><FiGift size={12} />Gift Cards</Link><Link className="ent-inline-link" to="/?section=careers">Careers</Link><span className="m3-chip text-[10px] uppercase">MFE-ready</span></div>
          {open === 'order' ? <section className="ent-dropdown absolute right-4 top-10 z-50 w-72 p-3"><h3 className="mb-2 text-sm font-semibold">Order lookup</h3><form className="space-y-2" onSubmit={onOrder}><input className="m3-input !h-10" onChange={(e) => setOrderId(e.target.value)} placeholder="ord-2026-001" value={orderId} /><button className="m3-btn m3-btn-filled w-full !rounded-2xl" type="submit">Track</button></form>{orderResult ? <p className="m3-subtitle mt-2 text-xs">{orderResult}</p> : null}</section> : null}
        </div>
      </div>

      <div className="mx-auto max-w-none px-4 py-3 sm:px-6 lg:px-8">
        <div className="grid items-center gap-3 md:grid-cols-[auto_1fr_auto]">
          <div className="flex min-w-0 items-center gap-3">
            <NavLink className="ent-logo-mark" to="/">Noura</NavLink>
            <span className="ent-logo-tag hidden xl:inline-flex">{user?.role === 'b2b' ? 'Enterprise verified' : 'Business since 1985'}</span>
          </div>

          <div className="relative hidden min-w-0 md:block">
            <form className="ent-search-shell" onSubmit={(e) => { e.preventDefault(); runSearch(query) }} role="search">
              <input aria-label="Search" className="ent-search-input" onBlur={() => window.setTimeout(() => setSearchOpen(false), 140)} onChange={(e) => { setQuery(e.target.value); setSearchOpen(true) }} onFocus={() => setSearchOpen(true)} placeholder="Search products, categories, or SKU" type="search" value={query} />
              <span aria-hidden="true" className="ent-search-divider" />
              <select aria-label="Search scope" className="ent-search-select" onChange={(e) => setScope(e.target.value)} value={activeScope}>{scopes.map((s) => <option key={s}>{s}</option>)}</select>
              <button aria-label="Voice search" className="ent-touch-btn" onClick={onVoice} type="button"><FiMic size={16} /></button>
              <button aria-label="Submit search" className="ent-search-submit" type="submit"><FiSearch size={16} /></button>
            </form>

            {searchOpen ? (
              <section className="ent-dropdown absolute left-0 right-0 z-50 mt-2 p-3" role="listbox">
                <div className="grid gap-3 lg:grid-cols-2">
                  <div>
                    <h3 className="mb-2 text-xs font-semibold uppercase">Suggestions</h3>
                    <ul className="grid gap-1">
                      {(suggestions.length > 0 ? suggestions : trends.slice(0, 4)).map((s) => (
                        <li key={s}><button className="ent-list-link" onClick={() => { setQuery(s); runSearch(s) }} type="button">{s}</button></li>
                      ))}
                    </ul>
                    {selectedStore ? <ul className="mt-2 grid gap-1">{suggestionProducts.map((product) => { const stock = product.storeInventory?.[selectedStore.id] ?? product.stock; return (<li className="flex items-center justify-between rounded-xl px-2 py-1 text-[11px]" key={`stock-${product.id}`} style={{ background: 'var(--m3-surface-container-high)' }}><button className="ent-list-link !w-auto !p-0" onClick={() => { setQuery(product.name); runSearch(product.name) }} type="button">{product.name}</button><span className={`m3-chip !text-[10px] ${stock > 0 ? '' : '!text-rose-600'}`}>{stock > 0 ? `${stock} in stock` : 'Out of stock'}</span></li>) })}</ul> : null}
                    {recent.length > 0 ? <div className="mt-3 flex flex-wrap gap-1.5">{recent.map((s) => <button className="m3-chip cursor-pointer text-[11px]" key={s} onClick={() => { setQuery(s); runSearch(s) }} type="button">{s}</button>)}</div> : null}
                  </div>
                  <div>
                    <h3 className="mb-2 text-xs font-semibold uppercase">Facets</h3>
                    <div className="flex flex-wrap gap-1.5">
                      {categories.slice(0, 4).map((c) => <button className="m3-chip cursor-pointer text-[11px]" key={c} onClick={() => runSearch(query, { category: c })} type="button">{c}</button>)}
                      <button className="m3-chip cursor-pointer text-[11px]" onClick={() => runSearch(query, { min: 0, max: 100 })} type="button">Under $100</button>
                      <button className="m3-chip cursor-pointer text-[11px]" onClick={() => runSearch(query, { min: 100, max: 500 })} type="button">$100-$500</button>
                      <button className={`m3-chip cursor-pointer text-[11px] ${availableAtMyStore ? 'm3-chip-active' : ''}`} onClick={() => runSearch(query, { inStore: !availableAtMyStore })} type="button">{availableAtMyStore ? 'Show all stock' : `At ${selectedStore?.name ?? 'my store'}`}</button>
                    </div>
                    <h4 className="mb-1 mt-3 text-xs font-semibold uppercase">Trending</h4>
                    <div className="flex flex-wrap gap-1.5">{trends.slice(0, 5).map((t) => <button className="m3-chip cursor-pointer text-[11px]" key={t} onClick={() => runSearch(t)} type="button">{t}</button>)}</div>
                  </div>
                </div>
              </section>
            ) : null}
          </div>

          <div className="hidden items-center justify-end gap-2 sm:flex">
            <ThemeToggle />
            <NotificationCenter />

            <div className="relative">
              <button className="ent-action-btn" onClick={() => setOpen(open === 'wishlist' ? null : 'wishlist')} type="button"><FiHeart size={16} /><span className="ent-count-badge">{wishCount}</span></button>
              {open === 'wishlist' ? <section className="ent-dropdown absolute right-0 z-50 mt-2 w-80 p-3"><h3 className="mb-2 text-sm font-semibold">Wishlist</h3>{wishPreview.length === 0 ? <p className="m3-subtitle text-xs">No saved items.</p> : <ul className="space-y-2">{wishPreview.map((p) => <li className="flex items-center gap-2" key={p.id}><img alt={p.name} className="h-10 w-10 rounded-xl object-cover" src={p.images[0]} /><div><p className="text-xs font-semibold">{p.name}</p><p className="m3-subtitle text-[11px]">{formatCurrency(p.price)}</p></div></li>)}</ul>}<button className="m3-btn m3-btn-outlined mt-3 w-full !rounded-2xl" onClick={() => navigate('/products')} type="button">View Wishlist</button></section> : null}
            </div>

            <div className="relative">
              <button className="ent-action-btn" onClick={() => setOpen(open === 'cart' ? null : 'cart')} type="button"><FiShoppingCart size={16} /><span className="ent-count-badge">{cartCount}</span></button>
              {open === 'cart' ? <section className="ent-dropdown absolute right-0 z-50 mt-2 w-96 p-3"><h3 className="mb-2 text-sm font-semibold">Mini Cart</h3>{cartItems.length === 0 ? <p className="m3-subtitle text-xs">Cart is empty.</p> : <ul className="space-y-2">{cartItems.slice(0, 3).map((i) => <li className="grid grid-cols-[40px_1fr_auto] gap-2" key={i.productId}><img alt={i.name} className="h-10 w-10 rounded-xl object-cover" src={i.image} /><div><p className="text-xs font-semibold">{i.name}</p><p className="m3-subtitle text-[11px]">Qty {i.quantity}</p></div><p className="text-xs font-semibold">{formatCurrency(i.price * i.quantity)}</p></li>)}</ul>}<div className="mt-3 text-xs"><p className="flex justify-between"><span>Subtotal</span><span>{formatCurrency(cartTotals.subtotal)}</span></p><p className="m3-subtitle">{cartTotals.shipping === 0 ? 'Free shipping unlocked.' : `Spend ${formatCurrency((selectedStore?.freeShippingThreshold ?? 300) - cartTotals.subtotal)} more.`}</p><p className="m3-subtitle mt-1">Prices and availability based on {selectedStore?.name ?? 'your selected store'}.</p></div><div className="mt-3 grid gap-2"><NavLink className="m3-btn m3-btn-filled !rounded-2xl !py-2.5" to={user ? '/checkout' : '/login'}>Proceed to Checkout</NavLink><NavLink className="m3-btn m3-btn-outlined !rounded-2xl !py-2.5" to="/cart">View Cart</NavLink></div></section> : null}
            </div>

            <div className="relative">
              <button className="m3-btn m3-btn-outlined !h-11 !gap-2 !rounded-full !px-4" onClick={() => setOpen(open === 'account' ? null : 'account')} type="button"><FiUser size={16} />{user ? `Hi, ${user.fullName.split(' ')[0]}` : 'Sign in'}<FiChevronDown size={13} /></button>
              {open === 'account' ? <section className="ent-dropdown absolute right-0 z-50 mt-2 w-[300px] p-3">{user ? <><div className="mb-2 rounded-2xl p-2.5" style={{ background: 'var(--m3-surface-container-high)' }}><p className="text-sm font-semibold">{user.fullName}</p><p className="m3-subtitle text-xs">{user.email}</p></div><ul className="grid gap-1"><li><NavLink className="ent-list-link" to="/account?tab=profile">Profile information</NavLink></li><li><NavLink className="ent-list-link" to="/account?tab=orders">Order history & tracking</NavLink></li><li><NavLink className="ent-list-link" to="/account?tab=addresses">Saved addresses</NavLink></li><li><NavLink className="ent-list-link" to="/account?tab=payments">Payment methods</NavLink></li><li><NavLink className="ent-list-link" to="/account?tab=rewards">Loyalty summary</NavLink></li><li><NavLink className="ent-list-link" to="/account?tab=returns">Returns & tickets</NavLink></li>{user.role === 'b2b' ? <><li><NavLink className="ent-list-link" to="/account?tab=company">Company Dashboard</NavLink></li><li><button className="ent-list-link" onClick={quickReorder} type="button">Quick Order / Reorder</button></li><li><NavLink className="ent-list-link" to="/account?tab=approvals">Approval Queue</NavLink></li></> : null}</ul><button className="m3-btn m3-btn-outlined mt-3 w-full !rounded-2xl" onClick={() => dispatch(logout())} type="button">Log out</button></> : <div className="grid gap-2"><NavLink className="m3-btn m3-btn-filled !rounded-2xl" to="/login">Log in</NavLink><NavLink className="m3-btn m3-btn-outlined !rounded-2xl" to="/register">Create account</NavLink></div>}</section> : null}
            </div>
          </div>

          <button className="ent-touch-btn justify-self-end sm:hidden" onClick={() => setMobile((c) => !c)} type="button">{mobile ? <FiX size={19} /> : <FiMenu size={19} />}</button>
        </div>

        <div className="mt-3 hidden items-center justify-between gap-3 lg:flex">
          <nav className="flex flex-wrap items-center gap-2" role="navigation">{nav.map((n) => <NavLink className="ent-nav-link" key={n} to="/products">{n}</NavLink>)}<button className="ent-nav-link" onClick={() => setOpen(open === 'mega' ? null : 'mega')} type="button">Mega Menu</button><Link className="ent-nav-link" to="/stores">Stores</Link><Link className="ent-nav-link" to="/?section=about">About</Link><Link className="ent-nav-link" to="/?section=contact">Contact</Link><Link className="ent-nav-link" to="/?section=blog">Blog</Link></nav>
          <div className="flex items-center gap-2"><button className="m3-btn m3-btn-outlined !h-10 !px-3" onClick={() => dispatch(setChatbotOpen(true))} type="button">AI Support</button>{user?.role === 'b2b' ? <button className="m3-btn m3-btn-tonal !h-10 !px-3" onClick={quickReorder} type="button">Quick Order</button> : null}</div>
        </div>

        {open === 'mega' ? <section className="ent-dropdown mt-3 hidden p-4 lg:block"><div className="grid gap-4 lg:grid-cols-[1.2fr_1.2fr_1fr]"><div><h3 className="mb-2 text-xs font-semibold uppercase">Departments</h3><ul className="grid gap-2">{categories.map((c) => <li key={c}><button className="ent-list-link" onClick={() => runSearch(query, { category: c })} type="button">{c}</button></li>)}</ul></div><div><h3 className="mb-2 text-xs font-semibold uppercase">Collections</h3><ul className="grid gap-2"><li><Link className="ent-list-link" to="/products?collection=new-arrivals">New arrivals</Link></li><li><Link className="ent-list-link" to="/products?collection=seasonal">Seasonal picks</Link></li><li><Link className="ent-list-link" to="/products?collection=best-sellers">Best sellers</Link></li><li><Link className="ent-list-link" to="/products?collection=enterprise">B2B bundles</Link></li></ul></div><div className="rounded-3xl border p-3" style={{ borderColor: 'var(--m3-outline-variant)', background: 'var(--m3-surface-container-low)' }}><p className="text-xs font-semibold uppercase">Enterprise</p><h3 className="mt-2 text-lg font-semibold">Procurement Desk</h3><p className="m3-subtitle mt-1 text-xs">Bulk templates, approvals, negotiated pricing.</p><button className="m3-btn m3-btn-filled mt-3 !h-10 !px-4" onClick={() => navigate(user?.role === 'b2b' ? '/account' : '/register')} type="button">{user?.role === 'b2b' ? 'Open Dashboard' : 'Apply B2B Access'}</button></div></div></section> : null}
      </div>

      {mobile ? <div className="border-t lg:hidden" style={{ borderColor: 'var(--m3-outline-variant)' }}><div className="mx-auto max-w-none space-y-3 px-4 py-4 sm:px-6"><form className="ent-search-shell" onSubmit={(e) => { e.preventDefault(); runSearch(query) }}><input className="ent-search-input" onChange={(e) => setQuery(e.target.value)} placeholder="Search..." type="search" value={query} /><button className="ent-search-submit" type="submit"><FiSearch size={16} /></button></form><nav className="grid gap-2">{nav.map((n) => <NavLink className="ent-list-link" key={`m-${n}`} onClick={() => setMobile(false)} to="/products">{n}</NavLink>)}<NavLink className="ent-list-link" onClick={() => setMobile(false)} to="/stores">Stores</NavLink></nav><div className="grid gap-2"><StoreSelector /><button className="m3-btn m3-btn-outlined !h-11 !justify-start !rounded-2xl" onClick={onStore} type="button"><FiMapPin size={14} />Find Store</button><button className="m3-btn m3-btn-outlined !h-11 !justify-start !rounded-2xl" onClick={() => dispatch(setChatbotOpen(true))} type="button">Live Chat</button><NavLink className="m3-btn m3-btn-tonal !h-11 !justify-start !rounded-2xl" onClick={() => setMobile(false)} to="/cart"><FiShoppingCart size={14} />Cart ({cartCount})</NavLink>{user ? <button className="m3-btn m3-btn-outlined !h-11 !justify-start !rounded-2xl" onClick={() => dispatch(logout())} type="button">Log out</button> : <div className="grid grid-cols-2 gap-2"><NavLink className="m3-btn m3-btn-outlined !h-11 !rounded-2xl" onClick={() => setMobile(false)} to="/register">Sign up</NavLink><NavLink className="m3-btn m3-btn-filled !h-11 !rounded-2xl" onClick={() => setMobile(false)} to="/login">Log in</NavLink></div>}</div></div></div> : null}

      <div className="ent-smart-bar"><div className="mx-auto grid max-w-none gap-2 px-4 py-2 text-xs sm:px-6 lg:grid-cols-[1.3fr_1.2fr_1fr] lg:px-8"><p className="truncate">Welcome back, {user?.fullName?.split(' ')[0] ?? 'Guest'}. {personal.headline}</p><p className="truncate m3-subtitle">Recently viewed: {viewedNames || 'Start browsing to build your list.'}</p><p className="truncate m3-subtitle">Recommended: {recNames || 'AI recommendations loading...'}</p></div></div>
    </header>
  )
}
