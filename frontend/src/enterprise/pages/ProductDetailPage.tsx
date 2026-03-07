import { useEffect, useMemo, useState } from 'react'
import { Link, Navigate, useParams } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { productsApi } from '@/api/productsApi'
import { RecommendationRail } from '@/components/ai/RecommendationRail'
import { Seo } from '@/components/common/Seo'
import { ProductGallery } from '@/components/product/ProductGallery'
import { RatingStars } from '@/components/product/RatingStars'
import { addBulkItems } from '@/features/cart/cartSlice'
import { selectProductById, trackProductView, upsertProduct } from '@/features/products/productsSlice'
import { toggleWishlist } from '@/features/wishlist/wishlistSlice'
import { getProductStockAtStore } from '@/lib/productAvailability'
import { getStoresSortedByDistance } from '@/lib/storeLocator'
import { Product } from '@/types'
import { formatCurrency } from '@/utils/currency'
import { getRelatedProducts } from '@/utils/recommendationEngine'

const variantCatalog: Record<string, { colors: string[]; sizes: string[] }> = {
  'p-001': { colors: ['Space Gray', 'Silver', 'Midnight'], sizes: ['16GB/512GB', '32GB/1TB'] },
  'p-003': { colors: ['Slate', 'Sand', 'Black'], sizes: ['Standard'] },
  'p-006': { colors: ['Crimson', 'Onyx', 'Snow'], sizes: ['US 8', 'US 9', 'US 10', 'US 11'] },
}

const productVideos: Record<string, string[]> = {
  'p-001': ['https://www.youtube.com/embed/5qap5aO4i9A'],
  'p-003': ['https://www.youtube.com/embed/jfKfPfyJRdk'],
}

/**
 * Renders the ProductDetailPage component.
 *
 * @returns The rendered component tree.
 */
export const ProductDetailPage = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const { productId } = useParams()
  const products = useAppSelector((state) => state.products.items)
  const product = useAppSelector((state) => (productId ? selectProductById(state, productId) : undefined))
  const selectedStore = useAppSelector((state) => state.stores.selectedStoreDetails)
  const stores = useAppSelector((state) => state.stores.availableStores)
  const isWishlisted = useAppSelector((state) =>
    productId ? state.wishlist.productIds.includes(productId) : false,
  )
  const [failedProductId, setFailedProductId] = useState<string | null>(null)
  const [remoteRelatedProducts, setRemoteRelatedProducts] = useState<Product[]>([])
  const [remoteFrequentlyBought, setRemoteFrequentlyBought] = useState<Product[]>([])
  const [railsProductId, setRailsProductId] = useState<string | null>(null)

  useEffect(() => {
    if (productId) {
      dispatch(trackProductView(productId))
    }
  }, [dispatch, productId])

  useEffect(() => {
    if (!productId) {
      return
    }

    if (product) {
      return
    }

    let cancelled = false

    void productsApi
      .getProductById(productId)
      .then((item) => {
        if (cancelled) {
          return
        }
        dispatch(upsertProduct(item))
      })
      .catch(() => {
        if (cancelled) {
          return
        }
        setFailedProductId(productId)
      })

    return () => {
      cancelled = true
    }
  }, [dispatch, product, productId])

  useEffect(() => {
    if (!productId) {
      return
    }

    let cancelled = false

    void (async () => {
      const [relatedResult, frequentlyBoughtResult] = await Promise.allSettled([
        productsApi.getRelatedProducts(productId),
        productsApi.getFrequentlyBoughtTogether(productId),
      ])

      if (cancelled) {
        return
      }

      const related = relatedResult.status === 'fulfilled' ? relatedResult.value.slice(0, 3) : []
      const frequentlyBought =
        frequentlyBoughtResult.status === 'fulfilled' ? frequentlyBoughtResult.value.slice(0, 3) : []

      for (const item of [...related, ...frequentlyBought]) {
        dispatch(upsertProduct(item))
      }

      setRemoteRelatedProducts(related)
      setRemoteFrequentlyBought(frequentlyBought)
      setRailsProductId(productId)
    })()

    return () => {
      cancelled = true
    }
  }, [dispatch, productId])

  const relatedProducts = useMemo(
    () =>
      railsProductId === productId && remoteRelatedProducts.length > 0
        ? remoteRelatedProducts
        : product
          ? getRelatedProducts(products, product, 3)
          : [],
    [product, productId, products, railsProductId, remoteRelatedProducts],
  )
  const frequentlyBought = useMemo(
    () =>
      railsProductId === productId && remoteFrequentlyBought.length > 0
        ? remoteFrequentlyBought
        : product
        ? products
            .filter(
              (item) =>
                item.id !== product.id &&
                (item.category === product.category || item.brand === product.brand),
            )
            .sort((left, right) => right.popularity - left.popularity)
            .slice(0, 3)
        : [],
    [product, productId, products, railsProductId, remoteFrequentlyBought],
  )

  const variants = useMemo(
    () =>
      productId
        ? variantCatalog[productId] ?? { colors: ['Standard'], sizes: ['Default'] }
        : { colors: ['Standard'], sizes: ['Default'] },
    [productId],
  )

  const [selectedColorsByProduct, setSelectedColorsByProduct] = useState<Record<string, string>>({})
  const [selectedSizesByProduct, setSelectedSizesByProduct] = useState<Record<string, string>>({})
  const [quantitiesByProduct, setQuantitiesByProduct] = useState<Record<string, number>>({})

  if (!productId) {
    return <Navigate replace to="/products" />
  }

  const isProductLoading = Boolean(productId && !product && failedProductId !== productId)

  if (!product && isProductLoading) {
    return <div className="panel p-8 text-center text-sm">Loading product details...</div>
  }

  if (!product) {
    return (
      <div className="panel p-8 text-center text-sm">
        Product not found. <Link className="m3-link" to="/products">Return to listing</Link>
      </div>
    )
  }

  const selectionKey = productId
  const selectedColor =
    selectionKey && selectedColorsByProduct[selectionKey] && variants.colors.includes(selectedColorsByProduct[selectionKey])
      ? selectedColorsByProduct[selectionKey]
      : (variants.colors[0] ?? 'Standard')
  const selectedSize =
    selectionKey && selectedSizesByProduct[selectionKey] && variants.sizes.includes(selectedSizesByProduct[selectionKey])
      ? selectedSizesByProduct[selectionKey]
      : (variants.sizes[0] ?? 'Default')
  const quantity = selectionKey ? Math.max(1, quantitiesByProduct[selectionKey] ?? 1) : 1

  const baseStockAtSelectedStore = getProductStockAtStore(product, selectedStore?.id)
  const selectedSizeIndex = Math.max(0, variants.sizes.indexOf(selectedSize))
  const selectedColorIndex = Math.max(0, variants.colors.indexOf(selectedColor))
  const variantStockPenalty = selectedSizeIndex + Math.floor(selectedColorIndex / 2)
  const stockAtSelectedStore = Math.max(0, baseStockAtSelectedStore - variantStockPenalty)
  const outOfStockAtSelectedStore = Boolean(selectedStore) && stockAtSelectedStore <= 0
  const nearbyStoresWithStock = selectedStore
    ? getStoresSortedByDistance(stores, { latitude: selectedStore.latitude, longitude: selectedStore.longitude })
        .filter((store) => store.id !== selectedStore.id && getProductStockAtStore(product, store.id) > 0)
        .slice(0, 3)
    : []

  /**
   * Handles add selection to cart.
   */
  const addSelectionToCart = (): void => {
    if (outOfStockAtSelectedStore) {
      return
    }

    dispatch(
      addBulkItems([
        {
          productId: product.id,
          name: `${product.name} (${selectedColor}, ${selectedSize})`,
          image: product.images[0],
          price: product.price,
          quantity,
          storeId: selectedStore?.id ?? null,
          storeName: selectedStore?.name,
        },
      ]),
    )
  }

  return (
    <div className="space-y-8 pb-24 md:pb-0">
      <Seo description={product.description} title={product.name} />

      <p className="m3-subtitle text-xs uppercase tracking-wide">
        Home / Products / {product.category} / {product.name}
      </p>

      <section className="grid gap-6 lg:grid-cols-2">
        <ProductGallery
          images={product.images}
          productName={product.name}
          videos={productVideos[product.id]}
        />

        <div className="panel space-y-4 p-6">
          <p className="m3-chip w-fit text-xs font-semibold uppercase tracking-wide">
            {product.category} / {product.brand}
          </p>
          <h1 className="text-3xl font-semibold">{product.name}</h1>
          <RatingStars rating={product.rating} />

          <p className="m3-subtitle text-sm leading-relaxed">{product.description}</p>
          {product.exclusiveStoreIds?.length ? (
            <p className="m3-chip w-fit text-[10px] uppercase tracking-wide">
              Store exclusive at {product.exclusiveStoreIds.length} locations
            </p>
          ) : null}

          <div className="flex items-end gap-2">
            <p className="text-3xl font-black">{formatCurrency(product.price)}</p>
            {product.originalPrice ? (
              <p className="text-sm line-through" style={{ color: 'var(--m3-on-surface-variant)' }}>
                {formatCurrency(product.originalPrice)}
              </p>
            ) : null}
          </div>

          <div className="rounded-3xl p-4 text-sm" style={{ background: 'var(--m3-surface-container-high)' }}>
            <p className="font-semibold">Configuration</p>
            <p className="m3-subtitle mt-1">
              Stock: {product.stock} units available. Ships in 1-2 business days.
            </p>
            <p className="mt-2 text-xs font-semibold" style={{ color: outOfStockAtSelectedStore ? '#b91c1c' : 'var(--m3-on-surface)' }}>
              {selectedStore
                ? outOfStockAtSelectedStore
                  ? `Out of stock at ${selectedStore.name}`
                  : stockAtSelectedStore <= 2
                    ? `Only ${stockAtSelectedStore} left at ${selectedStore.name}`
                    : `In stock - ready for pickup at ${selectedStore.name}`
                : 'Select a store in the header to check local stock'}
            </p>
            {selectedStore && outOfStockAtSelectedStore && nearbyStoresWithStock.length > 0 ? (
              <div className="mt-2 rounded-2xl border p-2 text-xs" style={{ borderColor: 'var(--m3-outline-variant)' }}>
                <p className="font-semibold">Available nearby:</p>
                <ul className="mt-1 space-y-1">
                  {nearbyStoresWithStock.map((store) => (
                    <li key={store.id}>
                      {store.name} ({store.city}) - {getProductStockAtStore(product, store.id)} in stock
                    </li>
                  ))}
                </ul>
                <Link className="m3-link mt-2 inline-flex text-xs" state={{ from: `/products/${product.id}` }} to="/stores">
                  Check other stores
                </Link>
              </div>
            ) : null}

            <div className="mt-3">
              <p className="mb-1 text-xs font-semibold uppercase tracking-wide">Color</p>
              <div className="flex flex-wrap gap-2">
                {variants.colors.map((color) => (
                  <button
                    aria-pressed={selectedColor === color}
                    className={`m3-btn !h-9 !rounded-full !px-3 !py-1 text-xs ${
                      selectedColor === color ? 'm3-btn-filled' : 'm3-btn-outlined'
                    }`}
                    key={color}
                    onClick={() => {
                      if (!selectionKey) {
                        return
                      }
                      setSelectedColorsByProduct((current) => ({ ...current, [selectionKey]: color }))
                    }}
                    type="button"
                  >
                    {color}
                  </button>
                ))}
              </div>
            </div>

            <div className="mt-3">
              <p className="mb-1 text-xs font-semibold uppercase tracking-wide">Size / Variant</p>
              <div className="flex flex-wrap gap-2">
                {variants.sizes.map((size) => (
                  <button
                    aria-pressed={selectedSize === size}
                    className={`m3-btn !h-9 !rounded-full !px-3 !py-1 text-xs ${
                      selectedSize === size ? 'm3-btn-filled' : 'm3-btn-outlined'
                    }`}
                    key={size}
                    onClick={() => {
                      if (!selectionKey) {
                        return
                      }
                      setSelectedSizesByProduct((current) => ({ ...current, [selectionKey]: size }))
                    }}
                    type="button"
                  >
                    {size}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <ul className="grid gap-2 rounded-3xl p-4 text-sm" style={{ background: 'var(--m3-surface-container-high)' }}>
            {product.features.map((feature) => (
              <li key={feature}>- {feature}</li>
            ))}
          </ul>

          <div className="flex items-center gap-3">
            <p className="text-sm font-semibold">Quantity</p>
            <div className="inline-flex items-center rounded-full border px-2 py-1" style={{ borderColor: 'var(--m3-outline-variant)' }}>
              <button
                aria-label="Decrease quantity"
                className="m3-btn !h-8 !min-w-[32px] !rounded-full !px-0 !py-0"
                onClick={() => {
                  if (!selectionKey) {
                    return
                  }
                  setQuantitiesByProduct((current) => ({
                    ...current,
                    [selectionKey]: Math.max(1, (current[selectionKey] ?? 1) - 1),
                  }))
                }}
                type="button"
              >
                -
              </button>
              <span className="w-8 text-center text-sm font-semibold">{quantity}</span>
              <button
                aria-label="Increase quantity"
                className="m3-btn !h-8 !min-w-[32px] !rounded-full !px-0 !py-0"
                onClick={() => {
                  if (!selectionKey) {
                    return
                  }
                  setQuantitiesByProduct((current) => ({
                    ...current,
                    [selectionKey]: Math.min(20, (current[selectionKey] ?? 1) + 1),
                  }))
                }}
                type="button"
              >
                +
              </button>
            </div>
          </div>

          <div className="flex flex-wrap gap-3">
            <button
              className="m3-btn m3-btn-filled"
              disabled={outOfStockAtSelectedStore}
              onClick={addSelectionToCart}
              type="button"
            >
              {outOfStockAtSelectedStore ? 'Unavailable at selected store' : `Add ${quantity} to cart`}
            </button>
            <button
              className={`m3-btn ${
                isWishlisted
                  ? 'bg-rose-100 text-rose-700 dark:bg-rose-500/20 dark:text-rose-300'
                  : 'm3-btn-outlined'
              }`}
              onClick={() => dispatch(toggleWishlist(product.id))}
              type="button"
            >
              {isWishlisted ? 'In wishlist' : 'Add to wishlist'}
            </button>
          </div>
        </div>
      </section>

      <section aria-label="Reviews and ratings" className="panel p-6">
        <h2 className="text-xl font-semibold">Reviews & Ratings</h2>
        <div className="mt-3 flex flex-wrap items-center gap-3 rounded-2xl p-3 text-sm" style={{ background: 'var(--m3-surface-container-high)' }}>
          <p className="text-2xl font-bold">{product.rating.toFixed(1)}</p>
          <RatingStars rating={product.rating} />
          <p className="m3-subtitle">{product.reviewCount} verified reviews</p>
        </div>
        <div className="mt-4 grid gap-4">
          {product.reviews.map((review) => (
            <article className="rounded-3xl border p-4" key={review.id} style={{ borderColor: 'var(--m3-outline-variant)' }}>
              <div className="flex items-center justify-between">
                <p className="font-semibold">{review.user}</p>
                <span className="text-xs" style={{ color: 'var(--m3-on-surface-variant)' }}>
                  {new Date(review.createdAt).toLocaleDateString()}
                </span>
              </div>
              <p className="mt-2 text-amber-500">{'★'.repeat(review.rating)}</p>
              <p className="m3-subtitle mt-2 text-sm">{review.comment}</p>
            </article>
          ))}
        </div>
      </section>

      <RecommendationRail
        products={relatedProducts}
        subtitle="AI-powered related products based on shared tags, category affinity, and purchase intent."
        title="Related For You"
      />

      <RecommendationRail
        products={frequentlyBought}
        subtitle="Cross-sell suggestions frequently purchased with this item."
        title="Frequently Bought Together"
      />

      <div className="fixed inset-x-0 bottom-0 z-40 border-t p-3 backdrop-blur md:hidden" style={{ borderColor: 'var(--m3-outline-variant)', background: 'color-mix(in oklab, var(--m3-surface) 92%, transparent)' }}>
        <div className="mx-auto flex max-w-7xl items-center gap-2">
          <p className="text-sm font-semibold">{formatCurrency(product.price)}</p>
          <button className="m3-btn m3-btn-filled ml-auto !rounded-full !px-5" disabled={outOfStockAtSelectedStore} onClick={addSelectionToCart} type="button">
            {outOfStockAtSelectedStore ? 'Unavailable' : 'Add to Cart'}
          </button>
        </div>
      </div>
    </div>
  )
}
