import { useMemo, useState } from 'react'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { Pagination } from '@/components/common/Pagination'
import { Seo } from '@/components/common/Seo'
import { FilterSidebar } from '@/components/product/FilterSidebar'
import { ProductCard } from '@/components/product/ProductCard'
import {
  resetFilters,
  selectFilterOptions,
  selectFilteredProducts,
  selectPaginatedProducts,
  selectPaginationMeta,
  setBrandFilters,
  setCategoryFilters,
  setCurrentPage,
  setMinRating,
  setPriceRange,
  setAvailableAtMyStore,
  setSortBy,
} from '@/features/products/productsSlice'
import { ProductSortBy } from '@/types'

const sortOptions: Array<{ label: string; value: ProductSortBy }> = [
  { label: 'Popularity', value: 'popularity' },
  { label: 'Newest', value: 'newest' },
  { label: 'Price: Low to High', value: 'price-asc' },
  { label: 'Price: High to Low', value: 'price-desc' },
  { label: 'Store availability', value: 'store-availability' },
]

const categoryNarratives: Record<string, { summary: string; promo: string; campaign: string }> = {
  Computing: {
    summary: 'Performance laptops, docking stations, and workstation gear for modern teams.',
    promo: 'Upgrade season: save up to 15% on certified business endpoints.',
    campaign: 'Bundle monitor + laptop and unlock free next-day setup.',
  },
  Audio: {
    summary: 'ANC headsets, conference audio, and studio-grade devices for hybrid work.',
    promo: 'Audio week: bonus warranty on premium headset purchases.',
    campaign: 'Buy 2+ accessories and receive free shipping worldwide.',
  },
  Home: {
    summary: 'Connected home essentials with intelligent automation and wellness features.',
    promo: 'Smart home bundle pricing for spring rollout.',
    campaign: 'Weekend deal: add 1 extra year support on eligible products.',
  },
}

/**
 * Renders the ProductListPage component.
 *
 * @returns The rendered component tree.
 */
export const ProductListPage = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const status = useAppSelector((state) => state.products.status)
  const error = useAppSelector((state) => state.products.error)
  const filters = useAppSelector((state) => state.products.filters)
  const sortBy = useAppSelector((state) => state.products.sortBy)
  const selectedStore = useAppSelector((state) => state.stores.selectedStoreDetails)
  const products = useAppSelector(selectPaginatedProducts)
  const filteredProducts = useAppSelector(selectFilteredProducts)
  const pagination = useAppSelector(selectPaginationMeta)
  const { categories, brands, highestPrice } = useAppSelector(selectFilterOptions)
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid')
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false)

  const headerLabel = useMemo(() => {
    if (pagination.totalItems === 0) {
      return 'No products match your current filters.'
    }
    return `Showing ${products.length} of ${pagination.totalItems} products`
  }, [pagination.totalItems, products.length])

  const activeCategory = filters.categories.length === 1 ? filters.categories[0] : 'All Departments'
  const categoryNarrative =
    categoryNarratives[activeCategory] ?? {
      summary: 'Explore curated inventory with AI ranking, transparent pricing, and verified availability.',
      promo: 'Limited-time promotion: use SAVE10 for featured categories.',
      campaign: 'Free shipping above $300 and extended returns on selected departments.',
    }

  const selectedFilterCount = useMemo(() => {
    const hasPriceFilter = filters.minPrice > 0 || filters.maxPrice < highestPrice
    return (
      filters.categories.length +
      filters.brands.length +
      (filters.minRating > 0 ? 1 : 0) +
      (hasPriceFilter ? 1 : 0) +
      (filters.availableAtMyStore ? 1 : 0)
    )
  }, [filters.availableAtMyStore, filters.brands.length, filters.categories.length, filters.maxPrice, filters.minPrice, filters.minRating, highestPrice])

  const promoPanels = useMemo(
    () => [
      {
        id: 'promo-main',
        title: `${activeCategory} spotlight`,
        text: selectedStore
          ? `${categoryNarrative.promo} This week at ${selectedStore.name}.`
          : categoryNarrative.promo,
        tone: 'from-cyan-500/20 to-sky-500/10',
      },
      {
        id: 'promo-campaign',
        title: 'Campaign highlight',
        text: categoryNarrative.campaign,
        tone: 'from-indigo-500/20 to-blue-500/10',
      },
      {
        id: 'promo-search',
        title: 'Search recommendation',
        text: filters.searchQuery
          ? `Showing AI-ranked results for "${filters.searchQuery}". Refine by brand and rating for tighter matching.`
          : 'Use predictive search to narrow large catalogs by keyword, specs, or SKU.',
        tone: 'from-emerald-500/20 to-teal-500/10',
      },
    ],
    [activeCategory, categoryNarrative.campaign, categoryNarrative.promo, filters.searchQuery, selectedStore],
  )

  /**
   * Executes toggle from list.
   *
   * @param list The list value.
   * @param value The value value.
   * @returns A list of matching items.
   */
  const toggleFromList = (list: string[], value: string): string[] =>
    list.includes(value) ? list.filter((item) => item !== value) : [...list, value]

  const filterSidebar = (
    <FilterSidebar
      brands={brands}
      categories={categories}
      filters={filters}
      highestPrice={highestPrice}
      selectedStoreName={selectedStore?.name ?? null}
      onBrandChange={(brand) => dispatch(setBrandFilters(toggleFromList(filters.brands, brand)))}
      onCategoryChange={(category) =>
        dispatch(setCategoryFilters(toggleFromList(filters.categories, category)))
      }
      onPriceChange={(field, value) =>
        dispatch(
          setPriceRange({
            minPrice: field === 'minPrice' ? value : filters.minPrice,
            maxPrice: field === 'maxPrice' ? value : filters.maxPrice,
          }),
        )
      }
      onRatingChange={(value) => dispatch(setMinRating(value))}
      onStoreAvailabilityChange={(enabled) => dispatch(setAvailableAtMyStore(enabled))}
      onReset={() => dispatch(resetFilters())}
    />
  )

  return (
    <div className="space-y-6">
      <Seo
        description="Browse products with advanced filters, AI search, sorting, and pagination."
        title="Product Listing"
      />

      <header className="panel p-6">
        <p className="m3-subtitle text-xs uppercase tracking-wide">Home / Products / {activeCategory}</p>
        <h1 className="m3-title">Product Listing</h1>
        <p className="m3-subtitle mt-2">{headerLabel}</p>
        <p className="m3-subtitle mt-3 text-sm">{categoryNarrative.summary}</p>
      </header>

      <section aria-label="Promotions" className="grid gap-3 md:grid-cols-3">
        {promoPanels.map((panel) => (
          <article
            className={`panel bg-gradient-to-br p-4 ${panel.tone}`}
            key={panel.id}
          >
            <p className="text-xs font-semibold uppercase tracking-wide">{panel.title}</p>
            <p className="m3-subtitle mt-2 text-sm">{panel.text}</p>
          </article>
        ))}
      </section>

      <div className="grid gap-6 lg:grid-cols-[290px_1fr]">
        <div className="hidden lg:block">{filterSidebar}</div>

        <section className="space-y-4">
          <div className="panel flex flex-col items-start justify-between gap-3 p-4 sm:flex-row sm:items-center">
            <div className="flex w-full flex-wrap items-center gap-2">
              <button
                className="m3-btn m3-btn-outlined lg:hidden"
                onClick={() => setMobileFiltersOpen(true)}
                type="button"
              >
                Filters ({selectedFilterCount})
              </button>

              <div className="m3-segment ml-auto flex gap-1">
                <button
                  aria-pressed={viewMode === 'grid'}
                  className={`m3-segment-btn ${viewMode === 'grid' ? 'm3-segment-btn-active' : ''}`}
                  onClick={() => setViewMode('grid')}
                  type="button"
                >
                  Grid
                </button>
                <button
                  aria-pressed={viewMode === 'list'}
                  className={`m3-segment-btn ${viewMode === 'list' ? 'm3-segment-btn-active' : ''}`}
                  onClick={() => setViewMode('list')}
                  type="button"
                >
                  List
                </button>
              </div>
            </div>

            <div className="flex w-full items-center gap-2 sm:w-auto">
              <p className="m3-subtitle hidden text-sm sm:block">Sort by</p>
              <select
                aria-label="Sort products"
                className="m3-select w-full sm:w-64"
                onChange={(event) => dispatch(setSortBy(event.target.value as ProductSortBy))}
                value={sortBy}
              >
                {sortOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {status === 'loading' ? (
            <div className="panel p-6 text-sm">Loading products...</div>
          ) : error ? (
            <div className="panel p-6 text-sm text-rose-600 dark:text-rose-300">{error}</div>
          ) : filteredProducts.length === 0 ? (
            <div className="panel p-8 text-center text-sm">
              No products found. Try relaxing your filters.
            </div>
          ) : (
            <div className={viewMode === 'grid' ? 'grid gap-4 sm:grid-cols-2 xl:grid-cols-3' : 'grid gap-4'}>
              {products.map((product) => (
                <ProductCard key={product.id} mode={viewMode} product={product} />
              ))}
            </div>
          )}

          <Pagination
            currentPage={pagination.currentPage}
            onPageChange={(page) => dispatch(setCurrentPage(page))}
            totalPages={pagination.totalPages}
          />
        </section>
      </div>

      {mobileFiltersOpen ? (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            aria-label="Close filters"
            className="absolute inset-0 bg-black/50"
            onClick={() => setMobileFiltersOpen(false)}
            type="button"
          />
          <div className="absolute right-0 top-0 h-full w-[88vw] max-w-sm overflow-y-auto p-4">
            {filterSidebar}
          </div>
        </div>
      ) : null}
    </div>
  )
}
