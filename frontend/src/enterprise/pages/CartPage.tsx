import { Link } from 'react-router-dom'
import { cartApi } from '@/api/cartApi'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { CartItem } from '@/components/cart/CartItem'
import { CouponInput } from '@/components/cart/CouponInput'
import { Seo } from '@/components/common/Seo'
import { applyCoupon, clearCart, removeFromCart, selectCartTotals, updateQuantity } from '@/features/cart/cartSlice'
import { pushNotification } from '@/features/notifications/notificationsSlice'
import { getProductStockAtStore } from '@/lib/productAvailability'
import { formatCurrency } from '@/utils/currency'

/**
 * Renders the CartPage component.
 *
 * @returns The rendered component tree.
 */
export const CartPage = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const items = useAppSelector((state) => state.cart.items)
  const totals = useAppSelector(selectCartTotals)
  const couponCode = useAppSelector((state) => state.cart.couponCode)
  const discountPercent = useAppSelector((state) => state.cart.discountPercent)
  const products = useAppSelector((state) => state.products.items)
  const selectedStore = useAppSelector((state) => state.stores.selectedStoreDetails)

  const unavailableAtSelectedStore = items.filter((item) => {
    const product = products.find((entry) => entry.id === item.productId)
    if (!product || !selectedStore) {
      return false
    }
    return getProductStockAtStore(product, selectedStore.id) <= 0
  })

  const fromDifferentStore = items.filter(
    (item) => selectedStore && item.storeId && item.storeId !== selectedStore.id,
  )

  /**
   * Handles adjust cart for store.
   */
  const adjustCartForStore = (): void => {
    unavailableAtSelectedStore.forEach((item) => dispatch(removeFromCart(item.productId)))
  }

  /**
   * Handles clear cart.
   */
  const onClearCart = async (): Promise<void> => {
    dispatch(clearCart())
    try {
      await cartApi.clearCart()
      dispatch(
        pushNotification({
          id: `cart-clear-${Date.now()}`,
          title: 'Cart cleared',
          description: 'Your cart was cleared successfully.',
          category: 'system',
          createdAt: new Date().toISOString(),
          read: false,
        }),
      )
    } catch (error) {
      dispatch(
        pushNotification({
          id: `cart-clear-error-${Date.now()}`,
          title: 'Cart sync warning',
          description: error instanceof Error ? error.message : 'Failed to sync cart reset with server.',
          category: 'system',
          createdAt: new Date().toISOString(),
          read: false,
        }),
      )
    }
  }

  return (
    <div className="space-y-6">
      <Seo description="Review your shopping cart and proceed to secure checkout." title="Shopping Cart" />

      <header className="panel p-6">
        <h1 className="m3-title">Shopping Cart</h1>
        <p className="m3-subtitle mt-2">Manage quantities, coupons, and pricing in real time.</p>
        {!selectedStore ? (
          <p className="mt-3 rounded-2xl p-3 text-xs" style={{ background: 'var(--m3-surface-container-high)' }}>
            No store selected. Choose a store from the header for accurate pickup stock and pricing rules.
          </p>
        ) : null}
        {selectedStore && (fromDifferentStore.length > 0 || unavailableAtSelectedStore.length > 0) ? (
          <div className="mt-3 rounded-2xl border p-3 text-xs" style={{ borderColor: 'var(--m3-outline-variant)', background: 'var(--m3-surface-container-high)' }}>
            <p className="font-semibold">Store changed to {selectedStore.name}.</p>
            <p className="m3-subtitle mt-1">
              {unavailableAtSelectedStore.length > 0
                ? `${unavailableAtSelectedStore.length} item(s) are unavailable at this store.`
                : `${fromDifferentStore.length} item(s) were added under a different store context.`}
            </p>
            <div className="mt-2 flex flex-wrap gap-2">
              <button className="m3-btn m3-btn-filled !h-9 !px-3 !py-1 text-xs" onClick={adjustCartForStore} type="button">
                Adjust cart
              </button>
              <button className="m3-btn m3-btn-outlined !h-9 !px-3 !py-1 text-xs" type="button">
                Keep current cart
              </button>
            </div>
          </div>
        ) : null}
      </header>

      {items.length === 0 ? (
        <section className="panel p-10 text-center">
          <h2 className="text-xl font-semibold">Your cart is empty</h2>
          <p className="m3-subtitle mt-2 text-sm">Add products to continue checkout.</p>
          <Link className="m3-btn m3-btn-filled mt-4" to="/products">
            Browse products
          </Link>
        </section>
      ) : (
        <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
          <section className="space-y-3" aria-label="Cart items">
            {items.map((item) => (
              <CartItem
                item={item}
                key={item.productId}
                onRemove={() => dispatch(removeFromCart(item.productId))}
                onUpdateQuantity={(quantity) => dispatch(updateQuantity({ productId: item.productId, quantity }))}
              />
            ))}
          </section>

          <aside className="panel h-fit space-y-4 p-5">
            <h2 className="text-lg font-semibold">Order Summary</h2>
            <CouponInput activeCode={couponCode} onApply={(coupon) => dispatch(applyCoupon(coupon))} />

            <dl className="space-y-2 border-t pt-3 text-sm" style={{ borderColor: 'var(--m3-outline-variant)' }}>
              <div className="flex justify-between">
                <dt>Subtotal</dt>
                <dd>{formatCurrency(totals.subtotal)}</dd>
              </div>
              <div className="flex justify-between">
                <dt>Discount {discountPercent > 0 ? `(${discountPercent}%)` : ''}</dt>
                <dd>-{formatCurrency(totals.discount)}</dd>
              </div>
              <div className="flex justify-between">
                <dt>Shipping</dt>
                <dd>{totals.shipping === 0 ? 'Free' : formatCurrency(totals.shipping)}</dd>
              </div>
              <div className="flex justify-between border-t pt-2 text-base font-bold" style={{ borderColor: 'var(--m3-outline-variant)' }}>
                <dt>Total</dt>
                <dd>{formatCurrency(totals.total)}</dd>
              </div>
            </dl>
            <p className="m3-subtitle text-xs">
              {selectedStore
                ? `Pricing and shipping rules are based on ${selectedStore.name}.`
                : 'Select a store to unlock store-specific pickup availability.'}
            </p>

            <Link
              className="m3-btn m3-btn-filled w-full"
              to="/checkout"
            >
              Proceed to Checkout
            </Link>
            <button className="m3-btn m3-btn-outlined w-full" onClick={() => void onClearCart()} type="button">
              Clear Cart
            </button>
          </aside>
        </div>
      )}
    </div>
  )
}
