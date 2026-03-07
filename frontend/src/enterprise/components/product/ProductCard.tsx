import { Link } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { addToCart } from '@/features/cart/cartSlice'
import { selectIsWishlisted, toggleWishlist } from '@/features/wishlist/wishlistSlice'
import { getProductStockAtStore } from '@/lib/productAvailability'
import { Product } from '@/types'
import { formatCurrency } from '@/utils/currency'
import { RatingStars } from '@/components/product/RatingStars'

interface ProductCardProps {
  product: Product
  mode?: 'grid' | 'list'
}

/**
 * Renders the ProductCard component.
 *
 * @param param1 The param1 value.
 * @returns The rendered component tree.
 */
export const ProductCard = ({ product, mode = 'grid' }: ProductCardProps): JSX.Element => {
  const dispatch = useAppDispatch()
  const isWishlisted = useAppSelector((state) => selectIsWishlisted(state, product.id))
  const selectedStore = useAppSelector((state) => state.stores.selectedStoreDetails)
  const isList = mode === 'list'
  const storeStock = getProductStockAtStore(product, selectedStore?.id)
  const outOfStockAtStore = Boolean(selectedStore) && storeStock <= 0

  return (
    <article
      className={`panel overflow-hidden transition hover:-translate-y-0.5 hover:shadow-xl ${
        isList ? 'sm:flex sm:items-stretch' : 'flex h-full flex-col'
      }`}
    >
      <Link
        aria-label={`Open product details for ${product.name}`}
        className={isList ? 'sm:w-72 sm:flex-shrink-0' : 'block'}
        to={`/products/${product.id}`}
      >
        <div
          className={`group relative overflow-hidden bg-[color:var(--m3-surface-container-high)] ${
            isList ? 'h-52 sm:h-full' : 'h-52'
          }`}
        >
          <img
            alt={product.name}
            className="h-full w-full object-cover transition duration-300 group-hover:scale-105"
            loading="lazy"
            src={product.images[0]}
          />
          {product.originalPrice ? (
            <span className="m3-chip absolute left-3 top-3 border-0 bg-rose-500 px-3 py-1 font-semibold text-white">
              Sale
            </span>
          ) : null}
        </div>
      </Link>

      <div className={`p-4 ${isList ? 'sm:flex sm:flex-1 sm:flex-col sm:justify-between sm:gap-4' : 'flex flex-1 flex-col gap-4'}`}>
        <div className="space-y-3">
          <div className="flex items-center justify-between gap-3">
            <p className="m3-chip">{product.category}</p>
            <p className="text-xs" style={{ color: 'var(--m3-on-surface-variant)' }}>
              {product.brand}
            </p>
          </div>
          <p className="text-xs" style={{ color: outOfStockAtStore ? '#b91c1c' : 'var(--m3-on-surface-variant)' }}>
            {selectedStore
              ? outOfStockAtStore
                ? `Out of stock at ${selectedStore.name}`
                : `In stock at ${selectedStore.name}${storeStock <= 2 ? ` - only ${storeStock} left` : ''}`
              : 'Select a store for pickup availability'}
          </p>
          <Link className="line-clamp-2 block text-base font-semibold hover:opacity-80" to={`/products/${product.id}`}>
            {product.name}
          </Link>
          <RatingStars rating={product.rating} />
          <div className="flex items-center gap-2">
            <span className="text-lg font-bold text-slate-900 dark:text-slate-100">{formatCurrency(product.price)}</span>
            {product.originalPrice ? (
              <span className="text-sm text-slate-400 line-through">{formatCurrency(product.originalPrice)}</span>
            ) : null}
          </div>
        </div>

        <div className="mt-auto flex items-center gap-2">
          <button
            className="m3-btn m3-btn-filled flex-1 !rounded-full !px-4 !py-2 sm:flex-initial"
            disabled={outOfStockAtStore}
            onClick={() =>
              dispatch(
                addToCart({
                  product,
                  storeId: selectedStore?.id ?? null,
                  storeName: selectedStore?.name,
                }),
              )
            }
            type="button"
          >
            {outOfStockAtStore ? 'Unavailable' : 'Add to cart'}
          </button>
          <button
            aria-label={isWishlisted ? 'Remove from wishlist' : 'Add to wishlist'}
            className={`m3-btn !rounded-full !px-4 !py-2 text-sm ${
              isWishlisted
                ? 'bg-rose-100 text-rose-700 dark:bg-rose-500/20 dark:text-rose-300'
                : 'm3-btn-outlined'
            }`}
            onClick={() => dispatch(toggleWishlist(product.id))}
            type="button"
          >
            {isWishlisted ? 'Saved' : 'Wishlist'}
          </button>
        </div>
      </div>
    </article>
  )
}
