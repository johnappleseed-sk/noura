import { ProductFilters } from '@/types'
import { formatCurrency } from '@/utils/currency'

interface FilterSidebarProps {
  filters: ProductFilters
  categories: string[]
  brands: string[]
  highestPrice: number
  selectedStoreName: string | null
  onCategoryChange: (category: string) => void
  onBrandChange: (brand: string) => void
  onPriceChange: (field: 'minPrice' | 'maxPrice', value: number) => void
  onRatingChange: (value: number) => void
  onStoreAvailabilityChange: (enabled: boolean) => void
  onReset: () => void
}

export const FilterSidebar = ({
  filters,
  categories,
  brands,
  highestPrice,
  selectedStoreName,
  onCategoryChange,
  onBrandChange,
  onPriceChange,
  onRatingChange,
  onStoreAvailabilityChange,
  onReset,
}: FilterSidebarProps): JSX.Element => (
  <aside aria-label="Product filters" className="panel h-fit space-y-6 p-5">
    <div className="flex items-center justify-between">
      <h2 className="text-lg font-semibold">Filters</h2>
      <button
        className="m3-link text-xs uppercase tracking-wide"
        onClick={onReset}
        type="button"
      >
        Reset
      </button>
    </div>

    <div>
      <h3 className="mb-2 text-sm font-semibold">Category</h3>
      <div className="space-y-2">
        {categories.map((category) => (
          <label className="flex cursor-pointer items-center gap-2 text-sm" key={category}>
            <input
              checked={filters.categories.includes(category)}
              className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
              onChange={() => onCategoryChange(category)}
              type="checkbox"
            />
            <span>{category}</span>
          </label>
        ))}
      </div>
    </div>

    <div>
      <h3 className="mb-2 text-sm font-semibold">Brand</h3>
      <div className="space-y-2">
        {brands.map((brand) => (
          <label className="flex cursor-pointer items-center gap-2 text-sm" key={brand}>
            <input
              checked={filters.brands.includes(brand)}
              className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
              onChange={() => onBrandChange(brand)}
              type="checkbox"
            />
            <span>{brand}</span>
          </label>
        ))}
      </div>
    </div>

    <div>
      <h3 className="mb-2 text-sm font-semibold">Price Range</h3>
      <div className="flex gap-2">
        <label className="flex-1">
          <span className="sr-only">Minimum price</span>
          <input
            className="m3-input !h-10 !rounded-xl !px-3"
            max={filters.maxPrice}
            min={0}
            onChange={(event) => onPriceChange('minPrice', Number(event.target.value))}
            type="number"
            value={filters.minPrice}
          />
        </label>
        <label className="flex-1">
          <span className="sr-only">Maximum price</span>
          <input
            className="m3-input !h-10 !rounded-xl !px-3"
            max={highestPrice}
            min={filters.minPrice}
            onChange={(event) => onPriceChange('maxPrice', Number(event.target.value))}
            type="number"
            value={filters.maxPrice}
          />
        </label>
      </div>
      <p className="mt-2 text-xs" style={{ color: 'var(--m3-on-surface-variant)' }}>
        {formatCurrency(filters.minPrice)} - {formatCurrency(filters.maxPrice)}
      </p>
    </div>

    <div>
      <h3 className="mb-2 text-sm font-semibold">Minimum Rating</h3>
      <select
        className="m3-select"
        onChange={(event) => onRatingChange(Number(event.target.value))}
        value={filters.minRating}
      >
        <option value={0}>All ratings</option>
        <option value={3}>3 stars & up</option>
        <option value={4}>4 stars & up</option>
        <option value={4.5}>4.5 stars & up</option>
      </select>
    </div>

    <div>
      <h3 className="mb-2 text-sm font-semibold">Store Availability</h3>
      <label className="flex cursor-pointer items-center gap-2 text-sm">
        <input
          checked={Boolean(filters.availableAtMyStore)}
          onChange={(event) => onStoreAvailabilityChange(event.target.checked)}
          type="checkbox"
        />
        <span>
          Available at my store
          {selectedStoreName ? ` (${selectedStoreName})` : ''}
        </span>
      </label>
      {!selectedStoreName ? (
        <p className="m3-subtitle mt-2 text-xs">
          Select a store in the header to enable accurate stock filtering.
        </p>
      ) : null}
    </div>
  </aside>
)
